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

package jd.gui.swing.jdgui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;

import javax.swing.JFrame;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;

public class GUIUtils {
    public static final Storage STORAGE = JSonStorage.getPlainStorage("gui.windows.dimensionsandlocations");

    public static Dimension getLastDimension(Component child) {
        String key = child.getName();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = screenSize.width;
        int height = screenSize.height;
        Integer x = STORAGE.get("dimension." + key + ".width", -1);
        Integer y = STORAGE.get("dimension." + key + ".height", -1);

        if (x >= 0 && y > 0) {
            if (x > width) x = width;
            if (y > height) y = height;

            return new Dimension(x, y);
        }

        return null;
    }

    /**
     * Gets the screen device for absolute point x,y
     * 
     * @param x
     * @param y
     * @return
     */
    public static GraphicsDevice getScreenDevice(int x, int y) {
        // search screen device for this location
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice[] screens = ge.getScreenDevices();

        for (final GraphicsDevice screen : screens) {
            final Rectangle bounds = screen.getDefaultConfiguration().getBounds();
            if (x >= bounds.x && x < bounds.x + bounds.width) {
                if (y >= bounds.y && y < bounds.y + bounds.height) {
                    //
                    return screen;

                }

            }

        }
        return null;
    }

    public static void saveLastLocation(Component parent) {
        String key = parent.getName();

        // do not save location if frame is not in normal state
        if (parent instanceof JFrame && ((JFrame) parent).getExtendedState() != JFrame.NORMAL) {

        return; }
        if (parent.isShowing()) {
            STORAGE.put("location." + key + ".x", parent.getLocationOnScreen().x);
            STORAGE.put("location." + key + ".y", parent.getLocationOnScreen().y);
        } else {
            System.out.println("Not showing");
        }
    }
}
