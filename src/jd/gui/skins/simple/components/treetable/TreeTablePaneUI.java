//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.gui.skins.simple.components.treetable;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.CellRendererPane;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicTableUI;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.TreePath;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.utils.JDTheme;

public class TreeTablePaneUI extends BasicTableUI {

    class OpaqueRenderPane extends CellRendererPane {

        private static final long serialVersionUID = 1L;

        @Override
        public void paintComponent(Graphics g, Component c, Container p, int x, int y, int w, int h, boolean shouldValidate) {
            if (c instanceof JComponent) {
                JComponent vComponent = (JComponent) c;
                vComponent.setOpaque(false);
            }
            try {
                super.paintComponent(g, c, p, x, y, w, h, shouldValidate);
            } catch (Exception e) {
            }
        }
    }

    private static Color COLOR_DISABLED;

    private static Color COLOR_DONE;

    private static Color COLOR_DONE_PACKAGE;
    private static Color COLOR_ERROR_POST;

    private static Color COLOR_ERROR;

    private static Color COLOR_WAIT;

    public static Color EVEN_ROW_COLOR;

    public static Color GRID_COLOR;

    public static Color ODD_ROW_COLOR;

    public static Color PACKAGE_ROW_COLOR;

    public static Color SELECTED_ROW_BORDER_COLOR;

    //--------------------------------------------------------------------------
    // ----------------------------------------
    // Custom installation methods
    //--------------------------------------------------------------------------
    // ----------------------------------------

    public static Color SELECTED_ROW_COLOR;

    //--------------------------------------------------------------------------
    // ----------------------------------------
    // Custom painting methods
    //--------------------------------------------------------------------------
    // ----------------------------------------

    @Override
    protected void installDefaults() {

        /* Ist für Nimbus LAF auskommentiert */
        // super.installDefaults();
        COLOR_ERROR_POST = JDTheme.C("gui.color.downloadlist.error_post", "ff9936");

        EVEN_ROW_COLOR = JDTheme.C("gui.color.downloadlist.row_a", "EDF3FE");

        ODD_ROW_COLOR = JDTheme.C("gui.color.downloadlist.row_b", "FFFFFF");

        SELECTED_ROW_COLOR = JDTheme.C("gui.color.downloadlist.row_selected", "3D80DF");

        GRID_COLOR = JDTheme.C("gui.color.downloadlist.grid", "CCCCCC");

        PACKAGE_ROW_COLOR = JDTheme.C("gui.color.downloadlist.row_package", "fffa7c");

        SELECTED_ROW_BORDER_COLOR = JDTheme.C("gui.color.downloadlist.row_selected_border", "000000");

        COLOR_DONE = JDTheme.C("gui.color.downloadlist.row_link_done", "c4ffd2");

        COLOR_DONE_PACKAGE = JDTheme.C("gui.color.downloadlist.row_package_done", "339933");

        COLOR_ERROR = JDTheme.C("gui.color.downloadlist.row_link_error", "ff0000");

        COLOR_DISABLED = JDTheme.C("gui.color.downloadlist.row_link_disabled", "adadad");

        COLOR_WAIT = JDTheme.C("gui.color.downloadlist.row_link_disabled", "4393d7");

        table.setShowGrid(false);
        table.setRowHeight(table.getFont().getSize() + 6);
        table.setGridColor(GRID_COLOR);

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                table.repaint();
            }
        });

        table.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
                table.repaint();
            }

            public void focusLost(FocusEvent e) {
                table.repaint();
            }
        });

    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);

        table.remove(rendererPane);

        rendererPane = new OpaqueRenderPane();
        table.add(rendererPane);

        ((DownloadTreeTable) table).setTreeCellRenderer(new TreeTableCellRenderer());
    }

    @Override
    public void paint(Graphics g, JComponent c) {

        int vRowHeight = c.getFont().getSize() + 6;

        Rectangle clip = g.getClipBounds();

        int x = clip.x;
        int y = clip.y;
        int w = clip.width;
        int h = clip.height;
        int row = 0;

        int y2 = y + h;

        if (y != 0) {
            int diff = y % vRowHeight;
            row = y / vRowHeight;
            y -= diff;
        }
        TreePath path;
        Color color;
        DownloadLink dLink;
        while (y < y2) {
            path = ((DownloadTreeTable) c).getPathForRow(row);
            color = row % 2 == 0 ? EVEN_ROW_COLOR : ODD_ROW_COLOR;
            // if (((JTable) c).isRowSelected(row)) {
            // if (c.hasFocus()) {
            // color = selectedFocusedColor;
            // }
            // else {
            // color = selectedNotFocusedColor;
            //
            // }
            // }
            // else {
            if (((DownloadTreeTable) c).isRowSelected(row)) {

                color = SELECTED_ROW_COLOR;

            } else {
                if (path != null && path.getLastPathComponent() instanceof FilePackage) {
                    if (((FilePackage) path.getLastPathComponent()).isFinished()) {
                        color = COLOR_DONE_PACKAGE;
                    } else {
                        color = PACKAGE_ROW_COLOR;
                    }
                    //c.setBorder(BorderFactory.createLineBorder(Color.darkGray,
                    // 1));
                    g.setColor(Color.BLACK);
                    g.drawLine(x, y, w, y);
                } else if (path != null && path.getLastPathComponent() instanceof DownloadLink) {
                    dLink = (DownloadLink) path.getLastPathComponent();

                    if (!dLink.isEnabled()) {
                        color = COLOR_DISABLED;
                    } else if (dLink.getLinkStatus().hasStatus(LinkStatus.ERROR_POST_PROCESS)) {
                        color = COLOR_ERROR_POST;
                    } else if (dLink.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                        color = COLOR_DONE;
                    } else if (dLink.getLinkStatus().isFailed()) {
                        color = COLOR_ERROR;
                    } else if (dLink.getLinkStatus().getRemainingWaittime() > 0 || dLink.getPlugin() == null || dLink.getPlugin().getRemainingHosterWaittime() > 0) {
                        color = COLOR_WAIT;
                    }

                }
            }
            // }
            if (((DownloadTreeTable) c).mouseOverRow == row) {

                if (((JTable) c).isRowSelected(row)) {
                    g.setColor(color.darker());
                    g.fillRect(x, y + 1, w, vRowHeight - 2);
                    g.setColor(SELECTED_ROW_BORDER_COLOR);
                    g.draw3DRect(x, y, w, vRowHeight - 1, true);
                } else {
                    g.setColor(color.darker());
                    g.fillRect(x, y + 1, w, vRowHeight - 1);

                }
            } else {
                if (((JTable) c).isRowSelected(row)) {
                    g.setColor(color);
                    g.fillRect(x, y + 1, w, vRowHeight - 2);
                    g.setColor(SELECTED_ROW_BORDER_COLOR);
                    g.draw3DRect(x, y, w, vRowHeight - 1, false);
                } else {
                    g.setColor(color);
                    g.fillRect(x, y + 1, w, vRowHeight - 1);
                }
            }
            y += vRowHeight;
            row++;
        }

        super.paint(g, c);

        x = 0;
        g.setColor(GRID_COLOR);
        TableColumnModel vModel = table.getColumnModel();
        for (int i = 0; i < vModel.getColumnCount(); i++) {
            TableColumn vColumn = vModel.getColumn(i);
            x += vColumn.getWidth();
            if (x >= clip.x && x <= clip.x + clip.width) {
                g.drawLine(x - 1, clip.y, x - 1, clip.y + clip.height);
            }
        }
    }
}