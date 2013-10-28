package jd.gui.swing.jdgui.components.premiumbar;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import jd.controlling.AccountController;
import jd.controlling.AccountControllerEvent;
import jd.controlling.AccountControllerListener;
import jd.controlling.accountchecker.AccountChecker;
import jd.controlling.accountchecker.AccountCheckerEventListener;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.AccountEntry;
import jd.nutils.Formatter;
import jd.plugins.AccountInfo;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.swing.MigPanel;
import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtCheckColumn;
import org.appwork.swing.exttable.columns.ExtComponentColumn;
import org.appwork.swing.exttable.columns.ExtDateColumn;
import org.appwork.swing.exttable.columns.ExtProgressColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.renderer.RendererMigPanel;
import org.jdownloader.DomainInfo;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.updatev2.gui.LAFOptions;

import sun.swing.SwingUtilities2;

public class AccountListTableModel extends ExtTableModel<AccountEntry> implements AccountCheckerEventListener, AccountControllerListener {

    private static final long                serialVersionUID = 3120481189794897020L;

    private DelayedRunnable                  delayedFill;

    private volatile boolean                 checkRunning     = false;

    private DelayedRunnable                  delayedUpdate;

    private ExtComponentColumn<AccountEntry> details;

    private LinkedList<AccountEntry>         accounts;

    private AccountTooltipOwner              owner;

    public AccountListTableModel(AccountTooltip accountTooltip, AccountTooltipOwner owner) {
        super("PremiumAccountfilteredTableModel3");
        this.owner = owner;
        ScheduledExecutorService scheduler = DelayedRunnable.getNewScheduledExecutorService();
        delayedFill = new DelayedRunnable(scheduler, 250l) {

            @Override
            public String getID() {
                return "PremiumAccountFilteredTableFill";
            }

            @Override
            public void delayedrun() {
                _refill();
            }

        };
        delayedUpdate = new DelayedRunnable(scheduler, 250l) {

            @Override
            public String getID() {
                return "PremiumAccountFilteredTableUpdate";
            }

            @Override
            public void delayedrun() {
                _update();
            }

        };
        AccountController.getInstance().getBroadcaster().addListener(this, true);
        AccountChecker.getInstance().getEventSender().addListener(AccountListTableModel.this, true);
        if (AccountChecker.getInstance().isRunning()) {
            onCheckStarted();
        }
        super.initModel();

    }

    protected void initModel() {

    }

    public void fill() {
        delayedFill.run();
    }

