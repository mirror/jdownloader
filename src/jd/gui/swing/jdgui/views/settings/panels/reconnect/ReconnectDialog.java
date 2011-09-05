package jd.gui.swing.jdgui.views.settings.panels.reconnect;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.Timer;

import jd.controlling.reconnect.ReconnectException;
import jd.controlling.reconnect.ReconnectPluginController;
import jd.controlling.reconnect.ReconnectResult;
import jd.controlling.reconnect.ipcheck.IPController;
import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.app.gui.MigPanel;
import org.appwork.swing.components.circlebar.CircledProgressBar;
import org.appwork.swing.components.circlebar.ImagePainter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class ReconnectDialog extends AbstractDialog<Object> {

    private CircledProgressBar progress;
    private JLabel             duration;
    private JLabel             old;
    private JLabel             newIP;
    private JLabel             header;
    private Thread             recThread;
    private Timer              updateTimer;
    private long               startTime;

    public ReconnectDialog() {
        super(Dialog.BUTTONS_HIDE_OK, _GUI._.ReconnectDialog_ReconnectDialog_(), null, null, null);
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
        p.add(header = new JLabel(), "spanx");
        SwingUtils.toBold(header);
        header.setText(_GUI._.ReconnectDialog_layoutDialogContent_header(ReconnectPluginController.getInstance().getActivePlugin().getName()));
        p.add(label(_GUI._.ReconnectDialog_layoutDialogContent_duration()));

        p.add(duration = new JLabel(), "width 50!");

        p.add(label(_GUI._.ReconnectDialog_layoutDialogContent_old()));
        p.add(old = new JLabel(), "width 100!");

        p.add(new JLabel(NewTheme.I().getIcon("go-next", 18)));
        p.add(label(_GUI._.ReconnectDialog_layoutDialogContent_new()));
        p.add(newIP = new JLabel(), "width 100!");
        newIP.setText("???");
        //
        IPController.getInstance().invalidate();
        recThread = new Thread("ReconnectTest") {

            public void run() {

                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        old.setText(IPController.getInstance().getIP().toString());
                    }
                };
                try {
                    ReconnectPluginController.getInstance().getActivePlugin().getReconnectInvoker().validate(new ReconnectResult() {

                        @Override
                        public void setSuccess(boolean success) {
                            super.setSuccess(success);
                        }

                        @Override
                        public void setStartTime(long startTime) {
                            super.setStartTime(startTime);
                        }

                        @Override
                        public void setOfflineTime(long offlineTime) {
                            super.setOfflineTime(offlineTime);
                        }

                        @Override
                        public void setSuccessTime(long successTime) {
                            super.setSuccessTime(successTime);
                        }

                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ReconnectException e) {
                    e.printStackTrace();
                }

            }
        };

        startTime = System.currentTimeMillis();
        updateTimer = new Timer(1000, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                duration.setText(TimeFormatter.formatMilliSeconds(System.currentTimeMillis() - startTime, 0));
                if (!IPController.getInstance().isInvalidated()) {
                    newIP.setText(IPController.getInstance().getIP().toString());
                    updateTimer.stop();
                }

            }
        });
        updateTimer.setRepeats(true);
        updateTimer.start();
        recThread.start();
        return p;
    }

    private Component label(String lbl) {
        JLabel ret = new JLabel(lbl);
        ret.setEnabled(false);
        return ret;
    }
}
