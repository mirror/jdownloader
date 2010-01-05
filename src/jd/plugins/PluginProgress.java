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

public class PluginProgress {

    private long total;

    private long current;

    private Color color;

    public PluginProgress(final long current, final long total, final Color color) {
        this.total = total;
        this.current = current;
        this.color = color;
    }

    public Color getColor() {
        return color;
    }

    public long getCurrent() {
        return current;
    }

    public double getPercent() {
        return Math.round((current * 10000.0) / total) / 100.0;
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

}
