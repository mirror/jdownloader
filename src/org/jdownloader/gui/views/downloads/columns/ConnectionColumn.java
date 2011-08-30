package org.jdownloader.gui.views.downloads.columns;

import jd.controlling.SingleDownloadController;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.download.DownloadInterface;

import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.gui.translate._GUI;

public class ConnectionColumn extends ExtTextColumn<AbstractNode> {

    public ConnectionColumn() {
        super(_GUI._.ConnectionColumn_ConnectionColumn());
    }

    @Override
    protected String getTooltipText(AbstractNode obj) {
        return super.getTooltipText(obj);
    }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        return obj.isEnabled();
    }

    @Override
    protected boolean isDefaultResizable() {
        return false;
    }

    @Override
    public int getDefaultWidth() {
        return 85;
    }

    // @Override
    // public int getMaxWidth() {
    //
    // return 150;
    // }
    public boolean isPaintWidthLockIcon() {
        return false;
    }

    @Override
    public String getStringValue(AbstractNode value) {
        if (value instanceof DownloadLink) {
            SingleDownloadController dlc = ((DownloadLink) value).getDownloadLinkController();
            if (dlc != null) {
                //
                DownloadInterface dli = ((DownloadLink) value).getDownloadInstance();
                if (dli == null) {
                    // return ("" + dlc.getCurrentProxy()+" (" +
                    // dli.getChunksDownloading() + "/" + dli.getChunkNum() +
                    // ")");

                    return _GUI._.ConnectionColumn_getStringValue_pluginonly(dlc.getCurrentProxy());
                } else {
                    return _GUI._.ConnectionColumn_getStringValue_withchunks(dlc.getCurrentProxy(), dli.getChunksDownloading(), dli.getChunkNum());
                }

            }
        }
        return null;
    }

}
