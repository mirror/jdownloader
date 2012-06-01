package jd.gui.swing.components.table;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;

import org.jdesktop.swingx.renderer.JRendererCheckBox;

public abstract class JDCheckBoxTableColumn extends JDTableColumn implements ActionListener {

    private static final long serialVersionUID = 5128718970314177880L;

    private JRendererCheckBox boolrend;
    private JCheckBox         checkbox;

    public JDCheckBoxTableColumn(String name, JDTableModel table) {
        super(name, table);

        boolrend = new JRendererCheckBox();
        boolrend.setHorizontalAlignment(JCheckBox.CENTER);
        checkbox = new JCheckBox();
        checkbox.setHorizontalAlignment(JCheckBox.CENTER);
    }

    protected abstract boolean getBooleanValue(Object value);

    protected abstract void setBooleanValue(boolean value, Object object);

    @Override
    public int getMaxWidth() {
        return 100;
    }

    @Override
    public final Object getCellEditorValue() {
        return checkbox.isSelected();
    }

    @Override
    public final Component myTableCellEditorComponent(JDTableModel table, Object value, boolean isSelected, int row, int column) {
        checkbox.removeActionListener(this);
        checkbox.setSelected(getBooleanValue(value));
        checkbox.addActionListener(this);
        return checkbox;
    }

    @Override
    public final Component myTableCellRendererComponent(JDTableModel table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        boolrend.setSelected(getBooleanValue(value));
        return boolrend;
    }

    @Override
    public final void setValue(Object value, Object object) {
        setBooleanValue((Boolean) value, object);
    }

    public void actionPerformed(ActionEvent e) {
        checkbox.removeActionListener(this);
        this.fireEditingStopped();
    }

}
