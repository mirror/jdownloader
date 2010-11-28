package jd.controlling.reconnect;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.controlling.reconnect.ipcheck.IP;
import jd.controlling.reconnect.ipcheck.IPController;
import jd.gui.swing.Factory;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.nutils.Formatter;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.StorageEvent;
import org.appwork.storage.StorageValueChangeEvent;
import org.appwork.utils.event.DefaultEventListener;
import org.appwork.utils.swing.EDTRunner;

public class ReconnectPluginConfigGUI extends SwitchPanel implements ActionListener, DefaultEventListener<StorageEvent<?>> {

    private static final long                     serialVersionUID = 1L;
    private static final ReconnectPluginConfigGUI INSTANCE         = new ReconnectPluginConfigGUI();

    public static ReconnectPluginConfigGUI getInstance() {
        return ReconnectPluginConfigGUI.INSTANCE;
    }

    private JComboBox             combobox;
    private JButton               btnTest;
    private JLabel                lblDuration;
    private JLabel                lblTime;
    private JLabel                lblCurrentIP;
    private JLabel                lblSuccessIcon;
    private JLabel                lblStatusMessage;
    private JLabel                lblBeforeIpLabel;
    private JLabel                lblBeforeIP;
    private JScrollPane           scrollPane;
    private JButton               autoButton;

    protected static final Logger LOG = JDLogger.getLogger();

