package org.jdownloader.gui.views.downloads.columns;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingConstants;

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

public class ETAColumn extends ExtTextColumn<AbstractNode> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private ImageIcon         download;
    private ImageIcon         wait;
    private ImageIcon         icon2Use;

    @Override
    public int getDefaultWidth() {
        return 80;
    }

    public boolean isPaintWidthLockIcon() {
        return false;
    }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        return obj.isEnabled();
    }

    // @Override
    // public int getMaxWidth() {
    //
    // return 85;line
    // }
    @Override
    protected boolean isDefaultResizable() {
        return false;
    }

    public ETAColumn() {
        super(_GUI._.ETAColumn_ETAColumn());
        rendererField.setHorizontalAlignment(SwingConstants.RIGHT);
        this.download = NewTheme.I().getIcon("download", 16);
        this.wait = NewTheme.I().getIcon("wait", 16);
    }

    @Override
    protected Icon getIcon(AbstractNode value) {
        icon2Use = null;
        if (value instanceof DownloadLink) {
            DownloadLink dlLink = ((DownloadLink) value);
            if (dlLink.isEnabled()) {
                if (dlLink.getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
                    icon2Use = download;
                } else {
                    getWaitingTimeout(dlLink);
                }
            }
        }
        return icon2Use;
    }

    private long getWaitingTimeout(DownloadLink dlLink) {
        if (dlLink.isEnabled()) {
            long time;
            if (dlLink.getLinkStatus().hasStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE) && (time = dlLink.getLinkStatus().getRemainingWaittime()) > 0) {
                icon2Use = wait;
                return time;
            }
            if (!dlLink.getLinkStatus().hasStatus(LinkStatus.TEMP_IGNORE) && !dlLink.getLinkStatus().isFinished()) {
                ProxyBlock timeout = null;
                if ((timeout = ProxyController.getInstance().getHostIPBlockTimeout(dlLink.getHost())) != null && timeout.getLink() == dlLink) {
                    icon2Use = wait;
                    return timeout.getBlockedTimeout();
                }
                if ((timeout = ProxyController.getInstance().getHostBlockedTimeout(dlLink.getHost())) != null && timeout.getLink() == dlLink) {
                    icon2Use = wait;
                    return timeout.getBlockedTimeout();
                }
            }
            PluginProgress progress = null;
            if ((progress = dlLink.getPluginProgress()) != null) {
                icon2Use = progress.getIcon();
                return Math.max(0, progress.getCurrent());
            }
        }
        return -1;
    }

    @Override
    public String getStringValue(AbstractNode value) {
        if (value instanceof DownloadLink) {
            DownloadLink dlLink = ((DownloadLink) value);
            if (dlLink.isEnabled()) {
                if (dlLink.getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
                    long speed = dlLink.getDownloadSpeed();
                    if (speed > 0) {
                        if (dlLink.getDownloadSize() < 0) {
                            return _JDT._.gui_download_filesize_unknown() + " \u221E";
                        } else {
                            long remainingBytes = (dlLink.getDownloadSize() - dlLink.getDownloadCurrent());
                            long eta = remainingBytes / speed;
                            return Formatter.formatSeconds(eta);
                        }
                    } else {
                        return _JDT._.gui_download_create_connection();
                    }
                } else {
                    long ret = getWaitingTimeout(dlLink);
                    if (ret > 0) return Formatter.formatSeconds(ret / 1000);
                }
            }
        } else if (value instanceof FilePackage) {
            long eta = ((FilePackage) value).getView().getETA();
            if (eta > 0) {
                return Formatter.formatSeconds(eta);
            } else if (eta == Integer.MIN_VALUE) {
                /*
                 * no size known, no eta,show infinite symbol
                 */
                return "\u221E";
            }
        }
        return null;
    }
}
