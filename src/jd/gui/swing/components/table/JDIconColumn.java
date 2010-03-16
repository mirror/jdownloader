package jd.gui.swing.components.table;

import java.awt.Component;

import javax.swing.Icon;

import org.jdesktop.swingx.renderer.JRendererLabel;

public abstract class JDIconColumn extends JDTableColumn {

    private static final long serialVersionUID = 2279945625539269778L;
    private JRendererLabel label;

    public JDIconColumn(String name, JDTableModel table) {
        super(name, table);

        label = new JRendererLabel();
        label.setHorizontalAlignment(JRendererLabel.CENTER);
        label.setBorder(null);
    }

    /**
     * Returns the icon for o1;
     * 
     * @param o1
     * @return
     */
    abstract protected Icon getIcon(Object o1);

    @Override
    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public boolean isEditable(Object obj) {
        return false;
    }

    @Override
    public boolean isEnabled(Object obj) {
        return true;
    }

    @Override
    public boolean isSortable(Object obj) {
        return false;
    }

    @Override
    public void setValue(Object value, Object object) {
    }

    @Override
    protected int getMaxWidth() {
        return 100;
    }

    @Override
    public Component myTableCellEditorComponent(JDTableModel table, Object value, boolean isSelected, int row, int column) {
        return null;
    }

    @Override
    public Component myTableCellRendererComponent(JDTableModel table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        label.setIcon(getIcon(value));
        return label;
    }

}
