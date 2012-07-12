package org.jdownloader.gui.views.downloads.columns;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
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
        return 70;
    }

    // @Override
    // public int getMaxWidth() {
    //
    // return 83;
    // }

    @Override
    public int getMinWidth() {
        return getDefaultWidth();
    }

    @Override
    protected long getBytes(AbstractNode o2) {

        if (o2 instanceof CrawledPackage) {
            return ((CrawledPackage) o2).getView().getFileSize();
        } else if (o2 instanceof CrawledLink) {
            return ((CrawledLink) o2).getSize();
        } else if (o2 instanceof DownloadLink) {
            return ((DownloadLink) o2).getDownloadSize();
        } else if (o2 instanceof FilePackage) {
            return ((FilePackage) o2).getView().getSize();
        } else
            return -1;
    }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        if (obj instanceof CrawledPackage) { return ((CrawledPackage) obj).getView().isEnabled(); }
        return obj.isEnabled();
    }

}
