package jd.gui.swing.jdgui.settings.panels.premium.Columns;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPasswordField;
import javax.swing.JTable;

import jd.gui.swing.components.JDTable.JDTableColumn;
import jd.gui.swing.components.JDTable.JDTableModel;
import jd.gui.swing.jdgui.settings.panels.premium.HostAccounts;
import jd.plugins.Account;
import jd.utils.JDUtilities;

class JDPasswordField extends JPasswordField implements ClipboardOwner {

    private static final long serialVersionUID = -7981118302661369727L;

    public JDPasswordField() {
        super();
    }

    @Override
    public void cut() {
        if (JDUtilities.getRunType() == JDUtilities.RUNTYPE_LOCAL) {
            StringSelection stringSelection = new StringSelection(String.valueOf(this.getSelectedText()));
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, this);

            String text = String.valueOf(this.getPassword());
            int position = this.getSelectionStart();
            String s1 = text.substring(0, position);
            String s2 = text.substring(this.getSelectionEnd(), text.length());
            this.setText(s1 + s2);

            this.setSelectionStart(position);
            this.setSelectionEnd(position);
        }
    }

    @Override
    public void copy() {
        if (JDUtilities.getRunType() == JDUtilities.RUNTYPE_LOCAL) {
            StringSelection stringSelection = new StringSelection(String.valueOf(this.getSelectedText()));
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, this);
        }
    }

    public void lostOwnership(Clipboard arg0, Transferable arg1) {
    }

}

public class PassColumn extends JDTableColumn implements ActionListener {

    private jd.gui.swing.jdgui.settings.panels.premium.Columns.JDPasswordField passw;

    public PassColumn(String name, JDTableModel table) {
        super(name, table);
        passw = new JDPasswordField();
    }

    /**
     * 
     */
    private static final long serialVersionUID = -5291590062503352550L;
    private Component co;
    private Component coedit;

    @Override
    public Component myTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        passw.removeActionListener(this);
        passw.setText(((Account) value).getPass());
        passw.addActionListener(this);
        coedit = passw;
        return coedit;
    }

    @Override
    public Component myTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof Account) {
            co = getDefaultTableCellRendererComponent(table, "*****", isSelected, hasFocus, row, column);
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
        if (o instanceof Account) ((Account) o).setPass(pw);
    }

    public Object getCellEditorValue() {
        if (coedit == null) return null;
        return new String(((JDPasswordField) coedit).getPassword());
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == passw) {
            passw.removeActionListener(this);
            this.fireEditingStopped();
        }
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
