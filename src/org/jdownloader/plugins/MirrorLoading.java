package org.jdownloader.plugins;

import javax.swing.ImageIcon;

import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;

import org.jdownloader.gui.views.downloads.columns.ETAColumn;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;

public class MirrorLoading implements ConditionalSkipReason, DownloadLinkCondition {

    private final DownloadLink dependency;
    private final ImageIcon    icon;
    private final String       mirror;

    public MirrorLoading(DownloadLink dependency) {
        this.dependency = dependency;
        icon = NewTheme.I().getIcon("download", 16);
        mirror = _JDT._.system_download_errors_linkisBlocked(dependency.getHost());
    }

    @Override
    public void finalize(DownloadLink link) {
        if (FinalLinkState.CheckFinished(getDownloadLink().getFinalLinkState())) {
            link.setFinalLinkState(FinalLinkState.FINISHED_MIRROR);
            link.setFinishedDate(getDownloadLink().getFinishedDate());
            link.setName(getDownloadLink().getName());
            link.setFinalFileOutput(getDownloadLink().getFinalFileOutput());
            long fileSize = getDownloadLink().getKnownDownloadSize();
            if (fileSize >= 0) link.setDownloadSize(fileSize);
            link.setDownloadCurrent(getDownloadLink().getDownloadCurrent());
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
        if (requestor instanceof ETAColumn) return null;
        return mirror;
    }

    @Override
    public ImageIcon getIcon(Object requestor, AbstractNode node) {
        if (requestor instanceof ETAColumn) return null;
        return icon;
    }

}
