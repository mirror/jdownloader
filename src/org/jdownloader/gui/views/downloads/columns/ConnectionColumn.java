package org.jdownloader.gui.views.downloads.columns;

import jd.controlling.SingleDownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.PackageLinkNode;
import jd.plugins.download.DownloadInterface;

import org.appwork.utils.swing.table.columns.ExtTextColumn;
import org.jdownloader.gui.translate._GUI;

public class ConnectionColumn extends ExtTextColumn<jd.plugins.PackageLinkNode> {

    public ConnectionColumn() {
        super(_GUI._.ConnectionColumn_ConnectionColumn());
    }

    @Override
    protected String getTooltipText(PackageLinkNode obj) {
        return super.getTooltipText(obj);
    }

    @Override
    public boolean isEnabled(PackageLinkNode obj) {
        return obj.isEnabled();
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

    @Override
    public String getStringValue(PackageLinkNode value) {
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
