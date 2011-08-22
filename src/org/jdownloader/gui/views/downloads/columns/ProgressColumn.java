package org.jdownloader.gui.views.downloads.columns;

import jd.controlling.proxy.ProxyController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PackageLinkNode;

import org.appwork.swing.exttable.columns.ExtProgressColumn;
import org.jdownloader.gui.translate._GUI;

public class ProgressColumn extends ExtProgressColumn<jd.plugins.PackageLinkNode> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public ProgressColumn() {
        super(_GUI._.ProgressColumn_ProgressColumn());
    }

    @Override
    public boolean isEnabled(PackageLinkNode obj) {
        return obj.isEnabled();
    }

    @Override
    public int getMinWidth() {

        return 30;
    }

    public boolean isPaintWidthLockIcon() {
        return false;
    }

    @Override
    public int getDefaultWidth() {
        return 100;
    }

    @Override
    protected String getString(PackageLinkNode value) {
        if (value instanceof FilePackage) {
            FilePackage fp = (FilePackage) value;

            return null;
        } else {
            DownloadLink dLink = (DownloadLink) value;
            if (dLink.getDefaultPlugin() == null) {

                return _GUI._.gui_treetable_error_plugin();
            } else if (dLink.getPluginProgress() != null) {
                return (dLink.getPluginProgress().getPercent() + " %");
            } else if ((dLink.getLinkStatus().hasStatus(LinkStatus.ERROR_IP_BLOCKED) && ProxyController.getInstance().getRemainingIPBlockWaittime(dLink.getHost()) > 0)) {

                return null;
            } else if ((dLink.getLinkStatus().hasStatus(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE) && ProxyController.getInstance().getRemainingTempUnavailWaittime(dLink.getHost()) > 0)) {

                return null;
            } else if (dLink.getLinkStatus().isFinished()) {
                return null;
            } else if (dLink.getDownloadCurrent() > 0 || dLink.getDownloadSize() > 0) { return null; }
        }
        return null;
    }

    @Override
    protected long getMax(PackageLinkNode value) {
        if (value instanceof FilePackage) {
            FilePackage fp = (FilePackage) value;
            if (fp.isFinished()) {

                return 100;
            } else {
                return (Math.max(1, fp.getTotalEstimatedPackageSize()));

            }

        } else {
            DownloadLink dLink = (DownloadLink) value;
            if (dLink.getDefaultPlugin() == null) {
                return 100;
            } else if (dLink.getPluginProgress() != null) {
                return (dLink.getPluginProgress().getTotal());

            } else if ((dLink.getLinkStatus().hasStatus(LinkStatus.ERROR_IP_BLOCKED) && ProxyController.getInstance().getRemainingIPBlockWaittime(dLink.getHost()) > 0)) {
                return (dLink.getLinkStatus().getTotalWaitTime());

            } else if ((dLink.getLinkStatus().hasStatus(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE) && ProxyController.getInstance().getRemainingTempUnavailWaittime(dLink.getHost()) > 0)) {
                return (dLink.getLinkStatus().getTotalWaitTime());

            } else if (dLink.getLinkStatus().isFinished()) {
                return 100;
            } else if (dLink.getDownloadCurrent() > 0 || dLink.getDownloadSize() > 0) { return (dLink.getDownloadSize());

            }
        }
        return 100;
    }

    @Override
    protected long getValue(PackageLinkNode value) {
        if (value instanceof FilePackage) {
            FilePackage fp = (FilePackage) value;
            if (fp.isFinished()) {

                return 100;
            } else {
                return (fp.getTotalKBLoaded());
            }

        } else {
            DownloadLink dLink = (DownloadLink) value;
            if (dLink.getDefaultPlugin() == null) {
                return -1;
            } else if (dLink.getPluginProgress() != null) {
                return (dLink.getPluginProgress().getCurrent());

            } else if ((dLink.getLinkStatus().hasStatus(LinkStatus.ERROR_IP_BLOCKED) && ProxyController.getInstance().getRemainingIPBlockWaittime(dLink.getHost()) > 0)) {

                return (dLink.getLinkStatus().getRemainingWaittime());

            } else if ((dLink.getLinkStatus().hasStatus(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE) && ProxyController.getInstance().getRemainingTempUnavailWaittime(dLink.getHost()) > 0)) {

                return (dLink.getLinkStatus().getRemainingWaittime());

            } else if (dLink.getLinkStatus().isFinished()) {

                return (100);

            } else if (dLink.getDownloadCurrent() > 0 || dLink.getDownloadSize() > 0) {

            return (dLink.getDownloadCurrent());

            }
        }
        return -1;
    }

}
