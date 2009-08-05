package jd.gui.swing.jdgui.settings.panels.premium;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;

import jd.plugins.Account;
import jd.utils.JDUtilities;

public class PremiumTableEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {

    private JCheckBox checkbox;
    private JComponent co;
    private boolean enabled;
    private JDPasswordField passw;
    private JTextField user;

    public PremiumTableEditor() {        
        checkbox = new JCheckBox();
        user = new JTextField();
        checkbox.setHorizontalAlignment(JCheckBox.CENTER);
        passw = new JDPasswordField();
        enabled = false;
    }

    private static final long serialVersionUID = 5282897873177369728L;

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        switch (column) {
        case PremiumJTableModel.COL_ENABLED: {
            if (value instanceof Account) {
                enabled = ((Account) value).isEnabled();
            } else {
                enabled = ((HostAccounts) value).isEnabled();
            }
            checkbox.removeActionListener(this);
            checkbox.setSelected(enabled);
            checkbox.addActionListener(this);
            co = checkbox;
            return co;
        }
        case PremiumJTableModel.COL_PASS: {
            passw.removeActionListener(this);
            passw.setText(((Account) value).getPass());
            passw.addActionListener(this);
            co = passw;
            return co;
        }
        case PremiumJTableModel.COL_USER: {
            user.removeActionListener(this);
            user.setText(((Account) value).getUser());
            user.addActionListener(this);
            co = user;
            return co;
        }
        default:
            co = null;
            break;
        }
        return co;
    }

    @Override
    public void cancelCellEditing() {
        System.out.println("cancel");

    }

    public Object getCellEditorValue() {
        if (co == null) return null;
        if (co instanceof JCheckBox) return ((JCheckBox) co).isSelected();
        if (co instanceof JDPasswordField) return new String(((JDPasswordField) co).getPassword());
        if (co instanceof JTextField) return ((JTextField) co).getText();
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
        return true;

    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == checkbox) {
            checkbox.removeActionListener(this);
            this.fireEditingStopped();
        } else if (e.getSource() == passw) {
            passw.removeActionListener(this);
            this.fireEditingStopped();
        } else if (e.getSource() == user) {
            user.removeActionListener(this);
            this.fireEditingStopped();
        }
    }

    private class JDPasswordField extends JPasswordField implements ClipboardOwner {

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

}
