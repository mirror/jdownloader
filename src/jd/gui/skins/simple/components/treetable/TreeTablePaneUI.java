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

public class TreeTablePaneUI extends BasicTableUI {

    public static final Color evenRowColor = new Color(0xEDF3FE);

    public static final Color oddRowColor = Color.WHITE;

    public static final Color selectedRowColor = Color.WHITE;

    public static final Color gridColor = new Color(150, 150, 150);

    public static final Color packageColor = new Color(0x94baff);

    public static final Color selectedFocusedColor = new Color(0x3D80DF);

    public static final Color selectedNotFocusedColor = new Color(0x3D80DF);

    public static final Color selectedBorderColor = Color.BLACK;

    private static final Color COLOR_DONE = new Color(0xacff9e).brighter();

    private static final Color COLOR_ERROR = new Color(255, 0, 0, 20);

    private static final Color COLOR_DISABLED = new Color(50, 50, 50, 50);

    private static final Color COLOR_WAIT = new Color(0, 0, 100, 20);

    private static final Color COLOR_ERROR_OFFLINE = new Color(255, 0, 0, 60);

    // ------------------------------------------------------------------------------------------------------------------
    // Custom installation methods
    // ------------------------------------------------------------------------------------------------------------------

    protected void installDefaults() {
        super.installDefaults();
        table.setShowGrid(false);
        table.setRowHeight(table.getFont().getSize() + 6);
        table.setGridColor(gridColor);

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

    // ------------------------------------------------------------------------------------------------------------------
    // Custom painting methods
    // ------------------------------------------------------------------------------------------------------------------

    public void paint(Graphics g, JComponent c) {

        ((DownloadTreeTable) c).onRefresh();
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
            color = row % 2 == 0 ? evenRowColor : oddRowColor;
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

                color = selectedFocusedColor;

            } else {
                if (path != null && path.getLastPathComponent() instanceof FilePackage) {

                    color = packageColor;
                    // c.setBorder(BorderFactory.createLineBorder(Color.darkGray,
                    // 1));
                    g.setColor(Color.BLACK);
                    g.drawLine(x, y, w, y);
                } else if (path != null && path.getLastPathComponent() instanceof DownloadLink) {
                    dLink = ((DownloadLink) path.getLastPathComponent());

                    if (!dLink.isEnabled()) {
                        color = (COLOR_DISABLED);
                    } else if (dLink.getRemainingWaittime() > 0) {
                        color = (COLOR_WAIT);
                    } else if (dLink.getStatus() == DownloadLink.STATUS_DONE) {
                        color = (COLOR_DONE);
                    } else if (dLink.getStatus() != DownloadLink.STATUS_TODO && dLink.getStatus() != DownloadLink.STATUS_ERROR_DOWNLOAD_LIMIT && dLink.getStatus() != DownloadLink.STATUS_DOWNLOAD_IN_PROGRESS) {
                        color = (COLOR_ERROR);
                    }

                    else if (dLink.isAvailabilityChecked() && !dLink.isAvailable()) {
                        color = (COLOR_ERROR_OFFLINE);
                    }

                }
            }
            // }
            if (((DownloadTreeTable) c).mouseOverRow == row) {

                if (((JTable) c).isRowSelected(row)) {
                    g.setColor(color.darker());
                    g.fillRect(x, y + 1, w, vRowHeight - 2);
                    g.setColor(selectedBorderColor);
                    g.draw3DRect(x, y, w, vRowHeight - 1, true);
                } else {
                    g.setColor(color.darker());
                    g.fillRect(x, y + 1, w, vRowHeight - 1);

                }
            } else {
                if (((JTable) c).isRowSelected(row)) {
                    g.setColor(color);
                    g.fillRect(x, y + 1, w, vRowHeight - 2);
                    g.setColor(selectedBorderColor);
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
        g.setColor(gridColor);
        TableColumnModel vModel = table.getColumnModel();
        for (int i = 0; i < vModel.getColumnCount(); i++) {
            TableColumn vColumn = vModel.getColumn(i);
            x += vColumn.getWidth();
            if ((x >= clip.x) && (x <= clip.x + clip.width)) {
                g.drawLine(x - 1, clip.y, x - 1, clip.y + clip.height);
            }
        }
    }

    public void installUI(JComponent c) {
        super.installUI(c);

        table.remove(rendererPane);

        rendererPane = new OpaqueRenderPane();
        table.add(rendererPane);

        ((DownloadTreeTable) table).setTreeCellRenderer(new TreeTableCellRenderer());
    }

    class OpaqueRenderPane extends CellRendererPane {

        public void paintComponent(Graphics g, Component c, Container p, int x, int y, int w, int h, boolean shouldValidate) {
            if (c instanceof JComponent) {
                JComponent vComponent = (JComponent) c;
                vComponent.setOpaque(false);
            }

            super.paintComponent(g, c, p, x, y, w, h, shouldValidate);
        }
    }
}