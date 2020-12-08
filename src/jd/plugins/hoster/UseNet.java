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
import jd.http.Browser;
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

import org.appwork.utils.Files;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.HTTPProxyException;
import org.appwork.utils.net.usenet.InvalidAuthException;
import org.appwork.utils.net.usenet.MessageBodyNotFoundException;
import org.appwork.utils.net.usenet.SimpleUseNet;
import org.appwork.utils.net.usenet.UUInputStream;
import org.appwork.utils.net.usenet.UnrecognizedCommandException;
import org.appwork.utils.net.usenet.YEncInputStream;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.components.usenet.SimpleUseNetDownloadInterface;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetConfigPanel;
import org.jdownloader.plugins.components.usenet.UsenetFile;
import org.jdownloader.plugins.components.usenet.UsenetFileSegment;
import org.jdownloader.plugins.components.usenet.UsenetServer;
import org.jdownloader.plugins.config.AccountConfigInterface;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "usenet" }, urls = { "usenet://.+" })
public class UseNet extends antiDDoSForHost {
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

    @Override
    protected PluginConfigPanelNG createConfigPanel() {
        return new UsenetConfigPanel() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            @Override
            protected void initAccountConfig(PluginForHost plgh, Account acc, Class<? extends AccountConfigInterface> cf) {
                extend(this, getHost(), getAvailableUsenetServer(), getAccountJsonConfig(acc));
            }
        };
    }

    protected int getAutoRetryMessageNotFound() {
        return 2;
    }

    protected Account convertNNTPLoginURI(Account account) throws Exception {
        final String nntpLoginURI[] = new Regex(account.getUser(), "nntp(s)?://(.*?)(:(.*?))?@([^:/]*?)(:(\\d+))?/?(\\d+)?$").getRow(0);
        if (nntpLoginURI != null && nntpLoginURI.length == 8) {
            final boolean isSSL = "s".equals(nntpLoginURI[0]);
            final String username = nntpLoginURI[1];
            if (StringUtils.isEmpty(username)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Usenet account username is missing", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            final String password = nntpLoginURI[3];
            if (StringUtils.isAllEmpty(password, account.getPass())) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Usenet account password is missing", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            final String host = nntpLoginURI[4];
            if (StringUtils.isEmpty(host)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Usenet account host is missing", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            final String port = nntpLoginURI[6];
            final String connections = nntpLoginURI[7];
            final UsenetAccountConfigInterface config = getAccountJsonConfig(account);
            config.setSSLEnabled(isSSL);
            config.setHost(host);
            config.setPort(port != null ? Integer.parseInt(port) : -1);
            account.setMaxSimultanDownloads(connections != null ? Integer.parseInt(connections) : 1);
            account.setUser(username);
            if (StringUtils.isNotEmpty(password)) {
                account.setPass(password);
            }
        }
        return account;
    }

    protected void verifyUseNetLogins(Account account) throws Exception, InvalidAuthException {
        final UsenetServer server = getUseNetServer(account);
        final URL url = new URL(null, "socket://" + server.getHost() + ":" + server.getPort(), ProxyController.SOCKETURLSTREAMHANDLER);
        final List<HTTPProxy> proxies = selectProxies(url);
        final HTTPProxy proxy = proxies.get(0);
        final SimpleUseNet client = new SimpleUseNet(proxy, getLogger()) {
            @Override
            public int getConnectTimeout() {
                return Browser.getGlobalConnectTimeout();
            }

            @Override
            public int getReadTimeout() {
                return Browser.getGlobalReadTimeout();
            }

            @Override
            protected Socket createSocket() {
                return SocketConnectionFactory.createSocket(getProxy());
            }
        };
        try {
            client.connect(server.getHost(), server.getPort(), server.isSSL(), getUseNetUsername(account), getUseNetPassword(account));
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

    protected String getUseNetUsername(final Account account) {
        return account.getUser();
    }

    protected String getUseNetPassword(final Account account) {
        return account.getPass();
    }

    @Override
    public String getAGBLink() {
        return null;
    }

    @Override
    public String buildContainerDownloadURL(DownloadLink downloadLink, PluginForHost buildForThisPlugin) {
        if (isUsenetLink(downloadLink)) {
            return null;
        } else {
            return super.buildContainerDownloadURL(downloadLink, buildForThisPlugin);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        if (isIncomplete(parameter)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else {
            return AvailableStatus.UNCHECKABLE;
        }
    }

    protected ProxySelectorInterface getProxySelector() {
        return BrowserSettingsThread.getThreadProxySelector();
    }

    protected boolean isIncomplete(DownloadLink link) {
        return link.getBooleanProperty("incomplete", Boolean.FALSE);
    }

    protected void setIncomplete(DownloadLink link, boolean b) {
        if (b) {
            link.setProperty("incomplete", Boolean.valueOf(b));
        } else {
            link.removeProperty("incomplete");
        }
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

    private final AtomicReference<SimpleUseNet> client                  = new AtomicReference<SimpleUseNet>(null);
    private final String                        PRECHECK_DONE           = "PRECHECK_DONE";
    private final String                        LAST_MESSAGE_NOT_FOUND  = "LAST_MESSAGE_NOT_FOUND";
    private final String                        MESSAGE_NOT_FOUND_COUNT = "MESSAGE_NOT_FOUND_COUNT";
    private UsenetServer                        lastUsedUsenetServer    = null;

    protected UsenetServer getLastUsedUsenetServer() {
        return lastUsedUsenetServer;
    }

    protected UsenetServer getUseNetServer(Account account) throws Exception {
        synchronized (account) {
            final UsenetAccountConfigInterface config = getAccountJsonConfig(account);
            UsenetServer server = new UsenetServer(config.getHost(), config.getPort(), config.isSSLEnabled());
            final List<UsenetServer> serverList = getAvailableUsenetServer();
            if (server == null || !server.validate() || !serverList.contains(server)) {
                server = null;
                for (UsenetServer entry : serverList) {
                    if (entry.isSSL() == config.isSSLEnabled()) {
                        server = entry;
                        break;
                    }
                }
                if (server == null) {
                    server = getAvailableUsenetServer().get(0);
                }
                config.setHost(server.getHost());
                config.setPort(server.getPort());
                config.setSSLEnabled(server.isSSL());
            }
            return server;
        }
    }

    @Override
    public boolean isResumeable(DownloadLink link, Account account) {
        if (isUsenetLink(link)) {
            return true;
        } else {
            return super.isResumeable(link, account);
        }
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.USENET };
    }

    protected boolean setUseNetFileName(DownloadLink downloadLink, UsenetFile usenetFile, String bodyFileName) {
        boolean changedFlag = false;
        if (StringUtils.isNotEmpty(bodyFileName)) {
            final String fileExtension = Files.getExtension(usenetFile.getName());
            final String bodyExtension = Files.getExtension(bodyFileName);
            final String finalFileName;
            if (StringUtils.isNotEmpty(fileExtension) && StringUtils.isEmpty(bodyExtension)) {
                finalFileName = usenetFile.getName();
            } else if (StringUtils.isEmpty(fileExtension) && StringUtils.isNotEmpty(bodyExtension)) {
                finalFileName = bodyFileName;
            } else if (StringUtils.equalsIgnoreCase(bodyExtension, fileExtension)) {
                finalFileName = bodyFileName;
            } else {
                finalFileName = null;
            }
            if (StringUtils.isNotEmpty(finalFileName)) {
                if (downloadLink.getFinalFileName() == null) {
                    downloadLink.setFinalFileName(finalFileName);
                    changedFlag = true;
                }
                if (!StringUtils.equals(usenetFile.getName(), finalFileName)) {
                    usenetFile.setName(finalFileName);
                    changedFlag = true;
                }
            }
        }
        return changedFlag;
    }

    @Override
    public void handleMultiHost(DownloadLink downloadLink, Account account) throws Exception {
        final UsenetFile usenetFile = UsenetFile._read(downloadLink);
        if (usenetFile == null) {
            logger.severe("UsenetFile is missing!");
            setIncomplete(downloadLink, true);
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String username = getUseNetUsername(account);
        final String password = getUseNetPassword(account);
        final UsenetServer server = getUseNetServer(account);
        final URL url = new URL(null, "socket://" + server.getHost() + ":" + server.getPort(), ProxyController.SOCKETURLSTREAMHANDLER);
        final List<HTTPProxy> proxies = selectProxies(url);
        final HTTPProxy proxy = proxies.get(0);
        final SimpleUseNet client = new SimpleUseNet(proxy, getLogger()) {
            @Override
            public int getConnectTimeout() {
                return Browser.getGlobalConnectTimeout();
            }

            @Override
            public int getReadTimeout() {
                return Browser.getGlobalReadTimeout();
            }

            @Override
            protected Socket createSocket() {
                return SocketConnectionFactory.createSocket(getProxy());
            }
        };
        try {
            lastUsedUsenetServer = server;
            this.client.set(client);
            client.connect(server.getHost(), server.getPort(), server.isSSL(), username, password);
            if (downloadLink.getBooleanProperty(PRECHECK_DONE, false) == false) {
                downloadLink.setProperty(PRECHECK_DONE, true);
                boolean writeUsenetFile = false;
                try {
                    final UsenetFileSegment firstSegment = usenetFile.getSegments().get(0);
                    final InputStream bodyInputStream = client.requestMessageBodyAsInputStream(firstSegment.getMessageID());
                    try {
                        if (bodyInputStream instanceof YEncInputStream) {
                            final YEncInputStream yEncInputStream = (YEncInputStream) bodyInputStream;
                            final String yEncFileName = yEncInputStream.getName();
                            if (StringUtils.isNotEmpty(yEncFileName)) {
                                writeUsenetFile = setUseNetFileName(downloadLink, usenetFile, yEncFileName);
                            }
                            if (StringUtils.isNotEmpty(yEncInputStream.getFileCRC32())) {
                                usenetFile.setHash(new HashInfo(yEncInputStream.getFileCRC32(), HashInfo.TYPE.CRC32, true).exportAsString());
                                writeUsenetFile = true;
                            } else if (usenetFile.getHash() != null) {
                                usenetFile.setHash(null);
                                writeUsenetFile = true;
                            }
                            final int totalParts = yEncInputStream.getPartTotal();
                            final boolean trustYEncFileSize;
                            if (totalParts >= 1 && totalParts != usenetFile.getSegments().size()) {
                                logger.severe("YEnc states different number of overall segments?:" + totalParts + "!=" + usenetFile.getSegments().size());
                                if (usenetFile.getNumSegments() == usenetFile.getSegments().size()) {
                                    // Fake YEnc meta information about size/number of segments/name
                                    // NZB contains the correct meta information
                                    trustYEncFileSize = false;
                                    logger.info("Ignore it because of matching known number of segments:" + usenetFile.getSegments().size() + "==" + usenetFile.getNumSegments());
                                } else {
                                    setIncomplete(downloadLink, true);
                                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                                }
                            } else {
                                trustYEncFileSize = true;
                            }
                            final long yEncFileSize = yEncInputStream.getSize();
                            if (yEncFileSize >= 0) {
                                if (!trustYEncFileSize) {
                                    // Fake YEnc meta information about size/number of segments/name
                                    // NZB contains the correct meta information
                                    logger.info("Don't trust YEnc FileSize:" + yEncFileSize + "|VerifiedFileSize:" + downloadLink.getVerifiedFileSize() + "|UseNetFileSize:" + usenetFile.getSize());
                                } else {
                                    logger.info("Trust YEnc FileSize:" + yEncFileSize + "|VerifiedFileSize:" + downloadLink.getVerifiedFileSize() + "|UseNetFileSize:" + usenetFile.getSize());
                                    final long verifiedFileSize = downloadLink.getVerifiedFileSize();
                                    if (verifiedFileSize == -1 || yEncFileSize != verifiedFileSize) {
                                        downloadLink.setVerifiedFileSize(yEncFileSize);
                                    }
                                    if (usenetFile.getSize() != yEncFileSize) {
                                        writeUsenetFile = true;
                                        usenetFile.setSize(yEncFileSize);
                                    }
                                }
                            }
                        } else if (bodyInputStream instanceof UUInputStream) {
                            final UUInputStream uuInputStream = (UUInputStream) bodyInputStream;
                            final String uuFileName = uuInputStream.getName();
                            if (StringUtils.isNotEmpty(uuFileName)) {
                                writeUsenetFile = setUseNetFileName(downloadLink, usenetFile, uuFileName);
                            }
                        }
                    } finally {
                        drainInputStream(bodyInputStream);
                    }
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
                logger.log(e);
                final String messageID = e.getMessageID();
                final int count;
                if (StringUtils.equals(messageID, downloadLink.getStringProperty(LAST_MESSAGE_NOT_FOUND, messageID))) {
                    count = downloadLink.getIntegerProperty(MESSAGE_NOT_FOUND_COUNT, 0);
                } else {
                    count = 0;
                }
                if (count < getAutoRetryMessageNotFound()) {
                    downloadLink.setProperty(MESSAGE_NOT_FOUND_COUNT, count + 1);
                    downloadLink.setProperty(LAST_MESSAGE_NOT_FOUND, messageID);
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Segment not found!", 1 * 60 * 1000l);
                } else {
                    setIncomplete(downloadLink, true);
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, null, e);
                }
            }
        } catch (InvalidAuthException e) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, null, PluginException.VALUE_ID_PREMIUM_DISABLE, e);
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
                logger.log(e);
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
            link.removeProperty(LAST_MESSAGE_NOT_FOUND);
            link.removeProperty(MESSAGE_NOT_FOUND_COUNT);
            setIncomplete(link, false);
        }
    }

    @Override
    public Boolean siteTesterDisabled() {
        return true;
    }
}
