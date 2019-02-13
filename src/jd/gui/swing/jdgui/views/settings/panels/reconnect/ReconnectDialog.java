package jd.gui.swing.jdgui.views.settings.panels.reconnect;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.atomic.AtomicReference;

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
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.logging.LogController;

public class ReconnectDialog extends AbstractDialog<Object> implements IPControllListener {
    private CircledProgressBar   progress;
    private JLabel               duration;
    private JLabel               old;
    private JLabel               newIP;
    private JLabel               header;
    private Thread               recThread;
    private Timer                updateTimer;
    private long                 startTime;
    private JLabel               state;
    private ReconnectResult      result;
    protected ReconnectException exception;

    public ReconnectException getException() {
        return exception;
    }

    public ReconnectDialog() {
        super(UIOManager.BUTTONS_HIDE_OK, _GUI.T.ReconnectDialog_ReconnectDialog_(), null, null, _GUI.T.literally_close());
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
        progress.setValueClipPainter(new ImagePainter(new AbstractIcon(IconKey.ICON_RECONNECT, 26), 1.0f));
        ((ImagePainter) progress.getValueClipPainter()).setBackground(Color.WHITE);
        ((ImagePainter) progress.getValueClipPainter()).setForeground(Color.GREEN);
        progress.setNonvalueClipPainter(new ImagePainter(new AbstractIcon(IconKey.ICON_RECONNECT, 26), 0.5f));
        ((ImagePainter) progress.getNonvalueClipPainter()).setBackground(Color.WHITE);
        ((ImagePainter) progress.getNonvalueClipPainter()).setForeground(Color.GREEN);
        p.add(progress, "height 40!,width 40!,spany 2");
        p.add(header = new JLabel(), "spanx 4");
        SwingUtils.toBold(header);
        p.add(state = new JLabel(), "spanx,alignx right");
        SwingUtils.toBold(header);
        state.setHorizontalAlignment(SwingConstants.RIGHT);
        header.setText(_GUI.T.ReconnectDialog_layoutDialogContent_header(ReconnectPluginController.getInstance().getActivePlugin().getName()));
        p.add(label(_GUI.T.ReconnectDialog_layoutDialogContent_duration()));
        p.add(duration = new JLabel("??m:??s"), "width 100!");
        p.add(label(_GUI.T.ReconnectDialog_layoutDialogContent_old()));
        p.add(old = new JLabel("???.???.???.???"), "width 100!,alignx right");
        state.setHorizontalAlignment(SwingConstants.RIGHT);
        p.add(new JLabel(new AbstractIcon(IconKey.ICON_GO_NEXT, 18)));
        p.add(label(_GUI.T.ReconnectDialog_layoutDialogContent_currentip()));
        p.add(newIP = new JLabel(), "width 100!");
        newIP.setHorizontalAlignment(SwingConstants.RIGHT);
        newIP.setText("???.???.???.???");
        //
        recThread = new Thread(getClass().getName()) {
            {
                setDaemon(true);
            }

            public void run() {
                IPController.getInstance().invalidate();
                final IPConnectionState ipState = IPController.getInstance().getIpState();
                new EDTRunner() {
                    @Override
                    protected void runInEDT() {
                        old.setText(ipState.toString());
                    }
                };
                LogSource logger = LogController.CL();
                try {
                    logger.setAllowTimeoutFlush(false);
                    if (startReconnectAndWait(logger)) {
                        logger.clear();
                        new EDTRunner() {
                            @Override
                            protected void runInEDT() {
                                state.setText(_GUI.T.ReconnectDialog_onIPValidated_());
                            }
                        };
                    } else {
                        new EDTRunner() {
                            @Override
                            protected void runInEDT() {
                                state.setText(_GUI.T.ReconnectDialog_failed());
                            }
                        };
                    }
                    update();
                    progress.setIndeterminate(false);
                    if (IPController.getInstance().isInvalidated()) {
                        ((ImagePainter) progress.getNonvalueClipPainter()).setBackground(Color.GRAY);
                        ((ImagePainter) progress.getNonvalueClipPainter()).setForeground(Color.RED);
                        progress.setValue(0);
                    } else {
                        progress.setValue(100);
                    }
                    onFinished();
                } catch (InterruptedException e) {
                    logger.log(e);
                } catch (ReconnectException e) {
                    exception = e;
                    logger.log(e);
                    reportException(e);
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
                update();
            }
        });
        IPController.getInstance().getEventSender().addListener(this, true);
        updateTimer.setRepeats(true);
        updateTimer.start();
        recThread.start();
        return p;
    }

