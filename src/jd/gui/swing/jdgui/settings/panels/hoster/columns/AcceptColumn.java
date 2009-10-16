package jd.gui.swing.jdgui.settings.panels.hoster.columns;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;

import jd.HostPluginWrapper;
import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.dialog.AgbDialog;

import org.jdesktop.swingx.renderer.JRendererCheckBox;

public class AcceptColumn extends JDTableColumn implements ActionListener {

    private static final long serialVersionUID = 6083883344402098948L;
    private JRendererCheckBox boolrend;
    private JCheckBox checkbox;

    public AcceptColumn(String name, JDTableModel table) {
        super(name, table);
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
        checkbox.setSelected(((HostPluginWrapper) value).isAGBChecked());
        checkbox.addActionListener(this);
        return checkbox;
    }

    @Override
    public Component myTableCellRendererComponent(JDTableModel table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        boolrend.setSelected(((HostPluginWrapper) value).isAGBChecked());
        return boolrend;
    }

    @Override
    public void setValue(Object value, Object object) {
        if ((Boolean) value) {
            AgbDialog.showDialog(((HostPluginWrapper) object).getPlugin());
        } else {
            ((HostPluginWrapper) object).setAGBChecked(false);
        }
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
    }

    public void actionPerformed(ActionEvent e) {
        checkbox.removeActionListener(this);
        this.fireEditingStopped();
    }

}
