package org.jdownloader.gui.views.downloads.columns;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PackageLinkNode;

import org.appwork.utils.swing.table.columns.ExtFileSizeColumn;
import org.jdownloader.gui.translate._GUI;

public class LoadedColumn extends ExtFileSizeColumn<PackageLinkNode> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public LoadedColumn() {
        super(_GUI._.LoadedColumn_LoadedColumn(), null);
    }

    @Override
    public int getDefaultWidth() {
        return 85;
    }

    // @Override
    // public int getMaxWidth() {
    //
    // return 90;
    // }

    @Override
    protected long getBytes(PackageLinkNode o2) {
        if (o2 instanceof DownloadLink) {
            return ((DownloadLink) o2).getDownloadCurrent();
        } else {
            return ((FilePackage) o2).getTotalKBLoaded();
        }
    }

    @Override
    public boolean isEnabled(PackageLinkNode obj) {
        return obj.isEnabled();
    }

}