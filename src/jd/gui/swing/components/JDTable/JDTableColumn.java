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

    public JDTableModel getJDTableModel() {
        return table;
    }

    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        columnIndex = this.getJDTableModel().toModel(columnIndex);
        Object obj = table.getValueAt(rowIndex, columnIndex);
        if (obj == null) return;
        setValue(value, obj);
    }

    public boolean shouldSelectCell(EventObject anEvent) {
        return true;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        columnIndex = this.getJDTableModel().toModel(columnIndex);
        Object obj = table.getValueAt(rowIndex, columnIndex);
        if (obj == null) return false;
        return isEditable(obj);
    }

    abstract public void setValue(Object value, Object object);

    abstract public boolean isEditable(Object obj);

    public Component getDefaultTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        hasFocus = false;
        column = this.getJDTableModel().toModel(column);
        return defaultrenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        hasFocus = false;
        column = this.getJDTableModel().toModel(column);
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

    abstract public Component myTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column);

    abstract public Component myTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column);
}
