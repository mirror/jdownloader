package org.jdownloader.gui.views.downloads.columns.candidatetooltip;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JLabel;

import jd.controlling.downloadcontroller.DownloadLinkCandidate;
import jd.controlling.downloadcontroller.DownloadLinkCandidateHistory;
import jd.controlling.downloadcontroller.DownloadLinkCandidateResult;

import org.appwork.swing.MigPanel;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtDateColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.swing.renderer.RendererMigPanel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.columns.CandidateAccountColumn;
import org.jdownloader.gui.views.downloads.columns.CandidateGatewayColumn;
import org.jdownloader.updatev2.gui.LAFOptions;

public class CandidateTooltipTableModel extends ExtTableModel<CandidateAndResult> {
    public static interface MaxWidthProvider {
        public int getMaxPreferredWitdh();
    }

    public static final class GatewayColumn extends ExtTextColumn<CandidateAndResult> implements MaxWidthProvider {
        private CandidateGatewayColumn delegate;
        private int                    maxWidth;
        {
            delegate = new CandidateGatewayColumn();
        }

        @Override
        public boolean isSortable(CandidateAndResult obj) {
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
        public void configureRendererComponent(CandidateAndResult value, boolean isSelected, boolean hasFocus, int row, int column) {
            prepareColumn(value);
            delegate.configureForCandidate(rendererField, rendererIcon, null, value.getCandidate());
            maxWidth = Math.max(renderer.getPreferredSize().width, maxWidth);

        }

        @Override
        protected Color getDefaultForeground() {
            return LAFOptions.getInstance().getColorForTooltipForeground();
        }

        @Override
        public String getStringValue(CandidateAndResult value) {
            return null;
        }
    }

    public static final class AccountColumn extends ExtTextColumn<CandidateAndResult> implements MaxWidthProvider {
        private CandidateAccountColumn delegate;
        private int                    maxWidth;
        {
            delegate = new CandidateAccountColumn();

        }

        @Override
        public boolean isSortable(CandidateAndResult obj) {
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
        public void configureRendererComponent(CandidateAndResult value, boolean isSelected, boolean hasFocus, int row, int column) {
            prepareColumn(value);
            delegate.configureRenderer(rendererField, rendererIcon, null, value.getCandidate());
            maxWidth = Math.max(renderer.getPreferredSize().width, maxWidth);

        }

        @Override
        protected Color getDefaultForeground() {
            return LAFOptions.getInstance().getColorForTooltipForeground();
        }

        @Override
        public String getStringValue(CandidateAndResult value) {
            return null;
        }
    }

    public static final class DateColumn extends ExtDateColumn<CandidateAndResult> implements MaxWidthProvider {
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
        public boolean isSortable(CandidateAndResult obj) {
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
        public void configureRendererComponent(CandidateAndResult value, boolean isSelected, boolean hasFocus, int row, int column) {
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
        protected Date getDate(CandidateAndResult o2, Date date) {
            return o2.getDate();
        }
    }

    private static final long serialVersionUID = 3120481189794897020L;

    public CandidateTooltipTableModel(DownloadLinkCandidateHistory history) {
        super("CandidateTooltipTableModel");

        Map<DownloadLinkCandidate, DownloadLinkCandidateResult> historyMap;
        synchronized (history) {
            historyMap = new HashMap<DownloadLinkCandidate, DownloadLinkCandidateResult>(history.getHistory());
        }
        List<CandidateAndResult> entries = new ArrayList<CandidateAndResult>(historyMap.size());
        for (Entry<DownloadLinkCandidate, DownloadLinkCandidateResult> es : historyMap.entrySet()) {
            entries.add(new CandidateAndResult(es.getKey(), es.getValue()));
        }
        _fireTableStructureChanged(entries, true);

    }

    @Override
    protected void initColumns() {
        ExtDateColumn<CandidateAndResult> sorton;
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
