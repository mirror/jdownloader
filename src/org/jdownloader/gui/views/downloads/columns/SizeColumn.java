package org.jdownloader.gui.views.downloads.columns;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PackageLinkNode;

import org.appwork.utils.swing.table.columns.ExtFileSizeColumn;

public class SizeColumn extends ExtFileSizeColumn<PackageLinkNode> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public SizeColumn() {
        super("SizeColumn", null);
    }

    @Override
    protected long getBytes(PackageLinkNode o2) {
        if (o2 instanceof DownloadLink) {
            return ((DownloadLink) o2).getDownloadSize();
        } else {
            return ((FilePackage) o2).getTotalEstimatedPackageSize();
        }
    }

}
