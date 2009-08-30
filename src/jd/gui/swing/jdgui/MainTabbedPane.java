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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.interfaces.SwitchPanelEvent;
import jd.gui.swing.jdgui.interfaces.View;
import jd.gui.swing.jdgui.maintab.ClosableTabHeader;
import jd.gui.swing.jdgui.views.ClosableView;
import jd.utils.JDUtilities;

public class MainTabbedPane extends JTabbedPane implements MouseListener {

    private static final long serialVersionUID = -1531827591735215594L;
    private static MainTabbedPane INSTANCE;

    public synchronized static MainTabbedPane getInstance() {
        if (INSTANCE == null) INSTANCE = new MainTabbedPane();
        return INSTANCE;
    }

    @Override
    public void remove(Component component) {
        throw new RuntimeException(" This method is not allowed");
    }

    @Override
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        boolean ret = super.processKeyBinding(ks, e, condition, pressed);

        if (getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(ks) != null) { return false; }
        if (getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(ks) != null) { return false; }
        return ret;
    }

    public void remove(View view) {
        super.remove(view);
        if (view != null) view.getBroadcaster().fireEvent(new SwitchPanelEvent(view, SwitchPanelEvent.ON_REMOVE));
        if (getTabCount() > 0) setSelectedComponent(getComponentAt(0));
    }

    protected View latestSelection;

    public void addTab(View view) {
        SwingGui.checkEDT();
        if (view instanceof ClosableView) {
            addClosableTab((ClosableView) view);
        } else {
            super.addTab(view.getTitle(), view.getIcon(), view, view.getTooltip());

            view.getBroadcaster().fireEvent(new SwitchPanelEvent(view, SwitchPanelEvent.ON_ADD));

            this.setFocusable(false);
        }

    }

    private void addClosableTab(ClosableView view) {

        if (JDUtilities.getJavaVersion() >= 1.6) {
            super.addTab(view.getTitle(), view.getIcon(), view, view.getTooltip());
            view.getBroadcaster().fireEvent(new SwitchPanelEvent(view, SwitchPanelEvent.ON_ADD));

            this.setTabComponentAt(this.getTabCount() - 1, new ClosableTabHeader(view));

        } else {
            super.addTab(view.getTitle(), new CloseTabIcon(view.getIcon()), view, view.getTooltip());
            view.getBroadcaster().fireEvent(new SwitchPanelEvent(view, SwitchPanelEvent.ON_ADD));
        }
        this.setFocusable(false);
    }

    private MainTabbedPane() {

        this.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        this.setOpaque(false);
        if (JDUtilities.getJavaVersion() < 1.6) {
            addMouseListener(this);
        }

        this.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                if (SwingGui.getInstance() != null) SwingGui.getInstance().setWaiting(true);
                try {
                    View comp = (View) getSelectedComponent();
                    if (comp == latestSelection) return;
                    if (latestSelection != null) {
                        latestSelection.setHidden();
                    }
                    latestSelection = comp;
                    comp.setShown();
                    revalidate();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }

            }

        });
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (SwingGui.getInstance() != null) SwingGui.getInstance().setWaiting(false);
    }

    /**
     * gets called form the main frame if it gets closed
     */
    public void onClose() {
        getSelectedView().setHidden();
    }

    /**
     * returns the currently selected View
     */
    public View getSelectedView() {
        SwingGui.checkEDT();
        return (View) super.getSelectedComponent();
    }

    @Override
    public void setSelectedComponent(Component e) {
        SwingGui.checkEDT();
        super.setSelectedComponent(getComponentEquals((View) e));
    }

    /**
     * returns the component in this tab that equals view
     * 
     * @param view
     * @return
     */
    public View getComponentEquals(View view) {
        SwingGui.checkEDT();
        for (int i = 0; i < this.getTabCount(); i++) {
            Component c = this.getComponentAt(i);
            if (c.equals(view)) return (View) c;
        }
        return null;
    }

    /**
     * CHecks if there is already a tabbepanel of this type in this pane.
     * 
     * @param view
     * @return
     */
    public boolean contains(View view) {
        for (int i = 0; i < this.getTabCount(); i++) {
            Component c = this.getComponentAt(i);
            if (c.equals(view)) return true;
        }
        return false;
    }

    public void mouseClicked(MouseEvent e) {
        int tabNumber = getUI().tabForCoordinate(this, e.getX(), e.getY());
        if (tabNumber < 0) return;
        Rectangle rect = ((CloseTabIcon) getIconAt(tabNumber)).getBounds();
        try {
            if (rect.contains(e.getX(), e.getY())) {
                // the tab is being closed
                ((ClosableView) this.getComponentAt(tabNumber)).getCloseAction().actionPerformed(null);

            }
        } catch (Exception e2) {

        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    /**
     * The class which generates the 'X' icon for the tabs. The constructor
     * accepts an icon which is extra to the 'X' icon, so you can have tabs like
     * in JBuilder. This value is null if no extra icon is required.
     */
    class CloseTabIcon implements Icon {
        private int x_pos;
        private int y_pos;
        private int width;
        private int height;
        private Icon fileIcon;

        public CloseTabIcon(Icon fileIcon) {
            this.fileIcon = fileIcon;
            width = 16;
            height = 16;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            this.x_pos = x;
            this.y_pos = y;
            Icon ic = UIManager.getIcon("InternalFrame.closeIcon");
            int y_p = y + 2;

            Color col;
            int w, h;
            if (ic == null) {
                // draw close X
                col = g.getColor();

                g.drawLine(x + 3, y_p + 3, x + 10, y_p + 10);
                g.drawLine(x + 3, y_p + 4, x + 9, y_p + 10);
                g.drawLine(x + 4, y_p + 3, x + 10, y_p + 9);
                g.drawLine(x + 10, y_p + 3, x + 3, y_p + 10);
                g.drawLine(x + 10, y_p + 4, x + 4, y_p + 10);
                g.drawLine(x + 9, y_p + 3, x + 3, y_p + 9);
                g.setColor(col);
                w = 13;
                h = 13;
            } else {
                // use icon
                ic.paintIcon(c, g, x - 1, y_p - 1);
                w = ic.getIconWidth() - 3;
                h = ic.getIconHeight() - 3;
            }
            // draw border
            col = g.getColor();

            // g.setColor(Color.black);

            // --- <<
            // | |
            // ---
            g.drawLine(x + 1, y_p, x + w - 1, y_p);
            // ---
            // | |
            // --- <<
            g.drawLine(x + 1, y_p + h, x + w - 1, y_p + h);
            // ---
            // >> | |
            // ---
            g.drawLine(x, y_p + 1, x, y_p + h - 1);
            // ---
            // | | <<
            // ---
            g.drawLine(x + w, y_p + 1, x + w, y_p + h - 1);
            g.setColor(col);

            if (fileIcon != null) {
                fileIcon.paintIcon(c, g, x + width + 2, y_p);
            }
        }

        public int getIconWidth() {
            return width + (fileIcon != null ? fileIcon.getIconWidth() : 0);
        }

        public int getIconHeight() {
            return height;
        }

        public Rectangle getBounds() {
            return new Rectangle(x_pos, y_pos, width, height);
        }
    }

}
