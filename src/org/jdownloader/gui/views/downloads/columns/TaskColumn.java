package org.jdownloader.gui.views.downloads.columns;

import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;

import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.FilePackageView;
import jd.plugins.PluginForHost;
import jd.plugins.PluginProgress;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.DomainInfo;
import org.jdownloader.extensions.extraction.ExtractionStatus;
import org.jdownloader.gui.translate._GUI;
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
        private ImageIcon icon = null;

        public ImageIcon getIcon() {
            return icon;
        }

        public void setIcon(ImageIcon icon) {
            this.icon = icon;
        }

        public String getString() {
            return string;
        }

        public void setString(String string) {
            this.string = string;
        }

        private String string = null;
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private ImageIcon         trueIcon;
    private ImageIcon         falseIcon;
    private ImageIcon         infoIcon;

    private ImageIcon         iconWait;

    private ImageIcon         trueIconExtracted;

    private ImageIcon         trueIconExtractedFailed;

    private ImageIcon         extracting;

    private ColumnHelper      columnHelper     = new ColumnHelper();

    private String            finishedText     = _GUI._.TaskColumn_getStringValue_finished_();
    private String            runningText      = _GUI._.TaskColumn_getStringValue_running_();

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
        super(_GUI._.StatusColumn_StatusColumn());
        this.trueIcon = NewTheme.I().getIcon("true", 16);
        this.falseIcon = NewTheme.I().getIcon("false", 16);
        this.infoIcon = NewTheme.I().getIcon("info", 16);
        this.iconWait = NewTheme.I().getIcon("wait", 16);
        this.extracting = NewTheme.I().getIcon("archive", 16);
        trueIconExtracted = new ImageIcon(ImageProvider.merge(trueIcon.getImage(), NewTheme.I().getImage("archive", 16), 0, 0, trueIcon.getIconWidth() + 4, (trueIcon.getIconHeight() - 16) / 2 + 2));
        trueIconExtractedFailed = new ImageIcon(ImageProvider.merge(trueIconExtracted.getImage(), NewTheme.I().getImage("error", 10), 0, 0, trueIcon.getIconWidth() + 12, trueIconExtracted.getIconHeight() - 10));
    }

    public boolean onSingleClick(final MouseEvent e, final AbstractNode value) {
        return handleIPBlockCondition(e, value);
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
                                try {
                                    Dialog.getInstance().showDialog(new PremiumInfoDialog(((DownloadLink) value).getDomainInfo(), _GUI._.TaskColumn_onSingleClick_object_(((DownloadLink) value).getHost()), "TaskColumnReconnect") {
                                        protected String getDescription(DomainInfo info2) {
                                            return _GUI._.TaskColumn_getDescription_object_(info2.getTld());
                                        }
                                    });
                                } catch (DialogClosedException e1) {
                                    e1.printStackTrace();
                                } catch (DialogCanceledException e1) {
                                    e1.printStackTrace();
                                }
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

    public void fillColumnHelper(ColumnHelper columnHelper, AbstractNode value) {
        if (value instanceof DownloadLink) {
            DownloadLink link = (DownloadLink) value;
            PluginProgress prog = link.getPluginProgress();
            if (prog != null) {
                columnHelper.icon = prog.getIcon();
                columnHelper.string = prog.getMessage(this);
                return;
            }
            ConditionalSkipReason conditionalSkipReason = link.getConditionalSkipReason();
            if (conditionalSkipReason != null && !conditionalSkipReason.isConditionReached()) {
                columnHelper.icon = conditionalSkipReason.getIcon(this, null);
                columnHelper.string = conditionalSkipReason.getMessage(this, null);
                return;
            }
            SkipReason skipReason = link.getSkipReason();
            if (skipReason != null) {
                columnHelper.icon = infoIcon;
                columnHelper.string = skipReason.getExplanation(this, link);
                return;
            }
            FinalLinkState finalLinkState = link.getFinalLinkState();
            if (finalLinkState != null) {
                if (FinalLinkState.CheckFailed(finalLinkState)) {
                    columnHelper.icon = falseIcon;
                    columnHelper.string = finalLinkState.getExplanation(this, link);
                    return;
                }
                ExtractionStatus extractionStatus = link.getExtractionStatus();
                if (extractionStatus != null) {
                    switch (extractionStatus) {
                    case ERROR:
                    case ERROR_PW:
                    case ERROR_CRC:
                    case ERROR_NOT_ENOUGH_SPACE:
                    case ERRROR_FILE_NOT_FOUND:
                        columnHelper.icon = trueIconExtractedFailed;
                        columnHelper.string = extractionStatus.getExplanation();
                        return;
                    case SUCCESSFUL:
                        columnHelper.icon = trueIconExtracted;
                        columnHelper.string = extractionStatus.getExplanation();
                        return;
                    case RUNNING:
                        columnHelper.icon = extracting;
                        columnHelper.string = extractionStatus.getExplanation();
                        return;
                    }
                }
                columnHelper.icon = trueIcon;
                columnHelper.string = finalLinkState.getExplanation(this, link);
                return;
            }
            columnHelper.icon = null;
            columnHelper.string = "";
        } else {
            FilePackage fp = (FilePackage) value;
            FilePackageView view = fp.getView();
            if (view.isFinished()) {
                columnHelper.icon = trueIcon;
                columnHelper.string = finishedText;
                return;
            } else if (view.getETA() != -1) {
                columnHelper.icon = null;
                columnHelper.string = runningText;
                return;
            }
            columnHelper.icon = null;
            columnHelper.string = "";
        }
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
