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
import javax.swing.ImageIcon;

import jd.event.ControlEvent;
import jd.event.MessageEvent;
import jd.event.MessageListener;
import jd.utils.JDUtilities;

import org.appwork.utils.event.Eventsender;
import org.jdownloader.images.NewTheme;

class ProgressControllerBroadcaster extends Eventsender<ProgressControllerListener, ProgressControllerEvent> {

    @Override
    protected void fireEvent(ProgressControllerListener listener, ProgressControllerEvent event) {
        listener.onProgressControllerEvent(event);
    }

}

/**
 * Diese Klasse kann dazu verwendet werden einen Fortschritt in der GUI
 * anzuzeigen. Sie bildet dabei die schnittstelle zwischen Interactionen,
 * plugins etc und der GUI
 * 
 * @author JD-Team
 */
public class ProgressController implements MessageListener, Comparable<ProgressController> {

    private static final int ICONSIZE = 19;

    public static enum Type {
        DIALOG, NORMAL
    }

    private static int                              idCounter     = 0;
    private long                                    currentValue;
    private boolean                                 finished;
    private boolean                                 finalizing    = false;

    private int                                     id;
    private boolean                                 indeterminate = false;
    private long                                    max;

    private Object                                  source;
    private String                                  statusText;
    private Color                                   progresscolor = null;
    private Icon                                    icon          = null;
    private String                                  initials      = null;

    private transient ProgressControllerBroadcaster broadcaster   = new ProgressControllerBroadcaster();
    private boolean                                 abort         = false;
    private Type                                    type;
    private String                                  name;

    public Type getType() {
        return type;
    }

    public ProgressController(String name, String iconKey) {
        this(name, 100l, iconKey);
    }

    public boolean isInterruptable() {
        return getBroadcaster().hasListener();
    }

    public ProgressController(String statusText, long max, String iconKey) {
        this(Type.NORMAL, statusText, statusText, max, iconKey);
    }

    public ProgressController(Type type, String name, String statusText, long max, String iconKey) {
        this(type, name, statusText, max, iconKey == null ? null : NewTheme.I().getIcon(iconKey, ICONSIZE));

    }

    public ProgressController(Type type, String name, String statusText, long max, ImageIcon icon19px) {
        this.name = name;
        this.type = type;
        id = idCounter++;
        this.max = max;
        this.statusText = statusText;
        currentValue = 0;
        finished = false;
        progresscolor = null;
        this.icon = icon19px;
        fireChanges();
        broadcaster = new ProgressControllerBroadcaster();
    }

    public ProgressController(int max, String statusText, ImageIcon icon19px) {
        this(Type.NORMAL, statusText, statusText, max, icon19px);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Eventsender<ProgressControllerListener, ProgressControllerEvent> getBroadcaster() {
        return this.broadcaster;
    }

    public void setColor(Color color) {
        progresscolor = color;
        fireChanges();
    }

    public Color getColor() {
        return progresscolor;
    }

    public void setIcon(String iconKey) {
        this.icon = NewTheme.I().getIcon(iconKey, ICONSIZE);
        fireChanges();
    }

    public void setInitials(String iconInitials) {
        if (iconInitials != null && iconInitials.length() > 2) return;
        this.initials = iconInitials;
        fireChanges();
    }

    public Icon getIcon() {
        return icon;
    }

    public String getInitials() {
        return initials;
    }

    public void addToMax(long length) {
        setRange(max + length);
    }

    public void decrease(long i) {
        setStatus(currentValue - i);
    }

    public void doFinalize() {
        if (finalizing) return;
        finished = true;
        currentValue = max;
        JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ON_PROGRESS, source));
    }

    public boolean isFinalizing() {
        return this.finalizing;
    }

    public void doFinalize(final long waittimer) {
        if (finalizing) return;
        finalizing = true;
        setRange(waittimer, 0);
    }

    public void setFinished() {
        finished = true;
        currentValue = max;
        JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ON_PROGRESS, source));
        getBroadcaster().fireEvent(new ProgressControllerEvent(this, ProgressControllerEvent.FINISHED));
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

    public float getPercent() {
        if (Math.min(currentValue, max) <= 0) return 0.0f;
        return ((10000 * currentValue / Math.max(1, Math.max(currentValue, max)))) / 100.0f;
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
        return finished || currentValue >= max;
    }

    public boolean isAbort() {
        return abort;
    }

    public void setRange(long max) {
        this.max = max;
        setStatus(currentValue);
    }

    public void setRange(long max, long cur) {
        this.max = max;
        setStatus(cur);
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

    @Override
    public String toString() {
        return "ProgressController " + id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof ProgressController)) return false;
        return this.getID() == ((ProgressController) o).getID();
    }

    @Override
    public int hashCode() {
        return this.getID();
    }

    public void fireCancelAction() {
        if (abort) return;
        abort = true;
        getBroadcaster().fireEvent(new ProgressControllerEvent(this, ProgressControllerEvent.CANCEL));
    }

    /**
     * @param indeterminate
     *            the indeterminate to set
     */
    public void setIndeterminate(boolean indeterminate) {
        this.indeterminate = indeterminate;
        fireChanges();
    }

    /**
     * @return the indeterminate
     */
    public boolean isIndeterminate() {
        return indeterminate;
    }

    public void onMessage(MessageEvent event) {
        this.setStatusText(event.getMessage());
    }

    public int compareTo(ProgressController o) {
        if (isFinalizing()) return 1;
        return ((Float) o.getPercent()).compareTo(getPercent());
    }

}
