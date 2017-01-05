package org.jdownloader.gui.notify.reconnect;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JLabel;

import jd.controlling.reconnect.Reconnecter.ReconnectResult;
import jd.controlling.reconnect.ipcheck.IPConnectionState;
import jd.controlling.reconnect.ipcheck.IPController;
import jd.gui.swing.jdgui.components.IconedProcessIndicator;

import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.notify.AbstractBubbleContentPanel;
import org.jdownloader.gui.notify.Element;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class ReconnectBubbleContent extends AbstractBubbleContentPanel {

    private final JLabel             newIP;
    private final JLabel             old;

    private final long               startTime      = System.currentTimeMillis();

    private final JLabel             duration;
    protected IconedProcessIndicator progressCircle = null;

    @Override
    public void stop() {
        final IconedProcessIndicator progressCircle = this.progressCircle;
        if (progressCircle != null) {
            progressCircle.setIndeterminate(false);
            progressCircle.setMaximum(100);
            progressCircle.setValue(100);
        }
        super.stop();
    }

    public ReconnectBubbleContent() {
        super("auto-reconnect");
        progressCircle = createProgress("auto-reconnect");
        add(progressCircle, "width 32!,height 32!,pushx,growx,pushy,growy,spany,aligny top");
        add(createHeaderLabel((_GUI.T.ReconnectDialog_layoutDialogContent_duration())));
        add(duration = new JLabel("-"));
        add(createHeaderLabel((_GUI.T.ReconnectDialog_layoutDialogContent_old())));
        add(old = new JLabel("???.???.???.???"));
        add(createHeaderLabel((_GUI.T.ReconnectDialog_layoutDialogContent_currentip())));
        add(newIP = new JLabel("???.???.???.???"));
        progressCircle.setIndeterminate(true);
        progressCircle.setValue(0);
        new Thread(getClass().getName()) {
            {
                setDaemon(true);
            }

            @Override
            public void run() {
                final IPConnectionState ip = IPController.getInstance().getIpState();
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        old.setText(ip.toString());
                    }
                };

            }
        }.start();
    }

    private final AtomicReference<Thread> ipStateThread = new AtomicReference<Thread>();

    public void update() {
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

    public void onResult(ReconnectResult result) {
        switch (result) {
        case SUCCESSFUL:
            add(createHeaderLabel(_GUI.T.ReconnectBubbleContent_onResult_result()));
            add(new JLabel(_GUI.T.ReconnectDialog_onIPValidated_(), new AbstractIcon(IconKey.ICON_OK, 20), JLabel.LEFT));
            break;
        case FAILED:
            add(createHeaderLabel(_GUI.T.ReconnectBubbleContent_onResult_result()));
            add(new JLabel(_GUI.T.ReconnectDialog_failed(), new AbstractIcon(IconKey.ICON_ERROR, 20), JLabel.LEFT));
            break;
        }
    }

    @Override
    public void updateLayout() {
    }

    public static void fill(ArrayList<Element> elements) {
    }

}
