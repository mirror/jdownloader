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

package jd.plugins.optional.customizer.columns;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JTextField;

import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.plugins.optional.customizer.CustomizeSetting;

import org.jdesktop.swingx.renderer.JRendererLabel;

public class PackageNameColumn extends JDTableColumn implements ActionListener {

    private static final long serialVersionUID = -2305836770033923728L;
    private JRendererLabel jlr;
    private JTextField text;

    public PackageNameColumn(String name, JDTableModel table) {
        super(name, table);
        jlr = new JRendererLabel();
        jlr.setBorder(null);
        text = new JTextField();
        setClickstoEdit(2);
    }

    @Override
    public Object getCellEditorValue() {
        return text.getText();
    }

    @Override
    public boolean isEditable(Object obj) {
        return isEnabled(obj);
    }

    @Override
    public boolean isEnabled(Object obj) {
        return ((CustomizeSetting) obj).isEnabled();
    }

    @Override
    public boolean isSortable(Object obj) {
        return false;
    }

    @Override
    public Component myTableCellEditorComponent(JDTableModel table, Object value, boolean isSelected, int row, int column) {
        text.removeActionListener(this);
        text.setText(((CustomizeSetting) value).getPackageName());
        text.addActionListener(this);
        return text;
    }

    @Override
    public Component myTableCellRendererComponent(JDTableModel table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        jlr.setText(((CustomizeSetting) value).getPackageName());
        return jlr;
    }

    @Override
    public void setValue(Object value, Object object) {
        ((CustomizeSetting) object).setPackageName((String) value);
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == text) {
            text.removeActionListener(this);
            this.fireEditingStopped();
        }
    }

}
