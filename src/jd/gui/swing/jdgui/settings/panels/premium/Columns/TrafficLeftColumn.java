package jd.gui.swing.jdgui.settings.panels.premium.Columns;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JTable;

import jd.gui.swing.components.JDTable.JDTableColumn;
import jd.gui.swing.components.JDTable.JDTableModel;
import jd.gui.swing.jdgui.settings.panels.premium.HostAccounts;
import jd.gui.swing.jdgui.views.downloadview.JDProgressBar;
import jd.nutils.Formatter;
import jd.plugins.Account;
import jd.plugins.AccountInfo;

public class TrafficLeftColumn extends JDTableColumn {

    private JDProgressBar progress;

    public TrafficLeftColumn(String name, JDTableModel table) {
        super(name, table);
        progress = new JDProgressBar();
        progress.setStringPainted(true);
        progress.setOpaque(true);
    }

    private static final long serialVersionUID = -5291590062503352550L;
    private Component co;
    private static Dimension dim = new Dimension(200, 30);

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        hasFocus = false;
        column = this.getJDTableModel().toModel(column);
        if (value instanceof Account) {
            Account ac = (Account) value;
            AccountInfo ai = ac.getAccountInfo();
            if (!ac.isValid()) {
                value = "Invalid account";
                progress.setMaximum(10);
                progress.setValue(0);
            } else if (ai == null) {
                value = "Unknown";
                progress.setMaximum(10);
                progress.setValue(0);
            } else {
                if (ai.getTrafficLeft() < 0) {
                    value = "Unlimited";
                    progress.setMaximum(10);
                    progress.setValue(10);
                } else {
                    value = Formatter.formatReadable(ai.getTrafficLeft());
                    progress.setMaximum(ai.getTrafficMax());
                    progress.setValue(ai.getTrafficLeft());
                }
            }
            progress.setString((String) value);
            co = progress;
            co.setEnabled(ac.isEnabled());
        } else {
            HostAccounts ha = (HostAccounts) value;
            if (!ha.gotAccountInfos()) {
                value = "Unknown";
            } else {
                if (ha.getTraffic() < 0) {
                    value = "Unlimited";
                } else {
                    value = Formatter.formatReadable(ha.getTraffic());
                }
            }
            co = getDefaultTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            co.setEnabled(ha.isEnabled());
            co.setBackground(table.getBackground().darker());
        }
        co.setSize(dim);
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

}
