//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.swing.jdgui.views.settings.panels.premium.Columns;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPasswordField;

import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.jdgui.views.settings.panels.premium.HostAccounts;
import jd.plugins.Account;
import jd.utils.JDUtilities;

import org.jdesktop.swingx.renderer.JRendererLabel;

public class PassColumn extends JDTableColumn implements ActionListener {

    private static final long serialVersionUID = -5291590062503352550L;
    private final JRendererLabel jlr;
    private final JPasswordField passw;

    public PassColumn(String name, JDTableModel table) {
        super(name, table);
        passw = new JPasswordField();
        if (JDUtilities.getRunType() == JDUtilities.RUNTYPE_LOCAL) {
            passw.putClientProperty("JPasswordField.cutCopyAllowed", Boolean.TRUE);
        }
        jlr = new JRendererLabel();
        jlr.setBorder(null);
        setClickstoEdit(2);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == passw) {
            passw.removeActionListener(this);
            this.fireEditingStopped();
        }
    }

    @Override
    public Object getCellEditorValue() {
        return new String(passw.getPassword());
    }

    @Override
    public boolean isEditable(Object ob) {
        return ob instanceof Account;
    }

    @Override
    public boolean isEnabled(Object obj) {
        if (obj instanceof Account) return ((Account) obj).isEnabled();
        if (obj instanceof HostAccounts) return ((HostAccounts) obj).isEnabled();
        return true;
    }

    @Override
    public boolean isSortable(Object obj) {
        return false;
    }

    @Override
    public Component myTableCellEditorComponent(JDTableModel table, Object value, boolean isSelected, int row, int column) {
        passw.removeActionListener(this);
        passw.setText(((Account) value).getPass());
        passw.addActionListener(this);
        return passw;
    }

    @Override
    public Component myTableCellRendererComponent(JDTableModel table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof Account) {
            jlr.setText("********");
        } else {
            jlr.setText("");
        }
        return jlr;
    }

    @Override
    public void setValue(Object value, Object o) {
        if (o instanceof Account) {
            String pw = (String) value;
            ((Account) o).setPass(pw);
        }
    }

}
