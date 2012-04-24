package org.jdownloader.gui.views.downloads.columns;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingConstants;

import jd.controlling.packagecontroller.AbstractNode;
import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;

import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;

public class ETAColumn extends ExtTextColumn<AbstractNode> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private long              time;
    private ImageIcon         download;
    private ImageIcon         wait;

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
        if (value instanceof DownloadLink) {
            DownloadLink dlLink = ((DownloadLink) value);
            if (dlLink.isEnabled()) {
                if (dlLink.getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
                    return download;
                } else if (dlLink.getLinkStatus().hasStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE) && (time = dlLink.getLinkStatus().getRemainingWaittime()) > 0) { return wait; }
            }
        }
        return null;
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
                } else if (dlLink.getLinkStatus().hasStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE) && (time = dlLink.getLinkStatus().getRemainingWaittime()) > 0) { return Formatter.formatSeconds(time / 1000); }
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
