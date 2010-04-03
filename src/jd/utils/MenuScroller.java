package jd.utils;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 * A class that provides scrolling capabilities to a long menu dropdown or popup
 * menu.
 * <P>
 * <B>Implementation note:</B> The default number of items to display at a time
 * is calculated at runtime depends on screen height, and the default scrolling
 * interval is 150 milliseconds.
 * <P>
 * 
 * Based on original code of <a
 * href="http://tips4java.wordpress.com/2009/02/01/menu-scroller">Darryl</a>
 * 
 * @author baongoc124
 */
public class MenuScroller {

    private JMenu menu;
    private JPopupMenu popupMenu;
    private Component[] menuItems;
    private MenuScrollItem upItem = new MenuScrollItem(MenuIcon.UP, -1);
    private MenuScrollItem downItem = new MenuScrollItem(MenuIcon.DOWN, +1);
    private final MenuScrollListener menuListener = new MenuScrollListener();
    private int interval = 150;
    private int firstIndex = 0;
    private int scrollCount;

    /**
     * Registers a menu to be scrolled with the default scrolling interval.
     * <P>
     * 
     * @param menu
     *            the menu
     * @return the MenuScroller
     */
    public static MenuScroller setScrollerFor(JMenu menu) {
        return new MenuScroller(menu);
    }

    /**
     * Registers a popup menu to be scrolled with the default scrolling
     * interval.
     * <P>
     * 
     * @param menu
     *            the popup menu
     * @return the MenuScroller
     */
    public static MenuScroller setScrollerFor(JPopupMenu menu) {
        return new MenuScroller(menu);
    }

    /**
     * Registers a menu to be scrolled, with the specified scrolling interval.
     * <P>
     * 
     * @param menu
     *            the menu
     * @param interval
     *            the scroll interval, in milliseconds
     * @return the MenuScroller
     * @throws IllegalArgumentException
     *             if interval is 0 or negative
     */
    public static MenuScroller setScrollerFor(JMenu menu, int interval) {
        return new MenuScroller(menu, interval);
    }

    /**
     * Registers a popup menu to be scrolled, with the specified scrolling
     * interval.
     * <P>
     * 
     * @param menu
     *            the popup menu
     * @param interval
     *            the scroll interval, in milliseconds
     * @return the MenuScroller
     * @throws IllegalArgumentException
     *             if interval is 0 or negative
     */
    public static MenuScroller setScrollerFor(JPopupMenu menu, int interval) {
        return new MenuScroller(menu, interval);
    }

    /**
     * Constructs a <code>MenuScroller</code> that scrolls a menu with default
     * scrolling interval.
     * <P>
     * 
     * @param menu
     *            the menu
     */
    public MenuScroller(JMenu menu) {
        this.menu = menu;
        menu.addMenuListener(menuListener);
    }

    /**
     * Constructs a <code>MenuScroller</code> that scrolls a popup menu with the
     * default scrolling interval.
     * <P>
     * 
     * @param menu
     *            the popup menu
     */
    public MenuScroller(JPopupMenu menu) {
        this.popupMenu = menu;
        menu.addPopupMenuListener(menuListener);
    }

    /**
     * Constructs a <code>MenuScroller</code> that scrolls a menu with the
     * specified scrolling interval.
     * <P>
     * 
     * @param menu
     *            the menu
     * @param scrollCount
     *            the number of items to display in the scrolling portion
     * @param interval
     *            the scroll interval, in milliseconds
     * @throws IllegalArgumentException
     *             if interval is 0 or negative
     */
    public MenuScroller(JMenu menu, int interval) {
        this(menu);
        setValues(interval);
    }

    /**
     * Constructs a <code>MenuScroller</code> that scrolls a popup menu with the
     * specified scrolling interval.
     * <P>
     * 
     * @param menu
     *            the popup menu
     * @param scrollCount
     *            the number of items to display in the scrolling portion
     * @param interval
     *            the scroll interval, in milliseconds
     * @throws IllegalArgumentException
     *             if interval is 0 or negative
     */
    public MenuScroller(JPopupMenu menu, int interval) {
        this(menu);
        setValues(interval);
    }

    private void setValues(int interval) {
        if (interval <= 0) { throw new IllegalArgumentException("interval must be greater than 0"); }

        setInterval(interval);
    }

    /**
     * Returns the scroll interval in milliseconds
     * <P>
     * 
     * @return the scroll interval in milliseconds
     */
    public int getInterval() {
        return interval;
    }

