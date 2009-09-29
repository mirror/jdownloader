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

import java.awt.Color;
import java.awt.Component;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import jd.gui.swing.jdgui.components.StatusLabel;
import jd.nutils.JDImage;

import org.jdesktop.swingx.renderer.DefaultTableRenderer;
import org.jdesktop.swingx.renderer.JRendererLabel;

public abstract class JDTableColumn extends AbstractCellEditor implements TableCellEditor, TableCellRenderer {

    private static final long serialVersionUID = -1748365070868647250L;
    private String name;
    private JDTableModel table;
    private DefaultTableRenderer defaultrenderer;
    private boolean sortingToggle = false; /* eg Asc and Desc sorting, a toggle */
    private Thread sortThread = null;
    private static Color background = null;
    private static Color foreground = null;
    private static Color backgroundselected = null;
    private static Color foregroundselected = null;
    private Color currentbackground = null;
    private Color currentforeground = null;
    private StatusLabel sl = null;

    public JDTableColumn(String name, JDTableModel table) {
        this.name = name;
        this.table = table;
        defaultrenderer = new DefaultTableRenderer();
    }

    public String getName() {
        return name;
    }

    public boolean defaultEnabled() {
        return true;
    }

    public String getID() {
        return getClass().getSimpleName();
    }

    public JDTableModel getJDTableModel() {
        return table;
    }

    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        Object obj = table.getValueAt(rowIndex, columnIndex);
        if (obj == null) return;
        setValue(value, obj);
    }

    public boolean shouldSelectCell(EventObject anEvent) {
        return true;
    }

    /* obj==null for sorting on columnheader */
    abstract public boolean isSortable(Object obj);

    protected void doSort(final Object obj) {
        if (sortThread != null) return;
        sortThread = new Thread() {
            public void run() {
                this.setName(getID());
                try {
                    sort(obj, sortingToggle);
                } catch (Exception e) {
                }
                sortingToggle = !sortingToggle;
                sortThread = null;
            }
        };
        sortThread.start();
    }

    abstract public void sort(Object obj, final boolean sortingToggle);

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        Object obj = table.getValueAt(rowIndex, columnIndex);
        if (obj == null) return false;
        return isEditable(obj);
    }

    public Component getDefaultTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        hasFocus = false;
        return defaultrenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        hasFocus = false;
        Component c = myTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        /* restore foreground,background color */
        if (background == null) background = getDefaultTableCellRendererComponent(table, value, false, hasFocus, row, column).getBackground();
        if (foreground == null) foreground = getDefaultTableCellRendererComponent(table, value, false, hasFocus, row, column).getForeground();
        if (backgroundselected == null) backgroundselected = getDefaultTableCellRendererComponent(table, value, true, hasFocus, row, column).getBackground();
        if (foregroundselected == null) foregroundselected = getDefaultTableCellRendererComponent(table, value, true, hasFocus, row, column).getForeground();
        if (isSelected) {
            currentbackground = backgroundselected;
            currentforeground = foregroundselected;
        } else {
            currentbackground = background;
            currentforeground = foreground;
            for (JDRowHighlighter high : this.table.getJDRowHighlighter()) {
                if (high.doHighlight(value)) {
                    currentbackground = high.getColor();
                    break;
                }
            }
        }
        if (c instanceof StatusLabel) {
            sl = (StatusLabel) c;
            sl.setBackground(currentbackground);
            sl.setForeground(currentforeground);
        } else {
            c.setBackground(currentbackground);
            c.setForeground(currentforeground);
        }
        /* check enabled,disabled */
        if (isEnabled(value)) {
            if (c instanceof StatusLabel) {
                ((StatusLabel) c).setEnabled(true);
            } else
                c.setEnabled(true);
        } else {
            if (c instanceof JRendererLabel) {
                ((JRendererLabel) c).setDisabledIcon(JDImage.getDisabledIcon(((JRendererLabel) c).getIcon()));
                c.setEnabled(false);
            } else if (c instanceof StatusLabel) {
                ((StatusLabel) c).setEnabled(false);
            } else
                c.setEnabled(false);
        }
        postprocessCell(c, table, value, isSelected, row, column);
        return c;
    }

    public void postprocessCell(Component c, JTable table, Object value, boolean isSelected, int row, int column) {
    }

    abstract public boolean isEnabled(Object obj);

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        return myTableCellEditorComponent(table, value, isSelected, row, column);
    }

    public abstract void setValue(Object value, Object object);

    public abstract boolean isEditable(Object obj);

    public abstract Component myTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column);

    public abstract Component myTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column);

    public abstract Object getCellEditorValue();

}
