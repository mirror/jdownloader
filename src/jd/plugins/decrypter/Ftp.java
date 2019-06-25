package jd.plugins.decrypter;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.linkcrawler.LinkCrawlerLock;
import jd.controlling.proxy.ProxyController;
import jd.http.Browser;
import jd.http.BrowserSettingsThread;
import jd.http.NoGateWayException;
import jd.http.ProxySelectorInterface;
import jd.http.SocketConnectionFactory;
import jd.nutils.SimpleFTP;
import jd.nutils.SimpleFTP.SimpleFTPListEntry;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.config.WeakHashSet;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.HTTPProxyException;
import org.jdownloader.auth.AuthenticationController;
import org.jdownloader.auth.AuthenticationInfo;
import org.jdownloader.auth.AuthenticationInfo.Type;
import org.jdownloader.auth.Login;
import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ftp" }, urls = { "ftp://.*?\\.[\\p{L}\\p{Nd}a-zA-Z0-9]{1,}(:\\d+)?/([^\"\r\n ]+|$)" })
public class Ftp extends PluginForDecrypt {
    private static Map<String, Integer>     LIMITS = new HashMap<String, Integer>();
    private static Map<String, Set<Thread>> LOCKS  = new HashMap<String, Set<Thread>>();

    public Ftp(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink cLink, ProgressController progress) throws Exception {
        final String lockHost = Browser.getHost(cLink.getCryptedUrl());
        final Set<Thread> hostLocks;
        synchronized (LOCKS) {
            Set<Thread> tmp = LOCKS.get(lockHost);
            if (tmp == null) {
                tmp = new WeakHashSet<Thread>();
                LOCKS.put(lockHost, tmp);
            }
            hostLocks = tmp;
        }
        final Thread thread = Thread.currentThread();
        while (true) {
            try {
                int limit = -1;
                while (true) {
                    synchronized (LIMITS) {
                        final Integer l = LIMITS.get(lockHost);
                        if (l == null) {
                            limit = -1;
                        } else {
                            limit = l;
                        }
                    }
                    synchronized (hostLocks) {
                        if (isAbort()) {
                            throw new InterruptedException();
                        } else if (limit == -1 || hostLocks.size() < limit) {
                            hostLocks.add(thread);
                            break;
                        } else if (hostLocks.size() > limit) {
                            hostLocks.wait(5000);
                        }
                    }
                }
                try {
                    return internalDecryptIt(cLink, progress, limit);
                } catch (WTFException e) {
                    if ("retry".equals(e.getMessage())) {
                        continue;
                    } else {
                        throw e;
                    }
                }
            } finally {
                synchronized (hostLocks) {
                    hostLocks.remove(thread);
                    hostLocks.notifyAll();
                }
            }
        }
    }

