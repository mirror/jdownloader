package jd.gui.swing.jdgui.views.settings.panels.reconnect;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import jd.controlling.reconnect.ReconnectConfig;
import jd.controlling.reconnect.Reconnecter;
import jd.controlling.reconnect.ipcheck.IP;
import jd.controlling.reconnect.ipcheck.IPController;
import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;
import jd.gui.swing.jdgui.views.settings.components.StateUpdateListener;
import jd.nutils.Formatter;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.MigPanel;
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
        Log.L.info("Reconnect Testing");

        btnTest.setEnabled(false);
        Log.L.info("Start Reconnect");
        this.lblStatusMessage.setText(_JDT._.gui_warning_reconnect_running());
        this.lblStatusMessage.setEnabled(true);
        this.lblBeforeIP.setText(IPController.getInstance().getIP().toString());

        this.lblBeforeIP.setEnabled(true);
        this.lblBeforeIpLabel.setEnabled(true);
        this.lblCurrentIP.setText("?");
        final long timel = System.currentTimeMillis();
        final ScheduledExecutorService scheduler = DelayedRunnable.getNewScheduledExecutorService();
        final ScheduledFuture<?> timer = scheduler.scheduleAtFixedRate(new Runnable() {

            public void run() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        lblTime.setText(Formatter.formatSeconds((System.currentTimeMillis() - timel) / 1000));
                        lblTime.setEnabled(true);
                        lblDuration.setEnabled(true);
                    }

                };
            }

        }, 1, 1, TimeUnit.SECONDS);
        final ReconnectConfig config = JsonConfig.create(ReconnectConfig.class);
        final int retries = config.getMaxReconnectRetryNum();
        new Thread() {
            @Override
            public void run() {
                try {
                    config.setMaxReconnectRetryNum(0);
                    if (Reconnecter.getInstance().forceReconnect()) {
                        new EDTRunner() {

                            @Override
                            protected void runInEDT() {
                                if (config.isIPCheckGloballyDisabled()) {
                                    lblStatusMessage.setText(_JDT._.gui_warning_reconnectunknown());
                                } else {
                                    lblStatusMessage.setText(_JDT._.gui_warning_reconnectSuccess());
                                }
                                lblSuccessIcon.setIcon(NewTheme.I().getIcon("true", 32));
                                lblSuccessIcon.setEnabled(true);
                                lblStatusMessage.setEnabled(true);
                                if (config.isIPCheckGloballyDisabled()) {
                                    lblCurrentIP.setText("?");
                                } else {
                                    lblCurrentIP.setText(IPController.getInstance().getIP().toString());
                                }
                            }

                        };
                    } else {
                        new EDTRunner() {

                            @Override
                            protected void runInEDT() {
                                lblStatusMessage.setText(_JDT._.gui_warning_reconnectFailed());
                                lblSuccessIcon.setIcon(NewTheme.I().getIcon("false", 32));
                                lblSuccessIcon.setEnabled(true);
                                if (config.isIPCheckGloballyDisabled()) {
                                    lblCurrentIP.setText("?");
                                } else {
                                    lblCurrentIP.setText(IPController.getInstance().getIP().toString());
                                }
                            }

                        };
                    }
                    config.setMaxReconnectRetryNum(retries);
                } finally {
                    timer.cancel(true);
                    scheduler.shutdown();
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