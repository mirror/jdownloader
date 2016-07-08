package jd.plugins.hoster;

import java.awt.Color;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.SpinnerNumberModel;

import jd.PluginWrapper;
import jd.controlling.proxy.ProxyController;
import jd.http.Browser;
import jd.http.SocketConnectionFactory;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DefaultEditAccountPanel;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.swing.components.ExtCheckBox;
import org.appwork.swing.components.ExtSpinner;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.HTTPProxyException;
import org.appwork.utils.net.usenet.InvalidAuthException;
import org.appwork.utils.net.usenet.SimpleUseNet;
import org.jdownloader.gui.InputChangedCallbackInterface;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.accounts.AccountBuilderInterface;
import org.jdownloader.plugins.components.usenet.UsenetConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetServer;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

@HostPlugin(revision = "$Revision: 31032 $", interfaceVersion = 2, names = { "genericusenet" }, urls = { "" }, flags = { 0 })
public class GenericUseNet extends UseNet {

    public GenericUseNet(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium();
    }

    private final String USENET_PORT = "USENET_PORT";
    private final String USENET_SSL  = "USENET_SSL";
    private final String USENET_HOST = "USENET_HOST";

    @Override
    public Class<? extends UsenetConfigInterface> getConfigInterface() {
        return null;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        if ("genericusenet".equals(link.getHost())) {
            final LazyHostPlugin usenetPlugin = HostPluginController.getInstance().get("usenet");
            usenetPlugin.getPrototype(null).assignPlugin(link);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        if ("genericusenet".equals(link.getHost())) {
            final LazyHostPlugin usenetPlugin = HostPluginController.getInstance().get("usenet");
            usenetPlugin.getPrototype(null).assignPlugin(link);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
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
            final AccountInfo ai = new AccountInfo();
            ai.setProperty("multiHostSupport", Arrays.asList(new String[] { "usenet" }));
            ai.setStatus("Generic usenet:maxDownloads(current)=" + account.getMaxSimultanDownloads());
            return ai;
        } catch (InvalidAuthException e) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
    public AccountBuilderInterface getAccountFactory(final InputChangedCallbackInterface callback) {
        return new DefaultEditAccountPanel(callback) {
            private final ExtTextField host;
            private final ExtCheckBox  ssl;
            private final ExtSpinner   port;
            private final ExtSpinner   connections;

            {
                JLabel label = null;
                add(label = new JLabel("This is a generic usenet plugin!"));
                label.setForeground(Color.RED);
                add(label = new JLabel("Please contact us via support@jdownloader.org!"));
                label.setForeground(Color.RED);

                add(new JLabel(_GUI.T.UsenetConfigPanel_Server()));
                add(host = new ExtTextField() {
                    @Override
                    public void onChanged() {
                        callback.onChangedInput(host);
                    }
                });
                host.setHelpText(_GUI.T.jd_gui_userio_defaulttitle_input());

                add(new JLabel(_GUI.T.UsenetConfigPanel_ssl()));
                add(ssl = new ExtCheckBox());

                add(new JLabel(_GUI.T.UsenetConfigPanel_port()));
                add(port = new ExtSpinner(new SpinnerNumberModel(119, 80, 65535, 1)));

                add(new JLabel(_GUI.T.PackagizerFilterRuleDialog_layoutDialogContent_chunks()));
                add(connections = new ExtSpinner(new SpinnerNumberModel(1, 1, 100, 1)));
            }

            @Override
            public void setAccount(Account defaultAccount) {
                super.setAccount(defaultAccount);
                if (defaultAccount != null) {
                    ssl.setSelected(defaultAccount.getBooleanProperty(USENET_SSL, false));
                    host.setText(defaultAccount.getStringProperty(USENET_HOST, null));
                    port.setValue(defaultAccount.getIntegerProperty(USENET_PORT, ssl.isSelected() ? 563 : 119));
                    connections.setValue(Math.max(1, defaultAccount.getMaxSimultanDownloads()));
                }
            }

            @Override
            public boolean validateInputs() {
                final String host = this.host.getText();
                return StringUtils.isNotEmpty(host);
            }

            @Override
            public boolean updateAccount(Account input, Account output) {
                super.updateAccount(input, output);
                output.setProperty(USENET_SSL, input.getBooleanProperty(USENET_SSL, false));
                output.setProperty(USENET_HOST, input.getStringProperty(USENET_HOST, null));
                output.setProperty(USENET_PORT, input.getIntegerProperty(USENET_PORT, ssl.isSelected() ? 563 : 119));
                output.setMaxSimultanDownloads(input.getMaxSimultanDownloads());
                return true;
            }

            @Override
            public Account getAccount() {
                final Account acc = super.getAccount();
                acc.setProperty(USENET_SSL, ssl.isSelected());
                acc.setProperty(USENET_HOST, host.getText().trim());
                acc.setProperty(USENET_PORT, port.getValue());
                acc.setMaxSimultanDownloads(Math.max(1, (Integer) connections.getValue()));
                return acc;
            }
        };
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.GENERIC, FEATURE.USENET };
    }

    @Override
    protected UsenetServer getUsenetServer(Account account) throws Exception {
        final boolean ssl = account.getBooleanProperty(USENET_SSL, false);
        final int port = account.getIntegerProperty(USENET_PORT, ssl ? 563 : 119);
        final String host = account.getStringProperty(USENET_HOST, null);
        if (host == null) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        final UsenetServer server = new UsenetServer(host, port, ssl);
        return server;
    }

    @Override
    public String getHost(final DownloadLink link, Account account) {
        if (account != null) {
            final String host = account.getStringProperty(USENET_HOST, null);
            if (host != null) {
                return Browser.getHost(host, true);
            }
        }
        return super.getHost(link, account);
    }
}
