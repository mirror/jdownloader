package org.jdownloader.gui.views.linkgrabber;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.Timer;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import jd.controlling.IOEQ;
import jd.controlling.linkcollector.LinkCollector;
import jd.gui.swing.jdgui.components.IconedProcessIndicator;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.swing.components.ExtButton;
import org.appwork.utils.event.predefined.changeevent.ChangeEvent;
import org.appwork.utils.event.predefined.changeevent.ChangeListener;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class AutoConfirmButton extends ExtButton implements ChangeListener, TableModelListener, ActionListener {
    protected static final int     SIZE     = 22;
    private static final long      MAXTIME  = 15000l;
    private IconedProcessIndicator progress = null;
    private DelayedRunnable        delayer;
    private Timer                  timer;
    private long                   startTime;

    public AutoConfirmButton() {
        super();

        progress = new IconedProcessIndicator(NewTheme.I().getIcon("paralell", 18)) {
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
        progress.setMaximum((int) MAXTIME);
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

        LinkGrabberTableModel.getInstance().addTableModelListener(this);
        // use global pool
        // ScheduledThreadPoolExecutor queue = new
        // ScheduledThreadPoolExecutor(1);

        timer = new Timer(100, this);
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
        delayer = new DelayedRunnable(IOEQ.TIMINGQUEUE, MAXTIME, -1) {
            @Override
            public void delayedrun() {
                new EDTRunner() {
                    @Override
                    protected void runInEDT() {
                        timer.stop();
                        System.out.println("AUTOADD");
                        setVisible(false);
                        LinkCollector.getInstance().clear();
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

    public void actionPerformed(ActionEvent e) {
        if ((System.currentTimeMillis() - startTime) < 1000) {
            progress.setValue((int) MAXTIME);
        } else {
            progress.setValue((int) (MAXTIME - (System.currentTimeMillis() - startTime)));
        }

    }
}