    /**
     * Sets the scroll interval in milliseconds
     * <P>
     * 
     * @param interval
     *            the scroll interval in milliseconds
     * @throws IllegalArgumentException
     *             if interval is 0 or negative
     */
    public void setInterval(int interval) {
        if (interval <= 0) { throw new IllegalArgumentException("interval must be greater than 0"); }
        upItem.setInterval(interval);
        downItem.setInterval(interval);
        this.interval = interval;
    }

    /**
     * Removes this MenuScroller from the associated menu and restores the
     * default behavior of the menu.
     */
    public void dispose() {
        if (menu != null) {
            menu.removeMenuListener(menuListener);
            menu = null;
        }

        if (popupMenu != null) {
            popupMenu.removePopupMenuListener(menuListener);
            popupMenu = null;
        }
    }

    /**
     * Ensures that the <code>dispose</code> method of this MenuScroller is
     * called when there are no more refrences to it.
     * <P>
     * 
     * @exception Throwable
     *                if an error occurs.
     * @see MenuScroller#dispose()
     */
    @Override
    public void finalize() throws Throwable {
        dispose();
    }

    private void setMenuItems() {
        if (menu != null) {
            menuItems = menu.getMenuComponents();
        }
        if (popupMenu != null) {
            menuItems = popupMenu.getComponents();
        }

        int maxItemHeight = 0;
        for (Component c : menuItems)
            maxItemHeight = Math.max(c.getPreferredSize().height, maxItemHeight);
        int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
        scrollCount = screenHeight / maxItemHeight - 2;

        if (scrollCount < menuItems.length) refreshMenu();
    }

    private void restoreMenuItems() {
        JComponent container = menu == null ? popupMenu : menu;
        container.removeAll();
        for (Component component : menuItems) {
            container.add(component);
        }
    }

    private void refreshMenu() {
        upItem.setEnabled(firstIndex > 0);
        downItem.setEnabled(firstIndex + scrollCount < menuItems.length);

        JComponent container = menu == null ? popupMenu : menu;
        container.removeAll();

        container.add(upItem);

        for (int i = firstIndex; i < scrollCount + firstIndex; i++) {
            container.add(menuItems[i]);
        }
        container.add(downItem);

        JComponent parent = (JComponent) upItem.getParent();
        parent.revalidate();
        parent.repaint();
    }

    private class MenuScrollItem extends JMenuItem implements ChangeListener {
        private static final long serialVersionUID = -5023847175836525717L;
        private MenuScrollTimer timer;

        public MenuScrollItem(MenuIcon icon, int increment) {
            setIcon(icon);
            setDisabledIcon(icon);
            timer = new MenuScrollTimer(increment, interval);
            addChangeListener(this);
        }

        public void setInterval(int interval) {
            timer.setDelay(interval);
        }

        public void stateChanged(ChangeEvent e) {
            if (isArmed() && !timer.isRunning()) {
                timer.start();
            }
            if (!isArmed() && timer.isRunning()) {
                timer.stop();
            }
        }
    }

    private class MenuScrollTimer extends Timer {
        private static final long serialVersionUID = -6611450143671071887L;

        public MenuScrollTimer(final int increment, int interval) {
            super(interval, new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    firstIndex += increment;
                    refreshMenu();
                }
            });
        }
    }

    private class MenuScrollListener implements MenuListener, PopupMenuListener {

        // MenuListener methods
        public void menuSelected(MenuEvent e) {
            setMenuItems();
        }

        public void menuDeselected(MenuEvent e) {
            restoreMenuItems();
        }

        public void menuCanceled(MenuEvent e) {
            restoreMenuItems();
        }

        // PopupMenuListener methods
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            setMenuItems();
        }

        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            restoreMenuItems();
        }

        public void popupMenuCanceled(PopupMenuEvent e) {
            restoreMenuItems();
        }
    }

    private static enum MenuIcon implements Icon {

        UP(9, 1, 9), DOWN(1, 9, 1);
        final int[] xPoints = { 1, 5, 9 };
        final int[] yPoints;

        MenuIcon(int... yPoints) {
            this.yPoints = yPoints;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Dimension size = c.getSize();
            Graphics g2 = g.create(size.width / 2 - 5, size.height / 2 - 5, 10, 10);
            g2.setColor(Color.GRAY);
            g2.drawPolygon(xPoints, yPoints, 3);
            if (c.isEnabled()) {
                g2.setColor(Color.BLACK);
                g2.fillPolygon(xPoints, yPoints, 3);
            }
            g2.dispose();
        }

        public int getIconWidth() {
            return 0;
        }

        public int getIconHeight() {
            return 10;
        }
    }
}
