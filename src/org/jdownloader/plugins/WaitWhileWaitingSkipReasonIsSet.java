package org.jdownloader.plugins;

import javax.swing.Icon;

import jd.controlling.downloadcontroller.HistoryEntry;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackageView;

import org.jdownloader.api.downloads.ChannelCollector;
import org.jdownloader.api.downloads.DownloadControllerEventPublisher;
import org.jdownloader.api.downloads.v2.DownloadsAPIV2Impl;
import org.jdownloader.gui.views.downloads.columns.TaskColumn;

public class WaitWhileWaitingSkipReasonIsSet implements ConditionalSkipReason, DownloadLinkCondition, ValidatableConditionalSkipReason {
    private final WaitingSkipReason reason;
    private final DownloadLink      source;
    private boolean                 valid = true;

    public WaitWhileWaitingSkipReasonIsSet(WaitingSkipReason reason, DownloadLink source) {
        this.reason = reason;
        this.source = source;
    }

    @Override
    public boolean isConditionReached() {
        return source.getConditionalSkipReason() != this || reason.isConditionReached();
    }

    @Override
    public String getMessage(Object requestor, AbstractNode node) {
        if (requestor instanceof CustomConditionalSkipReasonMessageIcon) {
            return ((CustomConditionalSkipReasonMessageIcon) requestor).getMessage(this, node);
        } else if (getDownloadLink() == node) {
            return getConditionalSkipReason().getMessage(requestor, node);
        } else if (requestor == this) {
            return getConditionalSkipReason().getMessage(requestor, node);
        } else if (requestor instanceof TaskColumn || requestor instanceof FilePackageView || requestor instanceof HistoryEntry) {
            return getConditionalSkipReason().getMessage(requestor, node);
        } else if (requestor instanceof DownloadControllerEventPublisher) {
            return getConditionalSkipReason().getMessage(requestor, node);
        } else if (requestor instanceof DownloadsAPIV2Impl) {
            return getConditionalSkipReason().getMessage(requestor, node);
        } else if (requestor instanceof ChannelCollector) {
            return getConditionalSkipReason().getMessage(requestor, node);
        } else {
            return null;
        }
    }

    @Override
    public Icon getIcon(Object requestor, AbstractNode node) {
        if (requestor instanceof CustomConditionalSkipReasonMessageIcon) {
            return ((CustomConditionalSkipReasonMessageIcon) requestor).getIcon(this, node);
        } else if (getDownloadLink() == node) {
            return getConditionalSkipReason().getIcon(requestor, node);
        } else if (requestor instanceof TaskColumn || requestor instanceof FilePackageView || requestor instanceof HistoryEntry) {
            return getConditionalSkipReason().getIcon(requestor, node);
        } else {
            return null;
        }
    }

    @Override
    public void finalize(DownloadLink link) {
    }

    public WaitingSkipReason.CAUSE getCause() {
        return getConditionalSkipReason().getCause();
    }

    @Override
    public DownloadLink getDownloadLink() {
        return source;
    }

    public WaitingSkipReason getConditionalSkipReason() {
        return reason;
    }

    @Override
    public boolean isValid() {
        if (valid == false) {
            return false;
        } else {
            return getConditionalSkipReason().isValid();
        }
    }

    @Override
    public void invalidate() {
        valid = false;
    }
}
