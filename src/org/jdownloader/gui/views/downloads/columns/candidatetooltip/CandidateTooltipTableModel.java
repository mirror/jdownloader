package org.jdownloader.gui.views.downloads.columns.candidatetooltip;

import java.awt.Color;
import java.util.Date;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JLabel;

import jd.controlling.downloadcontroller.HistoryEntry;
import jd.plugins.Account;

import org.appwork.swing.MigPanel;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtDateColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.StringUtils;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.swing.renderer.RendererMigPanel;
import org.jdownloader.DomainInfo;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.columns.CandidateAccountColumn;
import org.jdownloader.images.BadgeIcon;
import org.jdownloader.updatev2.gui.LAFOptions;

public class CandidateTooltipTableModel extends ExtTableModel<HistoryEntry> {
    public static interface MaxWidthProvider {
        public int getMaxPreferredWitdh();
    }

    public static final class GatewayColumn extends ExtTextColumn<HistoryEntry> implements MaxWidthProvider {

        private int maxWidth;

        @Override
        public boolean isSortable(HistoryEntry obj) {
            return false;
        }

        public GatewayColumn(String name) {
            super(name);
        }

        @Override
        public int getMinWidth() {
            return 100;
        }

        @Override
        public int getMaxPreferredWitdh() {
            return maxWidth;
        }

        protected MigPanel createRendererPanel() {
            // getprefered Size will notwork on renderlables
            rendererField = new JLabel();
            rendererIcon = new JLabel();
            return new RendererMigPanel("ins 0", "[]0[grow,fill]", "[grow,fill]");

        }

        @Override
        public void configureRendererComponent(HistoryEntry value, boolean isSelected, boolean hasFocus, int row, int column) {
            this.prepareColumn(value);
            this.rendererIcon.setIcon(value.getGatewayIcon(18));
            String str = value.getGatewayStatus();
            if (str == null) {
                // under substance, setting setText(null) somehow sets the label
                // opaque.
                str = "";
            }

            this.rendererField.setText(str);

            maxWidth = Math.max(renderer.getPreferredSize().width, maxWidth);

        }

        @Override
        protected Color getDefaultForeground() {
            return LAFOptions.getInstance().getColorForTooltipForeground();
        }

        @Override
        public String getStringValue(HistoryEntry value) {
            return null;
        }
    }

    public static final class ResultColumn extends ExtTextColumn<HistoryEntry> implements MaxWidthProvider {

        private int maxWidth;

        @Override
        public boolean isSortable(HistoryEntry obj) {
            return false;
        }

        public ResultColumn(String name) {
            super(name);
        }

        @Override
        public int getMinWidth() {
            return 100;
        }

        @Override
        public int getMaxPreferredWitdh() {
            return maxWidth;
        }

        protected MigPanel createRendererPanel() {
            // getprefered Size will notwork on renderlables
            rendererField = new JLabel();
            rendererIcon = new JLabel();
            return new RendererMigPanel("ins 0", "[]0[grow,fill]", "[grow,fill]");

        }

        @Override
        public void configureRendererComponent(HistoryEntry value, boolean isSelected, boolean hasFocus, int row, int column) {
            this.prepareColumn(value);
            this.rendererIcon.setIcon(value.getResultIcon(18));
            String str = value.getResultStatus();
            if (str == null) {
                // under substance, setting setText(null) somehow sets the label
                // opaque.
                str = "";
            }

            this.rendererField.setText(str);

            maxWidth = Math.max(renderer.getPreferredSize().width, maxWidth);

        }

        @Override
        protected Color getDefaultForeground() {
            return LAFOptions.getInstance().getColorForTooltipForeground();
        }

        @Override
        public String getStringValue(HistoryEntry value) {
            return null;
        }
    }

    public static final class AccountColumn extends ExtTextColumn<HistoryEntry> implements MaxWidthProvider {
        private CandidateAccountColumn delegate;
        private int                    maxWidth;
        {
            delegate = new CandidateAccountColumn();

        }

        @Override
        public boolean isSortable(HistoryEntry obj) {
            return false;
        }

        @Override
        public int getMinWidth() {
            return 100;
        }

        @Override
        public int getMaxPreferredWitdh() {
            return maxWidth;
        }

        public AccountColumn(String name) {
            super(name);
        }

        protected MigPanel createRendererPanel() {
            // getprefered Size will notwork on renderlables
            rendererField = new JLabel();
            rendererIcon = new JLabel();
            return new RendererMigPanel("ins 0", "[]0[grow,fill]", "[grow,fill]");

        }

