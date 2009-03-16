package jd.gui.skins.simple.components.Linkgrabber;

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

public class LinkGrabberV2TreeTablePaneUI extends BasicTableUI {

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

    private static Color COLOR_UNCHECKED;

    private static Color COLOR_ONLINE;

    private static Color COLOR_OFFLINE;

    private static Color COLOR_EXISTS;

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

        EVEN_ROW_COLOR = JDTheme.C("gui.color.downloadlist.row_a", "EDF3FE");

        ODD_ROW_COLOR = JDTheme.C("gui.color.downloadlist.row_b", "FFFFFF");

        SELECTED_ROW_COLOR = JDTheme.C("gui.color.downloadlist.row_selected", "3D80DF");

        GRID_COLOR = JDTheme.C("gui.color.downloadlist.grid", "CCCCCC");

        PACKAGE_ROW_COLOR = JDTheme.C("gui.color.downloadlist.row_package", "fffa7c");

        SELECTED_ROW_BORDER_COLOR = JDTheme.C("gui.color.downloadlist.row_selected_border", "000000");

        COLOR_ONLINE = new Color(0, 255, 0, 20);

        COLOR_OFFLINE = new Color(255, 0, 0, 60);

        COLOR_EXISTS = new Color(255, 127, 0, 60);

        COLOR_UNCHECKED = new Color(255, 127, 100, 60);

        table.setShowGrid(false);
        table.setRowHeight(table.getFont().getSize() + 6);
        table.setGridColor(GRID_COLOR);

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
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
        ((LinkGrabberV2TreeTable) table).setTreeCellRenderer(new LinkGrabberV2TreeTableCellRenderer());
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
            path = ((LinkGrabberV2TreeTable) c).getPathForRow(row);
            color = row % 2 == 0 ? EVEN_ROW_COLOR : ODD_ROW_COLOR;
            if (((LinkGrabberV2TreeTable) c).isRowSelected(row)) {
                color = SELECTED_ROW_COLOR;
            } else {
                if (path != null && path.getLastPathComponent() instanceof LinkGrabberV2FilePackage) {
                    color = PACKAGE_ROW_COLOR;
                    g.setColor(Color.BLACK);
                    g.drawLine(x, y, w, y);
                } else if (path != null && path.getLastPathComponent() instanceof DownloadLink) {
                    dLink = (DownloadLink) path.getLastPathComponent();
                    if (dLink.getLinkStatus().hasStatus(LinkStatus.ERROR_ALREADYEXISTS)) {
                        color = COLOR_EXISTS;
                    } else if (!dLink.isAvailabilityChecked()) {
                        color = COLOR_UNCHECKED;
                    } else if (dLink.isAvailable()) {
                        color = COLOR_ONLINE;
                    } else if (!dLink.isAvailable()) {
                        color = COLOR_OFFLINE;
                    }
                }
            }            
            if (((LinkGrabberV2TreeTable) c).mouseOverRow == row) {

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