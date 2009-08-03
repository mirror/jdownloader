package jd.gui.swing.jdgui.views.premiumview;

import java.awt.Component;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableCellEditor;

import jd.plugins.Account;

public class PremiumTableEditor extends AbstractCellEditor implements TableCellEditor {

    JCheckBox checkbox;
    JComponent co;
    private PremiumTable table;

    /**
     * 
     */

    public PremiumTableEditor(PremiumTable table) {
        this.table = table;
        checkbox = new JCheckBox();
        checkbox.setHorizontalAlignment(JCheckBox.CENTER);

    }

    private static final long serialVersionUID = 5282897873177369728L;

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        Account ac = (Account) value;
        switch (column) {
        case PremiumJTableModel.COL_ENABLED:
            checkbox.setSelected(ac.isEnabled());
            co = checkbox;
            break;
        default:
            co = null;
        }
        return co;
    }

    @Override
    public void addCellEditorListener(CellEditorListener l) {

    }

    @Override
    public void cancelCellEditing() {
        System.out.println("cancel");

    }

    @Override
    public Object getCellEditorValue() {
        if (co == null) return null;
        if (co instanceof JCheckBox) {
            boolean b = ((JCheckBox) co).isSelected();
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
    public void removeCellEditorListener(CellEditorListener l) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean shouldSelectCell(EventObject anEvent) {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean stopCellEditing() {
        System.out.println("stop");
        return true;
    }

}
