//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.gui.swing.jdgui.views.settings.panels.reconnect;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.ConfigPropertyListener;
import jd.config.Configuration;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.config.ConfigEntry.PropertyType;
import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.controlling.reconnect.BatchReconnect;
import jd.controlling.reconnect.ExternReconnect;
import jd.controlling.reconnect.ReconnectMethod;
import jd.controlling.reconnect.Reconnecter;
import jd.controlling.reconnect.plugins.ReconnectPluginController;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.interfaces.SwitchPanelEvent;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;
import jd.nrouter.IPCheck;
import jd.nutils.Formatter;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class ReconnectMethodSelector extends ConfigPanel implements ActionListener {

    private static final String JDL_PREFIX       = "jd.gui.swing.jdgui.settings.panels.reconnect.MethodSelection.";
    private static final long   serialVersionUID = 3383448498625377495L;

    public static String getIconKey() {
        return "gui.images.config.reconnect";
    }

    private static final ConfigPanel getPanelFor(final ReconnectMethod method) {
        final ConfigPanel cp = new ConfigPanel() {

            private static final long serialVersionUID = 8568369972083771808L;

            @Override
            protected ConfigContainer setupContainer() {
                return method.getConfig();
            }
        };
        cp.setBorder(null);
        cp.init();
        return cp;
    }

    public static String getTitle() {
        return JDL.L(ReconnectMethodSelector.JDL_PREFIX + "reconnect.title", "Reconnection");
    }

    private JButton             btn;

    private final Configuration configuration;

    private JTabbedPane         tabbed;

    private JLabel              currentip;

    private JLabel              success;

    private JLabel              beforeIPLabel;

    private JLabel              beforeIP;

    private JLabel              message;

    private JLabel              timeLabel;

    private JLabel              time;

    public ReconnectMethodSelector() {
        super();
        // controllistener to update gui if selected reconnect method got
        // changed externally
        JDController.getInstance().addControlListener(new ConfigPropertyListener(ReconnectMethod.PARAM_RECONNECT_TYPE) {

            @Override
            public void onPropertyChanged(final Property source, final String propertyName) {
                if (source == ReconnectMethodSelector.this.configuration) {

                    ReconnectMethodSelector.this.loadSpecial();
                }
            }

        });
        this.configuration = JDUtilities.getConfiguration();

        this.init();
    }

    public void actionPerformed(final ActionEvent e) {
        this.save();

        JDLogger.addHeader("Reconnect Testing");

        final ProgressController progress = new ProgressController(JDL.L("gui.warning.reconnect.pleaseWait", "Bitte Warten...Reconnect läuft"), 100, "gui.images.reconnect");

        this.logger.info("Start Reconnect");
        this.message.setText(JDL.L("gui.warning.reconnect.running", "running..."));
        this.message.setEnabled(true);
        this.beforeIP.setText(this.currentip.getText());
        this.beforeIP.setEnabled(true);
        this.beforeIPLabel.setEnabled(true);
        this.currentip.setText("?");
        final long timel = System.currentTimeMillis();

        final Thread timer = new Thread() {
            @Override
            public void run() {
                while (true) {
                    new GuiRunnable<Object>() {

                        @Override
                        public Object runSave() {
                            ReconnectMethodSelector.this.time.setText(Formatter.formatSeconds((System.currentTimeMillis() - timel) / 1000));
                            ReconnectMethodSelector.this.time.setEnabled(true);
                            ReconnectMethodSelector.this.timeLabel.setEnabled(true);
                            return null;
                        }

                    }.start();
                    try {
                        Thread.sleep(1000);
                    } catch (final InterruptedException e) {
                        return;
                    }
                    if (progress.isFinalizing()) {
                        break;
                    }
                }
            }
        };
        timer.start();
        final int retries = JDUtilities.getConfiguration().getIntegerProperty(ReconnectMethod.PARAM_RETRIES, 5);
        progress.setStatus(30);
        new Thread() {
            @Override
            public void run() {
                JDUtilities.getConfiguration().setProperty(ReconnectMethod.PARAM_RETRIES, 0);
                if (Reconnecter.doManualReconnect()) {
                    if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
                        progress.setStatusText(JDL.L("gui.warning.reconnectunknown", "Reconnect unknown"));
                    } else {
                        progress.setStatusText(JDL.L("gui.warning.reconnectSuccess", "Reconnect successfull"));
                    }
                    new GuiRunnable<Object>() {

                        @Override
                        public Object runSave() {
                            if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
                                ReconnectMethodSelector.this.message.setText(JDL.L("gui.warning.reconnectunknown", "Reconnect unknown"));
                            } else {
                                ReconnectMethodSelector.this.message.setText(JDL.L("gui.warning.reconnectSuccess", "Reconnect successfull"));
                            }
                            ReconnectMethodSelector.this.success.setIcon(JDTheme.II("gui.images.selected", 32, 32));
                            ReconnectMethodSelector.this.success.setEnabled(true);
                            if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
                                ReconnectMethodSelector.this.currentip.setText("?");
                            } else {
                                ReconnectMethodSelector.this.currentip.setText(IPCheck.getIPAddress());
                            }
                            return null;
                        }
                    }.start();
                } else {
                    progress.setStatusText(JDL.L("gui.warning.reconnectFailed", "Reconnect failed!"));
                    progress.setColor(Color.RED);
                    new GuiRunnable<Object>() {

                        @Override
                        public Object runSave() {
                            ReconnectMethodSelector.this.message.setText(JDL.L("gui.warning.reconnectFailed", "Reconnect failed!"));
                            ReconnectMethodSelector.this.success.setIcon(JDTheme.II("gui.images.unselected", 32, 32));
                            ReconnectMethodSelector.this.success.setEnabled(true);
                            if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
                                ReconnectMethodSelector.this.currentip.setText("?");
                            } else {
                                ReconnectMethodSelector.this.currentip.setText(IPCheck.getIPAddress());
                            }
                            return null;
                        }
                    }.start();
                }
                timer.interrupt();
                progress.setStatus(100);
                progress.doFinalize(5000);
                JDUtilities.getConfiguration().setProperty(ReconnectMethod.PARAM_RETRIES, retries);
            }
        }.start();
    }

    @Override
    public PropertyType hasChanges() {
        final PropertyType ret = this.tabbed.getSelectedIndex() != this.configuration.getIntegerProperty(ReconnectMethod.PARAM_RECONNECT_TYPE, ReconnectMethod.LIVEHEADER) ? PropertyType.NORMAL : PropertyType.NONE;
        if (this.tabbed.getSelectedComponent() instanceof ConfigPanel) {
            return PropertyType.getMax(ret, super.hasChanges(), ((ConfigPanel) this.tabbed.getSelectedComponent()).hasChanges());

        } else {
            return PropertyType.getMax(ret, super.hasChanges());

        }
    }

    @Override
    public void loadSpecial() {
        final int id = this.configuration.getIntegerProperty(ReconnectMethod.PARAM_RECONNECT_TYPE, 0);
        this.tabbed.setSelectedIndex(id);
    }

    @Override
    public void saveSpecial() {
        final int id = this.tabbed.getSelectedIndex();
        this.configuration.setProperty(ReconnectMethod.PARAM_RECONNECT_TYPE, id);
        this.configuration.save();
        if (this.tabbed.getSelectedComponent() instanceof ConfigPanel) {
            ((ConfigPanel) this.tabbed.getSelectedComponent()).save();
        }
    }

    @Override
    public void setHidden() {
        this.save();
        this.getBroadcaster().fireEvent(new SwitchPanelEvent(this, SwitchPanelEvent.ON_HIDE));
    }

    @Override
    protected ConfigContainer setupContainer() {

        this.tabbed = new JTabbedPane();
        this.tabbed.setTabPlacement(SwingConstants.TOP);

        this.tabbed.addTab(JDL.L("modules.reconnect.types.liveheader", "LiveHeader/Curl"), new SubPanelLiveHeaderReconnect(this.configuration));
        this.tabbed.addTab(JDL.L("modules.reconnect.types.extern", "Extern"), ReconnectMethodSelector.getPanelFor(new ExternReconnect()));
        this.tabbed.addTab(JDL.L("modules.reconnect.types.batch", "Batch"), ReconnectMethodSelector.getPanelFor(new BatchReconnect()));
        this.tabbed.addTab(JDL.L("modules.reconnect.types.clr", "CLR Script"), new SubPanelCLRReconnect(this.configuration));

        this.tabbed.addTab(JDL.L("modules.reconnect.types.custom", "Special"), ReconnectPluginController.getInstance().getGUI());
        final JPanel p = new JPanel(new MigLayout("ins 0, wrap 7", "[]5[fill]5[right]20[right]20[right]20[right]20[right]", "[][]"));

        p.add(this.btn = new JButton(JDL.L("gui.config.reconnect.showcase.reconnect", "Change IP")), "spany, aligny top");
        this.btn.setIcon(JDTheme.II("gui.images.config.reconnect", 16, 16));
        this.btn.addActionListener(this);

        p.add(new JPanel(), "h 32!, spany, alignx left, pushx");
        p.add(this.timeLabel = new JLabel(JDL.L("gui.config.reconnect.showcase.time", "Reconnect duration")));
        this.timeLabel.setEnabled(false);

        p.add(this.time = new JLabel("---"));
        this.time.setEnabled(false);

        p.add(new JLabel(JDL.L("gui.config.reconnect.showcase.currentip", "Your current IP")));
        p.add(this.currentip = new JLabel("---"));

        p.add(this.success = new JLabel(JDTheme.II("gui.images.selected", 32, 32)), "spany,alignx right");
        this.success.setEnabled(false);

        p.add(this.message = new JLabel(JDL.L("gui.config.reconnect.showcase.message.none", "Not tested yet")), "spanx 2");
        this.message.setEnabled(false);

        p.add(this.beforeIPLabel = new JLabel(JDL.L("gui.config.reconnect.showcase.lastip", "Ip before reconnect")));
        this.beforeIPLabel.setEnabled(false);

        p.add(this.beforeIP = new JLabel("---"));
        this.beforeIP.setEnabled(false);

        if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false) == false) {
            new Thread() {
                @Override
                public void run() {
                    final String ip = IPCheck.getIPAddress();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            ReconnectMethodSelector.this.currentip.setText(ip);
                        }
                    });
                }
            }.start();
        }

        final ConfigContainer container = new ConfigContainer();

        container.setGroup(new ConfigGroup(ReconnectMethodSelector.getTitle(), ReconnectMethodSelector.getIconKey()));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMPONENT, this.tabbed, "growy, pushy"));

        container.setGroup(new ConfigGroup(JDL.L("gui.config.reconnect.test", "Showcase"), "gui.images.reconnect_selection"));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMPONENT, p, ""));

        return container;

    }
}
