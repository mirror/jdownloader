package jd.gui.swing.jdgui.views.settings.panels.reconnect;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import jd.controlling.reconnect.ReconnectException;
import jd.controlling.reconnect.ReconnectInvoker;
import jd.controlling.reconnect.ReconnectPluginController;
import jd.controlling.reconnect.ReconnectResult;
import jd.controlling.reconnect.ipcheck.IPConnectionState;
import jd.controlling.reconnect.ipcheck.IPController;
import jd.controlling.reconnect.ipcheck.event.IPControllListener;
import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.circlebar.CircledProgressBar;
import org.appwork.swing.components.circlebar.ImagePainter;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;

public class ReconnectDialog extends AbstractDialog<Object> implements IPControllListener {

    private CircledProgressBar progress;
    private JLabel             duration;
    private JLabel             old;
    private JLabel             newIP;
    private JLabel             header;
    private Thread             recThread;
    private Timer              updateTimer;
    private long               startTime;
    private JLabel             state;

    public ReconnectDialog() {
        super(Dialog.BUTTONS_HIDE_OK, _GUI._.ReconnectDialog_ReconnectDialog_(), null, null, _GUI._.literally_close());
    }

    @Override
    protected Object createReturnValue() {
        return null;
    }

    public static void main(String[] args) {
        LookAndFeelController.getInstance().setUIManager();
        try {
            Dialog.getInstance().showDialog(new ReconnectDialog());
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }
    }

