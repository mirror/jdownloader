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

package jd.nutils;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;

import org.appwork.utils.swing.EDTHelper;

public class Screen {

    /**
     * Liefert einen Punkt zurueck, mit dem eine Komponente auf eine andere zentriert werden kann
     * 
     * @param parent
     *            Die Komponente, an der ausgerichtet wird
     * @param child
     *            Die Komponente die ausgerichtet werden soll
     * @return Ein Punkt, mit dem diese Komponente mit der setLocation Methode zentriert dargestellt werden kann
     */
    public static Point getCenterOfComponent(final Component parent, Component child) {
        Point center = new EDTHelper<Point>() {

            @Override
            public Point edtRun() {
                if (parent != null && parent.isShowing()) { return parent.getLocationOnScreen(); }
                return null;
            }

        }.getReturnValue();

        if (center == null) {
            // use default screen device instead of toolkit screensizes. This
            // should have the same behaviour on all systems
            final GraphicsDevice ge = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

            Rectangle screenSize = ge.getDefaultConfiguration().getBounds();
            int width = screenSize.width;
            int height = screenSize.height;
            center = new Point(screenSize.x + width / 2, screenSize.y + height / 2);
        } else {
            center.x += parent.getWidth() / 2;
            center.y += parent.getHeight() / 2;
        }
        // Dann Auszurichtende Komponente in die Berechnung einfliessen lassen
        center.x -= child.getWidth() / 2;
        center.y -= child.getHeight() / 2;
        return center;
    }

    public static Point getDockBottomRight(Component child) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        return new Point((int) (screenSize.getWidth() - child.getWidth()), (int) (screenSize.getHeight() - child.getHeight() - 60));
    }

}
