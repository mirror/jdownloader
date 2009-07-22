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

package jd.gui.skins.jdgui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Toolkit;

import javax.swing.JFrame;

import jd.config.SubConfiguration;
import jd.gui.skins.SwingGui;
import jd.nutils.Screen;

public class GUIUtils {

    public static Dimension getLastDimension(Component child, String key) {
        if (key == null) key = child.getName();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = screenSize.width;
        int height = screenSize.height;
        SubConfiguration cfg = getConfig();
        Object loc = getConfig().getProperty("DIMENSION_OF_" + key);
        if (loc != null && loc instanceof Dimension) {
            Dimension dim = (Dimension) loc;
            if (dim.width > width) dim.width = width;
            if (dim.height > height) dim.height = height;

            return dim;
        }

        return null;
    }

    /**
     * Returns the gui subconfiguration
     * 
     * @return
     */
    public static SubConfiguration getConfig() {

        return SubConfiguration.getConfig(JDGuiConstants.CONFIG_PARAMETER);
    }

    public static Point getLastLocation(Component parent, String key, Component child) {
        if (key == null) key = child.getName();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = screenSize.width;
        int height = screenSize.height;
        Object loc = getConfig().getProperty("LOCATION_OF_" + key);
        if (loc != null && loc instanceof Point) {
            Point point = (Point) loc;
            if (point.x < 0) point.x = 0;
            if (point.y < 0) point.y = 0;
            if (point.x > width) point.x = width;
            if (point.y > height) point.y = height;

            return point;
        }

        return Screen.getCenterOfComponent(parent, child);
    }

    public static void restoreWindow(JFrame parent, Component component) {
        if (parent == null) parent = SwingGui.getInstance();

        component.setLocation(getLastLocation(parent, null, component));
        Dimension dim = getLastDimension(component, null);
        if (dim != null) {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

            dim.width = Math.min(dim.width, screenSize.width);
            dim.height = Math.min(dim.height, screenSize.height);
            component.setSize(dim);
            if (component instanceof JFrame) ((JFrame) component).setExtendedState(getConfig().getIntegerProperty("MAXIMIZED_STATE_OF_" + component.getName(), JFrame.NORMAL));
        } else {
            component.validate();
        }

    }

    public static void saveLastDimension(Component child, String key) {
        if (getConfig() == null) return;
        if (key == null) key = child.getName();

        boolean max = false;
        if (child instanceof JFrame) {
            getConfig().setProperty("MAXIMIZED_STATE_OF_" + key, ((JFrame) child).getExtendedState());
            if (((JFrame) child).getExtendedState() != Frame.NORMAL) {
                max = true;
            }
        }
        // do not save dimension if frame is not in normal state
        if (!max) getConfig().setProperty("DIMENSION_OF_" + key, child.getSize());
        getConfig().save();
    }

    public static void saveLastLocation(Component parent, String key) {
        if (getConfig() == null) return;
        if (key == null) key = parent.getName();
        // don not save location if frame is not in normal state
        if (parent instanceof JFrame && ((JFrame) parent).getExtendedState() != Frame.NORMAL) return;
        if (parent.isShowing()) {
            getConfig().setProperty("LOCATION_OF_" + key, parent.getLocationOnScreen());
            getConfig().save();
        }

    }

}
