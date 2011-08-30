package org.jdownloader.gui.views.downloads.columns;

import jd.controlling.linkcrawler.CrawledLinkInfo;
import jd.controlling.linkcrawler.CrawledPackageInfo;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.swing.exttable.columns.ExtFileSizeColumn;
import org.jdownloader.gui.translate._GUI;

public class SizeColumn extends ExtFileSizeColumn<AbstractNode> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public SizeColumn() {
        super(_GUI._.SizeColumn_SizeColumn());

    }

    public boolean isPaintWidthLockIcon() {
        return false;
    }

    @Override
    protected boolean isDefaultResizable() {
        return false;
    }

    @Override
    public int getDefaultWidth() {
        return 65;
    }

    // @Override
    // public int getMaxWidth() {
    //
    // return 83;
    // }

    @Override
    protected long getBytes(AbstractNode o2) {
        if (o2 instanceof CrawledPackageInfo) {
            return 0;
        } else if (o2 instanceof CrawledLinkInfo) { return ((CrawledLinkInfo) o2).getSize(); }
        if (o2 instanceof DownloadLink) {
            return ((DownloadLink) o2).getDownloadSize();
        } else if (o2 instanceof FilePackage) {
            return ((FilePackage) o2).getTotalEstimatedPackageSize();
        } else
            return -1;
    }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        return obj.isEnabled();
    }

}
