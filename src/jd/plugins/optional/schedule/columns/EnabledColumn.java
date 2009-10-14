package jd.plugins.optional.schedule.columns;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;

import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.plugins.optional.schedule.Actions;
import jd.plugins.optional.schedule.Schedule;

import org.jdesktop.swingx.renderer.JRendererCheckBox;

public class EnabledColumn extends JDTableColumn implements ActionListener {

    private static final long serialVersionUID = 2684119930915940150L;
    private JRendererCheckBox boolrend;
    private JCheckBox checkbox;
    private Schedule schedule;

    public EnabledColumn(String name, JDTableModel table, Schedule schedule) {
        super(name, table);
        this.schedule = schedule;
        boolrend = new JRendererCheckBox();
        boolrend.setHorizontalAlignment(JCheckBox.CENTER);
        checkbox = new JCheckBox();
        checkbox.setHorizontalAlignment(JCheckBox.CENTER);
    }

    @Override
    public Object getCellEditorValue() {
        return checkbox.isSelected();
    }

    @Override
    public boolean isEditable(Object obj) {
        return true;
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
    public Component myTableCellEditorComponent(JDTableModel table, Object value, boolean isSelected, int row, int column) {
        checkbox.removeActionListener(this);
        checkbox.setSelected(((Actions) value).isEnabled());
        checkbox.addActionListener(this);
        return checkbox;
    }

    @Override
    public Component myTableCellRendererComponent(JDTableModel table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        boolrend.setSelected(((Actions) value).isEnabled());
        return boolrend;
    }

    @Override
    public void setValue(Object value, Object object) {
        ((Actions) object).setEnabled((Boolean) value);
        schedule.saveActions();
        schedule.updateTable();
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
    }

    public void actionPerformed(ActionEvent e) {
        checkbox.removeActionListener(this);
        this.fireEditingStopped();
    }

}
