package jd.plugins.hoster;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import jd.PluginWrapper;
import jd.controlling.proxy.ProxyController;
import jd.http.BrowserSettingsThread;
import jd.http.NoGateWayException;
import jd.http.ProxySelectorInterface;
import jd.http.SocketConnectionFactory;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginConfigPanelNG;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.HashInfo;

import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.HTTPProxyException;
import org.appwork.utils.net.usenet.InvalidAuthException;
import org.appwork.utils.net.usenet.MessageBodyNotFoundException;
import org.appwork.utils.net.usenet.SimpleUseNet;
import org.appwork.utils.net.usenet.UUInputStream;
import org.appwork.utils.net.usenet.UnrecognizedCommandException;
import org.appwork.utils.net.usenet.YEncInputStream;
import org.jdownloader.plugins.components.usenet.SimpleUseNetDownloadInterface;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetConfigPanel;
import org.jdownloader.plugins.components.usenet.UsenetFile;
import org.jdownloader.plugins.components.usenet.UsenetFileSegment;
import org.jdownloader.plugins.components.usenet.UsenetServer;
import org.jdownloader.plugins.config.AccountConfigInterface;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "usenet" }, urls = { "usenet://.+" })
public class UseNet extends PluginForHost {
    public UseNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    protected boolean isUsenetLink(DownloadLink link) {
        return link != null && "usenet".equals(link.getHost());
    }

    @Override
    public UsenetAccountConfigInterface getAccountJsonConfig(Account acc) {
        return (UsenetAccountConfigInterface) super.getAccountJsonConfig(acc);
    }

    // @Override
    // public PluginConfigPanelNG createConfigPanel() {r
    // if (!"usenet".equals(getHost())) {
    // UsenetConfigPanel<?> panel = this.configPanel;
    // if (panel == null) {
    // panel = new UsenetConfigPanel(getHost(), getAvailableUsenetServer().toArray(new UsenetServer[0]), getUsenetConfig());
    // this.configPanel = panel;
    // }
    // return panel;
    // }
    // return null;
    // }
    @Override
    protected PluginConfigPanelNG createConfigPanel() {
        return new UsenetConfigPanel() {
            @Override
            protected void initAccountConfig(PluginForHost plgh, Account acc, Class<? extends AccountConfigInterface> cf) {
                extend(this, getHost(), getAvailableUsenetServer(), getAccountJsonConfig(acc));
            }
        };
    }

    protected void verifyUseNetLogins(Account account) throws Exception, InvalidAuthException {
        final UsenetServer server = getUsenetServer(account);
        final URL url = new URL(null, "socket://" + server.getHost() + ":" + server.getPort(), ProxyController.SOCKETURLSTREAMHANDLER);
        final List<HTTPProxy> proxies = selectProxies(url);
        final HTTPProxy proxy = proxies.get(0);
        final SimpleUseNet client = new SimpleUseNet(proxy, getLogger()) {
            @Override
            protected Socket createSocket() {
                return SocketConnectionFactory.createSocket(getProxy());
            }
        };
        try {
            client.connect(server.getHost(), server.getPort(), server.isSSL(), getUsername(account), getPassword(account));
        } catch (HTTPProxyException e) {
            ProxyController.getInstance().reportHTTPProxyException(proxy, url, e);
            throw e;
        } finally {
            try {
                if (client.isConnected()) {
                    client.quit();
                } else {
                    client.disconnect();
                }
            } catch (final IOException ignore) {
            }
        }
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if ("usenet".equals(getHost())) {
            return false;
        } else {
            if (account == null) {
                return false;
            } else {
                return super.canHandle(downloadLink, account);
            }
        }
    }

    protected String getUsername(final Account account) {
        return account.getUser();
    }

    protected String getPassword(final Account account) {
        return account.getPass();
    }

