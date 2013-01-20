package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import jd.controlling.AccountController;
import jd.controlling.AccountControllerEvent;
import jd.controlling.AccountControllerListener;
import jd.controlling.IOEQ;
import jd.controlling.accountchecker.AccountChecker;
import jd.controlling.accountchecker.AccountCheckerEventListener;
import jd.nutils.Formatter;
import jd.plugins.Account;
import jd.plugins.AccountInfo;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtCheckColumn;
import org.appwork.swing.exttable.columns.ExtDateColumn;
import org.appwork.swing.exttable.columns.ExtPasswordEditorColumn;
import org.appwork.swing.exttable.columns.ExtProgressColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;

public class PremiumAccountTableModel extends ExtTableModel<Account> implements AccountCheckerEventListener {

    private static final long      serialVersionUID       = 3120481189794897020L;

    private AccountManagerSettings accountManagerSettings = null;

    private DelayedRunnable        delayedFill;

    private volatile boolean       checkRunning           = false;

    private DelayedRunnable        delayedUpdate;

    public PremiumAccountTableModel(final AccountManagerSettings accountManagerSettings) {
        super("PremiumAccountTableModel2");
        this.accountManagerSettings = accountManagerSettings;
        delayedFill = new DelayedRunnable(IOEQ.TIMINGQUEUE, 250l) {

            @Override
            public void delayedrun() {
                _refill();
            }

        };
        delayedUpdate = new DelayedRunnable(IOEQ.TIMINGQUEUE, 250l) {

            @Override
            public void delayedrun() {
                _update();
            }

        };
        AccountController.getInstance().getBroadcaster().addListener(new AccountControllerListener() {

            public void onAccountControllerEvent(AccountControllerEvent event) {
                if (accountManagerSettings.isShown()) {
                    switch (event.getType()) {
                    case UPDATE:
                        /* just repaint */
                        delayedUpdate.run();
                        break;
                    default:
                        /* structure changed */
                        delayedFill.run();
                    }
                }
            }
        });
        if (AccountChecker.getInstance().isRunning()) {
            onCheckStarted();
        }
        AccountChecker.getInstance().getEventSender().addListener(this);
    }

    public void fill() {
        delayedFill.run();
    }

    @Override
    protected void initColumns() {

        this.addColumn(new ExtCheckColumn<Account>(_GUI._.premiumaccounttablemodel_column_enabled()) {

            private static final long serialVersionUID = 1515656228974789237L;

            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

                    private static final long serialVersionUID = 3224931991570756349L;

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setIcon(NewTheme.I().getIcon("ok", 14));
                        setHorizontalAlignment(CENTER);
                        setText(null);
                        return this;
                    }

                };

                return ret;
            }

            @Override
            public int getMaxWidth() {
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
            protected void setBooleanValue(boolean value, final Account object) {
                object.setEnabled(value);
                AccountController.getInstance().saveDelayedRequest();
            }
        });
        this.addColumn(new ActionColumn());
        this.addColumn(new ExtTextColumn<Account>(_GUI._.premiumaccounttablemodel_column_hoster()) {

            private static final long serialVersionUID = -3693931358975303164L;

            @Override
            public boolean isEnabled(Account obj) {
                return obj.isEnabled();
            }

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            protected Icon getIcon(Account value) {
                return value.getDomainInfo().getFavIcon();
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
            protected String getTooltipText(Account obj) {
                return obj.getHoster();
            }

            @Override
            public String getStringValue(Account value) {
                if (getWidth() < 60) return "";

                return value.getHoster();
            }

        });

        this.addColumn(new ExtTextColumn<Account>(_GUI._.premiumaccounttablemodel_column_user()) {

            private static final long serialVersionUID = -8070328156326837828L;

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            public int getMaxWidth() {
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
                return true;
            }

            @Override
            protected void setStringValue(String value, Account object) {
                object.setUser(value);
                AccountController.getInstance().saveDelayedRequest();
            }

            @Override
            public String getStringValue(Account value) {
                return value.getUser();
            }
        });
        this.addColumn(new ExtPasswordEditorColumn<Account>(_GUI._.premiumaccounttablemodel_column_password()) {
            private static final long serialVersionUID = 3180414754658474808L;

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            public int getMaxWidth() {
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
                object.setPass(value);
                AccountController.getInstance().saveDelayedRequest();
            }
        });

        this.addColumn(new ExtDateColumn<Account>(_GUI._.premiumaccounttablemodel_column_expiredate()) {
            private static final long serialVersionUID = 5067606909520874358L;

            @Override
            public boolean isEnabled(Account obj) {
                return obj.isEnabled();
            }

            @Override
            public int getMaxWidth() {
                return 100;
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
                return "dd.MM.yy";
            }

            @Override
            protected Date getDate(Account o2, Date date) {
                AccountInfo ai = o2.getAccountInfo();
                if (ai == null) {
                    return null;
                } else {
                    if (ai.getValidUntil() <= 0) return null;
                    return new Date(ai.getValidUntil());
                }
            }
        });

        this.addColumn(new ExtProgressColumn<Account>(_GUI._.premiumaccounttablemodel_column_trafficleft()) {
            private static final long serialVersionUID = -8376056840172682617L;

            @Override
            public boolean isEnabled(Account obj) {
                return obj.isEnabled();
            }

            @Override
            public int getMinWidth() {
                return 120;
            }

            protected boolean isIndeterminated(final Account value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
                if (checkRunning) { return AccountChecker.getInstance().contains(value); }
                if (value.isValid() && value.isEnabled() && value.isTempDisabled()) return true;
                return false;

            }

            @Override
            protected String getString(Account ac, long current, long total) {
                AccountInfo ai = ac.getAccountInfo();
                long timeout = -1;
                if (!ac.isValid()) {
                    return "";
                } else if (ac.isEnabled() && ac.isTempDisabled() && ((timeout = ac.getTmpDisabledTimeout() - System.currentTimeMillis()) > 0)) {
                    return _GUI._.premiumaccounttablemodel_column_trafficleft_tempdisabled(TimeFormatter.formatMilliSeconds(timeout, 0));
                } else if (ai == null) {
                    return "";
                } else {
                    // COL_PROGRESS = COL_PROGRESS_NORMAL;
                    if (ai.isUnlimitedTraffic()) {
                        return _GUI._.premiumaccounttablemodel_column_trafficleft_unlimited();
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

    public void onCheckStarted() {
        checkRunning = true;
    }

    public void onCheckStopped() {
        checkRunning = false;
        _update();
    }

    protected void _update() {
        if (accountManagerSettings.isShown()) {
            new EDTRunner() {
                @Override
                protected void runInEDT() {
                    PremiumAccountTableModel.this.getTable().repaint();
                }
            };
        }
    }

    protected void _refill() {
        if (accountManagerSettings.isShown()) {
            final java.util.List<Account> newtableData = new ArrayList<Account>(this.getRowCount());
            for (LazyHostPlugin plugin : HostPluginController.getInstance().list()) {
                List<Account> accs = AccountController.getInstance().list(plugin.getDisplayName());
                if (accs != null) {
                    for (Account acc : accs) {
                        newtableData.add(acc);
                        acc.setHoster(plugin.getDisplayName());
                    }
                }
            }
            _fireTableStructureChanged(newtableData, true);
        }
    }

}
