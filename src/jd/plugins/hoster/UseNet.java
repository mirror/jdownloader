package jd.plugins.hoster;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URI;
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
import jd.plugins.components.UsenetConfigInterface;
import jd.plugins.components.UsenetConfigPanel;
import jd.plugins.components.UsenetFile;
import jd.plugins.components.UsenetFileSegment;
import jd.plugins.components.UsenetServer;
import jd.plugins.download.usenet.SimpleUseNetDownloadInterface;

import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.NullOutputStream;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.HTTPProxyException;
import org.appwork.utils.net.usenet.InvalidAuthException;
import org.appwork.utils.net.usenet.MessageBodyNotFoundException;
import org.appwork.utils.net.usenet.SimpleUseNet;
import org.appwork.utils.net.usenet.UUInputStream;
import org.appwork.utils.net.usenet.UnrecognizedCommandException;
import org.appwork.utils.net.usenet.YEncInputStream;
import org.jdownloader.plugins.config.PluginJsonConfig;

@HostPlugin(revision = "$Revision: 31032 $", interfaceVersion = 2, names = { "usenet" }, urls = { "usenet://.+" }, flags = { 0 })
public class UseNet extends PluginForHost {

    public UseNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    protected boolean isUsenetLink(DownloadLink link) {
        return link != null && "usenet".equals(link.getHost());
    }

    private UsenetConfigPanel<?> configPanel;

    @Override
    public PluginConfigPanelNG createConfigPanel() {
        if (!"usenet".equals(getHost())) {
            UsenetConfigPanel<?> panel = this.configPanel;
            if (panel == null) {
                panel = new UsenetConfigPanel(getHost(), getAvailableUsenetServer().toArray(new UsenetServer[0]), getUsenetConfig());
                this.configPanel = panel;
            }
            return panel;
        }
        return null;
    }

    protected UsenetConfigInterface getUsenetConfig() {
        return PluginJsonConfig.get(getConfigInterface());
    }

    @Override
    public Class<? extends UsenetConfigInterface> getConfigInterface() {
        if (!"usenet".equals(getHost())) {
            return UsenetConfigInterface.class;
        } else {
            return null;
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
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        /* handle premium should never be called */
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

    @Override
    public boolean hasConfig() {
        if (!"usenet".equals(getHost())) {
            return true;
        } else {
            return false;
        }
    }

    public List<UsenetServer> getAvailableUsenetServer() {
        return new ArrayList<UsenetServer>();
    }

    private final AtomicReference<SimpleUseNet> client        = new AtomicReference<SimpleUseNet>(null);
    private final String                        PRECHECK_DONE = "PRECHECK_DONE";

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
        final UsenetConfigInterface config = getUsenetConfig();
        UsenetServer server = new UsenetServer(config.getHost(), config.getPort(), config.isSSLEnabled());
        if (server == null || !getAvailableUsenetServer().contains(server)) {
            server = getAvailableUsenetServer().get(0);
            config.setHost(server.getHost());
            config.setPort(server.getPort());
            config.setSSLEnabled(server.isSSL());
        }
        final URI uri = new URI("socket://" + server.getHost() + ":" + server.getPort());
        final List<HTTPProxy> proxies = selectProxies(uri);
        final SimpleUseNet client = new SimpleUseNet(proxies.get(0), getLogger()) {
            @Override
            protected Socket createSocket() {
                return SocketConnectionFactory.createSocket(getProxy());
            }
        };
        try {
            this.client.set(client);
            client.connect(server.getHost(), server.getPort(), server.isSSL(), username, password);
            if (downloadLink.getFinalFileName() == null || downloadLink.getBooleanProperty(PRECHECK_DONE, false) == false) {
                downloadLink.setProperty(PRECHECK_DONE, true);
                final UsenetFileSegment firstSegment = usenetFile.getSegments().get(0);
                final InputStream bodyInputStream = client.requestMessageBodyAsInputStream(firstSegment.getMessageID());
                if (bodyInputStream instanceof YEncInputStream) {
                    final YEncInputStream yEnc = (YEncInputStream) bodyInputStream;
                    final int totalParts = yEnc.getPartTotal();
                    if (totalParts >= 1 && totalParts != usenetFile.getSegments().size()) {
                        logger.severe("Segments missing: " + totalParts + "!=" + usenetFile.getSegments().size());
                        setIncomplete(downloadLink, true);
                        drainInputStream(bodyInputStream);
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    final String fileName = yEnc.getName();
                    if (StringUtils.isNotEmpty(fileName) && downloadLink.getFinalFileName() == null) {
                        downloadLink.setFinalFileName(fileName);
                    }
                    final long fileSize = yEnc.getSize();
                    final long verifiedFileSize = downloadLink.getVerifiedFileSize();
                    if (fileSize >= 0 && (verifiedFileSize == -1 || fileSize > verifiedFileSize)) {
                        downloadLink.setVerifiedFileSize(fileSize);
                    }
                } else if (bodyInputStream instanceof UUInputStream) {
                    final UUInputStream uu = (UUInputStream) bodyInputStream;
                    final String fileName = uu.getName();
                    if (StringUtils.isNotEmpty(fileName) && downloadLink.getFinalFileName() == null) {
                        downloadLink.setFinalFileName(fileName);
                    }
                }
                drainInputStream(bodyInputStream);
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
            ProxyController.getInstance().reportHTTPProxyException(proxies.get(0), uri, e);
            throw e;
        } finally {
            quitClient();
        }
    }

    private void drainInputStream(final InputStream is) throws IOException {
        IO.readStreamToOutputStream(-1, is, new NullOutputStream(), false);
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

    protected List<HTTPProxy> selectProxies(URI uri) throws IOException {
        final ProxySelectorInterface selector = getProxySelector();
        if (selector == null) {
            final ArrayList<HTTPProxy> ret = new ArrayList<HTTPProxy>();
            ret.add(HTTPProxy.NONE);
            return ret;
        }
        final List<HTTPProxy> list;
        try {
            list = selector.getProxiesByURI(uri);
        } catch (Throwable e) {
            throw new NoGateWayException(selector, e);
        }
        if (list == null || list.size() == 0) {
            throw new NoGateWayException(selector, "No Gateway or Proxy Found: " + uri);
        }
        return list;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public Boolean siteTesterDisabled() {
        return true;
    }
}
