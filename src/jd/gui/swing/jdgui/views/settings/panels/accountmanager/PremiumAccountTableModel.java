package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import jd.HostPluginWrapper;
import jd.controlling.AccountController;
import jd.nutils.Formatter;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.utils.JDUtilities;

import org.appwork.utils.swing.table.ExtTableHeaderRenderer;
import org.appwork.utils.swing.table.ExtTableModel;
import org.appwork.utils.swing.table.columns.ExtCheckColumn;
import org.appwork.utils.swing.table.columns.ExtDateColumn;
import org.appwork.utils.swing.table.columns.ExtPasswordEditorColumn;
import org.appwork.utils.swing.table.columns.ExtProgressColumn;
import org.appwork.utils.swing.table.columns.ExtTextColumn;
import org.jdownloader.extensions.antireconnect.translate.T;
import org.jdownloader.images.Theme;

public class PremiumAccountTableModel extends ExtTableModel<Account> {

    public PremiumAccountTableModel() {
        super("PremiumAccountTableModel2");
        fill();
    }

    private void fill() {
        ArrayList<HostPluginWrapper> plugins = HostPluginWrapper.getHostWrapper();

        synchronized (this.tableData) {
            tableData.clear();

            for (HostPluginWrapper plugin : plugins) {
                ArrayList<Account> accs = AccountController.getInstance().getAllAccounts(plugin.getHost());
                for (Account acc : accs) {
                    tableData.add(acc);
                    acc.setHoster(plugin.getHost());
                }

            }
        }
    }

    @Override
    protected void initColumns() {

        this.addColumn(new ExtCheckColumn<Account>(T._.premiumaccounttablemodel_column_enabled()) {

            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setIcon(Theme.getIcon("ok", 14));
                        setHorizontalAlignment(CENTER);
                        setText(null);
                        return this;
                    }

                };

                return ret;
            }

            @Override
            protected int getMaxWidth() {
                return 30;
            }

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            protected boolean getBooleanValue(Account value) {
                return value.isEnabled();
            }

            @Override
            public boolean isEditable(Account obj) {
                return true;
            }

            @Override
            protected void setBooleanValue(boolean value, Account object) {
                object.setEnabled(value);
            }
        });
        this.addColumn(new ActionColumn());
        this.addColumn(new ExtTextColumn<Account>(T._.premiumaccounttablemodel_column_hoster()) {

            @Override
            public boolean isEnabled(Account obj) {
                return obj.isEnabled();
            }

            @Override
            protected Icon getIcon(Account value) {

                return JDUtilities.getPluginForHost(value.getHoster()).getHosterIconScaled();

            }

            @Override
            public int getDefaultWidth() {
                return 120;
            }

            @Override
            public int getMinWidth() {
                return 30;
            }

            @Override
            protected String getToolTip(Account obj) {
                return obj.getHoster();
            }

            @Override
            protected String getStringValue(Account value) {
                if (getWidth() < 60) return "";

                return value.getHoster();
            }

        });

        this.addColumn(new ExtTextColumn<Account>(T._.premiumaccounttablemodel_column_user()) {

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            protected int getMaxWidth() {
                return 140;
            }

            @Override
            public int getDefaultWidth() {
                return getMinWidth();
            }

            @Override
            public int getMinWidth() {
                return 100;
            }

            @Override
            public boolean isEditable(Account obj) {
                return obj.isEnabled();
            }

            @Override
            protected void setStringValue(String value, Account object) {
                super.setStringValue(value, object);
            }

            @Override
            protected String getStringValue(Account value) {
                return value.getUser();
            }
        });
        this.addColumn(new ExtPasswordEditorColumn<Account>(T._.premiumaccounttablemodel_column_password()) {
            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            protected int getMaxWidth() {
                return 140;
            }

            @Override
            public int getDefaultWidth() {
                return 110;
            }

            @Override
            public int getMinWidth() {
                return 100;
            }

            @Override
            protected String getPlainStringValue(Account value) {
                return value.getPass();
            }

            @Override
            protected void setStringValue(String value, Account object) {
                System.out.println(value);
            }
        });

        this.addColumn(new ExtDateColumn<Account>(T._.premiumaccounttablemodel_column_expiredate()) {
            @Override
            public boolean isEnabled(Account obj) {
                return obj.isEnabled();
            }

            @Override
            protected int getMaxWidth() {
                return 90;
            }

            @Override
            public int getDefaultWidth() {
                return getMinWidth();
            }

            @Override
            public int getMinWidth() {
                return 90;
            }

            @Override
            protected Date getDate(Account o2) {
                AccountInfo ai = o2.getAccountInfo();
                if (ai == null) {
                    return null;
                } else {
                    return new Date(ai.getValidUntil());
                }
            }
        });

        this.addColumn(new ExtProgressColumn<Account>(T._.premiumaccounttablemodel_column_trafficleft()) {
            @Override
            public boolean isEnabled(Account obj) {
                return obj.isEnabled();
            }

            @Override
            public int getMinWidth() {
                return 120;
            }

            @Override
            protected String getString(Account ac) {
                AccountInfo ai = ac.getAccountInfo();
                if (!ac.isValid()) {
                    return T._.premiumaccounttablemodel_column_trafficleft_invalid();
                } else if (ai == null) {
                    return T._.premiumaccounttablemodel_column_trafficleft_unchecked();
                } else {
                    // COL_PROGRESS = COL_PROGRESS_NORMAL;
                    if (ai.isUnlimitedTraffic()) {
                        return T._.premiumaccounttablemodel_column_trafficleft_unlimited();
                    } else {
                        return Formatter.formatReadable(ai.getTrafficLeft()) + "/" + Formatter.formatReadable(ai.getTrafficMax());

                    }
                }
            }

            @Override
            protected long getMax(Account ac) {
                AccountInfo ai = ac.getAccountInfo();
                if (!ac.isValid()) {
                    return 100;
                } else if (ai == null) {
                    return 100;
                } else {
                    if (ai.isUnlimitedTraffic()) {
                        return 100;
                    } else {

                        return ai.getTrafficMax();
                    }
                }
            }

            @Override
            protected long getValue(Account ac) {
                AccountInfo ai = ac.getAccountInfo();
                if (!ac.isValid()) {
                    return 0;
                } else if (ai == null) {
                    return 0;
                } else {

                    if (ai.isUnlimitedTraffic()) {
                        return 100;
                    } else {

                        return ai.getTrafficLeft();
                    }
                }
            }
        });

    }
}
