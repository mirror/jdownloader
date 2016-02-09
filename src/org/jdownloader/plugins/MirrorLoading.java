package org.jdownloader.plugins;

import javax.swing.Icon;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.views.downloads.columns.ETAColumn;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.translate._JDT;

import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;

public class MirrorLoading implements ConditionalSkipReason, DownloadLinkCondition {

    private final DownloadLink dependency;
    private final Icon         icon;
    private final String       mirror;

    public MirrorLoading(DownloadLink dependency) {
        this.dependency = dependency;
        icon = new AbstractIcon(IconKey.ICON_DOWNLOAD, 16);
        mirror = _JDT.T.system_download_errors_linkisBlocked(dependency.getHost());
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
        return dependency.getDownloadLinkController() == null;
    }

    @Override
    public DownloadLink getDownloadLink() {
        return dependency;
    }

    @Override
    public String getMessage(Object requestor, AbstractNode node) {
        if (requestor instanceof ETAColumn) {
            return null;
        }
        return mirror;
    }

    @Override
    public Icon getIcon(Object requestor, AbstractNode node) {
        if (requestor instanceof ETAColumn) {
            return null;
        }
        return icon;
    }

}
