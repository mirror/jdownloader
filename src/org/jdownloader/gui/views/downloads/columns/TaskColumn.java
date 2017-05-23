package org.jdownloader.gui.views.downloads.columns;

import java.awt.Dialog.ModalityType;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import jd.controlling.downloadcontroller.HistoryEntry;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.FilePackageView;
import jd.plugins.FilePackageView.PluginState;
import jd.plugins.PluginForHost;
import jd.plugins.PluginProgress;
import jd.plugins.PluginStateCollection;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.components.ExtMergedIcon;
import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.swing.components.tooltips.MultiLineLabelTooltip;
import org.appwork.swing.components.tooltips.MultiLineLabelTooltip.LabelInfo;
import org.appwork.swing.components.tooltips.ToolTipController;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtDefaultRowSorter;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.uio.UIOManager;
import org.jdownloader.DomainInfo;
import org.jdownloader.extensions.extraction.ExtractionStatus;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.columns.candidatetooltip.CandidateTooltip;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.plugins.ConditionalSkipReason;
import org.jdownloader.plugins.FinalLinkState;
import org.jdownloader.plugins.SkipReason;
import org.jdownloader.plugins.WaitWhileWaitingSkipReasonIsSet;
import org.jdownloader.plugins.WaitingSkipReason.CAUSE;
import org.jdownloader.premium.PremiumInfoDialog;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class TaskColumn extends ExtTextColumn<AbstractNode> {
    public static class ColumnHelper {
        private Icon icon = null;

        public Icon getIcon() {
            return icon;
        }

        public void setIcon(Icon icon) {
            this.icon = icon;
        }

        public String getString() {
            return string;
        }

        public void setString(String string) {
            this.string = string;
        }

        private String string = null;
        public String  tooltip;
    }

    /**
     *
     */
    private static final long  serialVersionUID = 1L;
    private final Icon         trueIcon;
    private final Icon         trueIconExtracted;
    private final Icon         extracting;
    private final ColumnHelper columnHelper     = new ColumnHelper();
    private final String       finishedText     = _GUI.T.TaskColumn_getStringValue_finished_();
    private final String       runningText      = _GUI.T.TaskColumn_getStringValue_running_();
    private final String       startingString;
    private final Icon         startingIcon;
    private final Icon         okIconExtracted;
    private final Icon         trueOrangaIconExtracted;
    private final Icon         falseIconExtracted;
    private final Icon         trueIconExtractedFailed;
    private final Icon         okIconExtractedFailed;
    private final Icon         trueOrangaIconExtractedFailed;
    private final Icon         falseIconExtractedFailed;

    @Override
    public int getDefaultWidth() {
        return 180;
    }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        return obj.isEnabled();
    }

    public JPopupMenu createHeaderPopup() {
        return FileColumn.createColumnPopup(this, getMinWidth() == getMaxWidth() && getMaxWidth() > 0);
    }

    public TaskColumn() {
        super(_GUI.T.StatusColumn_StatusColumn2());
        this.trueIcon = NewTheme.I().getIcon(IconKey.ICON_TRUE, 16);
        this.extracting = NewTheme.I().getIcon(org.jdownloader.gui.IconKey.ICON_EXTRACT, 16);
        startingIcon = NewTheme.I().getIcon(IconKey.ICON_RUN, 16);
        trueIconExtracted = new ExtMergedIcon(new AbstractIcon(IconKey.ICON_TRUE, 16)).add(new AbstractIcon(IconKey.ICON_EXTRACT_OK, 16), 16, 0);
        falseIconExtracted = new ExtMergedIcon(new AbstractIcon(IconKey.ICON_FALSE, 16)).add(new AbstractIcon(IconKey.ICON_EXTRACT_OK, 16), 16, 0);
        okIconExtracted = new ExtMergedIcon(new AbstractIcon(IconKey.ICON_OK, 16)).add(new AbstractIcon(IconKey.ICON_EXTRACT_OK, 16), 16, 0);
        trueOrangaIconExtracted = new ExtMergedIcon(new AbstractIcon(IconKey.ICON_TRUE_ORANGE, 16)).add(new AbstractIcon(IconKey.ICON_EXTRACT_OK, 16), 16, 0);
        trueIconExtractedFailed = new ExtMergedIcon(new AbstractIcon(IconKey.ICON_TRUE, 16)).add(new AbstractIcon(IconKey.ICON_EXTRACT_ERROR, 16), 16, 0);
        falseIconExtractedFailed = new ExtMergedIcon(new AbstractIcon(IconKey.ICON_FALSE, 16)).add(new AbstractIcon(IconKey.ICON_EXTRACT_ERROR, 16), 16, 0);
        okIconExtractedFailed = new ExtMergedIcon(new AbstractIcon(IconKey.ICON_OK, 16)).add(new AbstractIcon(IconKey.ICON_EXTRACT_ERROR, 16), 16, 0);
        trueOrangaIconExtractedFailed = new ExtMergedIcon(new AbstractIcon(IconKey.ICON_TRUE_ORANGE, 16)).add(new AbstractIcon(IconKey.ICON_EXTRACT_ERROR, 16), 16, 0);
        startingString = _GUI.T.TaskColumn_fillColumnHelper_starting();
        setRowSorter(new ExtDefaultRowSorter<AbstractNode>() {
            @Override
            public int compare(final AbstractNode o1, final AbstractNode o2) {
                fillColumnHelper(columnHelper, o1);
                String o1s = getStringValue(o1);
                fillColumnHelper(columnHelper, o2);
                String o2s = getStringValue(o2);
                if (o1s == null) {
                    o1s = "";
                }
                if (o2s == null) {
                    o2s = "";
                }
                if (getSortOrderIdentifier() == ExtColumn.SORT_ASC) {
                    return o1s.compareToIgnoreCase(o2s);
                } else {
                    return o2s.compareToIgnoreCase(o1s);
                }
            }
        });
    }

    public boolean onSingleClick(final MouseEvent e, final AbstractNode value) {
        return handleIPBlockCondition(e, value);
    }

    @Override
    public boolean onDoubleClick(final MouseEvent e, final AbstractNode obj) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (obj instanceof DownloadLink) {
                    List<HistoryEntry> his = ((DownloadLink) obj).getHistory();
                    if (his != null) {
                        ToolTipController.getInstance().show(CandidateTooltip.create(e.getPoint(), obj));
                    }
                }
            }
        });
        return true;
    }

    public static boolean handleIPBlockCondition(final MouseEvent e, final AbstractNode value) {
        if (value instanceof DownloadLink) {
            DownloadLink dl = (DownloadLink) value;
            if (JsonConfig.create(GraphicalUserInterfaceSettings.class).isPremiumAlertTaskColumnEnabled()) {
                if (dl.getDownloadLinkController() == null && dl.isEnabled() && dl.getLivePlugin() == null) {
                    ConditionalSkipReason conditionalSkipReason = dl.getConditionalSkipReason();
                    if (conditionalSkipReason != null && !dl.isSkipped() && !FinalLinkState.CheckFinished(dl.getFinalLinkState())) {
                        PluginForHost plugin = dl.getDefaultPlugin();
                        if (plugin == null || !plugin.isPremiumEnabled()) {
                            /* no account support yet for this plugin */
                            return false;
                        }
                        /* enabled links that are not running */
                        if (conditionalSkipReason instanceof WaitWhileWaitingSkipReasonIsSet) {
                            WaitWhileWaitingSkipReasonIsSet waitCondition = (WaitWhileWaitingSkipReasonIsSet) conditionalSkipReason;
                            if (waitCondition.getCause() == CAUSE.IP_BLOCKED && !waitCondition.getConditionalSkipReason().isConditionReached()) {
                                // modeless dialog
                                new Thread() {
                                    public void run() {
                                        PremiumInfoDialog dialog;
                                        UIOManager.I().show(null, dialog = new PremiumInfoDialog(((DownloadLink) value).getDomainInfo(), _GUI.T.TaskColumn_onSingleClick_object_(((DownloadLink) value).getHost()), "TaskColumnReconnect") {
                                            protected String getDescription(DomainInfo info2) {
                                                return _GUI.T.TaskColumn_getDescription_object_(info2.getTld());
                                            }

                                            @Override
                                            public String getDontShowAgainKey() {
                                                return null;
                                            }

                                            @Override
                                            public ModalityType getModalityType() {
                                                return ModalityType.MODELESS;
                                            }
                                        });
                                        if (dialog.isDontShowAgainSelected()) {
                                            JsonConfig.create(GraphicalUserInterfaceSettings.class).setPremiumAlertTaskColumnEnabled(false);
                                        }
                                    }
                                }.start();
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected void prepareColumn(AbstractNode value) {
        fillColumnHelper(columnHelper, value);
    }

    @Override
    public ExtTooltip createToolTip(Point position, AbstractNode value) {
        PluginStateCollection ps = null;
        if (value instanceof FilePackage) {
            FilePackage fp = (FilePackage) value;
            FilePackageView view = fp.getView();
            ps = view.getPluginStates();
        }
        if (ps != null && ps.size() > 0) {
            ArrayList<LabelInfo> lbls = new ArrayList<MultiLineLabelTooltip.LabelInfo>(ps.size());
            for (PluginState p : ps) {
                lbls.add(new LabelInfo(p.getDescription(), p.getIcon()));
            }
            return new MultiLineLabelTooltip(lbls);
        }
        return super.createToolTip(position, value);
    }

    public void fillColumnHelper(ColumnHelper columnHelper, AbstractNode value) {
        if (value instanceof DownloadLink) {
            DownloadLink link = (DownloadLink) value;
            PluginProgress prog = link.getPluginProgress();
            if (prog != null) {
                columnHelper.icon = prog.getIcon(this);
                columnHelper.string = prog.getMessage(this);
                columnHelper.tooltip = null;
                return;
            }
            ConditionalSkipReason conditionalSkipReason = link.getConditionalSkipReason();
            if (conditionalSkipReason != null && !conditionalSkipReason.isConditionReached()) {
                columnHelper.icon = conditionalSkipReason.getIcon(this, null);
                columnHelper.string = conditionalSkipReason.getMessage(this, null);
                columnHelper.tooltip = null;
                return;
            }
            SkipReason skipReason = link.getSkipReason();
            if (skipReason != null) {
                columnHelper.icon = skipReason.getIcon(this, 18);
                columnHelper.string = skipReason.getExplanation(this);
                columnHelper.tooltip = null;
                return;
            }
            final FinalLinkState finalLinkState = link.getFinalLinkState();
            if (finalLinkState != null) {
                if (FinalLinkState.CheckFailed(finalLinkState)) {
                    columnHelper.icon = finalLinkState.getIcon(16);
                    columnHelper.string = finalLinkState.getExplanation(this, link);
                    columnHelper.tooltip = null;
                    return;
                }
                final ExtractionStatus extractionStatus = link.getExtractionStatus();
                if (extractionStatus != null) {
                    final String iconKey = finalLinkState.getIconKey();
                    switch (extractionStatus) {
                    case ERROR:
                    case ERROR_PW:
                    case ERROR_CRC:
                    case ERROR_NOT_ENOUGH_SPACE:
                    case ERRROR_FILE_NOT_FOUND:
                        if (IconKey.ICON_FALSE.equals(iconKey)) {
                            columnHelper.icon = falseIconExtractedFailed;
                        } else if (IconKey.ICON_TRUE.equals(iconKey)) {
                            columnHelper.icon = trueIconExtractedFailed;
                        } else if (IconKey.ICON_OK.equals(iconKey)) {
                            columnHelper.icon = okIconExtractedFailed;
                        } else if (IconKey.ICON_TRUE_ORANGE.equals(iconKey)) {
                            columnHelper.icon = trueOrangaIconExtractedFailed;
                        } else {
                            columnHelper.icon = trueIconExtractedFailed;
                        }
                        columnHelper.string = extractionStatus.getExplanation() + ": " + finalLinkState.getExplanation(this, link);
                        columnHelper.tooltip = null;
                        return;
                    case SUCCESSFUL:
                        if (IconKey.ICON_FALSE.equals(iconKey)) {
                            columnHelper.icon = falseIconExtracted;
                        } else if (IconKey.ICON_TRUE.equals(iconKey)) {
                            columnHelper.icon = trueIconExtracted;
                        } else if (IconKey.ICON_OK.equals(iconKey)) {
                            columnHelper.icon = okIconExtracted;
                        } else if (IconKey.ICON_TRUE_ORANGE.equals(iconKey)) {
                            columnHelper.icon = trueOrangaIconExtracted;
                        } else {
                            columnHelper.icon = trueIconExtracted;
                        }
                        columnHelper.string = extractionStatus.getExplanation() + ": " + finalLinkState.getExplanation(this, link);
                        columnHelper.tooltip = null;
                        return;
                    case RUNNING:
                        columnHelper.icon = extracting;
                        columnHelper.string = extractionStatus.getExplanation();
                        columnHelper.tooltip = null;
                        return;
                    }
                }
                columnHelper.icon = finalLinkState.getIcon(16);
                columnHelper.string = finalLinkState.getExplanation(this, link);
                columnHelper.tooltip = null;
                return;
            }
            if (link.getDownloadLinkController() != null) {
                columnHelper.icon = startingIcon;
                columnHelper.string = startingString;
                columnHelper.tooltip = null;
                return;
            }
            columnHelper.icon = null;
            columnHelper.tooltip = null;
            columnHelper.string = "";
        } else {
            FilePackage fp = (FilePackage) value;
            FilePackageView view = fp.getView();
            PluginStateCollection ps = view.getPluginStates();
            if (ps.size() > 0) {
                columnHelper.icon = ps.getMergedIcon();
                columnHelper.string = ps.isMultiline() ? "" : ps.getText();
                columnHelper.tooltip = ps.getText();
                return;
            }
            if (view.isFinished()) {
                columnHelper.icon = trueIcon;
                columnHelper.string = finishedText;
                columnHelper.tooltip = null;
                return;
            } else if (view.getETA() != -1) {
                columnHelper.icon = null;
                columnHelper.string = runningText;
                columnHelper.tooltip = null;
                return;
            }
            columnHelper.tooltip = null;
            columnHelper.icon = null;
            columnHelper.string = "";
        }
    }

    @Override
    protected String getTooltipText(AbstractNode obj) {
        fillColumnHelper(columnHelper, obj);
        String ret = columnHelper.tooltip;
        if (ret == null) {
            ret = columnHelper.string;
        }
        return ret;
    }

    @Override
    public boolean isSortable(AbstractNode obj) {
        return true;
    }

    @Override
    protected Icon getIcon(AbstractNode value) {
        return columnHelper.icon;
    }

    @Override
    public String getStringValue(AbstractNode value) {
        return columnHelper.string;
    }
}
