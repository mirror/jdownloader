package jd.gui.swing.jdgui.settings.panels.premium.Columns;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JTable;
import javax.swing.JTextField;

import jd.gui.swing.components.JDTable.JDTableColumn;
import jd.gui.swing.components.JDTable.JDTableModel;
import jd.gui.swing.jdgui.settings.panels.premium.HostAccounts;
import jd.plugins.Account;

public class UserColumn extends JDTableColumn implements ActionListener {

    private JTextField user;

    public UserColumn(String name, JDTableModel table) {
        super(name, table);
        user = new JTextField();
    }

    private static final long serialVersionUID = -5291590062503352550L;
    private Component co;
    private Component coedit;

    @Override
    public Component myTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        user.removeActionListener(this);
        user.setText(((Account) value).getUser());
        user.addActionListener(this);
        coedit = user;
        return coedit;
    }

    @Override
    public Component myTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof Account) {
            Account ac = (Account) value;
            value = ac.getUser();
            co = getDefaultTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        } else {
            co = getDefaultTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
            co.setBackground(table.getBackground().darker());
        }
        return co;
    }

    @Override
    public boolean isEditable(Object ob) {
        if (ob != null && ob instanceof Account) return true;
        return false;
    }

    @Override
    public void setValue(Object value, Object o) {
        String pw = (String) value;
        if (o instanceof Account) ((Account) o).setUser(pw);
    }

    public Object getCellEditorValue() {
        if (coedit == null) return null;
        return ((JTextField) coedit).getText();
    }

    public void actionPerformed(ActionEvent e) {
        user.removeActionListener(this);
        this.fireEditingStopped();
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
