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

import jd.event.ControlEvent;
import jd.utils.JDUtilities;

/**
 * Diese Klasse kann dazu verwendet werden einen Fortschritt in der GUI
 * anzuzeigen. Sie bildet dabei die schnittstelle zwischen Interactionen,
 * plugins etc und der GUI
 * 
 * @author JD-Team
 * 
 */
public class ProgressController {

    private static int idCounter = 0;
    private long currentValue;
    private boolean finished;

    private int id;

    private long max;

    private Object source;
    private String statusText;
    private Color progresscolor;
    private String progressText;

    public ProgressController(String name) {
        this(name, 100l);
        progressText = null;
    }

    public ProgressController(String name, long max) {
        id = idCounter++;
        this.max = max;
        statusText = "init " + name;
        currentValue = 0;
        finished = false;
        progresscolor = null;
        progressText = null;
        fireChanges();
    }

    public void setColor(Color color) {
        progresscolor = color;
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

    @Override
    public void finalize() {
        finished = true;
        currentValue = max;
        JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ON_PROGRESS, source));

    }

    public void finalize(long timer) {
        this.setRange(timer);
        while (timer > 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {

            }
            timer -= 1000;
            this.increase(1000l);
        }
        finished = true;
        currentValue = max;
        JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ON_PROGRESS, source));
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

    public double getPercent() {
        long range = max;
        long current = currentValue;
        return (double) current / (double) range;
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

    public void increase(long i) {
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

    public void setProgressText(String text) {
        progressText = text;
        fireChanges();
    }

    public String getProgressText() {
        return progressText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
        fireChanges();
    }

    @Override
    public String toString() {
        return "ProgressController " + id;
    }
}
