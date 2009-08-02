package jd.gui.swing.jdgui.views.premiumview;

import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

import jd.HostPluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.plugins.Account;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class PremiumJTableModel extends AbstractTableModel {

    private static final long serialVersionUID = -5223658894343546346L;
    public static final byte COL_HOSTER = 0;
    public static final byte COL_ENABLED = 1;
    public static final byte COL_USER = 2;
    public static final byte COL_PASS = 3;
    public static final byte COL_STATUS = 4;
    public static final byte COL_EXPIREDATE = 5;
    public static final byte COL_TRAFFICLEFT = 6;

    /**
     * DO NOT MOVE THIS CONSTANT. IT's important to have it in this file for the
     * LFE to parse JDL Keys correct
     */
    private static final String IDENT_PREFIX = "jd.gui.swing.jdgui.views.downloadview.";

    private static final String[] COLUMN_NAMES = { JDL.L(IDENT_PREFIX + "hoster", "Hoster"), JDL.L(IDENT_PREFIX + "enabled", "Enabled"), JDL.L(IDENT_PREFIX + "user", "User"), JDL.L(IDENT_PREFIX + "pass", "Password"), JDL.L(IDENT_PREFIX + "status", "Status"), JDL.L(IDENT_PREFIX + "expiredate", "ExpireDate"), JDL.L(IDENT_PREFIX + "trafficleft", "Trafficleft") };
    private ArrayList<Object> list = new ArrayList<Object>();
    private SubConfiguration config;
    private ArrayList<HostPluginWrapper> plugins;

    public PremiumJTableModel() {
        super();
        config = SubConfiguration.getConfig("premiumview");
        plugins = JDUtilities.getPluginsForHost();
        refreshModel();
    }

    public void refreshModel() {
        synchronized (list) {
            list.clear();
            for (HostPluginWrapper plugin : plugins) {
                ArrayList<Account> accs = AccountController.getInstance().getAllAccounts(plugin.getHost());
                if (accs.size() == 0) continue;
                for (Account acc : accs) {
                    list.add(acc);
                }
            }
        }
    }

    public int getRowCount() {
        return list.size();
    }

    public int getRowforObject(Object o) {
        synchronized (list) {
            return list.indexOf(o);
        }
    }

    public Object getObjectforRow(int rowIndex) {
        synchronized (list) {
            if (rowIndex < list.size()) return list.get(rowIndex);
            return null;
        }
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        try {
            return list.get(rowIndex);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public int getRealColumnCount() {
        return COLUMN_NAMES.length;
    }

    public String getRealColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    public int getColumnCount() {
        int j = 0;
        for (int i = 0; i < COLUMN_NAMES.length; ++i) {
            if (isVisible(i)) ++j;
        }
        return j;
    }

    public boolean isVisible(int column) {
        return config.getBooleanProperty("VISABLE_COL_" + column, true);
    }

    public void setVisible(int column, boolean visible) {
        config.setProperty("VISABLE_COL_" + column, visible);
        config.save();
    }

    public int toModel(int column) {
        int i = 0;
        int k;
        for (k = 0; k < getRealColumnCount(); ++k) {
            if (isVisible(k)) {
                ++i;
            }
            if (i > column) break;
        }
        return k;
    }

    public int toVisible(int column) {
        int i = column;
        int k;
        for (k = column; k >= 0; --k) {
            if (!isVisible(k)) {
                --i;
            }
        }
        return i;
    }

    // @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[toModel(column)];
    }

    // @Override
    public Class<?> getColumnClass(int columnIndex) {
        return Object.class;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        switch (columnIndex) {
        case COL_ENABLED:
            return true;
        default:
            return false;
        }
    }

    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        System.out.println(rowIndex + " " + columnIndex + " " + (Boolean) value);
        switch (columnIndex) {
        case COL_ENABLED:
            ((Account) getValueAt(rowIndex, columnIndex)).setEnabled((Boolean) value);
            break;
        }
    }
}
