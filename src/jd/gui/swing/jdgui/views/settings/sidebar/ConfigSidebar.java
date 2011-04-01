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

package jd.gui.swing.jdgui.views.settings.sidebar;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionListener;

import jd.controlling.JDController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.laf.LookAndFeelController;
import net.miginfocom.swing.MigLayout;

public class ConfigSidebar extends JPanel implements ControlListener, MouseMotionListener, MouseListener {

    private static final long serialVersionUID = 6456662020047832983L;

    private JList             list;

    private Point             mouse;

    public ConfigSidebar() {
        super(new MigLayout("ins 0", "[grow,fill]", "[grow,fill]"));
        JDController.getInstance().addControlListener(this);
        list = new JList() {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {

                if (mouse != null) {
                    final Graphics2D g2 = (Graphics2D) g;
                    final AlphaComposite ac5 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f);
                    g2.setComposite(ac5);
                    g2.fillRect(0, (mouse.y / 50) * 50, list.getWidth(), 50);

                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                }
                super.paintComponent(g);
            }

            // public Dimension getPreferredScrollableViewportSize() {
            //
            // return this.getPreferredSize();
            // }
            //
            // public int getScrollableBlockIncrement(final Rectangle
            // visibleRect, final int orientation, final int direction) {
            // return Math.max(visibleRect.height * 9 / 10, 1);
            // }
            //
            // public boolean getScrollableTracksViewportHeight() {
            //
            // return false;
            // }
            //
            // public boolean getScrollableTracksViewportWidth() {
            // return true;
            // }
            //
            // public int getScrollableUnitIncrement(final Rectangle
            // visibleRect, final int orientation, final int direction) {
            // return Math.max(visibleRect.height / 10, 1);
            // }
        };
        list.addMouseMotionListener(this);
        list.addMouseListener(this);
        list.setModel(new ConfigListModel());
        list.setCellRenderer(new TreeRenderer());
        list.setOpaque(false);
        list.setBackground(null);

        setBackground(null);
        setOpaque(false);
        JScrollPane sp;
        this.add(sp = new JScrollPane(list));

        int c = LookAndFeelController.getInstance().getLAFOptions().getPanelBackgroundColor();
        if (c >= 0) {
            list.setBackground(new Color(c));
            list.setOpaque(true);
        }
        sp.setBorder(null);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        // add(new JScrollPane());

    }

    public void addListener(ListSelectionListener x) {
        list.getSelectionModel().addListSelectionListener(x);
    }

    // @Override
    // public void setOpaque(boolean isOpaque) {
    // // super.setOpaque(isOpaque);
    // }

    // public boolean isOpaque() {
    // return true;
    // }

    public void controlEvent(ControlEvent event) {
        if (event.getEventID() == ControlEvent.CONTROL_SYSTEM_EXIT) {
            saveCurrentState();
        }
    }

    public void setSelectedTreeEntry(Class<?> class1) {
        for (int i = 0; i < list.getModel().getSize(); i++) {
            if (class1 == list.getModel().getElementAt(i).getClass()) {
                list.setSelectedIndex(i);
                break;
            }
        }
    }

    /**
     * Saves the selected ConfigPanel
     */
    private void saveCurrentState() {
        /* getPanel is null in case the user selected a rootnode */
        // SwitchPanel panel = ((TreeEntry)
        // tree.getLastSelectedPathComponent()).getPanel();
        // if (panel == null) return;
        // GUIUtils.getConfig().setProperty(PROPERTY_LAST_PANEL,
        // panel.getClass().getName());
        // GUIUtils.getConfig().save();
    }

    /**
     * Updates the Addon subtree
     */
    public void updateAddons() {

    }

    public void mouseDragged(MouseEvent e) {
        mouse = e.getPoint();
        list.repaint();

    }

    public void mouseMoved(MouseEvent e) {
        mouse = e.getPoint();
        list.repaint();
    }

    public void mouseClicked(MouseEvent e) {
        // mouse = null;
        // list.repaint();
    }

    public void mousePressed(MouseEvent e) {
        // mouse = null;
        // list.repaint();
    }

    public void mouseReleased(MouseEvent e) {
        // mouse = null;
        // list.repaint();
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
        mouse = null;
        list.repaint();
    }

    public SwitchPanel getSelectedPanel() {
        return (SwitchPanel) list.getSelectedValue();
    }

}
