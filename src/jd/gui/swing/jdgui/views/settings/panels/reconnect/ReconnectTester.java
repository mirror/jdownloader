package jd.gui.swing.jdgui.views.settings.panels.reconnect;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import jd.config.Configuration;
import jd.controlling.JDLogger;
import jd.controlling.JSonWrapper;
import jd.controlling.ProgressController;
import jd.controlling.reconnect.Reconnecter;
import jd.controlling.reconnect.ipcheck.IP;
import jd.controlling.reconnect.ipcheck.IPController;
import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;
import jd.gui.swing.jdgui.views.settings.components.StateUpdateListener;
import jd.nutils.Formatter;
import jd.utils.JDUtilities;

import org.appwork.app.gui.MigPanel;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;

public class ReconnectTester extends MigPanel implements SettingsComponent, ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private JButton           btnTest;
    private JLabel            lblDuration;
    private JLabel            lblTime;
    private JLabel            lblCurrentIP;
    private JLabel            lblSuccessIcon;
    private JLabel            lblStatusMessage;
    private JLabel            lblBeforeIpLabel;
    private JLabel            lblBeforeIP;

    public ReconnectTester() {
        super("ins 0, wrap 6", "[]5[right]20[right]20[right]20[right]20[right]", "[fill]");
        this.initComponents();
        this.layoutComponents();
        this.fill();
    }

    private void fill() {
    }

    private void layoutComponents() {

        add(this.btnTest, "spany 2,pushy,growy");

        add(this.lblDuration);

        add(this.lblTime);
        JLabel lbl = new JLabel(_JDT._.gui_config_reconnect_showcase_currentip());
        lbl.setEnabled(false);
        add(lbl);
        add(this.lblCurrentIP, "growx,pushx");

        add(this.lblSuccessIcon, "spany,alignx right");

        add(this.lblStatusMessage, "spanx 2");

        add(this.lblBeforeIpLabel);

        add(this.lblBeforeIP, "growx,pushx");

    }

    private void initComponents() {
        this.btnTest = new JButton(_JDT._.gui_config_reconnect_showcase_reconnect2());
        this.btnTest.setIcon(NewTheme.I().getIcon("play", 20));
        this.btnTest.addActionListener(this);

        this.lblDuration = new JLabel(_JDT._.gui_config_reconnect_showcase_time());
        this.lblDuration.setEnabled(false);
        this.lblTime = new JLabel("---");
        this.lblCurrentIP = new JLabel("---");

        this.lblSuccessIcon = new JLabel(NewTheme.I().getIcon("ok", 32));
        this.lblSuccessIcon.setEnabled(false);
        this.lblSuccessIcon.setHorizontalTextPosition(SwingConstants.LEFT);

        this.lblStatusMessage = new JLabel(_JDT._.gui_config_reconnect_showcase_message_none());
        this.lblStatusMessage.setEnabled(false);

        this.lblBeforeIpLabel = new JLabel(_JDT._.gui_config_reconnect_showcase_lastip());
        this.lblBeforeIpLabel.setEnabled(false);

        this.lblBeforeIP = new JLabel("---");

    }

    public boolean isMultiline() {
        return true;
    }

    public void addStateUpdateListener(StateUpdateListener listener) {
        throw new IllegalStateException("Not implemented");
    }

    public String getConstraints() {
        return "wmin 10,height 30:n:n";
    }

    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == this.btnTest) {
            this.testReconnect();
        }
    }

    private void testReconnect() {
        JDLogger.addHeader("Reconnect Testing");

        final ProgressController progress = new ProgressController(100, _JDT._.gui_warning_reconnect_pleaseWait(), NewTheme.I().getIcon("reconnect", 20));
        btnTest.setEnabled(false);
        Log.L.info("Start Reconnect");
        this.lblStatusMessage.setText(_JDT._.gui_warning_reconnect_running());
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
                            lblTime.setText(Formatter.formatSeconds((System.currentTimeMillis() - timel) / 1000));
                            lblTime.setEnabled(true);
                            lblDuration.setEnabled(true);
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
                try {
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_RETRIES, 0);
                    if (Reconnecter.getInstance().forceReconnect()) {
                        if (JSonWrapper.get("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
                            progress.setStatusText(_JDT._.gui_warning_reconnectunknown());
                        } else {
                            progress.setStatusText(_JDT._.gui_warning_reconnectSuccess());
                        }
                        new EDTRunner() {

                            @Override
                            protected void runInEDT() {
                                if (JSonWrapper.get("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
                                    lblStatusMessage.setText(_JDT._.gui_warning_reconnectunknown());
                                } else {
                                    lblStatusMessage.setText(_JDT._.gui_warning_reconnectSuccess());
                                }
                                lblSuccessIcon.setIcon(NewTheme.I().getIcon("true", 32));
                                lblSuccessIcon.setEnabled(true);
                                lblStatusMessage.setEnabled(true);
                                if (JSonWrapper.get("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
                                    lblCurrentIP.setText("?");
                                } else {
                                    lblCurrentIP.setText(IPController.getInstance().fetchIP().toString());
                                }
                            }

                        };
                    } else {
                        progress.setStatusText(_JDT._.gui_warning_reconnectFailed());
                        progress.setColor(Color.RED);
                        new EDTRunner() {

                            @Override
                            protected void runInEDT() {
                                lblStatusMessage.setText(_JDT._.gui_warning_reconnectFailed());
                                lblSuccessIcon.setIcon(NewTheme.I().getIcon("false", 32));
                                lblSuccessIcon.setEnabled(true);
                                if (JSonWrapper.get("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
                                    lblCurrentIP.setText("?");
                                } else {
                                    lblCurrentIP.setText(IPController.getInstance().fetchIP().toString());
                                }
                            }

                        };
                    }
                    timer.interrupt();
                    progress.setStatus(100);
                    progress.doFinalize(5000);
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_RETRIES, retries);
                } finally {
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            btnTest.setEnabled(true);
                        }
                    };
                }
            }
        }.start();
    }

    public void updateCurrentIP(final IP ip) {

        new EDTRunner() {

            @Override
            protected void runInEDT() {
                lblCurrentIP.setText(ip.toString());
            }
        };
    }

}