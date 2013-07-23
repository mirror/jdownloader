//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package org.jdownloader.gui.jdtrayicon;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.TrayIcon;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.concurrent.atomic.AtomicReference;

import jd.gui.swing.jdgui.JDGui;

import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.EDTRunner;

public class TrayMouseAdapter implements MouseListener, MouseMotionListener {

    private TrayExtension           deligate;
    private AtomicReference<Thread> mouseLocationObserver = new AtomicReference<Thread>(null);
    private Point                   min;
    private Point                   max;
    private Dimension               size;
    private MouseEvent              lastEvent;
    private Component               dummy;
    private LogSource               logger;
    private static int              TOOLTIP_DELAY         = 1000;

    public TrayMouseAdapter(TrayExtension lightTray, TrayIcon trayIcon) {
        deligate = lightTray;
        // this.trayIcon = trayIcon;
        dummy = new Component() {
            private static final long serialVersionUID = 1L;
        };
        size = trayIcon.getSize();
        logger = JDGui.getInstance().getLogger();
    }

    public void mouseClicked(MouseEvent e) {
        deligate.mouseClicked(e);

    }

    public void mouseEntered(MouseEvent e) {
        if (mouseLocationObserver.get() != null) return;
        Thread localmouseLocationObserver = new Thread() {
            boolean    mouseStay = false;
            final long enterTime = System.currentTimeMillis();

            public void run() {
                setName("MouseLocationObserver");
                try {
                    while (mouseLocationObserver.get() == Thread.currentThread()) {
                        new EDTRunner() {
                            @Override
                            protected void runInEDT() {
                                Point point = MouseInfo.getPointerInfo().getLocation();
                                if (!isOver(point)) {
                                    MouseEvent me;
                                    me = new MouseEvent(dummy, 0, System.currentTimeMillis(), 0, point.x, point.y, 0, false);
                                    me.setSource(lastEvent.getSource());
                                    mouseExited(me);
                                    return;
                                } else {
                                    if ((System.currentTimeMillis() - enterTime) >= TOOLTIP_DELAY && !mouseStay) {
                                        mouseStay = true;
                                        MouseEvent me;
                                        me = new MouseEvent(dummy, 0, System.currentTimeMillis(), 0, point.x, point.y, 0, false);
                                        me.setSource(TrayMouseAdapter.this);

                                        deligate.mouseStay(me);
                                    }
                                }
                            }
                        };
                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {
                    return;
                } finally {
                    mouseLocationObserver.compareAndSet(Thread.currentThread(), null);
                }
            }

        };
        if (mouseLocationObserver.compareAndSet(null, localmouseLocationObserver)) {
            localmouseLocationObserver.setDaemon(true);
            localmouseLocationObserver.start();
        }
        deligate.mouseEntered(e);
    }

    public void mouseExited(MouseEvent e) {
        abortMouseLocationObserver();
        min = max = null;
        deligate.mouseExited(e);
    }

    public void abortMouseLocationObserver() {
        Thread localmouseLocationObserver = mouseLocationObserver.getAndSet(null);
        if (localmouseLocationObserver != null) localmouseLocationObserver.interrupt();
    }

    public void mousePressed(MouseEvent e) {
        abortMouseLocationObserver();
        min = max = null;
        deligate.mousePressed(e);
    }

    public void mouseReleased(MouseEvent e) {
        deligate.mouseReleased(e);
    }

    public void mouseDragged(MouseEvent e) {
        deligate.mouseDragged(e);
    }

    public void mouseMoved(MouseEvent e) {
        if (e == null || e.getPoint() == null) return;
        Point point = e.getPoint();

        lastEvent = e;
        /**
         * the more the user moves over the tray, the better we know it's location *
         */

        if (this.min == null) {
            this.min = new Point(point.x, point.y);
            this.max = new Point(point.x, point.y);
        } else {
            min.x = Math.min(point.x, min.x);

            min.y = Math.min(point.y, min.y);

            max.x = Math.max(point.x, max.x);
            max.y = Math.max(point.y, max.y);
            // System.out.println(min+" - "+max);
        }

        if (mouseLocationObserver.get() == null) {
            this.mouseEntered(e);
        } else {
            deligate.mouseMoved(e);
        }

    }

    public Point getEstimatedTopLeft() {
        int midx = (max.x + min.x) / 2;
        int midy = (max.y + min.y) / 2;

        return new Point(midx - size.width / 2, midy - size.height / 2);
    }

    /**
     * Passt die iconsize in die festgestellte geschätzte position ein. und prüft ob point darin ist
     * 
     * @param point
     * @return
     */
    protected boolean isOver(Point point) {
        if (max == null || min == null || size == null) { return false; }
        int midx = (max.x + min.x) / 2;
        int midy = (max.y + min.y) / 2;
        // int width = Math.min(size.width, max.x - min.x);
        // int height = Math.min(size.height, max.y - min.y);

        /* the code above causes flickering of the tooltip because isOver only returns true in inner middle of the trayicon */
        int width = size.width;
        int height = size.height;
        int minx = midx - width / 2;
        int miny = midy - height / 2;
        int maxx = midx + width / 2;
        int maxy = midy + height / 2;
        // java.awt.Point[x=1274,y=1175] - java.awt.Point[x=1309,y=1185]
        if (point.x >= minx && point.x <= maxx) {
            if (point.y >= miny && point.y <= maxy) { return true; }

        }
        return false;
    }

}
