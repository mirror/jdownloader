package org.jdownloader.gui.views.linkgrabber;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.Icon;
import javax.swing.Timer;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import jd.controlling.IOEQ;
import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkchecker.LinkCheckerEvent;
import jd.controlling.linkchecker.LinkCheckerListener;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorCrawler;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledLink.LinkState;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.linkcrawler.LinkCrawlerEvent;
import jd.controlling.linkcrawler.LinkCrawlerListener;
import jd.controlling.packagecontroller.AbstractNode;
import jd.gui.swing.jdgui.components.IconedProcessIndicator;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.swing.components.ExtButton;
import org.appwork.utils.event.predefined.changeevent.ChangeEvent;
import org.appwork.utils.event.predefined.changeevent.ChangeListener;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.linkgrabber.actions.ConfirmAction;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.staticreferences.CFG_LINKFILTER;

public class AutoConfirmButton extends ExtButton implements ChangeListener, TableModelListener {
    /**
     * 
     */
    private static final long      serialVersionUID = 1L;

    protected static final int     SIZE             = 22;

    private IconedProcessIndicator progress         = null;
    private DelayedRunnable        delayer;
    private Timer                  timer;
    private long                   startTime;
    private boolean                active           = false;

    private int                    waittime;

    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag);
    }

    public AutoConfirmButton() {
        super();
        setVisible(false);
        waittime = org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.CFG.getAutoConfirmDelay();
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
        progress.setMaximum(waittime);
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
                if ((System.currentTimeMillis() - startTime) < 1000) {
                    progress.setValue((int) waittime);
                } else {
                    progress.setValue((int) (waittime - (System.currentTimeMillis() - startTime)));
                }
            }
        });
        timer.setRepeats(true);
        setToolTipText(_GUI._.AutoConfirmButton_AutoConfirmButton_tooltip_());
        addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setTooltipsEnabled(false);
                delayer.stop();
                timer.stop();
                setVisible(false);
            }
        });
        try {
            LinkCrawler.getEventSender().addListener(new LinkCrawlerListener() {

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
        delayer = new DelayedRunnable(IOEQ.TIMINGQUEUE, waittime, -1) {
            @Override
            public void delayedrun() {
                new EDTRunner() {
                    @Override
                    protected void runInEDT() {
                        timer.stop();
                        setVisible(false);
                        IOEQ.add(new Runnable() {

                            public void run() {
                                java.util.List<AbstractNode> list = new ArrayList<AbstractNode>();
                                boolean autoStart = org.jdownloader.settings.staticreferences.CFG_LINKFILTER.LINKGRABBER_AUTO_START_ENABLED.getValue();

                                for (CrawledLink l : LinkGrabberTableModel.getInstance().getAllChildrenNodes()) {
                                    if (l.getLinkState() == LinkState.OFFLINE) continue;
                                    if (l.isAutoConfirmEnabled() || CFG_LINKFILTER.LINKGRABBER_AUTO_CONFIRM_ENABLED.isEnabled()) {
                                        list.add(l);
                                        if (l.isAutoStartEnabled()) autoStart = true;
                                    }
                                }

                                ConfirmAction ca = new ConfirmAction(new SelectionInfo<CrawledPackage, CrawledLink>(list).setShiftDown(autoStart));
                                ca.setAutostart(autoStart);
                                ca.actionPerformed(null);
                            }
                        });
                    }
                };
            }
        };
    }

    public void onChangeEvent(ChangeEvent event) {
        repaint();
    }

    public void tableChanged(TableModelEvent e) {
        boolean hasAutoConfirms = LinkGrabberTableModel.getInstance().isAutoConfirm();
        this.setVisible(hasAutoConfirms);
        if (hasAutoConfirms) {
            delayer.resetAndStart();
            startTime = System.currentTimeMillis();
            if (!timer.isRunning()) {
                timer.start();
                setTooltipsEnabled(true);
            }
        } else {
            setTooltipsEnabled(false);
            delayer.stop();
            timer.stop();
        }

    }

    private synchronized void update() {
        final boolean enabled = !LinkChecker.isChecking() && !LinkCrawler.isCrawling();
        if (enabled == active) return;
        if (enabled) {
            this.active = true;
            LinkGrabberTableModel.getInstance().removeTableModelListener(this);
            LinkGrabberTableModel.getInstance().addTableModelListener(this);
        } else {
            active = false;
            LinkGrabberTableModel.getInstance().removeTableModelListener(this);
        }
    }

}