    @Override
    public String getAGBLink() {
        return null;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        if (isIncomplete(parameter)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.UNCHECKABLE;
    }

    protected ProxySelectorInterface getProxySelector() {
        return BrowserSettingsThread.getThreadProxySelector();
    }

    protected boolean isIncomplete(DownloadLink link) {
        return link.getBooleanProperty("incomplete", Boolean.FALSE);
    }

    protected void setIncomplete(DownloadLink link, boolean b) {
        link.setProperty("incomplete", Boolean.valueOf(b));
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        /* handle free should never be called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 1;
    }

    public List<UsenetServer> getAvailableUsenetServer() {
        return new ArrayList<UsenetServer>();
    }

    private final AtomicReference<SimpleUseNet> client        = new AtomicReference<SimpleUseNet>(null);
    private final String                        PRECHECK_DONE = "PRECHECK_DONE";

    protected UsenetServer getUsenetServer(Account account) throws Exception {
        final UsenetAccountConfigInterface config = getAccountJsonConfig(account);
        UsenetServer server = new UsenetServer(config.getHost(), config.getPort(), config.isSSLEnabled());
        if (server == null || !getAvailableUsenetServer().contains(server)) {
            server = getAvailableUsenetServer().get(0);
            config.setHost(server.getHost());
            config.setPort(server.getPort());
            config.setSSLEnabled(server.isSSL());
        }
        return server;
    }

    @Override
    public boolean isResumeable(DownloadLink link, Account account) {
        if (isUsenetLink(link)) {
            return true;
        }
        return super.isResumeable(link, account);
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.USENET };
    }

    @Override
    public void handleMultiHost(DownloadLink downloadLink, Account account) throws Exception {
        final UsenetFile usenetFile = UsenetFile._read(downloadLink);
        if (usenetFile == null) {
            logger.severe("UsenetFile is missing!");
            setIncomplete(downloadLink, true);
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String username = getUsername(account);
        final String password = getPassword(account);
        final UsenetServer server = getUsenetServer(account);
        final URL url = new URL(null, "socket://" + server.getHost() + ":" + server.getPort(), ProxyController.SOCKETURLSTREAMHANDLER);
        final List<HTTPProxy> proxies = selectProxies(url);
        final HTTPProxy proxy = proxies.get(0);
        final SimpleUseNet client = new SimpleUseNet(proxy, getLogger()) {
            @Override
            protected Socket createSocket() {
                return SocketConnectionFactory.createSocket(getProxy());
            }
        };
        try {
            this.client.set(client);
            client.connect(server.getHost(), server.getPort(), server.isSSL(), username, password);
            if (downloadLink.getBooleanProperty(PRECHECK_DONE, false) == false) {
                downloadLink.setProperty(PRECHECK_DONE, true);
                boolean writeUsenetFile = false;
                try {
                    final UsenetFileSegment firstSegment = usenetFile.getSegments().get(0);
                    final InputStream bodyInputStream = client.requestMessageBodyAsInputStream(firstSegment.getMessageID());
                    if (bodyInputStream instanceof YEncInputStream) {
                        final YEncInputStream yEnc = (YEncInputStream) bodyInputStream;
                        final String fileName = yEnc.getName();
                        if (StringUtils.isNotEmpty(fileName)) {
                            if (downloadLink.getFinalFileName() == null) {
                                downloadLink.setFinalFileName(fileName);
                            }
                            writeUsenetFile = true;
                            usenetFile.setName(fileName);
                        }
                        final long fileSize = yEnc.getSize();
                        final long verifiedFileSize = downloadLink.getVerifiedFileSize();
                        if (fileSize >= 0) {
                            if (verifiedFileSize == -1 || fileSize != verifiedFileSize) {
                                downloadLink.setVerifiedFileSize(fileSize);
                            }
                            writeUsenetFile = true;
                            usenetFile.setSize(fileSize);
                        }
                        drainInputStream(bodyInputStream);
                        if (StringUtils.isNotEmpty(yEnc.getFileCRC32())) {
                            usenetFile.setHash(new HashInfo(yEnc.getFileCRC32(), HashInfo.TYPE.CRC32, true).exportAsString());
                            writeUsenetFile = true;
                        } else {
                            if (usenetFile.getHash() != null) {
                                usenetFile.setHash(null);
                                writeUsenetFile = true;
                            }
                        }
                        final int totalParts = yEnc.getPartTotal();
                        if (totalParts >= 1 && totalParts != usenetFile.getSegments().size()) {
                            logger.severe("Segments missing: " + totalParts + "!=" + usenetFile.getSegments().size());
                            setIncomplete(downloadLink, true);
                            drainInputStream(bodyInputStream);
                            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        }
                    } else if (bodyInputStream instanceof UUInputStream) {
                        final UUInputStream uu = (UUInputStream) bodyInputStream;
                        final String fileName = uu.getName();
                        if (StringUtils.isNotEmpty(fileName)) {
                            if (downloadLink.getFinalFileName() == null) {
                                downloadLink.setFinalFileName(fileName);
                            }
                            writeUsenetFile = true;
                            usenetFile.setName(fileName);
                        }
                    }
                    drainInputStream(bodyInputStream);
                } finally {
                    if (writeUsenetFile) {
                        usenetFile._write(downloadLink);
                    }
                }
            }
            dl = new SimpleUseNetDownloadInterface(client, downloadLink, usenetFile);
            try {
                dl.startDownload();
            } catch (MessageBodyNotFoundException e) {
                setIncomplete(downloadLink, true);
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } catch (InvalidAuthException e) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        } catch (HTTPProxyException e) {
            ProxyController.getInstance().reportHTTPProxyException(proxy, url, e);
            throw e;
        } finally {
            quitClient();
        }
    }

    @Override
    public boolean isSpeedLimited(DownloadLink link, Account account) {
        return false;
    }

    private void drainInputStream(final InputStream is) throws IOException {
        final byte[] drainBuffer = new byte[1024];
        while (is.read(drainBuffer) != -1) {
        }
    }

    @Override
    public long getAvailableStatusTimeout(DownloadLink link, AvailableStatus availableStatus) {
        if (isUsenetLink(link)) {
            if (availableStatus != null) {
                switch (availableStatus) {
                case TRUE:
                case FALSE:
                case UNCHECKABLE:
                    return 10 * 60 * 1000l;
                default:
                    return 2 * 60 * 1000l;
                }
            } else {
                return 1 * 60 * 1000l;
            }
        } else {
            return super.getAvailableStatusTimeout(link, availableStatus);
        }
    }

    protected AvailableStatus checkCompleteness(DownloadLink link, SimpleUseNet client, UsenetFile usenetFile) throws Exception {
        if (isIncomplete(link)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final long lastAvailableStatusChange = link.getLastAvailableStatusChange();
        final long availableStatusChangeTimeout = getAvailableStatusTimeout(link, link.getAvailableStatus());
        AvailableStatus status = AvailableStatus.UNCHECKED;
        if (lastAvailableStatusChange + availableStatusChangeTimeout < System.currentTimeMillis()) {
            try {
                for (final UsenetFileSegment segment : usenetFile.getSegments()) {
                    if (!client.isMessageExisting(segment.getMessageID())) {
                        setIncomplete(link, true);
                        status = AvailableStatus.FALSE;
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                }
                status = AvailableStatus.TRUE;
            } catch (final UnrecognizedCommandException e) {
                status = AvailableStatus.UNCHECKABLE;
            } finally {
                link.setAvailableStatus(status);
            }
        }
        return status;
    }

    private void quitClient() {
        try {
            final SimpleUseNet client = this.client.getAndSet(null);
            if (client != null) {
                client.quit();
            }
        } catch (final Throwable e) {
        }
    }

    @Override
    public void clean() {
        try {
            quitClient();
        } finally {
            super.clean();
        }
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
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        if (link != null) {
            link.removeProperty(PRECHECK_DONE);
        }
    }

    @Override
    public Boolean siteTesterDisabled() {
        return true;
    }
}