    @Override
    protected void initColumns() {
        if (owner != null && owner instanceof ServicePanel) {
            this.addColumn(new ExtCheckColumn<AccountEntry>(_GUI._.premiumaccounttablemodel_column_enabled()) {

                private static final long serialVersionUID = 1515656228974789237L;

                public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                    final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {
                        {
                            AccountListTable.setHeaderRendererColors(this);

                        }

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
                protected boolean getBooleanValue(AccountEntry value) {
                    return value.getAccount().isEnabled();
                }

                @Override
                public boolean isEditable(AccountEntry obj) {
                    return true;
                }

                @Override
                protected void setBooleanValue(boolean value, final AccountEntry object) {
                    object.getAccount().setEnabled(value);
                    fireTableStructureChanged();
                }
            });
        }
        this.addColumn(new ExtTextColumn<AccountEntry>(_GUI._.premiumaccounttablemodel_column_hoster()) {

            private static final long serialVersionUID = -3693931358975303164L;

            @Override
            public boolean isEnabled(AccountEntry obj) {
                return obj.getAccount().isEnabled();
            }

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            public void resetRenderer() {
                super.resetRenderer();
                rendererField.setForeground(LAFOptions.getInstance().getColorForTooltipForeground());

            }

            /**
             * @return
             */
            protected Color getDefaultForeground() {

                return LAFOptions.getInstance().getColorForTooltipForeground();
            }

            @Override
            protected Icon getIcon(AccountEntry value) {
                return DomainInfo.getInstance(value.getAccount().getHoster()).getFavIcon();
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
            protected String getTooltipText(AccountEntry obj) {
                return obj.getAccount().getHoster();
            }

            @Override
            public void configureRendererComponent(AccountEntry value, boolean isSelected, boolean hasFocus, int row, int column) {
                this.rendererIcon.setIcon(this.getIcon(value));
                String str = null;
                if (getWidth() > 60) str = this.getStringValue(value);
                if (str == null) {
                    str = "";
                }
                if (this.getTableColumn() != null) {
                    this.rendererField.setText(SwingUtilities2.clipStringIfNecessary(this.rendererField, this.rendererField.getFontMetrics(this.rendererField.getFont()), str, this.getTableColumn().getWidth() - this.rendererIcon.getPreferredSize().width - 5));
                } else {
                    this.rendererField.setText(str);
                }
            }

            @Override
            public String getStringValue(AccountEntry value) {
                return value.getAccount().getHoster();
            }

        });
        this.addColumn(new ExtTextColumn<AccountEntry>(_GUI._.premiumaccounttablemodel_column_user()) {

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
            public void resetRenderer() {
                super.resetRenderer();
                rendererField.setForeground(LAFOptions.getInstance().getColorForTooltipForeground());

            }

            protected Color getDefaultForeground() {

                return LAFOptions.getInstance().getColorForTooltipForeground();
            }

            @Override
            public int getMinWidth() {
                return 100;
            }

            @Override
            public boolean isEnabled(AccountEntry obj) {
                return obj.getAccount().isEnabled();
            }

            @Override
            public boolean isEditable(AccountEntry obj) {
                return false;
            }

            @Override
            protected void setStringValue(String value, AccountEntry object) {
                object.getAccount().setUser(value);
            }

            @Override
            public String getStringValue(AccountEntry value) {
                return value.getAccount().getUser();
            }
        });

        this.addColumn(new ExtDateColumn<AccountEntry>(_GUI._.premiumaccounttablemodel_column_expiredate()) {
            private static final long serialVersionUID = 5067606909520874358L;

            @Override
            public boolean isEnabled(AccountEntry obj) {
                return obj.getAccount().isEnabled();
            }

            @Override
            public int getMaxWidth() {
                return 100;
            }

            protected Color getDefaultForeground() {

                return LAFOptions.getInstance().getColorForTooltipForeground();
            }

            @Override
            public void resetRenderer() {
                super.resetRenderer();
                rendererField.setForeground(LAFOptions.getInstance().getColorForTooltipForeground());

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
                return _GUI._.PremiumAccountTableModel_getDateFormatString_();
            }

            @Override
            protected Date getDate(AccountEntry o2, Date date) {
                AccountInfo ai = o2.getAccount().getAccountInfo();
                if (ai == null) {
                    return null;
                } else {
                    if (ai.getValidUntil() <= 0) return null;
                    return new Date(ai.getValidUntil());
                }
            }
        });

        this.addColumn(new ExtProgressColumn<AccountEntry>(_GUI._.premiumaccounttablemodel_column_trafficleft()) {
            private static final long serialVersionUID = -8376056840172682617L;

            @Override
            public boolean isEnabled(AccountEntry obj) {
                return obj.getAccount().isEnabled();
            }

            @Override
            public int getMinWidth() {
                return 140;
            }

            protected Color getDefaultForeground() {

                return null;
            }

            protected boolean isIndeterminated(final AccountEntry value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
                if (checkRunning) { return AccountChecker.getInstance().contains(value.getAccount()); }
                if (value.getAccount().isValid() && value.getAccount().isEnabled() && value.getAccount().isTempDisabled()) return true;
                return false;

            }

            @Override
            protected String getString(AccountEntry ac, long current, long total) {
                AccountInfo ai = ac.getAccount().getAccountInfo();
                long timeout = -1;
                if (!ac.getAccount().isValid()) {
                    return "";
                } else if (ac.getAccount().isEnabled() && ac.getAccount().isTempDisabled() && ((timeout = ac.getAccount().getTmpDisabledTimeout() - System.currentTimeMillis()) > 0)) {
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
            protected long getMax(AccountEntry ac) {
                AccountInfo ai = ac.getAccount().getAccountInfo();
                if (!ac.getAccount().isValid()) {
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
            protected long getValue(AccountEntry ac) {
                AccountInfo ai = ac.getAccount().getAccountInfo();
                if (!ac.getAccount().isValid()) {
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
        if (owner != null && owner instanceof ServicePanel) {
            this.addColumn(details = new ExtComponentColumn<AccountEntry>(_GUI._.premiumaccounttablemodel_column_info()) {
                private JButton      button;
                private MigPanel     panel;
                private JButton      rbutton;
                private MigPanel     rpanel;
                private AccountEntry editing;

                {
                    button = new JButton(_GUI._.premiumaccounttablemodel_column_info_button());

                    panel = new RendererMigPanel("ins 2", "[]", "[16!]");
                    panel.add(button);
                    button.setOpaque(false);

                    rbutton = new JButton(_GUI._.premiumaccounttablemodel_column_info_button());

                    rbutton.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            if (editing != null) {
                                editing.showAccountInfoDialog();
                            }
                        }
                    });

                    rpanel = new MigPanel("ins 2", "[]", "[18!]");
                    rpanel.add(rbutton);
                    rbutton.setOpaque(false);
                }

                @Override
                public int getMaxWidth() {
                    return panel.getPreferredSize().width;
                }

                @Override
                public boolean isEnabled(AccountEntry obj) {
                    return obj.getAccount().isEnabled() && obj.isDetailsDialogSupported();
                }

                @Override
                public int getDefaultWidth() {
                    return panel.getPreferredSize().width;
                }

                @Override
                public int getMinWidth() {
                    return panel.getPreferredSize().width;
                }

                @Override
                public boolean isEditable(AccountEntry obj) {
                    return super.isEditable(obj);
                }

                @Override
                public boolean onSingleClick(MouseEvent e, AccountEntry obj) {
                    return super.onSingleClick(e, obj);
                }

                @Override
                protected JComponent getInternalEditorComponent(AccountEntry value, boolean isSelected, int row, int column) {
                    return rpanel;
                }

                @Override
                protected JComponent getInternalRendererComponent(AccountEntry value, boolean isSelected, boolean hasFocus, int row, int column) {

                    return panel;
                }

                @Override
                public void configureEditorComponent(AccountEntry value, boolean isSelected, int row, int column) {
                    editing = value;
                    // rbutton.setEnabled(isEnabled(value));
                }

                @Override
                public void configureRendererComponent(AccountEntry value, boolean isSelected, boolean hasFocus, int row, int column) {
                    // button.setEnabled(isEnabled(value));
                    ;
                }

                @Override
                public void resetEditor() {
                    rpanel.setBackground(null);
                    rpanel.setOpaque(false);
                }

                @Override
                public void resetRenderer() {
                    panel.setBackground(null);
                    panel.setOpaque(false);
                }

            });
        }
    }

    public void onCheckStarted() {
        checkRunning = true;
    }

    public void onCheckStopped() {
        checkRunning = false;
        _update();
    }

    protected void _update() {

        new EDTRunner() {
            @Override
            protected void runInEDT() {
                AccountListTableModel.this.getTable().repaint();
            }
        };

    }

    protected void _refill() {

        final java.util.List<AccountEntry> newtableData = new ArrayList<AccountEntry>(accounts);
        boolean hasDetailsButton = false;

        for (AccountEntry acc : accounts) {

            if (acc.isDetailsDialogSupported() && acc.getAccount().isValid()) {
                hasDetailsButton = true;

            }
        }
        if (details != null) setColumnVisible(details, hasDetailsButton);
        _fireTableStructureChanged(newtableData, true);
    }

    public void setData(LinkedList<AccountEntry> domains) {
        accounts = domains;
        _refill();
    }

    @Override
    public void onAccountControllerEvent(AccountControllerEvent event) {

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
