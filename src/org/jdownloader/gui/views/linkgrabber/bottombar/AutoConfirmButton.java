package org.jdownloader.gui.views.linkgrabber.bottombar;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.Timer;

import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkchecker.LinkCheckerEvent;
import jd.controlling.linkchecker.LinkCheckerListener;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorCrawler;
import jd.controlling.linkcollector.autostart.AutoStartManagerListener;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.linkcrawler.LinkCrawlerEvent;
import jd.controlling.linkcrawler.LinkCrawlerListener;
import jd.gui.swing.jdgui.components.IconedProcessIndicator;

import org.appwork.swing.components.ExtButton;
import org.appwork.utils.event.predefined.changeevent.ChangeEvent;
import org.appwork.utils.event.predefined.changeevent.ChangeListener;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class AutoConfirmButton extends ExtButton implements ChangeListener, AutoStartManagerListener {
    /**
     * 
     */
    private static final long      serialVersionUID = 1L;

    protected static final int     SIZE             = 22;

    private IconedProcessIndicator progress         = null;

    private Timer                  timer;
    private long                   startTime;
    private boolean                active           = false;

    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag);
    }

    public AutoConfirmButton() {
        super();

        setVisible(false);

        progress = new IconedProcessIndicator(NewTheme.I().getIcon("paralell", 18)) {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            public Dimension getSize() {
                return new Dimension(SIZE, SIZE);
            }

            public boolean isDisplayable() {
                return true;
            }

            public boolean isShowing() {
                return true;
            }

            @Override
            public int getWidth() {

                return SIZE;
            }

            @Override
            public int getHeight() {
                return SIZE;
            }

        };
        progress.getEventSender().addListener(this);

        setIcon(new Icon() {

            public void paintIcon(Component c, Graphics g, int x, int y) {
                g.translate(1, 1);
                progress.paint(g);
            }

            public int getIconWidth() {
                return SIZE;
            }

            public int getIconHeight() {
                return SIZE;
            }

        });

        // use global pool
        // ScheduledThreadPoolExecutor queue = new
        // ScheduledThreadPoolExecutor(1);

        timer = new Timer(100, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                progress.setMaximum(LinkCollector.getInstance().getAutoStartManager().getMaximum());
                progress.setValue(LinkCollector.getInstance().getAutoStartManager().getValue());

            }
        });
        timer.setRepeats(true);
        setToolTipText(_GUI._.AutoConfirmButton_AutoConfirmButton_tooltip_());
        addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setTooltipsEnabled(false);
                LinkCollector.getInstance().getAutoStartManager().interrupt();
                setVisible(false);
            }
        });
        try {
            LinkCrawler.getGlobalEventSender().addListener(new LinkCrawlerListener() {

                public void onLinkCrawlerEvent(LinkCrawlerEvent event) {
                    if (event.getCaller() instanceof LinkCollectorCrawler) {
                        /*
                         * we only want to react on the LinkCrawler of the LinkCollector
                         */
                        update();
                    }
                }
            });
        } catch (VerifyError e) {
            Dialog.getInstance().showExceptionDialog("Eclipse Java 1.7 Bug", "This is an eclipse Java 7 bug. See here: http://goo.gl/REs9c\r\nAdd -XX:-UseSplitVerifier as JVM Parameter", e);

            throw e;
        }
        LinkChecker.getEventSender().addListener(new LinkCheckerListener() {

            public void onLinkCheckerEvent(LinkCheckerEvent event) {
                if (LinkCollector.getInstance().getLinkChecker() == event.getCaller()) {
                    /*
                     * we only want to react on the LinkChecker of the LinkCollector
                     */
                    update();
                }
            }
        });

    }

    public void onChangeEvent(ChangeEvent event) {
        repaint();
    }

    // public void tableChanged(TableModelEvent e) {
    // boolean hasAutoConfirms = LinkGrabberTableModel.getInstance().isAutoConfirm();
    // this.setVisible(hasAutoConfirms);
    // if (hasAutoConfirms) {
    // delayer.resetAndStart();
    // startTime = System.currentTimeMillis();
    // if (!timer.isRunning()) {
    // timer.start();
    // setTooltipsEnabled(true);
    // }
    // } else {
    // setTooltipsEnabled(false);
    // delayer.stop();
    // timer.stop();
    // }
    //
    // }

    private void update() {
        final boolean enabled = !LinkChecker.isChecking() && !LinkCrawler.isCrawling();
        if (enabled == active) return;
        synchronized (this) {
            if (enabled == active) return;
            if (enabled) {
                this.active = true;
                LinkCollector.getInstance().getAutoStartManager().getEventSender().addListener(this, true);
                // LinkGrabberTableModel.getInstance().addTableModelListener(this);
            } else {
                active = false;
                // LinkGrabberTableModel.getInstance().removeTableModelListener(this);
                LinkCollector.getInstance().getAutoStartManager().getEventSender().removeListener(this);
            }
        }
    }

    @Override
    public void onAutoStartManagerDone() {

        new EDTRunner() {

            @Override
            protected void runInEDT() {
                timer.stop();
                setVisible(false);
            }
        };

    }

    @Override
    public void onAutoStartManagerReset() {

        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (!timer.isRunning()) {
                    timer.start();
                }
                setVisible(true);
            }
        };

    }

    @Override
    public void onAutoStartManagerRunning() {

    }

}
