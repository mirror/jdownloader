package jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel.dialog;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JComponent;

import jd.controlling.accountchecker.AccountChecker;
import jd.controlling.accountchecker.AccountCheckerEventListener;
import jd.gui.swing.jdgui.GUIUtils;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel.AccountInterface;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel.AccountWrapper;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel.GroupWrapper;
import jd.nutils.Formatter;
import jd.plugins.AccountTrafficView;

import org.appwork.swing.exttable.columns.ExtDateColumn;
import org.appwork.swing.exttable.columns.ExtProgressColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.swing.exttable.tree.ExtTreeTableModel;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.swing.renderer.RendererMigPanel;
import org.jdownloader.controlling.hosterrule.FreeAccountReference;
import org.jdownloader.gui.translate._GUI;

public class HosterPriorityTableModel extends ExtTreeTableModel<AccountInterface> implements AccountCheckerEventListener {
    public HosterPriorityTableModel() {
        super("HosterPriorityTableModel");
        AccountChecker.getInstance().getEventSender().addListener(this, true);
        checkRunning = AccountChecker.getInstance().isRunning();
    }

    public void onCheckStarted() {
        checkRunning = true;
        fireTableDataChanged();
    }

    public void onCheckStopped() {
        checkRunning = false;
        fireTableDataChanged();
    }

    private volatile boolean checkRunning = false;

    @Override
    protected void initColumns() {
        this.addColumn(new PackageColumn());
        addColumn(new GroupRuleColumn());
        this.addColumn(new ExtTextColumn<AccountInterface>(_GUI.T.premiumaccounttablemodel_column_user()) {
            private static final long serialVersionUID = -8070328156326837828L;

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            public int getDefaultWidth() {
                return getMinWidth();
            }

            @Override
            public boolean isSortable(final AccountInterface obj) {
                return false;
            }

            @Override
            public int getMinWidth() {
                return 100;
            }

            @Override
            public boolean isEditable(AccountInterface obj) {
                return false;
            }

            @Override
            protected void setStringValue(String value, AccountInterface object) {
            }

            @Override
            public String getStringValue(AccountInterface value) {
                return GUIUtils.getAccountName(value.getUser());
            }
        });
        this.addColumn(new ExtDateColumn<AccountInterface>(_GUI.T.premiumaccounttablemodel_column_expiredate()) {
            private static final long serialVersionUID = 5067606909520874358L;

            @Override
            public boolean isEnabled(AccountInterface obj) {
                return obj.isEnabled();
            }

            @Override
            public int getMaxWidth() {
                return 100;
            }

            @Override
            public boolean isSortable(final AccountInterface obj) {
                return false;
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
            protected String getDateFormatString() {
                DateFormat sd = SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT);
                if (sd instanceof SimpleDateFormat) {
                    return ((SimpleDateFormat) sd).toPattern();
                }
                return _GUI.T.PremiumAccountTableModel_getDateFormatString_();
            }

            @Override
            protected Date getDate(AccountInterface o2, Date date) {
                if (o2 instanceof AccountWrapper) {
                    return ((AccountWrapper) o2).getExpireDate();
                }
                return null;
            }
        });
        this.addColumn(new ExtProgressColumn<AccountInterface>(_GUI.T.premiumaccounttablemodel_column_trafficleft()) {
            private static final long serialVersionUID = -8376056840172682617L;
            private final JComponent  empty            = new RendererMigPanel("ins 0", "[]", "[]");

            @Override
            public boolean isEnabled(AccountInterface obj) {
                return obj.isEnabled();
            }

            @Override
            public boolean isSortable(final AccountInterface obj) {
                return false;
            }

            @Override
            public int getMinWidth() {
                return 120;
            }

            protected boolean isIndeterminated(final AccountInterface value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
                if (value instanceof AccountWrapper) {
                    AccountWrapper aw = (AccountWrapper) value;
                    if (aw.getAccount() instanceof FreeAccountReference) {
                        return false;
                    }
                    if (checkRunning) {
                        return AccountChecker.getInstance().contains(aw.getAccount().getAccount());
                    }
                    if (aw.isValid() && aw.getAccount().isEnabled() && aw.isTempDisabled()) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public JComponent getRendererComponent(AccountInterface value, boolean isSelected, boolean hasFocus, int row, int column) {
                if (value instanceof GroupWrapper) {
                    return empty;
                }
                JComponent ret = super.getRendererComponent(value, isSelected, hasFocus, row, column);
                return ret;
            }

            @Override
            protected String getString(AccountInterface value, long current, long total) {
                if (value instanceof AccountWrapper) {
                    final AccountWrapper accountWrapper = (AccountWrapper) value;
                    if (!accountWrapper.isValid()) {
                        return "";
                    } else {
                        long timeout = -1;
                        if (accountWrapper.getAccount().isEnabled() && accountWrapper.isTempDisabled() && ((timeout = accountWrapper.getTmpDisabledTimeout() - System.currentTimeMillis()) > 0)) {
                            return _GUI.T.premiumaccounttablemodel_column_trafficleft_tempdisabled(TimeFormatter.formatMilliSeconds(timeout, 0));
                        } else {
                            final AccountTrafficView accountTrafficView = accountWrapper.getAccount().getAccountTrafficView();
                            if (accountTrafficView == null) {
                                return "";
                            } else {
                                // COL_PROGRESS = COL_PROGRESS_NORMAL;
                                if (accountTrafficView.isUnlimitedTraffic()) {
                                    return _GUI.T.premiumaccounttablemodel_column_trafficleft_unlimited();
                                } else {
                                    return Formatter.formatReadable(accountTrafficView.getTrafficLeft()) + "/" + Formatter.formatReadable(accountTrafficView.getTrafficMax());
                                }
                            }
                        }
                    }
                }
                return "";
            }

            @Override
            protected long getMax(AccountInterface value) {
                if (value instanceof AccountWrapper) {
                    final AccountWrapper accountWrapper = (AccountWrapper) value;
                    if (!accountWrapper.isValid()) {
                        return 100;
                    }
                    final AccountTrafficView accountTrafficView = accountWrapper.getAccount().getAccountTrafficView();
                    if (accountTrafficView == null) {
                        return 100;
                    } else {
                        if (accountTrafficView.isUnlimitedTraffic()) {
                            return 100;
                        } else {
                            return accountTrafficView.getTrafficMax();
                        }
                    }
                }
                return 100;
            }

            @Override
            protected long getValue(AccountInterface value) {
                if (value instanceof AccountWrapper) {
                    final AccountWrapper accountWrapper = (AccountWrapper) value;
                    if (!accountWrapper.isValid()) {
                        return 0;
                    }
                    final AccountTrafficView accountTrafficView = accountWrapper.getAccount().getAccountTrafficView();
                    if (accountTrafficView == null) {
                        return 0;
                    } else {
                        if (accountTrafficView.isUnlimitedTraffic()) {
                            return 100;
                        } else {
                            return accountTrafficView.getTrafficLeft();
                        }
                    }
                }
                return -1;
            }
        });
    }
}
