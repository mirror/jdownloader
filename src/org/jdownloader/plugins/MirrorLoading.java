package org.jdownloader.plugins;

import javax.swing.Icon;

import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.views.downloads.columns.ETAColumn;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.translate._JDT;

public class MirrorLoading implements ConditionalSkipReason, DownloadLinkCondition {
    private final DownloadLink dependency;
    private final Icon         icon;

    public Icon getIcon() {
        return icon;
    }

    public String getMessage() {
        return message;
    }

    private final String message;

    public MirrorLoading(DownloadLink dependency) {
        this.dependency = dependency;
        icon = new AbstractIcon(IconKey.ICON_DOWNLOAD, 16);
        message = _JDT.T.system_download_errors_linkisBlocked(dependency.getHost());
    }

    @Override
    public void finalize(DownloadLink link) {
        if (FinalLinkState.CheckFinished(getDownloadLink().getFinalLinkState())) {
            link.setSkipReason(null);
            link.setFinalLinkState(FinalLinkState.FINISHED_MIRROR);
            link.setFinishedDate(getDownloadLink().getFinishedDate());
            link.setName(getDownloadLink().getView().getDisplayName());
            final long fileSize = getDownloadLink().getView().getBytesTotal();
            if (fileSize >= 0) {
                link.setDownloadSize(fileSize);
            }
            link.setDownloadCurrent(getDownloadLink().getView().getBytesLoaded());
        }
    }

    @Override
    public boolean isConditionReached() {
        return getDownloadLink().getDownloadLinkController() == null;
    }

    @Override
    public DownloadLink getDownloadLink() {
        return dependency;
    }

    @Override
    public String getMessage(Object requestor, AbstractNode node) {
        if (requestor instanceof CustomConditionalSkipReasonMessageIcon) {
            return ((CustomConditionalSkipReasonMessageIcon) requestor).getMessage(this, node);
        } else if (requestor instanceof ETAColumn) {
            return null;
        } else {
            return getMessage();
        }
    }

    @Override
    public Icon getIcon(Object requestor, AbstractNode node) {
        if (requestor instanceof CustomConditionalSkipReasonMessageIcon) {
            return ((CustomConditionalSkipReasonMessageIcon) requestor).getIcon(this, node);
        } else if (requestor instanceof ETAColumn) {
            return null;
        } else {
            return getIcon();
        }
    }
}
