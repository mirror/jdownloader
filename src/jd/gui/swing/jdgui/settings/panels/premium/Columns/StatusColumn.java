package jd.gui.swing.jdgui.settings.panels.premium.Columns;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JTable;

import jd.gui.swing.components.JDTable.JDTableColumn;
import jd.gui.swing.components.JDTable.JDTableModel;
import jd.gui.swing.jdgui.settings.panels.premium.HostAccounts;
import jd.plugins.Account;
import jd.plugins.AccountInfo;

public class StatusColumn extends JDTableColumn {

    public StatusColumn(String name, JDTableModel table) {
        super(name, table);
    }

    private static final long serialVersionUID = -5291590062503352550L;
    private Component co;
    private static Dimension dim = new Dimension(200, 30);

    @Override
    public Component myTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Component myTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof Account) {
            Account ac = (Account) value;
            AccountInfo ai = ac.getAccountInfo();
            if (ai == null) {
                value = "";
            } else {
                value = ai.getStatus();
            }
            co = getDefaultTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        } else {
            co = getDefaultTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
            co.setBackground(table.getBackground().darker());
        }
        return co;
    }

    @Override
    public boolean isEditable(Object obj) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setValue(Object value, Object object) {
        // TODO Auto-generated method stub

    }

    public Object getCellEditorValue() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isSortable(Object obj) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isEnabled(Object obj) {
        if (obj == null) return false;
        if (obj instanceof Account) return ((Account) obj).isEnabled();
        if (obj instanceof HostAccounts) return ((HostAccounts) obj).isEnabled();
        return true;
    }

}
