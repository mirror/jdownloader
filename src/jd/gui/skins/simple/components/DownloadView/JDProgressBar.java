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

package jd.gui.skins.simple.components.DownloadView;

import java.awt.Graphics;

import javax.swing.ImageIcon;
import javax.swing.JProgressBar;

/**
 * Diese Klasse skaliert die Werte der JProgressbar auf Integer ranges herunter
 */
public class JDProgressBar extends JProgressBar {

    private static final long serialVersionUID = 7787146508749392032L;

    private int faktor = 1;

    private ImageIcon icon;

    private long realMax = 0;

    private long realCur = 0;

    public JDProgressBar() {
        super();

    }

    public void setMaximum(int value) {
        setMaximum((long) value);
    }

    public void setMaximum(long value) {
        realMax = value;
        while ((value / faktor) >= Integer.MAX_VALUE) {
            faktor *= 2;
        }

        update();

    }

    private void update() {
        super.setMaximum((int) (realMax / faktor));
        super.setValue((int) (realCur / faktor));        
    }

    public void setValue(int value) {

        setValue((long) value);
    }

    public void setValue(long value) {
        realCur = value;
        while ((value / faktor) >= Integer.MAX_VALUE) {
            faktor *= 2;
        }
        update();
    }

    public long getRealValue() {
        return realCur;
    }

    public long getRealMax() {
        return realMax;
    }

    public void setIcon(ImageIcon hosterIcon) {
        this.icon = hosterIcon;

    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (icon != null) {
            g.drawImage(icon.getImage(), 0, (getHeight() - icon.getIconHeight()) / 2 + 1, null);
        }
    }
}
