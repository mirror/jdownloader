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
import java.awt.event.MouseEvent;
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
    protected static Color background = null;
    protected static Color foreground = null;
    protected static Color backgroundselected = null;
    protected static Color foregroundselected = null;
    private Color currentbackground = null;
    private Color currentforeground = null;
    private StatusLabel sl = null;
    private int clickcount = 1;
    private int curWidth = -1;

    public JDTableColumn(String name, JDTableModel table) {
        this.name = name;
        this.table = table;
        defaultrenderer = new DefaultTableRenderer();
    }

    /**
     * Should be overwritten when there should be a maximal width for this
     * column (e.g. for checkboxes)
     */
    public int getMaxWidth() {
        return -1;
    }

    public void setClickstoEdit(int i) {
        clickcount = Math.max(0, i);
    }

    public String getName() {
        return name;
    }

    public boolean defaultVisible() {
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

    public boolean isCellEditable(EventObject evt) {
        if (evt instanceof MouseEvent) { return ((MouseEvent) evt).getClickCount() >= clickcount; }
        return true;
    }

    /** obj==null for sorting on columnheader */
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

    /**
     * Overwrite to implement the sorting for this column
     * 
     * @param obj
     *            the object on which the sorting process was called or
     *            <code>null</code> when sorting on columnheader
     * @param sortingToggle
     *            the toggle of the sorting
     */
    public void sort(Object obj, boolean sortingToggle) {

    }

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
        /* render the component */
        Component c = myTableCellRendererComponent(this.table, value, isSelected, hasFocus, row, column);
        /* store default foreground,background color for later use */
        if (background == null) background = getDefaultTableCellRendererComponent(table, value, false, hasFocus, row, column).getBackground();
        if (foreground == null) foreground = getDefaultTableCellRendererComponent(table, value, false, hasFocus, row, column).getForeground();
        if (backgroundselected == null) backgroundselected = getDefaultTableCellRendererComponent(table, value, true, hasFocus, row, column).getBackground();
        if (foregroundselected == null) foregroundselected = getDefaultTableCellRendererComponent(table, value, true, hasFocus, row, column).getForeground();
        /* call functions for processing component */
        handleSelected(c, this.table, value, isSelected, row, column);
        handleEnabled(c, this.table, value, isSelected, row, column);
        postprocessCell(c, this.table, value, isSelected, row, column);
        return c;
    }

    /**
     * default function to handle Enabled State, overwrite to customize
     */
    public void handleEnabled(Component c, JDTableModel table, Object value, boolean isSelected, int row, int column) {
        /* check enabled,disabled */
        if (isEnabled(value)) {
            if (c instanceof StatusLabel) {
                /*
                 * statuslabe has its own setEnabled function, so we have to
                 * cast
                 */
                ((StatusLabel) c).setEnabled(true);
            } else
                c.setEnabled(true);
        } else {
            if (c instanceof JRendererLabel) {
                /*
                 * to avoid the memory leak in java caused by the laf iconcache,
                 * we have to set the disabled icon here
                 */
                ((JRendererLabel) c).setDisabledIcon(JDImage.getDisabledIcon(((JRendererLabel) c).getIcon()));
                c.setEnabled(false);
            } else if (c instanceof StatusLabel) {
                /*
                 * statuslabe has its own setEnabled function, so we have to
                 * cast
                 * 
                 * this setEnabled also sets the disabled icons, so no memleak
                 * happens
                 */
                ((StatusLabel) c).setEnabled(false);
            } else
                c.setEnabled(false);
        }
    }

    /**
     * default function to handle Selected State, overwrite to customize
     */
    public void handleSelected(Component c, JDTableModel table, Object value, boolean isSelected, int row, int column) {
        /* check selected state */
        if (isSelected) {
            currentbackground = backgroundselected;
            currentforeground = foregroundselected;
        } else {
            currentbackground = background;
            currentforeground = foreground;
            /* check if we have to highlight an unselected cell */
            for (JDRowHighlighter high : table.getJDRowHighlighter()) {
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
    }

    /**
     * default (empty) function to postprocess the component, overwrite to
     * customize
     */
    public void postprocessCell(Component c, JDTableModel table, Object value, boolean isSelected, int row, int column) {
    }

    abstract public boolean isEnabled(Object obj);

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        return myTableCellEditorComponent(this.table, value, isSelected, row, column);
    }

    /**
     * value should be set for object
     */
    public abstract void setValue(Object value, Object object);

    public abstract boolean isEditable(Object obj);

    public abstract Component myTableCellEditorComponent(JDTableModel table, Object value, boolean isSelected, int row, int column);

    public abstract Component myTableCellRendererComponent(JDTableModel table, Object value, boolean isSelected, boolean hasFocus, int row, int column);

    public abstract Object getCellEditorValue();

    public int getCurWidth() {
        return curWidth;
    }

    public void setCurWidth(int curWidth) {
        this.curWidth = curWidth;
    }

}
