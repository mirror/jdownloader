package jd.controlling.reconnect.plugins.liveheader;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import jd.controlling.reconnect.ReconnectPluginController;
import jd.controlling.reconnect.ReconnectResult;
import jd.controlling.reconnect.ipcheck.IPConnectionState;
import jd.controlling.reconnect.ipcheck.IPController;
import jd.controlling.reconnect.ipcheck.event.IPControllListener;

import org.appwork.app.gui.MigPanel;
import org.appwork.swing.components.ExtTextArea;
import org.appwork.swing.components.circlebar.CircledProgressBar;
import org.appwork.swing.components.circlebar.ImagePainter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public abstract class ReconnectFindDialog extends AbstractDialog<Object> implements IPControllListener {

    public void onIPForbidden(IPConnectionState parameter) {
    }

    public void onIPInvalidated(IPConnectionState parameter) {
    }

    public void onIPChanged(IPConnectionState parameter, IPConnectionState parameter2) {
        setNewIP(parameter2.isOffline() ? "  -  " : parameter2.getExternalIp().toString());
    }

    public void onIPOffline() {
        setSubStatusState(_GUI._.ReconnectDialog_onIPOffline_(), NewTheme.I().getIcon("network-error", 16));
        setNewIP(_GUI._.literally_offline());
    }

    public void onIPValidated(IPConnectionState parameter, IPConnectionState parameter2) {
    }

    public void onIPOnline(IPConnectionState parameter) {
        setSubStatusState(_GUI._.ReconnectDialog_onIPOnline_(), NewTheme.I().getIcon("network-idle", 16));
    }

    public void onIPStateChanged(IPConnectionState parameter, IPConnectionState parameter2) {
    }

    private JProgressBar                         bar;
    private CircledProgressBar                   circle;
    private JLabel                               header;
    private JLabel                               state;
    private JLabel                               duration;

    private JLabel                               newIP;
    private Thread                               th;
    private Timer                                updateTimer;
    private long                                 startTime;
    private ArrayList<? extends ReconnectResult> foundList;

    public ReconnectFindDialog() {

        super(Dialog.STYLE_HIDE_ICON, _GUI._.AutoDetectAction_actionPerformed_d_title(), null, _GUI._.ReconnectFindDialog_ReconnectFindDialog_ok(), null);

    }

    @Override
    protected Object createReturnValue() {
        return null;
    }

    public void setBarText(final String txt) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                bar.setString(txt);
            }
        };
    }

    public void setBarProgress(final int prog) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                bar.setValue(prog);
            }
        };
    }

    @Override
    public JComponent layoutDialogContent() {
        MigPanel p = new MigPanel("ins 0,wrap 1", "[grow,fill]", "[]");
        ExtTextArea txt = new ExtTextArea();
        txt.setLabelMode(true);
        p.add(txt);
        txt.setText(_GUI._.AutoDetectAction_actionPerformed_d_msg());
        bar = new JProgressBar();
        bar.setStringPainted(true);
        bar.setMaximum(100);
        p.add(bar);
        circle = new CircledProgressBar();
        circle.setValueClipPainter(new ImagePainter(NewTheme.I().getImage("reconnect", 26), 1.0f));
        ((ImagePainter) circle.getValueClipPainter()).setBackground(Color.WHITE);
        ((ImagePainter) circle.getValueClipPainter()).setForeground(Color.GREEN);

        circle.setNonvalueClipPainter(new ImagePainter(NewTheme.I().getImage("reconnect", 26), 0.5f));
        ((ImagePainter) circle.getNonvalueClipPainter()).setBackground(Color.WHITE);
        ((ImagePainter) circle.getNonvalueClipPainter()).setForeground(Color.GREEN);
        MigPanel sp = new MigPanel("ins 0", "[fill][fill][grow,fill][fill][fill]", "[fill][grow,fill]");
        sp.add(circle, "spany 2,gapright 10,height 42!,width 42!");
        sp.add(header = new JLabel(), "spanx 3");
        SwingUtils.toBold(header);
        sp.add(state = new JLabel(), "spanx,alignx right");
        state.setHorizontalTextPosition(SwingConstants.LEFT);
        SwingUtils.toBold(header);
        state.setHorizontalAlignment(SwingConstants.RIGHT);
        circle.setIndeterminate(true);
        header.setText(_GUI._.ReconnectDialog_layoutDialogContent_header(ReconnectPluginController.getInstance().getActivePlugin().getName()));
        sp.add(label(_GUI._.ReconnectDialog_layoutDialogContent_duration()));

        sp.add(duration = new JLabel());

        state.setHorizontalAlignment(SwingConstants.RIGHT);
        sp.add(new JLabel(NewTheme.I().getIcon("go-next", 18)));
        sp.add(label(_GUI._.ReconnectDialog_layoutDialogContent_currentip()));
        sp.add(newIP = new JLabel(), "width 100!");
        newIP.setHorizontalAlignment(SwingConstants.RIGHT);
        IPController.getInstance().invalidate();
        IPController.getInstance().validate();
        if (IPController.getInstance().getIpState().isOffline()) {
            setSubStatusState(_GUI._.ReconnectDialog_onIPOffline_(), NewTheme.I().getIcon("network-error", 16));
            setNewIP(_GUI._.literally_offline());
        } else {
            setSubStatusState(_GUI._.ReconnectDialog_onIPOnline_(), NewTheme.I().getIcon("network-idle", 16));
            setNewIP(IPController.getInstance().getIpState().getExternalIp().toString());
        }
        p.add(sp);

        th = new Thread(getClass().getName()) {
            public void run() {
                try {
                    ReconnectFindDialog.this.run();
                } catch (InterruptedException e) {
                    e.printStackTrace();

                }
                dispose();
            }
        };
        startTime = System.currentTimeMillis();
        updateTimer = new Timer(1000, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                duration.setText(TimeFormatter.formatMilliSeconds(System.currentTimeMillis() - startTime, 0));

            }
        });
        updateTimer.setRepeats(true);
        updateTimer.start();
        th.start();
        IPController.getInstance().getEventSender().addListener(this);
        return p;
    }

    @Override
    protected void packed() {
        super.packed();
        okButton.setEnabled(false);

        okButton.setToolTipText(_GUI._.ReconnectFindDialog_packed_no_found_script_tooltip());
    }

    @Override
    protected void setReturnmask(boolean b) {
        if (b) {
            // interrupt scxanning and use best script found so far

            Collections.sort(foundList, new Comparator<ReconnectResult>() {

                public int compare(ReconnectResult o1, ReconnectResult o2) {
                    return new Long(o2.getAverageSuccessDuration()).compareTo(new Long(o1.getAverageSuccessDuration()));
                }
            });

            foundList.get(0).getInvoker().getPlugin().setSetup(foundList.get(0));
        }
        super.setReturnmask(b);
    }

    public void setInterruptEnabled(ArrayList<? extends ReconnectResult> list) {
        this.foundList = list;
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                okButton.setEnabled(true);
                okButton.setIcon(NewTheme.I().getIcon("ok", 18));
                okButton.setToolTipText(_GUI._.ReconnectFindDialog_packed_interrupt_tooltip());
            }

        };

    }

    @Override
    public void dispose() {
        super.dispose();
        IPController.getInstance().getEventSender().removeListener(this);
        updateTimer.stop();
        th.interrupt();

    }

    public void setSubStatusState(final String txt, final ImageIcon imageIcon) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                state.setText(txt);
                state.setIcon(imageIcon);
            }
        };
    }

    public void setNewIP(final String txt) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                newIP.setText(txt);
            }
        };
    }

    public void setSubStatusHeader(final String txt) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                header.setText(txt);
            }
        };
    }

    abstract public void run() throws InterruptedException;

    private Component label(String lbl) {
        JLabel ret = new JLabel(lbl);
        ret.setEnabled(false);
        return ret;
    }
}
//

