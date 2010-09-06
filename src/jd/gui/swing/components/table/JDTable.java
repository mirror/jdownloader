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

package jd.gui.swing.components.table;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import jd.config.SubConfiguration;
import jd.gui.swing.components.JExtCheckBoxMenuItem;
import jd.gui.swing.jdgui.interfaces.JDMouseAdapter;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.jdesktop.swingx.JXTable;

public class JDTable extends JXTable {

    private static final long      serialVersionUID = -6631229711568284941L;
    private static final String    JDL_PREFIX       = "jd.gui.swing.components.table.JDTable.";
    private final JDTableModel     model;
    private final SubConfiguration tableconfig;
    private final SortMenuItem     defaultSortMenuItem;
    public static final int        ROWHEIGHT        = 19;

    public JDTable(final JDTableModel model) {
        super(model);
        this.model = model;
        model.setJDTable(this);
        this.tableconfig = model.getConfig();
        this.createColumns();
        this.setSortable(false);
        this.setShowHorizontalLines(false);
        this.setShowVerticalLines(false);
        UIManager.put("Table.focusCellHighlightBorder", null);
        this.defaultSortMenuItem = new SortMenuItem();
        this.getTableHeader().addMouseListener(new JDMouseAdapter() {

            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    final int col = JDTable.this.realColumnAtPoint(e.getPoint());
                    if (JDTable.this.getJDTableModel().getJDTableColumn(col).isSortable(null)) {
                        JDTable.this.getJDTableModel().getJDTableColumn(col).doSort(null);
                    }
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    JDTable.this.columControlMenu().show(JDTable.this.getTableHeader(), e.getX(), e.getY());
                }
            }

        });
        this.getTableHeader().addMouseMotionListener(new JDMouseAdapter() {
            @Override
            public void mouseMoved(final MouseEvent e) {
                final int col = JDTable.this.realColumnAtPoint(e.getPoint());
                JDTable.this.getTableHeader().setToolTipText(JDTable.this.getJDTableModel().getJDTableColumn(col).getName());
            }
        });
        this.getTableHeader().setReorderingAllowed(true);
        this.getTableHeader().setResizingAllowed(true);
        this.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.setAutoscrolls(true);
        this.setRowHeight(JDTable.ROWHEIGHT);
        this.installColumnControlButton();
        this.getTableHeader().setPreferredSize(new Dimension(this.getColumnModel().getTotalColumnWidth(), 19));
        // This method is 1.6 only
        if (JDUtilities.getJavaVersion() >= 1.6) {
            this.setFillsViewportHeight(true);
        }

        this.getColumnModel().addColumnModelListener(new TableColumnModelListener() {

            public void columnAdded(final TableColumnModelEvent e) {
            }

            public void columnMarginChanged(final ChangeEvent e) {
            }

            public void columnMoved(final TableColumnModelEvent e) {
                if (e == null) { return; }
                if (e.getFromIndex() == e.getToIndex()) { return; }
                final TableColumnModel tcm = JDTable.this.getColumnModel();
                for (int i = 0; i < tcm.getColumnCount(); i++) {
                    JDTable.this.tableconfig.setProperty("POS_COL_" + i, JDTable.this.getJDTableModel().getJDTableColumn(tcm.getColumn(i).getModelIndex()).getID());
                }
                JDTable.this.tableconfig.save();
            }

            public void columnRemoved(final TableColumnModelEvent e) {
            }

            public void columnSelectionChanged(final ListSelectionEvent e) {
            }

        });
    }

    public void addJDRowHighlighter(final JDRowHighlighter high) {
        this.model.addJDRowHighlighter(high);
    }

    public JPopupMenu columControlMenu() {
        final JPopupMenu popup = new JPopupMenu();
        final JCheckBoxMenuItem[] mis = new JCheckBoxMenuItem[this.getJDTableModel().getColumnCount()];

        for (int i = 0; i < this.getJDTableModel().getColumnCount(); ++i) {
            final int j = i;
            final JExtCheckBoxMenuItem mi = new JExtCheckBoxMenuItem(this.getJDTableModel().getColumnName(i));
            mi.setHideOnClick(false);
            mis[i] = mi;
            if (i == 0) {
                mi.setEnabled(false);
            }
            mi.setSelected(this.getJDTableModel().isVisible(i));
            mi.addActionListener(new ActionListener() {

                public void actionPerformed(final ActionEvent e) {
                    JDTable.this.getJDTableModel().setVisible(j, mi.isSelected());
                    JDTable.this.createColumns();
                    JDTable.this.revalidate();
                    JDTable.this.repaint();
                }

            });
            popup.add(mi);
        }
        return popup;
    }

    private void createColumns() {
        this.setAutoCreateColumnsFromModel(false);
        final TableColumnModel tcm = this.getColumnModel();
        while (tcm.getColumnCount() > 0) {
            tcm.removeColumn(tcm.getColumn(0));
        }
        final LinkedHashMap<String, TableColumn> columns = new LinkedHashMap<String, TableColumn>();
        for (int i = 0; i < this.getModel().getColumnCount(); ++i) {
            final int j = i;

            final TableColumn tableColumn = new TableColumn(i);
            tableColumn.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(final PropertyChangeEvent evt) {
                    if (evt.getPropertyName().equals("width")) {
                        JDTable.this.tableconfig.setProperty("WIDTH_COL_" + JDTable.this.model.getJDTableColumn(j).getID(), evt.getNewValue());
                        JDTable.this.tableconfig.save();
                        JDTable.this.model.getJDTableColumn(j).setCurWidth(Integer.parseInt(evt.getNewValue().toString()));
                    }
                }
            });
            if (this.model.getJDTableColumn(j).getMaxWidth() >= 0) {
                tableColumn.setMaxWidth(this.model.getJDTableColumn(j).getMaxWidth());
            }
            final int w = this.tableconfig.getIntegerProperty("WIDTH_COL_" + this.model.getJDTableColumn(j).getID(), tableColumn.getWidth());
            tableColumn.setPreferredWidth(w);
            this.model.getJDTableColumn(j).setCurWidth(w);
            if (!this.model.isVisible(i)) {
                continue;
            }
            columns.put(this.model.getJDTableColumn(j).getID(), tableColumn);
        }
        int index = 0;
        while (true) {
            if (columns.isEmpty()) {
                break;
            }
            if (index < this.getModel().getColumnCount()) {
                final String id = this.tableconfig.getStringProperty("POS_COL_" + index, null);
                index++;
                if (id != null) {
                    final TableColumn item = columns.remove(id);
                    if (item != null) {
                        this.addColumn(item);
                    }
                }
            } else {
                for (final TableColumn ritem : columns.values()) {
                    this.addColumn(ritem);
                }
                break;
            }
        }
    }

    @Override
    public TableCellEditor getCellEditor(final int row, final int col) {
        return this.model.getJDTableColumn(this.convertColumnIndexToModel(col));
    }

    @Override
    public TableCellRenderer getCellRenderer(final int row, final int col) {
        return this.model.getJDTableColumn(this.convertColumnIndexToModel(col));
    }

    public SortMenuItem getDefaultSortMenuItem() {
        return this.defaultSortMenuItem;
    }

    public ArrayList<JDRowHighlighter> getJDRowHighlighter() {
        return this.model.getJDRowHighlighter();
    }

    public JDTableModel getJDTableModel() {
        return this.model;
    }

    public Point getPointinCell(final Point x) {
        final int row = this.rowAtPoint(x);
        if (row == -1) { return null; }
        final Rectangle cellPosition = this.getCellRect(row, this.columnAtPoint(x), true);
        final Point p = new Point();
        p.setLocation(x.getX() - cellPosition.getX(), x.getY() - cellPosition.getY());
        return p;
    }

    private void installColumnControlButton() {
        final JButton button = new JButton(((JButton) this.getColumnControl()).getIcon());
        button.setToolTipText(JDL.L(JDTable.JDL_PREFIX + "columnControl", "Change Columns"));
        button.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent event) {
                final JButton source = (JButton) event.getSource();
                final int x = source.getLocation().x;
                final int y = source.getLocation().y;
                JDTable.this.columControlMenu().show(JDTable.this.getTableHeader(), x, y);
            }

        });
        this.setColumnControl(button);
        this.setColumnControlVisible(true);
    }

    public int realColumnAtPoint(final Point point) {
        final int x = this.columnAtPoint(point);
        return this.convertColumnIndexToModel(x);
    }

}
