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
import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.plaf.ProgressBarUI;
import javax.swing.plaf.basic.BasicProgressBarUI;

/**
 * Diese Klasse skaliert die Werte der JProgressbar auf Integer ranges herunter
 */
public class JDProgressBar extends JProgressBar {

    private static final long serialVersionUID = 7787146508749392032L;

    private int faktor = 1;

    private ImageIcon icon;



    public JDProgressBar() {
        super();

    }

    public void setMaximum(long value) {
        while ((value / faktor) >= Integer.MAX_VALUE) {
            increaseFaktor();
        }
        this.setMaximum((int) (value / faktor));
    }

    public void setValue(long value) {
        while ((value / faktor) >= Integer.MAX_VALUE) {
            increaseFaktor();
        }
        this.setValue((int) (value / faktor));
    }

    private void increaseFaktor() {
        faktor += 2;
        this.setValue(getValue() / 2);
        this.setMaximum(getMaximum() / 2);
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
