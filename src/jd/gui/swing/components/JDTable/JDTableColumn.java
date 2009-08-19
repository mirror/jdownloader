package jd.gui.swing.components.JDTable;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import jd.gui.swing.jdgui.views.downloadview.JDProgressBar;

import org.jdesktop.swingx.renderer.DefaultTableRenderer;

public abstract class JDTableColumn extends AbstractCellEditor implements TableCellEditor, TableCellRenderer {

    private static final long serialVersionUID = -1748365070868647250L;
    private String name;
    private JDTableModel table;
    private DefaultTableRenderer defaultrenderer;
    private boolean sortingToggle = false; /* eg Asc and Desc sorting, a toggle */
    private Thread sortThread = null;
    private static Color color = UIManager.getColor("TableHeader.background");
    private static Dimension dim = new Dimension(200, 30);

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
        c.setEnabled(isEnabled(value));
        if (c instanceof JDProgressBar) return c;
        if (!isSelected) {
            for (JDRowHighlighter high : this.table.getJDRowHighlighter()) {
                if (high.doHighlight(value)) {
                    c.setBackground(high.getColor());
                    break;
                }
            }
        } else {
            if (color == null) {
                ((JComponent) c).setBackground(c.getBackground().darker());
            } else {
                ((JComponent) c).setBackground(color.darker());
            }
        }
        c.setSize(dim);
        return c;
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
