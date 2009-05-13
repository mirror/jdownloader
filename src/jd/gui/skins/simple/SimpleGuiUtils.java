package jd.gui.skins.simple;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;

import javax.swing.JFrame;

import jd.nutils.Screen;

public class SimpleGuiUtils {

    public static Dimension getLastDimension(Component child, String key) {
        if (key == null) key = child.getName();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = screenSize.width;
        int height = screenSize.height;
        Object loc = SimpleGuiConstants.GUI_CONFIG.getProperty("DIMENSION_OF_" + key);
        if (loc != null && loc instanceof Dimension) {
            Dimension dim = (Dimension) loc;
            if (dim.width > width) dim.width = width;
            if (dim.height > height) dim.height = height;

            return dim;
        }

        return null;
    }

    public static Point getLastLocation(Component parent, String key, Component child) {
        if (key == null) key = child.getName();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = screenSize.width;
        int height = screenSize.height;
        Object loc = SimpleGuiConstants.GUI_CONFIG.getProperty("LOCATION_OF_" + key);
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
        if (parent == null) parent = SimpleGUI.CURRENTGUI;

        component.setLocation(getLastLocation(parent, null, component));
        Dimension dim = getLastDimension(component, null);
        if (dim != null) {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

            dim.width = Math.min(dim.width, screenSize.width);
            dim.height = Math.min(dim.height, screenSize.height);
            component.setSize(dim);
            if (component instanceof JFrame) ((JFrame) component).setExtendedState(SimpleGuiConstants.GUI_CONFIG.getIntegerProperty("MAXIMIZED_STATE_OF_" + component.getName(), JFrame.NORMAL));
        } else {
            component.validate();
        }

    }

    public static void saveLastDimension(Component child, String key) {
        if (SimpleGuiConstants.GUI_CONFIG == null) return;
        if (key == null) key = child.getName();

        SimpleGuiConstants.GUI_CONFIG.setProperty("DIMENSION_OF_" + key, child.getSize());
        if (child instanceof JFrame) SimpleGuiConstants.GUI_CONFIG.setProperty("MAXIMIZED_STATE_OF_" + key, ((JFrame) child).getExtendedState());
        SimpleGuiConstants.GUI_CONFIG.save();
    }

    public static void saveLastLocation(Component parent, String key) {
        if (SimpleGuiConstants.GUI_CONFIG == null) return;
        if (key == null) key = parent.getName();

        if (parent.isShowing()) {
            SimpleGuiConstants.GUI_CONFIG.setProperty("LOCATION_OF_" + key, parent.getLocationOnScreen());
            SimpleGuiConstants.GUI_CONFIG.save();
        }

    }

}
