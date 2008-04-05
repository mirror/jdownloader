/*
 * $Id: WindowUtils.java,v 1.12 2007/03/16 21:38:07 rbair Exp $
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.jdesktop.swingx.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Encapsulates various utilities for windows (ie: <code>Frame</code> and
 * <code>Dialog</code> objects and descendants, in particular).
 *
 * @author Richard Bair
 */
public final class WindowUtils {
    private static final Logger LOG = Logger.getLogger(WindowUtils.class
            .getName());

    /**
     * Hide the constructor - don't wan't anybody creating an instance of this
     */
    private WindowUtils() {
    }

    /**
     * <p/>
     * Returns the <code>Point</code> at which a window should be placed to
     * center that window on the screen.
     * </p>
     * <p/>
     * Some thought was taken as to whether to implement a method such as this,
     * or to simply make a method that, given a window, will center it.  It was
     * decided that it is better to not alter an object within a method.
     * </p>
     *
     * @param window The window to calculate the center point for.  This object
     *               can not be null.
     *
     * @return the <code>Point</code> at which the window should be placed to
     *         center that window on the screen.
     */
    public static Point getPointForCentering(Window window) {
        //assert window != null;
        try {
            Point mousePoint = MouseInfo.getPointerInfo().getLocation();
            GraphicsDevice[] devices = GraphicsEnvironment
                    .getLocalGraphicsEnvironment().getScreenDevices();
            for (GraphicsDevice device : devices) {
                Rectangle bounds = device.getDefaultConfiguration().getBounds();
                //check to see if the mouse cursor is within these bounds
                if (mousePoint.x >= bounds.x && mousePoint.y >= bounds.y
                    && mousePoint.x <= (bounds.x + bounds.width)
                    && mousePoint.y <= (bounds.y + bounds.height)) {
                    //this is it
                    int screenWidth = bounds.width;
                    int screenHeight = bounds.height;
                    int width = window.getWidth();
                    int height = window.getHeight();
                    return new Point(((screenWidth - width) / 2) + bounds.x,
                                        ((screenHeight - height) / 2) + bounds
                                                .y);
                }
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, e.getLocalizedMessage() +
                                " - this can occur do to a Security exception in sandboxed apps");
        }
        return new Point(0, 0);
    }

    /**
     * <p/>
     * Returns the <code>Point</code> at which a window should be placed to
     * center that window on the given desktop.
     * </p>
     * <p/>
     * Some thought was taken as to whether to implement a method such as this,
     * or to simply make a method that, given a window, will center it.  It was
     * decided that it is better to not alter an object within a method.
     * </p>
     *
     * @param window  The window (JInternalFrame) to calculate the center point
     *                for.  This object can not be null.
     *
     * @return the <code>Point</code> at which the window should be placed to
     *         center that window on the given desktop
     */
    public static Point getPointForCentering(JInternalFrame window) {
        try {
            //assert window != null;
            Point mousePoint = MouseInfo.getPointerInfo().getLocation();
            GraphicsDevice[] devices = GraphicsEnvironment
                    .getLocalGraphicsEnvironment().getScreenDevices();
            for (GraphicsDevice device : devices) {
                Rectangle bounds = device.getDefaultConfiguration().getBounds();
                //check to see if the mouse cursor is within these bounds
                if (mousePoint.x >= bounds.x && mousePoint.y >= bounds.y
                    && mousePoint.x <= (bounds.x + bounds.width)
                    && mousePoint.y <= (bounds.y + bounds.height)) {
                    //this is it
                    int screenWidth = bounds.width;
                    int screenHeight = bounds.height;
                    int width = window.getWidth();
                    int height = window.getHeight();
                    return new Point(((screenWidth - width) / 2) + bounds.x,
                                        ((screenHeight - height) / 2) + bounds
                                                .y);
                }
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, e.getLocalizedMessage() +
                                " - this can occur do to a Security exception in sandboxed apps");
        }
        return new Point(0, 0);
    }

    /**
     * <p/>
     * Returns the <code>Point</code> at which a window should be placed in
     * order to be staggered slightly from another &quot;origin&quot; window to
     * ensure that the title areas of both windows remain visible to the user.
     * </p>
     *
     * @param originWindow Window from which the staggered location will be calculated
     *
     * @return location staggered from the upper left location of the origin
     *         window
     */
    public static Point getPointForStaggering(Window originWindow) {
        Point origin = originWindow.getLocation();
        Insets insets = originWindow.getInsets();
        origin.x += insets.top;
        origin.y += insets.top;
        return origin;
    }

    /**
     * Utility method used to load a GridBagConstraints object (param gbc) with the
     * data in the other parameters.  This method saves code space over doing the
     * assignments by hand, and also allows you to reuse the same GridBagConstraints
     * object reducing temporary object creating (at the expense of a method call.
     * Go figure).
     */
    public static void setConstraints(GridBagConstraints gbc, int gridx,
                                      int gridy, int gridwidth, int gridheight,
                                      double weightx, double weighty,
                                      int anchor, int fill, int top, int left,
                                      int bottom, int right) {
        gbc.gridx = gridx;
        gbc.gridy = gridy;
        gbc.gridwidth = gridwidth;
        gbc.gridheight = gridheight;
        gbc.weightx = weightx;
        gbc.weighty = weighty;
        gbc.anchor = anchor;
        gbc.fill = fill;
        gbc.insets = new Insets(top, left, bottom, right);
    }

