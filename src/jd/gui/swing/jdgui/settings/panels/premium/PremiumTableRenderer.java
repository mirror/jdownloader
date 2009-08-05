package jd.gui.swing.jdgui.settings.panels.premium;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.UIResource;
import javax.swing.table.TableCellRenderer;

import jd.controlling.AccountController;
import jd.gui.swing.jdgui.views.downloadview.JDProgressBar;
import jd.nutils.Formatter;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.utils.JDUtilities;

import org.jdesktop.swingx.renderer.DefaultTableRenderer;
import org.jdesktop.swingx.renderer.JRendererLabel;

class BooleanRenderer extends JCheckBox implements TableCellRenderer, UIResource {

    private static final long serialVersionUID = 8136614456518376700L;
    private static final Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

    public BooleanRenderer() {
        super();
        setHorizontalAlignment(JLabel.CENTER);
        setBorderPainted(true);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (isSelected) {
            setForeground(table.getSelectionForeground());
            super.setBackground(table.getSelectionBackground());
        } else {
            setForeground(table.getForeground());
            setBackground(table.getBackground());
        }
        setSelected((value != null && ((Boolean) value).booleanValue()));

        if (hasFocus) {
            setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
        } else {
            setBorder(noFocusBorder);
        }

        return this;
    }
}

public class PremiumTableRenderer extends DefaultTableRenderer {

    private static final long serialVersionUID = -3916572910439565199L;

    private Component co;

    // private StatusLabel statuspanel;

    private PremiumTable table;

    private BooleanRenderer boolrend;

    private Border leftGap;

    private JDProgressBar progress;

    public PremiumTableRenderer(PremiumTable table) {
        this.table = table;
        // statuspanel = new StatusLabel();
        boolrend = new BooleanRenderer();
        leftGap = BorderFactory.createEmptyBorder(0, 30, 0, 0);
        progress = new JDProgressBar();
        progress.setStringPainted(true);
        progress.setOpaque(true);
    }

    // @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        hasFocus = false;
        column = this.table.getTableModel().toModel(column);
        if (value instanceof Account) {
            co = getAccountCell(table, value, isSelected, hasFocus, row, column);
            if (!((Account) value).isEnabled()) {
                co.setEnabled(false);
            } else {
                co.setEnabled(true);
            }
        } else {
            co = getHostAccountsCell(table, value, isSelected, hasFocus, row, column);
            if (!((HostAccounts) value).isEnabled()) {
                co.setEnabled(false);
            } else {
                co.setEnabled(true);
            }
        }
        co.setSize(new Dimension(200, 30));
        return co;
    }

    private Component getAccountCell(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Account ac = (Account) value;
        AccountInfo ai = ac.getAccountInfo();
        String host = AccountController.getInstance().getHosterName(ac);
        switch (column) {
        case PremiumJTableModel.COL_HOSTER:
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            ((JRendererLabel) co).setText(host);
            ((JRendererLabel) co).setBorder(leftGap);
            return co;
        case PremiumJTableModel.COL_ENABLED:
            value = ac.isEnabled();
            co = boolrend.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            return co;
        case PremiumJTableModel.COL_USER:
            value = ac.getUser();
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            return co;
        case PremiumJTableModel.COL_PASS:
            value = "*****";
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            return co;
        case PremiumJTableModel.COL_STATUS:
            if (ai == null) {
                value = "";
            } else {
                value = ai.getStatus();
            }
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            return co;
        case PremiumJTableModel.COL_EXPIREDATE:
            if (!ac.isValid()) {
                value = "Invalid account";
            } else if (ai == null) {
                value = "Unkown";
            } else {
                if (ai.getValidUntil() == -1) {
                    value = "Unlimited";
                } else if (ai.isExpired()) {
                    value = "Expired";
                } else {
                    value = Formatter.formatTime(ai.getValidUntil());
                }
            }
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            return co;
        case PremiumJTableModel.COL_TRAFFICLEFT:
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
            return co;
        }
        co = super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
        return co;
    }

    private Component getHostAccountsCell(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        HostAccounts ha = (HostAccounts) value;
        String host = ha.getHost();
        switch (column) {
        case PremiumJTableModel.COL_HOSTER:
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            ((JRendererLabel) co).setIcon(JDUtilities.getPluginForHost(host).getHosterIcon());
            ((JRendererLabel) co).setText(host);
            return co;
        case PremiumJTableModel.COL_ENABLED:
            value = ha.isEnabled();
            co = boolrend.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            return co;
        case PremiumJTableModel.COL_USER:
        case PremiumJTableModel.COL_PASS:
        case PremiumJTableModel.COL_STATUS:
        case PremiumJTableModel.COL_EXPIREDATE:
            co = super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
            return co;
        case PremiumJTableModel.COL_TRAFFICLEFT:
            if (!ha.gotAccountInfos()) {
                value = "Unknown";
            } else {
                if (ha.getTraffic() < 0) {
                    value = "Unlimited";
                } else {
                    value = Formatter.formatReadable(ha.getTraffic());
                }
            }
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            return co;
        }
        co = super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
        return co;
    }
}