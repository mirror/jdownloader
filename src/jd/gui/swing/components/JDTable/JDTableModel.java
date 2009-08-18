package jd.gui.swing.components.JDTable;

import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

import jd.config.SubConfiguration;

public abstract class JDTableModel extends AbstractTableModel {

    /**
     * 
     */
    private static final long serialVersionUID = -7560879256832858265L;

    protected SubConfiguration config;

    protected ArrayList<Object> list = new ArrayList<Object>();
    protected ArrayList<JDTableColumn> columns = new ArrayList<JDTableColumn>();

    public JDTableModel(String configname) {
        super();
        config = SubConfiguration.getConfig(configname);
    }

    public void addColumn(JDTableColumn e) {
        columns.add(e);
    }

    public void addColumn(JDTableColumn e, int index) {
        columns.add(index, e);
    }

    public SubConfiguration getConfig() {
        return config;
    }

    abstract public void refreshModel();

    public int getRowCount() {
        return list.size();
    }

    public int getRowforObject(Object o) {
        synchronized (list) {
            return list.indexOf(o);
        }
    }

    public Object getObjectforRow(int rowIndex) {
        synchronized (list) {
            if (rowIndex < list.size()) return list.get(rowIndex);
            return null;
        }
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        try {
            return list.get(rowIndex);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public int getRealColumnCount() {
        return columns.size();
    }

    public String getRealColumnName(int column) {
        return columns.get(column).getName();
    }

    public int getColumnCount() {
        int j = 0;
        for (int i = 0; i < columns.size(); ++i) {
            if (isVisible(i)) ++j;
        }
        return j;
    }

    public boolean isVisible(int column) {
        JDTableColumn col = getJDTableColumn(column);
        return config.getBooleanProperty("VISABLE_COL_" + col.getName(), col.defaultEnabled());
    }

    public void setVisible(int column, boolean visible) {
        JDTableColumn col = getJDTableColumn(column);
        config.setProperty("VISABLE_COL_" + col.getName(), visible);
        config.save();
    }

    public int toModel(int column) {
        int i = 0;
        int k;
        for (k = 0; k < getRealColumnCount(); ++k) {
            if (isVisible(k)) {
                ++i;
            }
            if (i > column) break;
        }
        return k;
    }

    public int toVisible(int column) {
        int i = column;
        int k;
        for (k = column; k >= 0; --k) {
            if (!isVisible(k)) {
                --i;
            }
        }
        return i;
    }

    @Override
    public String getColumnName(int column) {
        return columns.get(toModel(column)).getName();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return Object.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columns.get(columnIndex).isCellEditable(rowIndex, columnIndex);
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        columns.get(columnIndex).setValueAt(value, rowIndex, columnIndex);
    }

    public JDTableColumn getJDTableColumn(int columnIndex) {
        return columns.get(columnIndex);
    }

}