    /**
     * Get a <code>Rectangle</code> object representing the given window's position and
     * magnitude in space.
     *
     * @param win The window to get a Rectangle object for
     *
     * @return a Rectangle object.  @see com.jgui.Rectangle
     */
    public static Rectangle getRectangle(Window win) {
        return new Rectangle(win.getY(), win.getX(), win.getWidth(), win.getHeight());
    }

    /**
     * Get a <code>Rectangle</code> object representing the given JComponent's position and
     * magnitude in space.
     *
     * @param comp The JComponent to get a Rectangle object for
     *
     * @return a Rectangle object.  @see com.jgui.Rectangle
     */
    public static Rectangle getRectangle(JComponent comp) {
        return new Rectangle(comp.getY(), comp.getX(), comp.getWidth(), comp.getHeight());
    }

    /**
     * Locates the RootPaneContainer for the given component
     *
     * @param c
     *
     * @return the RootPaneContainer to which the component belongs to
     */
    public static RootPaneContainer findRootPaneContainer(Component c) {
        if (c == null) {
            return null;
        } else if (c instanceof RootPaneContainer) {
            return (RootPaneContainer) c;
        } else {
            return findRootPaneContainer(c.getParent());
        }
    }

    /**
     * Locates the JFrame for the given component
     *
     * @param c
     *
     * @return the JFrame to which the component belongs to
     */
    public static JFrame findJFrame(Component c) {
        if (c == null) {
            return null;
        } else if (c instanceof RootPaneContainer) {
            return (JFrame) c;
        } else {
            return findJFrame(c.getParent());
        }
    }

    /**
     * Locates the JDialog for the given component
     *
     * @param c
     *
     * @return the JDialog to which the component belongs to
     */
    public static JDialog findJDialog(Component c) {
        if (c == null) {
            return null;
        } else if (c instanceof JDialog) {
            return (JDialog) c;
        } else {
            return findJDialog(c.getParent());
        }
    }

    public static Window findWindow(Component c) {
        if (c == null) {
            return JOptionPane.getRootFrame();
        } else if (c instanceof Window) {
            return (Window) c;
        } else {
            return findWindow(c.getParent());
        }
    }

    public static List<Component> getAllComponents(final Container c) {
        Component[] comps = c.getComponents();
        List<Component> compList = new ArrayList<Component>();
        for (Component comp : comps) {
            compList.add(comp);
            if (comp instanceof Container) {
                compList.addAll(getAllComponents((Container) comp));
            }
        }
        return compList;
    }

    public static void setFontRecursively(Container c, Font font) {
        for (Component comp : getAllComponents(c)) {
            comp.setFont(font);
        }
    }

    /**
     * Installs/resets a ComponentListener to resize the
     * given window to minWidth/Height if needed.
     *
     * @param window
     * @param minWidth
     * @param minHeight
     */
    public static void setMinimumSizeManager(Window window, int minWidth,
                                             int minHeight) {
        ComponentListener[] listeners = window.getComponentListeners();
        ComponentListener listener = null;
        for (ComponentListener l : listeners) {
            if (l instanceof MinSizeComponentListener) {
                listener = l;
                break;
            }
        }
        if (listener == null) {
            window.addComponentListener(new MinSizeComponentListener(
                    window, minWidth, minHeight));
        } else {
            ((MinSizeComponentListener) listener).resetSizes(minWidth,
                                                             minHeight);
        }
    }

    /**
     * Resets window size to minSize if needed.
     *
     * @author Patrick Wright
     */
    public static class MinSizeComponentListener extends ComponentAdapter {
        private Window window;
        private int minHeight;
        private int minWidth;

        MinSizeComponentListener(Window frame, int minWidth, int minHeight) {
            this.window = frame;
            resetSizes(minWidth, minHeight);
        }

        public void resetSizes(int minWidth, int minHeight) {
            this.minWidth = minWidth;
            this.minHeight = minHeight;
            adjustIfNeeded(window);
        }

        @Override
        public void componentResized(java.awt.event.ComponentEvent evt) {
            adjustIfNeeded((Window) evt.getComponent());
        }

        private void adjustIfNeeded(final Window window) {
            boolean doSize = false;
            int newWidth = window.getWidth();
            int newHeight = window.getHeight();
            if (newWidth < minWidth) {
                newWidth = minWidth;
                doSize = true;
            }
            if (newHeight < minHeight) {
                newHeight = minHeight;
                doSize = true;
            }
            if (doSize) {
                final int w = newWidth;
                final int h = newHeight;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        window.setSize(w, h);
                    }
                });
            }
        }
    }
}
