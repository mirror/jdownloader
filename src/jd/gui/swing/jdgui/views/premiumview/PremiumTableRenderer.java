package jd.gui.swing.jdgui.views.premiumview;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.UIResource;
import javax.swing.table.TableCellRenderer;

import jd.controlling.AccountController;
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

//    private StatusLabel statuspanel;

    private PremiumTable table;

    private BooleanRenderer boolrend;

    public PremiumTableRenderer(PremiumTable table) {
        this.table = table;
//        statuspanel = new StatusLabel();
        boolrend = new BooleanRenderer();
    }

    // @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        hasFocus = false;
        column = this.table.getTableModel().toModel(column);
        co = getDownloadLinkCell(table, value, isSelected, hasFocus, row, column);
        if (!((Account) value).isEnabled()) {
            co.setEnabled(false);
        } else {
            co.setEnabled(true);
        }
        co.setSize(new Dimension(200, 30));
        return co;
    }

    private Component getDownloadLinkCell(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Account ac = (Account) value;
        AccountInfo ai = ac.getAccountInfo();
        String host = AccountController.getInstance().getHosterName(ac);
        switch (column) {
        case PremiumJTableModel.COL_HOSTER:
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            ((JRendererLabel) co).setIcon(JDUtilities.getPluginForHost(host).getHosterIcon());
            ((JRendererLabel) co).setText(host);
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
            if (ai == null) {
                value = "Unkown";
            } else {
                if (ai.getValidUntil() < 0 || ai.getValidUntil() < System.currentTimeMillis()) {
                    value = "Expired";
                } else {
                    value = Formatter.formatTime(ai.getValidUntil());
                }
            }
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            return co;
        case PremiumJTableModel.COL_TRAFFICLEFT:
            if (ai == null) {
                value = "Unknown";
            } else {
                if (ai.getTrafficLeft() < 0) {
                    value = "Unlimited";
                } else {
                    value = Formatter.formatReadable(ai.getTrafficLeft());
                }
            }
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            return co;
        }
        co = super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
        return co;
    }
}