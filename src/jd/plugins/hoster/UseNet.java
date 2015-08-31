package jd.plugins.hoster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.BrowserSettings;
import jd.http.NoGateWayException;
import jd.http.ProxySelectorInterface;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.UsenetFile;
import jd.plugins.components.UsenetFileSegment;
import jd.plugins.download.usenet.InvalidAuthException;
import jd.plugins.download.usenet.MessageBodyNotFoundException;
import jd.plugins.download.usenet.SimpleUseNet;
import jd.plugins.download.usenet.SimpleUseNetDownloadInterface;
import jd.plugins.download.usenet.UnrecognizedCommandException;

import org.appwork.utils.Application;
import org.appwork.utils.net.httpconnection.HTTPProxy;

@HostPlugin(revision = "$Revision: 31032 $", interfaceVersion = 2, names = { "usenet" }, urls = { "usenet://.+" }, flags = { 0 })
public class UseNet extends PluginForHost {

    private final String USENET_SELECTED_PORT    = "usenet_selected_port";
    private final String USENET_SELECTED_SSLPORT = "usenet_selected_sslport";
    private final String USENET_PREFER_SSL       = "usenet_prefer_ssl";

    public UseNet(PluginWrapper wrapper) {
        super(wrapper);
        setUseNetConfigElements();
    }

    protected boolean isUsenetLink(DownloadLink link) {
        return link != null && "usenet".equals(link.getHost());
    }

    public void setUseNetConfigElements() {
        if (!"usenet".equals(getHost())) {
            final Integer[] ports = getPortSelection(getAvailablePorts());
            if (ports.length > 1) {
                getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), USENET_SELECTED_PORT, ports, "Select (Usenet)ServerPort").setDefaultValue(0));
            }
            if (supportsSSL()) {
                final Integer[] sslPorts = getPortSelection(getAvailableSSLPorts());
                if (sslPorts.length > 1 || ports.length == 0) {
                    getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), USENET_SELECTED_SSLPORT, sslPorts, "Select (Usenet)ServerPort(SSL)").setDefaultValue(0));
                }
                if (sslPorts.length > 0 && ports.length > 0) {
                    getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), USENET_PREFER_SSL, "Use (Usenet)SSL?").setDefaultValue(false));
                }
            }
        }
    }

    protected boolean supportsSSL() {
        return false && (getAvailableSSLPorts().length > 0 && Application.getJavaVersion() >= Application.JAVA17);
    }

    protected int getPort(final String ID, int[] ports) {
        final int index = getPluginConfig().getIntegerProperty(ID, 0);
        if (index >= ports.length) {
            return ports[0];
        } else {
            return ports[index];
        }
    }

    protected Integer[] getPortSelection(int[] ports) {
        final Integer[] ret = new Integer[ports.length];
        for (int i = 0; i < ports.length; i++) {
            ret[i] = ports[i];
        }
        return ret;
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
        final Thread currentThread = Thread.currentThread();
        if (currentThread != null && currentThread instanceof BrowserSettings) {
            final BrowserSettings settings = (BrowserSettings) currentThread;
            return settings.getProxySelector();
        }
        return null;
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

    protected String getServerAdress() throws Exception {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    protected int[] getAvailablePorts() {
        return new int[] { 119 };
    }

    protected int[] getAvailableSSLPorts() {
        return new int[0];
    }

    private final AtomicReference<SimpleUseNet> client = new AtomicReference<SimpleUseNet>(null);

    @Override
    public void handleMultiHost(DownloadLink downloadLink, Account account) throws Exception {
        final UsenetFile usenetFile = UsenetFile._read(downloadLink);
        if (usenetFile == null) {
            logger.info("UsenetFile is missing!");
            setIncomplete(downloadLink, true);
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final List<HTTPProxy> proxies = selectProxies();
        final SimpleUseNet client = new SimpleUseNet(proxies.get(0), getLogger());
        this.client.set(client);
        try {
            client.connect(getServerAdress(), getPort(USENET_SELECTED_PORT, getAvailablePorts()), false, account.getUser(), account.getPass());
        } catch (InvalidAuthException e) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        // checkCompleteness(downloadLink, client, usenetFile);
        dl = new SimpleUseNetDownloadInterface(client, downloadLink, usenetFile);
        try {
            dl.startDownload();
        } catch (MessageBodyNotFoundException e) {
            setIncomplete(downloadLink, true);
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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

    @Override
    public void clean() {
        try {
            try {
                final SimpleUseNet client = this.client.getAndSet(null);
                if (client != null) {
                    client.quit();
                }
            } catch (final Throwable e) {
            }
        } finally {
            super.clean();
        }
    }

    protected List<HTTPProxy> selectProxies() throws IOException {
        final ProxySelectorInterface selector = getProxySelector();
        if (selector == null) {
            final ArrayList<HTTPProxy> ret = new ArrayList<HTTPProxy>();
            ret.add(HTTPProxy.NONE);
            return ret;
        }
        final List<HTTPProxy> list;
        try {
            list = selector.getProxiesByUrl(getHost());
        } catch (Throwable e) {
            throw new NoGateWayException(selector, e);
        }
        if (list == null || list.size() == 0) {
            throw new NoGateWayException(selector, "No Gateway or Proxy Found");
        }
        return list;
    }

    @Override
    public String getHost(DownloadLink link, Account account) {
        return super.getHost(link, account);
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
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
