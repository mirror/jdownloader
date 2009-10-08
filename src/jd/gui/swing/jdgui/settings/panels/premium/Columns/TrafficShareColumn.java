package jd.gui.swing.jdgui.settings.panels.premium.Columns;

import java.awt.Component;

import jd.gui.swing.components.table.JDTableColumn;
import jd.gui.swing.components.table.JDTableModel;
import jd.gui.swing.jdgui.settings.panels.premium.HostAccounts;
import jd.nutils.Formatter;
import jd.plugins.Account;
import jd.plugins.AccountInfo;

import org.jdesktop.swingx.renderer.JRendererLabel;

public class TrafficShareColumn extends JDTableColumn {

    private JRendererLabel jlr;

    public TrafficShareColumn(String name, JDTableModel table) {
        super(name, table);
        jlr = new JRendererLabel();
        jlr.setBorder(null);
    }

    private static final long serialVersionUID = -5291590062503352550L;

    @Override
    public Component myTableCellEditorComponent(JDTableModel table, Object value, boolean isSelected, int row, int column) {
        return null;
    }

    @Override
    public boolean defaultEnabled() {
        return false;
    }

    @Override
    public Component myTableCellRendererComponent(JDTableModel table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof Account) {
            Account ac = (Account) value;
            AccountInfo ai = ac.getAccountInfo();
            if (ai == null || ai.getTrafficShareLeft() == -1) {
                jlr.setText("");
            } else {
                jlr.setText(Formatter.formatReadable(ai.getTrafficShareLeft()));
            }
        } else {
            jlr.setText("");
        }
        return jlr;
    }

    @Override
    public boolean isEditable(Object obj) {
        return false;
    }

    @Override
    public void setValue(Object value, Object object) {
    }

    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public boolean isSortable(Object obj) {
        return false;
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
    }

    @Override
    public boolean isEnabled(Object obj) {
        if (obj == null) return false;
        if (obj instanceof Account) return ((Account) obj).isEnabled();
        if (obj instanceof HostAccounts) return ((HostAccounts) obj).isEnabled();
        return true;
    }

}
