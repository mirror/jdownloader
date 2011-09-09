package org.jdownloader.gui.views.downloads.columns;

import javax.swing.Icon;
import javax.swing.SwingConstants;

import jd.controlling.DownloadWatchDog;
import jd.controlling.packagecontroller.AbstractNode;
import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;

import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.translate._JDT;

public class SpeedColumn extends ExtTextColumn<AbstractNode> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public SpeedColumn() {
        super(_GUI._.SpeedColumn_SpeedColumn());
        rendererField.setHorizontalAlignment(SwingConstants.RIGHT);
    }

    public boolean isPaintWidthLockIcon() {
        return false;
    }

    @Override
    protected boolean isDefaultResizable() {
        return false;
    }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        return obj.isEnabled();
    }

    @Override
    public int getDefaultWidth() {
        return 65;
    }

    // @Override
    // public int getMaxWidth() {
    //
    // return 80;
    // }

    @Override
    protected Icon getIcon(AbstractNode value) {

        return null;
    }

    @Override
    public String getStringValue(AbstractNode value) {
        if (value instanceof DownloadLink) {
            if (((DownloadLink) value).getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) {
                if (((DownloadLink) value).getDownloadSpeed() > 0) {
                    return Formatter.formatReadable(((DownloadLink) value).getDownloadSpeed()) + "/s";
                } else {
                    return _JDT._.gui_download_create_connection();
                }
            }
        } else if (value instanceof FilePackage) {
            long speed = DownloadWatchDog.getInstance().getDownloadSpeedbyFilePackage((FilePackage) value);
            if (speed >= 0) { return Formatter.formatReadable(speed) + "/s"; }
        }
        return null;
    }
}