    private ReconnectPluginConfigGUI() {
        super(new MigLayout("ins 15 15 5 5,wrap 1", "[grow,fill]", "[][][grow,fill][][][]"));
        this.initGUI();
        ReconnectPluginController.getInstance().getStorage().getEventSender().addListener(this);
    }

    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == this.btnTest) {
            this.testReconnect();
        } else if (e.getSource() == this.autoButton) {
            ReconnectPluginController.getInstance().autoFind();
        } else {
            ReconnectPluginController.getInstance().setActivePlugin((RouterPlugin) this.combobox.getSelectedItem());
        }
    }

    private void initGUI() {

        this.combobox = new JComboBox(ReconnectPluginController.getInstance().getPlugins().toArray(new RouterPlugin[] {}));
        this.scrollPane = new JScrollPane();
        this.autoButton = new JButton("Reconnect Wizard");
        this.autoButton.addActionListener(this);
        this.add(Factory.createHeader("Reconnection", JDTheme.II("gui.images.config.reconnect", 32, 32)), "spanx");
        this.add(new JLabel(JDL.L("jd.controlling.reconnect.plugins.ReconnectPluginConfigGUI.initGUI.comboboxlabel", "Reconnect Method:")), "split 3,shrinkx,gapleft 37");
        this.add(this.combobox, "growx, pushx");
        this.add(this.autoButton);
        this.add(this.scrollPane, "gapleft 37");

        this.combobox.addActionListener(this);
        this.combobox.setSelectedItem(ReconnectPluginController.getInstance().getActivePlugin());
        // we have to set view here, because this.combobox.setSelectedItem will
        // not throw an changeevent (because nothign changed ;-P)
        ReconnectPluginConfigGUI.this.scrollPane.getViewport().setView(((RouterPlugin) ReconnectPluginConfigGUI.this.combobox.getSelectedItem()).getGUI());
        this.add(Box.createGlue(), "pushy,growy");
        this.add(Factory.createHeader(JDL.L("gui.config.reconnect.test", "Showcase"), JDTheme.II("gui.images.reconnect_selection", 32, 32)), "spanx");

        final JPanel p = new JPanel(new MigLayout("ins 0, wrap 6", "[]5[right]20[right]20[right]20[right]20[right]", "[][]"));

        p.add(this.btnTest = new JButton(JDL.L("gui.config.reconnect.showcase.reconnect", "Change IP")), "spanx,shrinkx, aligny top,wrap");
        this.btnTest.setIcon(JDTheme.II("gui.images.config.reconnect", 16, 16));
        this.btnTest.addActionListener(this);

        p.add(this.lblDuration = new JLabel(JDL.L("gui.config.reconnect.showcase.time", "Reconnect duration")));
        this.lblDuration.setEnabled(false);

        p.add(this.lblTime = new JLabel("---"));
        this.lblTime.setEnabled(false);

        p.add(new JLabel(JDL.L("gui.config.reconnect.showcase.currentip", "Your current IP")));
        p.add(this.lblCurrentIP = new JLabel("---"));
        p.add(Box.createGlue(), "spany,pushx,growx");
        p.add(this.lblSuccessIcon = new JLabel(JDTheme.II("gui.images.selected", 32, 32)), "spany,alignx right");
        this.lblSuccessIcon.setEnabled(false);
        this.lblSuccessIcon.setHorizontalTextPosition(SwingConstants.LEFT);

        p.add(this.lblStatusMessage = new JLabel(JDL.L("gui.config.reconnect.showcase.message.none", "Not tested yet")), "spanx 2");
        this.lblStatusMessage.setEnabled(false);

        p.add(this.lblBeforeIpLabel = new JLabel(JDL.L("gui.config.reconnect.showcase.lastip", "Ip before reconnect")));
        this.lblBeforeIpLabel.setEnabled(false);

        p.add(this.lblBeforeIP = new JLabel("---"));
        this.lblBeforeIP.setEnabled(false);

        this.add(p, "gapleft 37");
    }

    /**
     * Update GUI
     */
    public void onEvent(final StorageEvent<?> event) {
        if (event instanceof StorageValueChangeEvent<?>) {
            final StorageValueChangeEvent<?> changeEvent = (StorageValueChangeEvent<?>) event;
            if (changeEvent.getKey().equals(ReconnectPluginController.PRO_ACTIVEPLUGIN)) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        ReconnectPluginConfigGUI.this.combobox.setSelectedItem(ReconnectPluginController.getInstance().getActivePlugin());
                        ReconnectPluginConfigGUI.this.scrollPane.getViewport().setView(((RouterPlugin) ReconnectPluginConfigGUI.this.combobox.getSelectedItem()).getGUI());
                    }

                };
            }
        }
    }

    @Override
    protected void onHide() {
    }

    @Override
    protected void onShow() {
        new Thread() {
            @Override
            public void run() {
                final IP ip = IPController.getInstance().getIP();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        ReconnectPluginConfigGUI.this.lblCurrentIP.setText(ip.toString());
                    }
                });
            }
        }.start();
    }

    private void testReconnect() {
        JDLogger.addHeader("Reconnect Testing");

        final ProgressController progress = new ProgressController(JDL.L("gui.warning.reconnect.pleaseWait", "Bitte Warten...Reconnect läuft"), 100, "gui.images.reconnect");

        ReconnectPluginConfigGUI.LOG.info("Start Reconnect");
        this.lblStatusMessage.setText(JDL.L("gui.warning.reconnect.running", "running..."));
        this.lblStatusMessage.setEnabled(true);
        this.lblBeforeIP.setText(IPController.getInstance().fetchIP().toString());

        this.lblBeforeIP.setEnabled(true);
        this.lblBeforeIpLabel.setEnabled(true);
        this.lblCurrentIP.setText("?");
        final long timel = System.currentTimeMillis();

        final Thread timer = new Thread() {
            @Override
            public void run() {
                while (true) {
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            ReconnectPluginConfigGUI.this.lblTime.setText(Formatter.formatSeconds((System.currentTimeMillis() - timel) / 1000));
                            ReconnectPluginConfigGUI.this.lblTime.setEnabled(true);
                            ReconnectPluginConfigGUI.this.lblDuration.setEnabled(true);
                        }

                    };
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
        final int retries = JDUtilities.getConfiguration().getIntegerProperty(Configuration.PARAM_RETRIES, 5);
        progress.setStatus(30);
        new Thread() {
            @Override
            public void run() {
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_RETRIES, 0);
                if (Reconnecter.getInstance().forceReconnect()) {
                    if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
                        progress.setStatusText(JDL.L("gui.warning.reconnectunknown", "Reconnect unknown"));
                    } else {
                        progress.setStatusText(JDL.L("gui.warning.reconnectSuccess", "Reconnect successfull"));
                    }
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
                                ReconnectPluginConfigGUI.this.lblStatusMessage.setText(JDL.L("gui.warning.reconnectunknown", "Reconnect unknown"));
                            } else {
                                ReconnectPluginConfigGUI.this.lblStatusMessage.setText(JDL.L("gui.warning.reconnectSuccess", "Reconnect successfull"));
                            }
                            ReconnectPluginConfigGUI.this.lblSuccessIcon.setIcon(JDTheme.II("gui.images.selected", 32, 32));
                            ReconnectPluginConfigGUI.this.lblSuccessIcon.setEnabled(true);
                            ReconnectPluginConfigGUI.this.lblStatusMessage.setEnabled(true);
                            if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
                                ReconnectPluginConfigGUI.this.lblCurrentIP.setText("?");
                            } else {
                                ReconnectPluginConfigGUI.this.lblCurrentIP.setText(IPController.getInstance().fetchIP().toString());
                            }
                        }

                    };
                } else {
                    progress.setStatusText(JDL.L("gui.warning.reconnectFailed", "Reconnect failed!"));
                    progress.setColor(Color.RED);
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            ReconnectPluginConfigGUI.this.lblStatusMessage.setText(JDL.L("gui.warning.reconnectFailed", "Reconnect failed!"));
                            ReconnectPluginConfigGUI.this.lblSuccessIcon.setIcon(JDTheme.II("gui.images.unselected", 32, 32));
                            ReconnectPluginConfigGUI.this.lblSuccessIcon.setEnabled(true);
                            if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
                                ReconnectPluginConfigGUI.this.lblCurrentIP.setText("?");
                            } else {
                                ReconnectPluginConfigGUI.this.lblCurrentIP.setText(IPController.getInstance().fetchIP().toString());
                            }
                        }

                    };
                }
                timer.interrupt();
                progress.setStatus(100);
                progress.doFinalize(5000);
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_RETRIES, retries);
            }
        }.start();
    }
}
