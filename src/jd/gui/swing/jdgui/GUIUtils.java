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
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;

import javax.swing.JFrame;

import jd.gui.swing.SwingGui;
import jd.nutils.Screen;

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

    public static Point getLastLocation(Component parent, Component child) {
        String key = child.getName();

        if (STORAGE.hasProperty("location." + key + ".x") && STORAGE.hasProperty("location." + key + ".y")) {
            Integer x = STORAGE.get("location." + key + ".x", -1);
            Integer y = STORAGE.get("location." + key + ".y", -1);

            GraphicsDevice screen = getScreenDevice(x, y);
            if (screen != null) return new Point(x, y);
        }
        if (parent != null) {
            Point center = Screen.getCenterOfComponent(parent, child);
            GraphicsDevice screen = getScreenDevice(center.x, center.y);
            if (screen != null) return center;
        }
        return Screen.getCenterOfComponent(null, child);
    }

    /**
     * Gets the screen device for absolute point x,y
     * 
     * @param x
     * @param y
     * @return
     */
    private static GraphicsDevice getScreenDevice(int x, int y) {
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

    public static void restoreWindow(JFrame parent, Component component) {
        if (parent == null) parent = SwingGui.getInstance().getMainFrame();

        component.setLocation(getLastLocation(parent, component));
        Dimension dim = getLastDimension(component);
        if (dim != null) {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

            dim.width = Math.min(dim.width, screenSize.width);
            dim.height = Math.min(dim.height, screenSize.height);
            component.setSize(dim);
            if (component instanceof JFrame) {
                ((JFrame) component).setExtendedState(STORAGE.get("extendedstate." + component.getName(), JFrame.NORMAL));
            }
        } else {
            component.validate();
        }

    }

    public static void saveLastDimension(Component child) {
        String key = child.getName();

        boolean max = false;
        if (child instanceof JFrame) {
            STORAGE.put("extendedstate." + key, ((JFrame) child).getExtendedState());

            if (((JFrame) child).getExtendedState() != JFrame.NORMAL) {
                max = true;
            }
        }
        // do not save dimension if frame is not in normal state
        if (!max) {
            STORAGE.put("dimension." + key + ".width", child.getSize().width);
            STORAGE.put("dimension." + key + ".height", child.getSize().height);
        }

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
