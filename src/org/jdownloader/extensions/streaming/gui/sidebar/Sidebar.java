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

package org.jdownloader.extensions.streaming.gui.sidebar;

import java.awt.AlphaComposite;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JList;
import javax.swing.event.ListSelectionListener;

import jd.gui.swing.jdgui.views.settings.sidebar.CheckBoxedEntry;

import org.jdownloader.extensions.streaming.gui.MediaArchiveTable;
import org.jdownloader.translate._JDT;

public class Sidebar extends JList implements MouseMotionListener, MouseListener {

    private Point mouse;

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Point lmouse = mouse;
        if (lmouse != null) {
            final Graphics2D g2 = (Graphics2D) g;
            final AlphaComposite ac5 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f);
            g2.setComposite(ac5);
            int index = locationToIndex(lmouse);

            // if (index >= 0 && getModel().getElementAt(index) instanceof ExtensionHeader) { return; }
            // if (index >= 0 && getModel().getElementAt(index) instanceof AdvancedSettings) {
            // Point p = indexToLocation(index);
            // if (p != null) {
            // g2.fillRect(0, p.y, getWidth(), 25);
            // g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            // }
            // } else {
            Point p = indexToLocation(index);
            if (p != null) {
                g2.fillRect(0, p.y, getWidth(), 55);

                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            }
            // }
        }

    }

    public Sidebar(MediaArchiveTable table) {
        super();

        addMouseMotionListener(this);
        addMouseListener(this);
        setModel(new SettingsSidebarModel());
        setCellRenderer(new TreeRenderer());

        setOpaque(false);
        setBackground(null);

        addMouseMotionListener(new MouseAdapter() {

            @Override
            public void mouseMoved(MouseEvent e) {
                int index = locationToIndex(e.getPoint());
                try {
                    if (getModel().getElementAt(index) instanceof CheckBoxedEntry) {
                        Point point = indexToLocation(index);
                        int x = 0;
                        int y = 0;
                        if (point != null) {
                            x = e.getPoint().x - point.x;
                            y = e.getPoint().y - point.y;
                        }
                        if (x > 3 && x < 18 && y > 3 && y < 18) {

                            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                            setToolTipText(_JDT._.settings_sidebar_tooltip_enable_extension());
                        } else {
                            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                            if (getModel().getElementAt(index) instanceof CheckBoxedEntry) {
                                setToolTipText(((CheckBoxedEntry) getModel().getElementAt(index)).getDescription());

                            } else {
                                setToolTipText(null);
                            }
                        }
                    }
                } catch (final ArrayIndexOutOfBoundsException e2) {
                }
            }
        });
        setBackground(null);
        setOpaque(false);

    }

    public void addListener(ListSelectionListener x) {
        getSelectionModel().addListSelectionListener(x);
    }

    // @Override
    // public void setOpaque(boolean isOpaque) {
    // // super.setOpaque(isOpaque);
    // }

    // public boolean isOpaque() {
    // return true;
    // }

    public void mouseDragged(MouseEvent e) {
        mouse = e.getPoint();
        repaint();

    }

    public void mouseMoved(MouseEvent e) {
        mouse = e.getPoint();
        repaint();
    }

    public void mouseClicked(MouseEvent e) {
        // mouse = null;
        // repaint();
    }

    public void mousePressed(MouseEvent e) {

        int index = locationToIndex(e.getPoint());

    }

    public void mouseReleased(MouseEvent e) {
        // mouse = null;
        // repaint();
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
        mouse = null;
        repaint();
    }

}
