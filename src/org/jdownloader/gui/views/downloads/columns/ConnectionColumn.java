package org.jdownloader.gui.views.downloads.columns;

import jd.controlling.DownloadWatchDog;
import jd.controlling.SingleDownloadController;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.download.DownloadInterface;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtDefaultRowSorter;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.gui.translate._GUI;

public class ConnectionColumn extends ExtTextColumn<AbstractNode> {

    public ConnectionColumn() {
        super(_GUI._.ConnectionColumn_ConnectionColumn());
        this.setRowSorter(new ExtDefaultRowSorter<AbstractNode>() {

            @Override
            public int compare(final AbstractNode o1, final AbstractNode o2) {
                final long l1 = getDownloads(o1);
                final long l2 = getDownloads(o2);
                if (l1 == l2) { return 0; }
                if (this.getSortOrderIdentifier() == ExtColumn.SORT_ASC) {
                    return l1 > l2 ? -1 : 1;
                } else {
                    return l1 < l2 ? -1 : 1;
                }
            }

        });
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
        } else if (value instanceof FilePackage) { return DownloadWatchDog.getInstance().getDownloadsbyFilePackage((FilePackage) value) + "/" + ((FilePackage) value).size(); }
        return null;
    }

    private int getDownloads(AbstractNode value) {
        if (value instanceof DownloadLink) {
            SingleDownloadController dlc = ((DownloadLink) value).getDownloadLinkController();
            if (dlc != null) {
                DownloadInterface dli = ((DownloadLink) value).getDownloadInstance();
                if (dli != null) return 1;
            }
        } else if (value instanceof FilePackage) { return DownloadWatchDog.getInstance().getDownloadsbyFilePackage((FilePackage) value); }
        return 0;
    }

}
