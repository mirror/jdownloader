package org.jdownloader.gui.views.downloads.columns;

import javax.swing.Icon;

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.proxy.ProxyBlock;
import jd.controlling.proxy.ProxyController;
import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginProgress;

import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;

public class TaskColumn extends ExtTextColumn<AbstractNode> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public int getDefaultWidth() {
        return 180;
    }

    public boolean isPaintWidthLockIcon() {
        return false;
    }

    // @Override
    // public int getMaxWidth() {
    //
    // return 150;
    // }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        return obj.isEnabled();
    }

    public TaskColumn() {
        super(_GUI._.StatusColumn_StatusColumn());
    }

    @Override
    protected Icon getIcon(AbstractNode value) {
        if (value instanceof DownloadLink) {
            LinkStatus ls = ((DownloadLink) value).getLinkStatus();

            PluginProgress prog = ((DownloadLink) value).getPluginProgress();
            if (prog != null && prog.getPercent() > 0.0 && prog.getPercent() < 100.0) {
                NewTheme.I().getIcon("update", 16);
            } else if (ls.getStatusIcon() != null) {
                return ls.getStatusIcon();
            } else if (ls.isFinished()) {
                return NewTheme.I().getIcon("true", 16);
            } else if (ls.isFailed() || (((DownloadLink) value).isAvailabilityStatusChecked() && !((DownloadLink) value).isAvailable())) {
                //
                return NewTheme.I().getIcon("false", 16);
            }
        } else if (value instanceof FilePackage) {
            if (((FilePackage) value).getView().isFinished()) { return NewTheme.I().getIcon("true", 16); }
        }
        return null;
    }

    @Override
    public String getStringValue(AbstractNode value) {
        if (value instanceof DownloadLink) {
            DownloadLink dl = (DownloadLink) value;
            ProxyBlock ipTimeout = null;
            if (dl.getLivePlugin() == null && !dl.getLinkStatus().isPluginActive() && (ipTimeout = ProxyController.getInstance().getHostIPBlockTimeout(dl.getHost())) != null) {
                if (ipTimeout.getLink() == value) {
                    return _JDT._.gui_download_waittime_status2(Formatter.formatSeconds(ipTimeout.getBlockedTimeout() / 1000));
                } else {
                    return _JDT._.gui_downloadlink_hosterwaittime();
                }
            }
            ProxyBlock hostTimeout = null;
            if (dl.getLivePlugin() == null && !dl.getLinkStatus().isPluginActive() && (hostTimeout = ProxyController.getInstance().getHostBlockedTimeout(dl.getHost())) != null) {
                if (hostTimeout.getLink() == value) {
                    return _JDT._.gui_download_waittime_status2(Formatter.formatSeconds(hostTimeout.getBlockedTimeout() / 1000));
                } else {
                    return _JDT._.gui_downloadlink_hostertempunavail();
                }
            }
            return ((DownloadLink) value).getLinkStatus().getStatusString();

        } else if (value instanceof FilePackage) {
            if (((FilePackage) value).getView().isFinished()) { return "Finished"; }
        }
        return "";
    }
}
