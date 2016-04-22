package org.jdownloader.plugins.components.usenet;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import org.jdownloader.gui.translate._GUI;

import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.ComboBox;
import jd.plugins.PluginConfigPanelNG;

public class UsenetConfigPanel<T extends UsenetConfigInterface> extends PluginConfigPanelNG implements ActionListener {

    private final T              cf;
    private final UsenetServer[] availableServers;
    private ComboBox<String>     cmbServer;
    private ComboBox<Integer>    cmbPort;
    private Checkbox             cbSSL;

    public UsenetConfigPanel(final String host, final UsenetServer[] availableServers, final T cf) {
        this.cf = cf;
        addStartDescription(_GUI.T.UsenetConfigPanel_description(host));
        this.availableServers = availableServers;

        final ArrayList<String> hosts = new ArrayList<String>();
        for (UsenetServer us : availableServers) {
            if (!hosts.contains(us.getHost())) {
                hosts.add(us.getHost());
            }
        }

        addPair(_GUI.T.UsenetConfigPanel_Server(), null, null, cmbServer = new ComboBox<String>(hosts.toArray(new String[] {})) {
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
                    supportedProtocols.append("SLL");
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

        addPair(_GUI.T.UsenetConfigPanel_port(), null, null, cmbPort = new ComboBox<Integer>(new Integer[] {}) {
            @Override
            protected String getLabel(int index, Integer port) {
                String host = cf.getHost();
                if (host == null) {
                    host = availableServers[0].getHost();
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
                    supportedProtocols.append("SLL");
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

        addPair(_GUI.T.UsenetConfigPanel_ssl(), null, null, cbSSL = new jd.gui.swing.jdgui.views.settings.components.Checkbox());

        cmbPort.addActionListener(this);
        cmbServer.addActionListener(this);
        cbSSL.addActionListener(this);
        cmbServer.setSelectedItem(cf.getHost());

    }

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

        updateModels();

    }

    private void updateModels() {
        String host = cf.getHost();
        if (host == null) {
            host = availableServers[0].getHost();
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
    public void reset() {
        cf.setHost(availableServers[0].getHost());
        cf.setSSLEnabled(availableServers[0].isSSL());
        cf.setPort(availableServers[0].getPort());
        updateModels();
    }

    @Override
    public void save() {
    }

    @Override
    public void updateContents() {
    }

}
