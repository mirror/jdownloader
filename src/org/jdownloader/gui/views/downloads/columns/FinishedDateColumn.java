package org.jdownloader.gui.views.downloads.columns;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PackageLinkNode;

public class FinishedDateColumn extends DateColumn {

    /**
     * 
     */
    private static final long serialVersionUID = -8841119846403017974L;

    public FinishedDateColumn() {
        super("FinishedDate");
    }

    @Override
    public long getDate(PackageLinkNode node) {
        if (node instanceof DownloadLink) {
            return Math.max(0, ((DownloadLink) node).getFinishedDate());
        } else {
            return Math.max(0, ((FilePackage) node).getFinishedDate());
        }
    }
}