    private ArrayList<DownloadLink> internalDecryptIt(final CryptedLink cLink, ProgressController progress, final int maxFTPConnections) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>() {
            @Override
            public boolean add(final DownloadLink link) {
                if (maxFTPConnections >= 1) {
                    link.setProperty("MAX_FTP_CONNECTIONS", maxFTPConnections);
                }
                return super.add(link);
            }
        };
        final URL url = new URL(cLink.getCryptedUrl());
        final List<HTTPProxy> proxies = selectProxies(url);
        final HTTPProxy proxy = proxies.get(0);
        final SimpleFTP ftp = new SimpleFTP(proxy, logger) {
            @Override
            protected Socket createSocket() {
                return SocketConnectionFactory.createSocket(getProxy());
            }
        };
        try {
            try {
                ftp.connect(url);
            } catch (IOException e) {
                logger.log(e);
                final String message = e.getMessage();
                final Integer limit = jd.plugins.hoster.Ftp.getConnectionLimit(e);
                if (limit != null && maxFTPConnections == -1) {
                    final String lockHost = Browser.getHost(cLink.getCryptedUrl());
                    final int maxConcurrency = Math.max(1, limit);
                    synchronized (LIMITS) {
                        LIMITS.put(lockHost, maxConcurrency);
                    }
                    getCrawler().addSequentialLockObject(new LinkCrawlerLock() {
                        @Override
                        public int maxConcurrency() {
                            return 1;
                        }

                        private final String pluginID = getPluginID(Ftp.this.getLazyC());
                        private final String host     = Browser.getHost(cLink.getCryptedUrl());

                        @Override
                        public String toString() {
                            return pluginID + "|" + host + "|" + maxConcurrency;
                        }

                        @Override
                        public boolean matches(LazyCrawlerPlugin plugin, CrawledLink crawledLink) {
                            return StringUtils.equals(pluginID, getPluginID(plugin)) && StringUtils.equalsIgnoreCase(host, Browser.getHost(crawledLink.getURL()));
                        }
                    });
                    sleep(5000, cLink);
                    throw new WTFException("retry", e);
                } else if (StringUtils.contains(message, "was unable to log in with the supplied") || StringUtils.contains(message, "530 Login or Password incorrect")) {
                    final DownloadLink dummyLink = new DownloadLink(null, null, url.getHost(), cLink.getCryptedUrl(), true);
                    final Login login = requestLogins(org.jdownloader.translate._JDT.T.DirectHTTP_getBasicAuth_message(), null, dummyLink);
                    if (login != null) {
                        final String host = url.getHost();
                        int port = url.getPort();
                        if (port <= 0) {
                            port = 21;
                        }
                        try {
                            ftp.connect(host, port, login.getUsername(), login.getPassword());
                            if (false && login.isRememberSelected()) {
                                // TODO: finish me
                                final AuthenticationInfo auth = new AuthenticationInfo();
                                auth.setUsername(login.getUsername());
                                auth.setPassword(login.getPassword());
                                auth.setHostmask(login.getHost());
                                auth.setType(Type.FTP);
                                AuthenticationController.getInstance().add(auth);
                            }
                        } catch (IOException e2) {
                            if (StringUtils.contains(message, "was unable to log in with the supplied") || StringUtils.contains(message, "530 Login or Password incorrect")) {
                                throw new DecrypterException(DecrypterException.PASSWORD, e2);
                            } else {
                                throw e2;
                            }
                        }
                    } else {
                        throw new DecrypterException(DecrypterException.PASSWORD, e);
                    }
                } else {
                    throw e;
                }
            }
            final String currentDir = ftp.getDir();
            String filePath = url.getPath();
            if (!filePath.startsWith("/")) {
                filePath = "/" + filePath;
            }
            if (!filePath.startsWith(currentDir)) {
                if (currentDir.endsWith("/")) {
                    filePath = currentDir + filePath.substring(1);
                } else {
                    filePath = currentDir + filePath;
                }
            }
            final String finalFilePath = filePath;
            String nameString = filePath.substring(filePath.lastIndexOf("/") + 1);
            String nameStringUpper = nameString.toUpperCase(Locale.ENGLISH);
            final byte[] nameBytes, nameBytesUpper;
            if (StringUtils.isEmpty(nameString)) {
                nameString = null;
                nameStringUpper = null;
                nameBytes = null;
                nameBytesUpper = null;
            } else {
                nameBytes = SimpleFTP.toRawBytes(nameString);
                nameBytesUpper = SimpleFTP.toRawBytes(nameStringUpper);
            }
            filePath = filePath.substring(0, filePath.lastIndexOf("/") + 1);
            String packageName = new Regex(filePath, "/([^/]+)(/$|$)").getMatch(0);
            final String auth;
            if (!StringUtils.equals("anonymous", ftp.getUser()) || !StringUtils.equals("anonymous", ftp.getUser())) {
                auth = ftp.getUser() + ":" + ftp.getPass() + "@";
            } else {
                auth = "";
            }
            // ftp.listFeatures();
            // final List<FEATURE> features = ftp.listFeatures();
            // ftp.sendClientID("JDownloader");
            // ftp.setUTF8(true);
            final DownloadLink direct = checkLinkFile(ftp, nameString, auth, url, finalFilePath);
            if (direct == null && ftp.ascii() && ftp.cwd(filePath)) {
                SimpleFTPListEntry[] entries = ftp.listEntries();
                if (entries != null) {
                    /*
                     * logic for only adding a given file, ie. ftp://domain/directory/file.exe, you could also have subdirectory of the same
                     * name ftp.../file.exe/file.exe -raztoki
                     */
                    SimpleFTPListEntry found = null;
                    if (nameBytes != null && nameBytesUpper != null) {
                        for (final SimpleFTPListEntry entry : entries) {
                            // we compare bytes because of hex encoding
                            if (Arrays.equals(SimpleFTP.toRawBytes(entry.getName()), nameBytes)) {
                                found = entry;
                                break;
                            }
                        }
                        if (found == null) {
                            for (final SimpleFTPListEntry entry : entries) {
                                // we compare bytes because of hex encoding
                                if (Arrays.equals(SimpleFTP.toRawBytes(entry.getName()), nameBytesUpper)) {
                                    found = entry;
                                    break;
                                }
                            }
                        }
                    }
                    if (found != null) {
                        final DownloadLink linkFile = found.isLink() ? checkLinkFile(ftp, found) : null;
                        if (linkFile != null) {
                            ret.add(linkFile);
                        } else if (found.isFile()) {
                            ret.add(createDirectFile(found));
                        } else if (found.isDir()) {
                            if (ftp.cwd(found.getName())) {
                                entries = ftp.listEntries();
                            } else {
                                entries = new SimpleFTPListEntry[0];
                            }
                        }
                    }
                    /*
                     * logic for complete directory! will not walk aka recursive! - raztoki
                     */
                    if (ret.isEmpty()) {
                        /*
                         * if 'name' == file then packagename == correct, ELSE if name != file then it should be packagename! -raztoki
                         */
                        if (nameString != null) {
                            packageName = nameString;
                        }
                        for (final SimpleFTPListEntry entry : entries) {
                            final DownloadLink linkFile = entry.isLink() ? checkLinkFile(ftp, entry) : null;
                            if (linkFile != null) {
                                ret.add(linkFile);
                            } else if (entry.isFile()) {
                                ret.add(createDirectFile(entry));
                            }
                        }
                    }
                }
            } else if (direct != null) {
                ret.add(direct);
            }
            if (ret.size() > 0 && packageName != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(SimpleFTP.BestEncodingGuessingURLDecode(packageName));
                fp.setProperty(LinkCrawler.PACKAGE_ALLOW_MERGE, Boolean.TRUE);
                fp.addLinks(ret);
            }
        } catch (UnknownHostException e) {
            logger.log(e);
            ret.add(createOfflinelink(cLink.getCryptedUrl()));
            return ret;
        } catch (HTTPProxyException e) {
            ProxyController.getInstance().reportHTTPProxyException(proxy, url, e);
            throw e;
        } finally {
            ftp.disconnect();
        }
        return ret;
    }

    private DownloadLink checkLinkFile(SimpleFTP ftp, final String nameString, final String auth, final URL url, final String finalFilePath) throws IOException {
        if (nameString != null && ftp.bin()) {
            final long size = ftp.getSize(finalFilePath);
            if (size >= 0) {
                final DownloadLink link = createDownloadlink("ftpviajd://" + auth + url.getHost() + (url.getPort() != -1 ? (":" + url.getPort()) : "") + finalFilePath);
                link.setAvailable(true);
                link.setVerifiedFileSize(size);
                link.setFinalFileName(SimpleFTP.BestEncodingGuessingURLDecode(nameString));
                return link;
            }
        }
        return null;
    }

    private DownloadLink checkLinkFile(final SimpleFTP ftp, final SimpleFTPListEntry entry) throws IOException {
        if (entry != null && ftp.bin()) {
            final String path = entry.getURL().getPath();
            final long size = ftp.getSize(path);
            if (size >= 0) {
                final String url = entry.getURL().toString();
                final DownloadLink ret = createDownloadlink(url.replace("ftp://", "ftpviajd://"));
                ret.setAvailable(true);
                ret.setVerifiedFileSize(size);
                ret.setFinalFileName(SimpleFTP.BestEncodingGuessingURLDecode(entry.getName()));
                return ret;
            }
        }
        return null;
    }

    private DownloadLink createDirectFile(SimpleFTPListEntry entry) throws IOException {
        final String url = entry.getURL().toString();
        final DownloadLink ret = createDownloadlink(url.replace("ftp://", "ftpviajd://"));
        ret.setAvailable(true);
        if (entry.getSize() >= 0) {
            ret.setVerifiedFileSize(entry.getSize());
        }
        ret.setFinalFileName(SimpleFTP.BestEncodingGuessingURLDecode(entry.getName()));
        return ret;
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
        }
        return list;
    }

    @Override
    public Boolean siteTesterDisabled() {
        return true;
    }
}