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
package jd.plugins;

import java.awt.Color;

import javax.swing.Icon;

import org.jdownloader.gui.views.downloads.columns.ETAColumn;
import org.jdownloader.plugins.PluginTaskID;

public abstract class PluginProgress {
    protected volatile long total                          = 0;
    protected volatile long current                        = 0;
    protected volatile long eta                            = -1;
    protected Color         color;
    protected Icon          icon                           = null;
    protected Object        progressSource                 = null;
    protected final long    startedTimestamp;
    private boolean         displayInProgressColumnEnabled = true;

    public void setDisplayInProgressColumnEnabled(boolean displayInProgressColumnEnabled) {
        this.displayInProgressColumnEnabled = displayInProgressColumnEnabled;
    }

    public abstract String getMessage(Object requestor);

    public abstract PluginTaskID getID();

    public PluginProgress(final long current, final long total, final Color color) {
        this.total = total;
        this.current = current;
        this.color = color;
        startedTimestamp = System.currentTimeMillis();
    }

    public Color getColor() {
        return color;
    }

    public long getCurrent() {
        return current;
    }

    public double getPercent() {
        return Math.round((getCurrent() * 10000.0) / getTotal()) / 100.0;
    }

    public long getTotal() {
        return total;
    }

    public void setColor(final Color color) {
        this.color = color;
    }

    public void setCurrent(final long current) {
        this.current = current;
    }

    public void setTotal(final long total) {
        this.total = total;
    }

    public void updateValues(final long current, final long total) {
        this.current = current;
        this.total = total;
        // try to calculate the eta
        long runtime = System.currentTimeMillis() - startedTimestamp;
        if (runtime > 0) {
            double speed = current / (double) runtime;
            if (speed > 0) {
                setETA((long) ((total - current) / speed));
            } else {
                setETA(-1);
            }
        }
    }

    public Icon getIcon(Object requestor) {
        if (requestor instanceof ETAColumn) {
            return null;
        }
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    public Object getProgressSource() {
        return progressSource;
    }

    public void setProgressSource(Object progressSource) {
        this.progressSource = progressSource;
    }

    /**
     * @return the eTA
     */
    public long getETA() {
        return eta;
    }

    /**
     * @param eTA
     *            the eTA to set
     */
    public void setETA(long eTA) {
        eta = eTA;
    }

    public long getStarted() {
        return startedTimestamp;
    }

    public void abort() {
    }

    public boolean isDisplayInProgressColumnEnabled() {
        return displayInProgressColumnEnabled;
    }
}
