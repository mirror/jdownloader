package org.jdownloader.gui.views.downloads.columns.candidatetooltip;

import java.awt.Color;

import javax.swing.Icon;
import javax.swing.JLabel;

import jd.controlling.downloadcontroller.DownloadLinkCandidateResult;
import jd.plugins.DownloadLink;

import org.appwork.swing.MigPanel;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.renderer.RendererMigPanel;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.columns.candidatetooltip.CandidateTooltipTableModel.MaxWidthProvider;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.ConditionalSkipReason;
import org.jdownloader.plugins.FinalLinkState;
import org.jdownloader.plugins.SkipReason;
import org.jdownloader.plugins.WaitingSkipReason;
import org.jdownloader.plugins.WaitingSkipReason.CAUSE;
import org.jdownloader.updatev2.gui.LAFOptions;

public final class ResultColumn extends ExtTextColumn<CandidateAndResult> implements MaxWidthProvider {
    private Icon iconRunning = new AbstractIcon(IconKey.ICON_RUN, 20);
    private int  maxWidth;
    private Icon falseIcon   = new AbstractIcon(IconKey.ICON_FALSE, 16);
    private Icon okIcon      = new AbstractIcon(IconKey.ICON_OK, 16);

    public ResultColumn(String name) {
        super(name);
    }

    @Override
    public boolean isSortable(CandidateAndResult obj) {
        return false;
    }

    @Override
    public int getMinWidth() {
        return 100;
    }

    protected MigPanel createRendererPanel() {
        // getprefered Size will notwork on renderlables
        rendererField = new JLabel();
        rendererIcon = new JLabel();
        return new RendererMigPanel("ins 0", "[]0[grow,fill]", "[grow,fill]");

    }

    public void configureRendererComponent(CandidateAndResult value, boolean isSelected, boolean hasFocus, int row, int column) {

        this.prepareColumn(value);
        this.rendererIcon.setIcon(this.getIcon(value));
        String str = null;
        Icon icon = null;
        final DownloadLink link = value.getCandidate().getLink();

        DownloadLinkCandidateResult result = value.getResult();

        if (result == null || result.getResult() == null) {
            icon = iconRunning;

            str = _GUI._.CandidateTooltipTableModel_initColumns_running_();

        } else {
            switch (result.getResult()) {
            case PROXY_UNAVAILABLE:

                break;
            case CONDITIONAL_SKIPPED:
                ConditionalSkipReason conditionalSkipReason = result.getConditionalSkip();
                str = conditionalSkipReason.getMessage(this, link);
                icon = conditionalSkipReason.getIcon(this, link);

            case IP_BLOCKED:

                WaitingSkipReason sr = new WaitingSkipReason(CAUSE.IP_BLOCKED, result.getWaitTime(), result.getMessage());
                str = sr.getMessage(this, link);
                icon = sr.getIcon(this, link);

                break;
            case HOSTER_UNAVAILABLE:

                sr = new WaitingSkipReason(CAUSE.HOST_TEMP_UNAVAILABLE, result.getWaitTime(), result.getMessage());
                str = sr.getMessage(this, link);
                icon = sr.getIcon(this, link);

                break;
            case FILE_UNAVAILABLE:

                sr = new WaitingSkipReason(CAUSE.FILE_TEMP_UNAVAILABLE, result.getWaitTime(), result.getMessage());
                str = sr.getMessage(this, link);
                icon = sr.getIcon(this, link);

                break;
            case CONNECTION_ISSUES:

                sr = new WaitingSkipReason(CAUSE.CONNECTION_TEMP_UNAVAILABLE, result.getWaitTime(), result.getMessage());
                str = sr.getMessage(this, link);
                icon = sr.getIcon(this, link);

                break;
            case SKIPPED:
                str = result.getSkipReason().getExplanation(this);
                icon = result.getSkipReason().getIcon(this, 20);
                break;
            case PLUGIN_DEFECT:
                str = FinalLinkState.PLUGIN_DEFECT.getExplanation(this, link);
                icon = falseIcon;
                break;
            case OFFLINE_TRUSTED:
                str = FinalLinkState.OFFLINE.getExplanation(this, link);
                icon = falseIcon;
                break;
            case FINISHED_EXISTS:

                // currentSession.removeHistory(link);
                str = FinalLinkState.FINISHED_MIRROR.getExplanation(this, link);
                icon = okIcon;

                break;
            case FINISHED:
                str = FinalLinkState.FINISHED.getExplanation(this, link);
                icon = okIcon;

                break;
            case FAILED_EXISTS:

                str = FinalLinkState.FAILED_EXISTS.getExplanation(this, link);
                icon = falseIcon;

                break;
            case FAILED:
                str = FinalLinkState.FAILED.getExplanation(this, link);
                icon = falseIcon;
                break;
            case STOPPED:
                str = _GUI._.CandidateTooltipTableModel_configureRendererComponent_stopped_();
                icon = null;
                break;
            case ACCOUNT_ERROR:

                /* there was an unknown account issue */

                if (result.getThrowable() != null) {
                    str = result.getThrowable().getMessage();
                } else {
                    str = null;
                }
                icon = falseIcon;

                break;
            case ACCOUNT_INVALID:

                /* account has been recognized as valid and/or premium but now throws invalid messages */

                if (result.getThrowable() != null) {
                    str = result.getThrowable().getMessage();
                } else {
                    str = null;
                }
                icon = falseIcon;
                break;
            case ACCOUNT_UNAVAILABLE:
                str = _GUI._.CandidateTooltipTableModel_configureRendererComponent_account_unavailable();
                icon = falseIcon;
                break;
            case ACCOUNT_REQUIRED:
                str = _GUI._.CandidateTooltipTableModel_configureRendererComponent_account_required();
                icon = falseIcon;
                break;
            case CAPTCHA:
                str = SkipReason.CAPTCHA.getExplanation(this);
                icon = SkipReason.CAPTCHA.getIcon(this, 20);
                break;
            case FATAL_ERROR:

                str = FinalLinkState.FAILED_FATAL.getExplanation(this, link);
                if (StringUtils.isNotEmpty(result.getMessage())) {
                    str = result.getMessage();
                }
                icon = falseIcon;
                break;
            default:
                str = result.getResult() + "";
                if (StringUtils.isNotEmpty(result.getMessage())) {
                    str = result.getMessage();
                }
                break;
            }

            if (StringUtils.isEmpty(str)) {
                str = result.getMessage();
                if (str == null) {
                    str = result.getResult() + "";
                }

                // under substance, setting setText(null) somehow sets the label
                // opaque.

            }
        }

        this.rendererField.setText(str);
        rendererIcon.setIcon(icon);
        maxWidth = Math.max(renderer.getPreferredSize().width, maxWidth);

    }

    @Override
    protected Color getDefaultForeground() {
        return LAFOptions.getInstance().getColorForTooltipForeground();
    }

    protected javax.swing.Icon getIcon(CandidateAndResult value) {

        return null;
    }

    @Override
    public String getStringValue(CandidateAndResult value) {
        return null;
    }

    @Override
    public int getMaxPreferredWitdh() {
        return maxWidth;
    }
}