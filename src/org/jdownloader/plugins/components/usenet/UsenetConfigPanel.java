package org.jdownloader.plugins.components.usenet;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.Box;

import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.ComboBox;
import jd.plugins.Plugin;
import jd.plugins.PluginConfigPanelNG;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.config.PluginConfigPanelEventSenderListener;

public class UsenetConfigPanel extends PluginConfigPanelNG {
    @Override
    protected boolean useCustomUI(KeyHandler<?> h) {
        return true;
    }

    public static void extend(PluginConfigPanelNG panel, final String host, final List<UsenetServer> availableServers, final UsenetAccountConfigInterface cf) {
        // panel.addDescription(_GUI.T.UsenetConfigPanel_description(host));
        panel.addHeader(_GUI.T.lit_usenet_settings(), new AbstractIcon(IconKey.ICON_LOGO_NZB, 20));
        final ArrayList<String> hosts = new ArrayList<String>();
        for (UsenetServer us : availableServers) {
            if (!hosts.contains(us.getHost())) {
                hosts.add(us.getHost());
            }
        }
        final ComboBox<String> cmbServer;
        panel.addPair(_GUI.T.UsenetConfigPanel_Server(), null, cmbServer = new ComboBox<String>(hosts.toArray(new String[] {})) {
            @Override
            protected String getLabel(int index, String value) {
                boolean sslSupported = false;
                boolean nonsslSupported = false;
                for (UsenetServer us : availableServers) {
                    if (value.equals(us.getHost())) {
                        if (us.isSSL()) {
                            sslSupported = true;
                        } else {
                            nonsslSupported = true;
                        }
                    }
                }
                StringBuilder supportedProtocols = new StringBuilder();
                supportedProtocols.append(" (");
                if (sslSupported) {
                    supportedProtocols.append("SSL");
                }
                if (nonsslSupported) {
                    if (supportedProtocols.length() > 2) {
                        supportedProtocols.append(", ");
                    }
                    supportedProtocols.append("PLAIN");
                }
                supportedProtocols.append(")");
                return value + supportedProtocols.toString();
            }

            @Override
            public void setSelectedItem(Object anObject) {
                if (anObject == null) {
                    super.setSelectedItem(hosts.get(0));
                } else {
                    super.setSelectedItem(anObject);
                }
            }
        });
        cmbServer.setEnabled(hosts.size() > 1);
        final ComboBox<Integer> cmbPort;
        panel.addPair(_GUI.T.UsenetConfigPanel_port(), null, cmbPort = new ComboBox<Integer>(new Integer[] {}) {
            @Override
            protected String getLabel(int index, Integer port) {
                String host = cf.getHost();
                if (host == null) {
                    host = availableServers.get(0).getHost();
                }
                boolean sslSupported = false;
                boolean nonsslSupported = false;
                for (UsenetServer us : availableServers) {
                    if (host.equals(us.getHost()) && port == us.getPort()) {
                        if (us.isSSL()) {
                            sslSupported = true;
                        } else {
                            nonsslSupported = true;
                        }
                    }
                }
                StringBuilder supportedProtocols = new StringBuilder();
                supportedProtocols.append(" (");
                if (sslSupported) {
                    supportedProtocols.append("SSL");
                }
                if (nonsslSupported) {
                    if (supportedProtocols.length() > 2) {
                        supportedProtocols.append(", ");
                    }
                    supportedProtocols.append("PLAIN");
                }
                supportedProtocols.append(")");
                return port + supportedProtocols.toString();
            }

            @Override
            public void setSelectedItem(Object anObject) {
                if (anObject == null) {
                    super.setSelectedItem(0);
                } else {
                    super.setSelectedItem(anObject);
                }
            }
        });
        final Checkbox cbSSL;
        panel.addPair(_GUI.T.UsenetConfigPanel_ssl(), null, cbSSL = new jd.gui.swing.jdgui.views.settings.components.Checkbox());
        // IntegerKeyHandler handler = cf._getStorageHandler().getKeyHandler("Connections", IntegerKeyHandler.class);
        // panel.addHandler(cf, handler);
        ActionListener al = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == cmbServer) {
                    cf.setHost(cmbServer.getSelectedItem());
                } else if (e.getSource() == cmbPort) {
                    cf.setPort(cmbPort.getSelectedItem());
                } else if (e.getSource() == cbSSL) {
                    cf.setSSLEnabled(cbSSL.isSelected());
                    return;
                }
                updateModels(cf, availableServers, cmbServer, cmbPort, cbSSL);
            }
        };
        cmbPort.addActionListener(al);
        cmbServer.addActionListener(al);
        cbSSL.addActionListener(al);
        cmbServer.setSelectedItem(cf.getHost());
        panel.getEventSender().addListener(new PluginConfigPanelEventSenderListener() {
            @Override
            public void onConfigPanelReset(Plugin plugin, PluginConfigPanelNG pluginConfigPanelNG, Set<ConfigInterface> interfaces) {
                new EDTRunner() {
                    @Override
                    protected void runInEDT() {
                        cf.setHost(availableServers.get(0).getHost());
                        cf.setSSLEnabled(availableServers.get(0).isSSL());
                        cf.setPort(availableServers.get(0).getPort());
                        updateModels(cf, availableServers, cmbServer, cmbPort, cbSSL);
                    }
                };
            }
        });
        panel.add(Box.createGlue(), "gapbottom 5,pushx,growx,spanx" + panel.getRightGap());
    }

    protected static void updateModels(UsenetAccountConfigInterface cf, List<UsenetServer> availableServers, ComboBox<String> cmbServer, ComboBox<Integer> cmbPort, Checkbox cbSSL) {
        String host = cf.getHost();
        if (host == null) {
            host = availableServers.get(0).getHost();
        }
        int port = cf.getPort();
        ArrayList<Integer> ports = new ArrayList<Integer>();
        for (UsenetServer us : availableServers) {
            if (host.equals(us.getHost()) && !ports.contains(us.getPort())) {
                ports.add(us.getPort());
            }
        }
        if (!ports.contains(port)) {
            port = ports.get(0);
        }
        cmbPort.setModel(ports.toArray(new Integer[] {}));
        boolean sslSupported = false;
        boolean nonsslSupported = false;
        for (UsenetServer us : availableServers) {
            if (host.equals(us.getHost()) && port == us.getPort()) {
                if (us.isSSL()) {
                    sslSupported = true;
                } else {
                    nonsslSupported = true;
                }
            }
        }
        cbSSL.setEnabled(sslSupported && nonsslSupported);
        if (!sslSupported) {
            cf.setSSLEnabled(false);
        } else if (!nonsslSupported) {
            cf.setSSLEnabled(true);
        }
        cbSSL.setSelected(cf.isSSLEnabled());
        cmbPort.setSelectedItem(cf.getPort());
    }

    @Override
    public void save() {
    }

    @Override
    public void updateContents() {
    }
}
