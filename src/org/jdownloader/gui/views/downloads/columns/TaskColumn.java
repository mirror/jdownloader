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

    private ImageIcon         trueIcon;
    private ImageIcon         falseIcon;
    private ImageIcon         infoIcon;

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
        this.trueIcon = NewTheme.I().getIcon("true", 16);
        this.falseIcon = NewTheme.I().getIcon("false", 16);
        this.infoIcon = NewTheme.I().getIcon("info", 16);
    }

    @Override
    protected Icon getIcon(AbstractNode value) {
        if (value instanceof DownloadLink) {
            DownloadLink dl = (DownloadLink) value;
            LinkStatus ls = dl.getLinkStatus();
            PluginProgress prog = dl.getPluginProgress();
            ImageIcon icon = null;
            if (prog != null && prog.getPercent() > 0.0 && prog.getPercent() < 100.0) {
                return prog.getIcon();
            } else if ((icon = ls.getStatusIcon()) != null) {
                return icon;
            } else if (ls.isFinished()) {
                return trueIcon;
            } else if (ls.hasStatus(LinkStatus.TEMP_IGNORE)) {
                return infoIcon;
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
            if (!dl.getLinkStatus().isPluginActive() && dl.isEnabled() && dl.getLivePlugin() == null) {
                if (!dl.getLinkStatus().hasStatus(LinkStatus.TEMP_IGNORE) && !dl.getLinkStatus().isFinished()) {
                    /* enabled links that are not running */
                    ProxyBlock timeout = null;
                    if ((timeout = ProxyController.getInstance().getHostIPBlockTimeout(dl.getHost())) != null) {
                        if (timeout.getLink() == value) {
                            return _JDT._.gui_download_waittime_status2(Formatter.formatSeconds(timeout.getBlockedTimeout() / 1000));
                        } else {
                            return _JDT._.gui_downloadlink_hosterwaittime();
                        }
                    }
                    if ((timeout = ProxyController.getInstance().getHostBlockedTimeout(dl.getHost())) != null) {
                        if (timeout.getLink() == value) {
                            return _JDT._.gui_download_waittime_status2(Formatter.formatSeconds(timeout.getBlockedTimeout() / 1000));
                        } else {
                            return _JDT._.gui_downloadlink_hostertempunavail();
                        }
                    }
                }
            }
            return dl.getLinkStatus().getMessage();

        } else if (value instanceof FilePackage) {
            if (((FilePackage) value).getView().isFinished()) { return "Finished"; }
            if (((FilePackage) value).getView().getETA() != -1) { return "Working"; }
        }
        return "";
    }
}
