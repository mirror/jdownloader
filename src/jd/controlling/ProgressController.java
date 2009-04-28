//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.controlling;

import java.awt.Color;

import javax.swing.Icon;

import jd.event.ControlEvent;
import jd.event.JDBroadcaster;
import jd.utils.JDUtilities;

class ProgressControllerBroadcaster extends JDBroadcaster<ProgressControllerListener, ProgressControllerEvent> {

    //@Override
    protected void fireEvent(ProgressControllerListener listener, ProgressControllerEvent event) {
        listener.handle_ProgressControllerEvent(event);

    }

}

/**
 * Diese Klasse kann dazu verwendet werden einen Fortschritt in der GUI
 * anzuzeigen. Sie bildet dabei die schnittstelle zwischen Interactionen,
 * plugins etc und der GUI
 * 
 * @author JD-Team
 */
public class ProgressController {

    private static int idCounter = 0;
    private long currentValue;
    private boolean finished;
    private boolean finalizing = false;

    private int id;

    private long max;

    private Object source;
    private String statusText;
    private Color progresscolor;

    public Icon icon = null;

    private transient ProgressControllerBroadcaster broadcaster = new ProgressControllerBroadcaster();

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    public ProgressController(String name) {
        this(name, 100l);
    }

    public boolean isInterruptable() {
        return getBroadcaster().hasListener();
    }

    public ProgressController(String name, long max) {
        id = idCounter++;
        this.max = max;
        statusText = name;
        currentValue = 0;
        finished = false;
        progresscolor = null;
        fireChanges();
    }

    public synchronized JDBroadcaster<ProgressControllerListener, ProgressControllerEvent> getBroadcaster() {
        if (broadcaster == null) broadcaster = new ProgressControllerBroadcaster();
        return this.broadcaster;
    }

    public void setColor(Color color) {
        progresscolor = color;
        fireChanges();
    }

    public Color getColor() {
        return progresscolor;
    }

    public void addToMax(long length) {
        setRange(max + length);
    }

    public void decrease(long i) {
        setStatus(currentValue - 1);
    }

    //@Override
    public void finalize() {
        if (finalizing) return;
        finished = true;
        currentValue = max;
        JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ON_PROGRESS, source));

    }

    public boolean isFinalizing() {
        return this.finalizing;
    }

    public void finalize(final long waittimer) {
        finalizing = true;
        final ProgressController instance = this;
        new Thread() {
            public void run() {
                long timer = waittimer;
                instance.setRange(timer);
                while (timer > 0) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                    timer -= 1000;
                    instance.increase(1000l);
                }
                finished = true;
                currentValue = max;
                if (JDUtilities.getController() != null) JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ON_PROGRESS, source));
            }
        }.start();
    }

    public void fireChanges() {
        if (!isFinished()) {
            if (JDUtilities.getController() != null) JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ON_PROGRESS, source));
        }
    }

    public int getID() {
        return id;
    }

    public long getMax() {
        return max;

    }

    public int getPercent() {
        if (Math.min(currentValue, max) <= 0) return 0;
        return (int) (10000 * currentValue / Math.max(1, Math.max(currentValue, max)));
    }

    public Object getSource() {
        return source;
    }

    public String getStatusText() {
        return statusText;
    }

    public long getValue() {
        return currentValue;
    }

    public synchronized void increase(long i) {
        setStatus(currentValue + i);
    }

    public boolean isFinished() {
        return finished;
    }

    public void setRange(long max) {
        this.max = max;
        setStatus(currentValue);
    }

    public void setSource(Object src) {
        source = src;
        fireChanges();
    }

    public void setStatus(long value) {
        if (value < 0) {
            value = 0;
        }
        if (value > max) {
            value = max;
        }
        currentValue = value;
        fireChanges();
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
        fireChanges();
    }

    //@Override
    public String toString() {
        return "ProgressController " + id;
    }

    public void fireCancelAction() {
        getBroadcaster().fireEvent(new ProgressControllerEvent(this, ProgressControllerEvent.CANCEL));
    }
}
