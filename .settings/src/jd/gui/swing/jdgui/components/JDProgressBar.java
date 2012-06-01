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

package jd.gui.swing.jdgui.components;

import javax.swing.JProgressBar;

/**
 * Diese Klasse skaliert die Werte der JProgressbar auf Integer ranges herunter
 */
public class JDProgressBar extends JProgressBar {

    private static final long serialVersionUID = 7787146508749392032L;

    private int scale = 1;

    private long realMax = 0;

    private long realCur = 0;

    private boolean autofaktor = true;

    public JDProgressBar() {
        super();
        this.setIndeterminate(false);
    }

    @Override
    public void setMaximum(int value) {
        setMaximum((long) value);
    }

    public void setMaximum(long value) {
        realMax = value;
        update();
    }

    /** enable/disable autoscaling */
    public void setAutoScaling(boolean b) {
        autofaktor = b;
    }

    public void setScale(int f) {
        scale = f;
    }

    public int getScale() {
        return scale;
    }

    private void update() {
        long biggest = Math.max(realMax, realCur);
        if (autofaktor) {
            scale = 1;
            while ((biggest / scale) >= Integer.MAX_VALUE) {
                scale *= 2;
            }
        }
        super.setMaximum((int) (realMax / scale));
        super.setValue((int) (realCur / scale));
    }

    @Override
    public void setValue(int value) {
        setValue((long) value);
    }

    public void setValue(long value) {
        realCur = value;
        update();
    }

    public long getRealValue() {
        return realCur;
    }

    public long getRealMax() {
        return realMax;
    }

}
