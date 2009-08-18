package jd.gui.swing.jdgui.settings.panels.premium.Columns;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import jd.gui.swing.components.JDTable.JDTableColumn;
import jd.gui.swing.components.JDTable.JDTableModel;
import jd.gui.swing.jdgui.settings.panels.premium.HostAccounts;
import jd.plugins.Account;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.jdesktop.swingx.renderer.JRendererLabel;

public class HosterColumn extends JDTableColumn {

    private static final long serialVersionUID = -6741644821097309670L;
    private Component co;
    private static Border leftGap = BorderFactory.createEmptyBorder(0, 30, 0, 0);
    private static Dimension dim = new Dimension(200, 30);

    public HosterColumn(String name, JDTableModel table) {
        super(name, table);
    }

    @Override
    public Component myTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        return null;
    }

    @Override
    public Component myTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof Account) {
            co = getDefaultTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            ((JRendererLabel) co).setText(JDL.L("jd.gui.swing.jdgui.settings.panels.premium.PremiumTableRenderer.account", "Account"));
            ((JRendererLabel) co).setBorder(leftGap);
            ((JRendererLabel) co).setHorizontalAlignment(SwingConstants.RIGHT);
            co.setEnabled(((Account) value).isEnabled());
        } else {
            HostAccounts ha = (HostAccounts) value;
            String host = ha.getHost();
            co = getDefaultTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            ((JRendererLabel) co).setIcon(JDUtilities.getPluginForHost(host).getHosterIcon());
            ((JRendererLabel) co).setText(host);
            co.setEnabled(ha.isEnabled());
            co.setBackground(table.getBackground().darker());
        }
        co.setSize(dim);
        return co;
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

}