    protected void onFinished() {
    }

    @Override
    public void dispose() {
        super.dispose();
        IPController.getInstance().getEventSender().removeListener(this);
        recThread.interrupt();
        updateTimer.stop();
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
                state.setText(_GUI.T.ReconnectDialog_onIPForbidden_(parameter.getExternalIp().toString()));
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
                state.setText(_GUI.T.ReconnectDialog_onIPOffline_());
            }
        };
    }

    public void onIPValidated(IPConnectionState parameter, IPConnectionState parameter2) {
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                state.setText(_GUI.T.ReconnectDialog_onIPValidated_());
            }
        };
    }

    public void onIPOnline(IPConnectionState parameter) {
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                state.setText(_GUI.T.ReconnectDialog_onIPOnline_());
            }
        };
    }

    public void onIPStateChanged(IPConnectionState parameter, IPConnectionState parameter2) {
    }

    protected boolean startReconnectAndWait(LogSource logger) throws ReconnectException, InterruptedException {
        final ReconnectInvoker plg = getInvoker();
        if (plg == null) {
            throw new ReconnectException(_GUI.T.ReconnectDialog_run_failed_not_setup_());
        }
        plg.setLogger(logger);
        Timer time = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new EDTRunner() {
                    private String oldStatus;

                    @Override
                    protected void runInEDT() {
                        String status = plg.getStatusString();
                        if (status != null && !StringUtils.equals(status, oldStatus)) {
                            state.setText(status);
                        }
                        oldStatus = status;
                    }
                };
            }
        });
        time.start();
        try {
            result = plg.validate();
        } finally {
            time.stop();
        }
        return result.isSuccess();
    }

    public ReconnectResult getResult() {
        return result;
    }

    private ReconnectInvoker invoker = null;

    public void setInvoker(ReconnectInvoker invoker) {
        this.invoker = invoker;
    }

    protected ReconnectInvoker getInvoker() {
        return invoker == null ? ReconnectPluginController.getInstance().getActivePlugin().getReconnectInvoker() : invoker;
    }

    private final AtomicReference<Thread> ipStateThread = new AtomicReference<Thread>();

    protected void update() {
        synchronized (ipStateThread) {
            new EDTRunner() {
                @Override
                protected void runInEDT() {
                    duration.setText(TimeFormatter.formatMilliSeconds(System.currentTimeMillis() - startTime, 0));
                }
            };
            Thread ipStateThread = this.ipStateThread.get();
            if (ipStateThread == null || !ipStateThread.isAlive()) {
                ipStateThread = new Thread(getClass().getName()) {
                    {
                        setDaemon(true);
                    }

                    @Override
                    public void run() {
                        final IPConnectionState ip = IPController.getInstance().getIpState();
                        new EDTRunner() {
                            @Override
                            protected void runInEDT() {
                                newIP.setText(ip.toString());
                            }
                        };
                    }
                };
                this.ipStateThread.set(ipStateThread);
                ipStateThread.start();
            }
        }
    }

    protected void reportException(ReconnectException e) {
        if (!StringUtils.isEmpty(e.getMessage())) {
            Dialog.getInstance().showErrorDialog(e.getMessage());
        } else {
            Dialog.getInstance().showErrorDialog(_GUI.T.ReconnectDialog_layoutDialogContent_error());
        }
    }
}
