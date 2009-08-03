package jd.gui.swing.jdgui.settings.panels.premium;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import jd.plugins.Account;

public class PremiumTableEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {

    private JCheckBox checkbox;
    private JComponent co;

    // private PremiumTable table;

    public PremiumTableEditor(PremiumTable table) {
        // this.table = table;
        checkbox = new JCheckBox();
        checkbox.setHorizontalAlignment(JCheckBox.CENTER);

    }

    private static final long serialVersionUID = 5282897873177369728L;

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        Account ac = (Account) value;
        switch (column) {
        case PremiumJTableModel.COL_ENABLED:
            checkbox.removeActionListener(this);
            checkbox.setSelected(ac.isEnabled());
            checkbox.addActionListener(this);
            co = checkbox;
            break;
        default:
            co = null;
        }
        return co;
    }

    @Override
    public void cancelCellEditing() {
        System.out.println("cancel");

    }

    public Object getCellEditorValue() {
        if (co == null) return null;
        if (co instanceof JCheckBox) {
            boolean b = ((JCheckBox) co).isSelected();
            System.out.println("set to " + b);
            return b;
        }
        return null;
    }

    @Override
    public boolean isCellEditable(EventObject anEvent) {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean shouldSelectCell(EventObject anEvent) {
        // TODO Auto-generated method stub
        return false;

    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == checkbox) {
            checkbox.removeActionListener(this);
            this.fireEditingStopped();
        }

    }

}
