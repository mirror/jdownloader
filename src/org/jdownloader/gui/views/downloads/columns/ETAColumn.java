package org.jdownloader.gui.views.downloads.columns;

import javax.swing.Icon;
import javax.swing.SwingConstants;

import jd.controlling.packagecontroller.AbstractNode;
import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;

import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.translate._JDT;

public class ETAColumn extends ExtTextColumn<AbstractNode> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

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
    }

    @Override
    protected Icon getIcon(AbstractNode value) {

        return null;
    }

    @Override
    public String getStringValue(AbstractNode value) {
        if (value instanceof DownloadLink) {
            if (((DownloadLink) value).getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
                long speed = ((DownloadLink) value).getDownloadSpeed();
                if (speed > 0) {
                    if (((DownloadLink) value).getDownloadSize() < 0) {
                        return _JDT._.gui_download_filesize_unknown() + " \u221E";
                    } else {
                        long remainingBytes = ((DownloadLink) value).getDownloadSize() - ((DownloadLink) value).getDownloadCurrent();
                        long eta = remainingBytes / speed;
                        return Formatter.formatSeconds(eta);
                    }
                } else {
                    return _JDT._.gui_download_create_connection();
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