        @Override
        public void configureRendererComponent(HistoryEntry history, boolean isSelected, boolean hasFocus, int row, int column) {
            this.prepareColumn(history);
            Icon icon = history.getAccountIcon(18);
            String str = history.getAccountStatus();
            Account account = history.getAccount();
            if (account != null) {
                if (icon == null) {
                    icon = DomainInfo.getInstance(account.getHoster()).getFavIcon();
                } else {
                    icon = new BadgeIcon(DomainInfo.getInstance(account.getHoster()).getFavIcon(), IconIO.getScaledInstance(icon, 12, 12), 4, 2);
                }
                String accountType = null;
                switch (history.getAccountType()) {
                case MULTI:
                    accountType = _GUI._.CandidateAccountColumn_account_multi(account.getType().getLabel());
                    break;
                case NONE:
                    break;
                case ORIGINAL:
                    accountType = _GUI._.CandidateAccountColumn_account_original(account.getType().getLabel());
                    break;
                }

                if (!StringUtils.isEmpty(accountType)) {
                    str = _GUI._.CandidateAccountColumn_getStringValue_account_type(account.getUser(), account.getHoster(), accountType);
                } else {
                    str = _GUI._.CandidateAccountColumn_getStringValue_account(account.getUser(), account.getHoster());
                }
            } else {
                if (icon == null) {
                    icon = history.getLink().getDomainInfo().getFavIcon();
                } else {
                    icon = new BadgeIcon(history.getLink().getDomainInfo().getFavIcon(), IconIO.getScaledInstance(icon, 12, 12), 4, 2);
                }

            }
            if (str == null) {
                // under substance, setting setText(null) somehow sets the label
                // opaque.
                str = "";
            }

            this.rendererField.setText(str);
            rendererIcon.setIcon(icon);
            maxWidth = Math.max(renderer.getPreferredSize().width, maxWidth);

        }

        @Override
        protected Color getDefaultForeground() {
            return LAFOptions.getInstance().getColorForTooltipForeground();
        }

        @Override
        public String getStringValue(HistoryEntry value) {
            return null;
        }
    }

    public static final class DateColumn extends ExtDateColumn<HistoryEntry> implements MaxWidthProvider {
        @Override
        protected String getDateFormatString() {
            return _GUI._.CandidateTooltipTableModel_getDateFormatString_timeformat();
        }

        private int maxWidth;

        @Override
        public String getSortOrderIdentifier() {
            return ExtColumn.SORT_ASC;
        }

        public DateColumn(String string) {
            super(string);
        }

        @Override
        public int getMaxPreferredWitdh() {
            return maxWidth;
        }

        @Override
        public int getMinWidth() {
            return 100;
        }

        @Override
        public boolean isSortable(HistoryEntry obj) {
            return false;
        }

        @Override
        protected Color getDefaultForeground() {
            return LAFOptions.getInstance().getColorForTooltipForeground();
        }

        protected MigPanel createRendererPanel() {
            // getprefered Size will notwork on renderlables
            rendererField = new JLabel();
            rendererIcon = new JLabel();
            return new RendererMigPanel("ins 0", "[]0[grow,fill]", "[grow,fill]");

        }

        @Override
        public void configureRendererComponent(HistoryEntry value, boolean isSelected, boolean hasFocus, int row, int column) {
            this.prepareColumn(value);
            this.rendererIcon.setIcon(this.getIcon(value));
            String str = this.getStringValue(value);
            if (str == null) {
                // under substance, setting setText(null) somehow sets the label
                // opaque.
                str = "";
            }

            this.rendererField.setText(str);

            maxWidth = Math.max(renderer.getPreferredSize().width, maxWidth);

        }

        @Override
        protected Date getDate(HistoryEntry o2, Date date) {
            return new Date(o2.getCreateTime());
        }
    }

    private static final long serialVersionUID = 3120481189794897020L;

    public CandidateTooltipTableModel(List<HistoryEntry> history) {
        super("CandidateTooltipTableModel");

        _fireTableStructureChanged(history, true);

    }

    @Override
    protected void initColumns() {
        ExtDateColumn<HistoryEntry> sorton;
        addColumn(sorton = new DateColumn(_GUI._.CandidateTooltipTableModel_time()));

        this.addColumn(new AccountColumn(_GUI._.CandidateTooltipTableModel_account())

        );

        this.addColumn(new GatewayColumn(_GUI._.CandidateTooltipTableModel_gateway())

        );

        this.addColumn(new ResultColumn(_GUI._.CandidateTooltipTableModel_result())

        );

        this.sortColumn = sorton;
    }

}