    @Override
    public JComponent layoutDialogContent() {
        MigPanel p = new MigPanel("ins 0,wrap 8", "[][][][][][][][]", "[grow,fill]");
        progress = new CircledProgressBar();
        progress.setIndeterminate(true);
        progress.setValueClipPainter(new ImagePainter(NewTheme.I().getImage("reconnect", 26), 1.0f));
        ((ImagePainter) progress.getValueClipPainter()).setBackground(Color.WHITE);
        ((ImagePainter) progress.getValueClipPainter()).setForeground(Color.GREEN);

        progress.setNonvalueClipPainter(new ImagePainter(NewTheme.I().getImage("reconnect", 26), 0.5f));
        ((ImagePainter) progress.getNonvalueClipPainter()).setBackground(Color.WHITE);
        ((ImagePainter) progress.getNonvalueClipPainter()).setForeground(Color.GREEN);
        p.add(progress, "height 40!,width 40!,spany 2");
        p.add(header = new JLabel(), "spanx 4");
        SwingUtils.toBold(header);
        p.add(state = new JLabel(), "spanx,alignx right");
        SwingUtils.toBold(header);
        state.setHorizontalAlignment(SwingConstants.RIGHT);

        header.setText(_GUI._.ReconnectDialog_layoutDialogContent_header(ReconnectPluginController.getInstance().getActivePlugin().getName()));
        p.add(label(_GUI._.ReconnectDialog_layoutDialogContent_duration()));

        p.add(duration = new JLabel(), "width 50!");

        p.add(label(_GUI._.ReconnectDialog_layoutDialogContent_old()));
        p.add(old = new JLabel(), "width 100!,alignx right");
        state.setHorizontalAlignment(SwingConstants.RIGHT);
        p.add(new JLabel(NewTheme.I().getIcon("go-next", 18)));
        p.add(label(_GUI._.ReconnectDialog_layoutDialogContent_currentip()));
        p.add(newIP = new JLabel(), "width 100!");
        newIP.setHorizontalAlignment(SwingConstants.RIGHT);
        newIP.setText("???");
        //
        IPController.getInstance().invalidate();
        recThread = new Thread("ReconnectTest") {

            public void run() {

                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        old.setText(IPController.getInstance().getIpState().toString());
                    }
                };
                LogSource logger = LogController.CL();
                try {
                    logger.setAllowTimeoutFlush(false);
                    ReconnectInvoker plg = ReconnectPluginController.getInstance().getActivePlugin().getReconnectInvoker();
                    if (plg == null) throw new ReconnectException(_GUI._.ReconnectDialog_run_failed_not_setup_());
                    plg.setLogger(logger);

                    // if (IPController.getInstance().getIpState().isOffline()) {
                    // ((ImagePainter) progress.getNonvalueClipPainter()).setBackground(Color.GRAY);
                    // ((ImagePainter) progress.getNonvalueClipPainter()).setForeground(Color.RED);
                    // progress.setValue(0);
                    // Dialog.getInstance().showMessageDialog(_GUI._.reconnect_test_offline());
                    // new EDTRunner() {
                    //
                    // @Override
                    // protected void runInEDT() {
                    // dispose();
                    // }
                    // };
                    // return;
                    // }
                    ReconnectResult result = plg.validate();
                    if (result.isSuccess()) {
                        logger.clear();
                        new EDTRunner() {

                            @Override
                            protected void runInEDT() {
                                state.setText(_GUI._.ReconnectDialog_onIPValidated_());
                            }
                        };
                    } else {
                        new EDTRunner() {

                            @Override
                            protected void runInEDT() {
                                state.setText(_GUI._.ReconnectDialog_failed());
                            }
                        };
                    }
                    progress.setIndeterminate(false);
                    if (IPController.getInstance().isInvalidated()) {
                        ((ImagePainter) progress.getNonvalueClipPainter()).setBackground(Color.GRAY);
                        ((ImagePainter) progress.getNonvalueClipPainter()).setForeground(Color.RED);
                        progress.setValue(0);
                    } else {
                        progress.setValue(100);
                    }
                } catch (InterruptedException e) {
                    logger.log(e);
                } catch (ReconnectException e) {
                    logger.log(e);
                    if (!StringUtils.isEmpty(e.getMessage())) {
                        Dialog.getInstance().showErrorDialog(e.getMessage());
                    } else {
                        Dialog.getInstance().showErrorDialog(_GUI._.ReconnectDialog_layoutDialogContent_error());
                    }
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            dispose();
                        }
                    };
                } catch (Throwable e) {
                    logger.log(e);
                } finally {
                    logger.close();
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            updateTimer.stop();
                        }
                    };
                }

            }
        };

        startTime = System.currentTimeMillis();
        updateTimer = new Timer(1000, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                duration.setText(TimeFormatter.formatMilliSeconds(System.currentTimeMillis() - startTime, 0));
                // if (!IPController.getInstance().isInvalidated()) {
                newIP.setText(IPController.getInstance().getIP().toString());
                // updateTimer.stop();
                // }

            }
        });
        updateTimer.setRepeats(true);
        updateTimer.start();
        recThread.start();
        IPController.getInstance().getEventSender().addListener(this);
        return p;
    }

    @Override
    public void dispose() {
        super.dispose();
        recThread.interrupt();
        updateTimer.stop();
        IPController.getInstance().getEventSender().removeListener(this);
    }

    private Component label(String lbl) {
        JLabel ret = new JLabel(lbl);
        ret.setEnabled(false);
        return ret;
    }

    public void onIPForbidden(final IPConnectionState parameter) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                state.setText(_GUI._.ReconnectDialog_onIPForbidden_(parameter.getExternalIp().toString()));

            }
        };
    }

    public void onIPInvalidated(IPConnectionState parameter) {
    }

    public void onIPChanged(IPConnectionState parameter, IPConnectionState parameter2) {
    }

    public void onIPOffline() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                state.setText(_GUI._.ReconnectDialog_onIPOffline_());

            }
        };
    }

    public void onIPValidated(IPConnectionState parameter, IPConnectionState parameter2) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                state.setText(_GUI._.ReconnectDialog_onIPValidated_());
            }
        };
    }

    public void onIPOnline(IPConnectionState parameter) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                state.setText(_GUI._.ReconnectDialog_onIPOnline_());
            }
        };

    }

    public void onIPStateChanged(IPConnectionState parameter, IPConnectionState parameter2) {
    }
}
