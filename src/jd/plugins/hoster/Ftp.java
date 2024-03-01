//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.HTTPProxyException;
import org.jdownloader.DomainInfo;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.linkcrawler.CheckableLink;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.proxy.ProxyController;
import jd.http.Browser;
import jd.http.BrowserSettingsThread;
import jd.http.NoGateWayException;
import jd.http.ProxySelectorInterface;
import jd.http.SocketConnectionFactory;
import jd.nutils.SimpleFTP;
import jd.nutils.SimpleFTP.SimpleFTPListEntry;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.SimpleFTPDownloadInterface;

// DEV NOTES:
// - ftp filenames can contain & characters!
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ftp" }, urls = { "ftpviajd://.*?\\.[\\p{L}\\p{Nd}a-zA-Z0-9]{1,}(:\\d+)?/([^\\?&\"\r\n ]+|$)" })
public class Ftp extends PluginForHost {
    public static Set<String> AUTH_TLS_DISABLED = new HashSet<String>();

    public Ftp(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getHost(final DownloadLink link, Account account, boolean includeSubdomain) {
        if (link != null) {
            // prefer domain via public suffic list
            return Browser.getHost(link.getDownloadURL(), includeSubdomain);
        } else if (account != null) {
            return account.getHoster();
        } else {
            return null;
        }
    }

    @Override
    public CrawledLink convert(DownloadLink link) {
        final String url = link.getPluginPatternMatcher();
        link.setPluginPatternMatcher(url.replace("ftpviajd://", "ftp://"));
        link.setLinkID(null);
        return super.convert(link);
    }

    @Override
    public boolean isSpeedLimited(DownloadLink link, Account account) {
        return false;
    }

    private void connect(SimpleFTP ftp, final Account account, final DownloadLink downloadLink, URL url) throws Exception {
        try {
            ftp.connect(url);
        } catch (IOException e) {
            Integer limit = ftp.getConnectionLimitByException(e);
            if (limit != null) {
                limit = setMaxRunning(account, downloadLink, limit);
                if (limit > 0) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Connection limit reached:" + limit, 60 * 1000l, e);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Connection limit reached", 5 * 60 * 1000l, e);
                }
            } else {
                throw e;
            }
        }
    }

    private static WeakHashMap<DomainInfo, AtomicInteger> freeRunning = new WeakHashMap<DomainInfo, AtomicInteger>();
    private static WeakHashMap<DomainInfo, Integer>       maxRunning  = new WeakHashMap<DomainInfo, Integer>();

    protected int setMaxRunning(final Account account, final DownloadLink link, final Integer limit) {
        final DomainInfo domainInfo = link.getDomainInfo();
        synchronized (maxRunning) {
            if (limit == null) {
                link.removeProperty(MAX_FTP_CONNECTIONS);
                maxRunning.remove(domainInfo);
                return -1;
            } else if (limit > 0) {
                link.setProperty(MAX_FTP_CONNECTIONS, limit);
                maxRunning.put(domainInfo, limit);
                logger.info("setMaxRunning:" + domainInfo + "=" + limit);
                return limit;
            } else {
                final int running = getFreeRunning(account, link).get();
                if (running > 0) {
                    maxRunning.put(domainInfo, running);
                    logger.info("setMaxRunning:" + domainInfo + "~" + running);
                    return running;
                } else {
                    return -1;
                }
            }
        }
    }

    protected int getMaxRunning(final Account account, final DownloadLink link) {
        final boolean logging = Thread.currentThread().equals(link.getDownloadLinkController());
        final DomainInfo domainInfo = link.getDomainInfo();
        synchronized (maxRunning) {
            Integer ret = maxRunning.get(domainInfo);
            if (ret == null) {
                ret = link.getIntegerProperty(MAX_FTP_CONNECTIONS, -1);
                if (logging) {
                    logger.info("getMaxRunning:" + domainInfo + ":link:" + ret);
                }
            } else if (logging) {
                logger.info("getMaxRunning:" + domainInfo + ":map:" + ret);
            }
            if (ret != null && ret.intValue() > 0) {
                return ret.intValue();
            } else {
                return -1;
            }
        }
    }

    protected AtomicInteger getFreeRunning(final Account account, final DownloadLink link) {
        final DomainInfo domainInfo = link.getDomainInfo();
        synchronized (freeRunning) {
            AtomicInteger ret = freeRunning.get(domainInfo);
            if (ret == null) {
                ret = new AtomicInteger(0);
                freeRunning.put(domainInfo, ret);
            }
            return ret;
        }
    }

    @Override
    protected int getMaxSimultanDownload(DownloadLink link, Account account) {
        if (link != null) {
            int max = getMaxRunning(account, link);
            if (max <= 0) {
                max = getMaxSimultanFreeDownloadNum();
            }
            final int running = getFreeRunning(account, link).get();
            final int ret = Math.min(running + 1, max);
            return ret;
        } else {
            return super.getMaxSimultanDownload(link, account);
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    protected void controlMaxFreeDownloads(final Account account, final DownloadLink link, final int num) {
        final AtomicInteger freeRunning = getFreeRunning(account, link);
        synchronized (freeRunning) {
            final int before = freeRunning.get();
            final int after = before + num;
            freeRunning.set(after);
            logger.info("freeRunning(" + link.getDomainInfo() + ")|max:" + getMaxSimultanDownload(link, account) + "|before:" + before + "|after:" + after + "|num:" + num);
        }
    }

    protected SimpleFTP createSimpleFTP(final URL url) throws IOException {
        final List<HTTPProxy> proxies = selectProxies(url);
        final HTTPProxy proxy = proxies.get(0);
        return new SimpleFTP(proxy, logger) {
            @Override
            public int getConnectTimeout() {
                return Browser.getGlobalConnectTimeout();
            }

            @Override
            public int getReadTimeout(STATE state) {
                switch (state) {
                case CLOSING:
                    return super.getReadTimeout(state);
                default:
                    return Browser.getGlobalReadTimeout();
                }
            }

            @Override
            protected boolean AUTH_TLS_CC() throws IOException {
                final Set<String> set = Ftp.AUTH_TLS_DISABLED;
                final String host = url.getHost().toLowerCase(Locale.ENGLISH);
                synchronized (set) {
                    if (set.contains(host)) {
                        return false;
                    }
                }
                final boolean ret = super.AUTH_TLS_CC();
                if (!ret) {
                    synchronized (set) {
                        set.add(host);
                    }
                }
                return ret;
            }

            @Override
            protected Socket createSocket() {
                return SocketConnectionFactory.createSocket(getProxy());
            }
        };
    }

    public void download(String downloadUrl, final Account account, final DownloadLink downloadLink, boolean throwException) throws Exception {
        final URL url = new URL(downloadUrl);
        final SimpleFTP ftp = createSimpleFTP(url);
        try {
            /* cut off all ?xyz at the end */
            final String filePath = new Regex(downloadUrl, "://[^/]+/(.+?)(\\?|$)").getMatch(0);
            connect(ftp, account, downloadLink, url);
            final String downloadFilePath = checkFile(ftp, downloadLink, filePath);
            dl = new SimpleFTPDownloadInterface(ftp, downloadLink, downloadFilePath);
            try {
                /* add a download slot */
                controlMaxFreeDownloads(account, downloadLink, +1);
                dl.startDownload();
            } finally {
                /* add a download slot */
                controlMaxFreeDownloads(account, downloadLink, -1);
            }
        } catch (HTTPProxyException e) {
            ProxyController.getInstance().reportHTTPProxyException(ftp.getProxy(), url, e);
            throw e;
        } catch (IOException e) {
            if (ftp.isWrongLoginException(e)) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Login incorrect", e);
            } else {
                throw e;
            }
        } finally {
            try {
                ftp.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.GENERIC };
    }

    protected ProxySelectorInterface getProxySelector() {
        return BrowserSettingsThread.getThreadProxySelector();
    }

    protected List<HTTPProxy> selectProxies(URL url) throws IOException {
        final ProxySelectorInterface selector = getProxySelector();
        if (selector == null) {
            final ArrayList<HTTPProxy> ret = new ArrayList<HTTPProxy>();
            ret.add(HTTPProxy.NONE);
            return ret;
        }
        final List<HTTPProxy> list;
        try {
            list = selector.getProxiesByURL(url);
        } catch (Throwable e) {
            throw new NoGateWayException(selector, e);
        }
        if (list == null || list.size() == 0) {
            throw new NoGateWayException(selector, "No Gateway or Proxy Found: " + url);
        } else {
            return list;
        }
    }

    private String buildFilePath(final String currentDirectory, final String filePath) {
        final String ret;
        if (!filePath.startsWith("/")) {
            ret = "/" + filePath;
        } else {
            ret = filePath;
        }
        if (currentDirectory != null && !ret.startsWith(currentDirectory)) {
            if (currentDirectory.endsWith("/")) {
                return currentDirectory + ret.substring(1);
            } else {
                return currentDirectory + ret;
            }
        } else {
            return ret;
        }
    }

    private String checkFile(SimpleFTP ftp, DownloadLink downloadLink, String filePath) throws Exception {
        final String currentDir = ftp.getDir();
        /* switch binary mode */
        ftp.bin();
        /*
         * some servers do not allow to list the folder, so this may fail but file still might be online
         */
        filePath = buildFilePath(currentDir, filePath);
        long size = ftp.getSize(filePath);
        String name = null;
        if (size == -1) {
            if (ftp.wasLatestOperationNotPermitted()) {
                final SimpleFTPListEntry fileInfo = ftp.getFileInfo(filePath);
                if (fileInfo != null) {
                    size = fileInfo.getSize();
                    name = SimpleFTP.BestEncodingGuessingURLDecode(name);
                }
            } else {
                /* some server need / at the beginning */
                filePath = "/" + filePath;
                size = ftp.getSize(filePath);
            }
        }
        if (size != -1) {
            if (name == null) {
                /* cut off all ?xyz at the end */
                name = new Regex(filePath, ".*/(.+?)(\\?|$)").getMatch(0);
                if (name != null) {
                    name = SimpleFTP.BestEncodingGuessingURLDecode(name);
                } else {
                    logger.severe("could not get filename from ftpurl");
                    name = downloadLink.getName();
                }
            }
        }
        if (name == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (downloadLink.getFinalFileName() == null) {
            downloadLink.setFinalFileName(name);
        }
        if (size >= 0 && downloadLink.getVerifiedFileSize() < 0) {
            downloadLink.setVerifiedFileSize(size);
        }
        return filePath;
    }

    @Override
    public String getAGBLink() {
        return "http://jdownloader.org";
    }

    public static final String MAX_FTP_CONNECTIONS = "MAX_FTP_CONNECTIONS";

    @Override
    protected boolean supportsUpdateDownloadLink(CheckableLink checkableLink) {
        return checkableLink != null && checkableLink.getDownloadLink() != null;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        download(downloadLink.getDownloadURL(), null, downloadLink, false);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        final URL url = new URL(downloadLink.getDownloadURL());
        final SimpleFTP ftp = createSimpleFTP(url);
        try {
            /* cut off all ?xyz at the end */
            final String filePath = new Regex(downloadLink.getDownloadURL(), "://[^/]+/(.+?)(\\?|$)").getMatch(0);
            connect(ftp, null, downloadLink, url);
            checkFile(ftp, downloadLink, filePath);
            return AvailableStatus.TRUE;
        } catch (HTTPProxyException e) {
            ProxyController.getInstance().reportHTTPProxyException(ftp.getProxy(), url, e);
            throw e;
        } catch (ConnectException e) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, null, e);
        } catch (UnknownHostException e) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, null, e);
        } catch (IOException e) {
            if (ftp.isWrongLoginException(e)) {
                logger.log(e);
                downloadLink.getLinkStatus().setErrorMessage("Login incorrect");
                return AvailableStatus.UNCHECKABLE;
            } else {
                throw e;
            }
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                ftp.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        link.setProperty("RESUME", true);
        link.removeProperty(MAX_FTP_CONNECTIONS);
        setMaxRunning(null, link, null);
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public Boolean siteTesterDisabled() {
        return Boolean.TRUE;
    }

    public static String createURLForThisPlugin(final String url) {
        return url == null ? null : url.replaceFirst("^(?i)ftp://", "ftpviajd://");
    }
}