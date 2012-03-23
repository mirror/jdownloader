package org.jdownloader.gui.views.downloads.columns;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.proxy.ProxyBlock;
import jd.controlling.proxy.ProxyController;
import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
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
    private ImageIcon         updateIcon;
    private ImageIcon         trueIcon;
    private ImageIcon         falseIcon;

    @Override
    public int getDefaultWidth() {
        return 180;
    }

    public boolean isPaintWidthLockIcon() {
        return false;
    }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        return obj.isEnabled();
    }

    public TaskColumn() {
        super(_GUI._.StatusColumn_StatusColumn());
        this.updateIcon = NewTheme.I().getIcon("update", 16);
        this.trueIcon = NewTheme.I().getIcon("true", 16);
        this.falseIcon = NewTheme.I().getIcon("false", 16);
    }

    @Override
    protected Icon getIcon(AbstractNode value) {
        if (value instanceof DownloadLink) {
            DownloadLink dl = (DownloadLink) value;
            LinkStatus ls = dl.getLinkStatus();
            PluginProgress prog = dl.getPluginProgress();
            if (prog != null && prog.getPercent() > 0.0 && prog.getPercent() < 100.0) {
                return updateIcon;
            } else if (ls.getStatusIcon() != null) {
                return ls.getStatusIcon();
            } else if (ls.isFinished()) {
                return trueIcon;
            } else if (ls.isFailed() || dl.getAvailableStatus() == AvailableStatus.FALSE) { return falseIcon; }
        } else if (value instanceof FilePackage) {
            if (((FilePackage) value).getView().isFinished()) { return trueIcon; }
        }
        return null;
    }

    @Override
    public String getStringValue(AbstractNode value) {
        if (value instanceof DownloadLink) {
            DownloadLink dl = (DownloadLink) value;
            if (!dl.getLinkStatus().isPluginActive()) {
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
            }
            return dl.getLinkStatus().getStatusString();

        } else if (value instanceof FilePackage) {
            if (((FilePackage) value).getView().isFinished()) { return "Finished"; }
        }
        return "";
    }
}
