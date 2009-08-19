package jd.gui.swing.components.JDTable;

import java.awt.Component;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.swingx.renderer.DefaultTableRenderer;

public abstract class JDTableColumn extends AbstractCellEditor implements TableCellEditor, TableCellRenderer {

    private static final long serialVersionUID = -1748365070868647250L;
    private String name;
    private JDTableModel table;
    private DefaultTableRenderer defaultrenderer;
    private boolean sortingToggle = false; /* eg Asc and Desc sorting, a toggle */

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
        new Thread() {
            public void run() {
                this.setName(getID());
                sort(obj, sortingToggle);
            }
        }.start();
        sortingToggle = !sortingToggle;
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
        for (JDRowHighlighter high : this.table.getJDRowHighlighter()) {
            if (high.doHighlight(value)) {
                if (!isSelected) c.setBackground(high.getColor());
                break;
            }
        }
        return c;
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        return myTableCellEditorComponent(table, value, isSelected, row, column);
    }

    public abstract void setValue(Object value, Object object);

    public abstract boolean isEditable(Object obj);

    public abstract Component myTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column);

    public abstract Component myTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column);

    public abstract Object getCellEditorValue();

}
