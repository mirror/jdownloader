package org.jdownloader.gui.views.downloads.columns;

import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.FilePackageView;

import org.appwork.swing.exttable.columns.ExtFileSizeColumn;
import org.jdownloader.gui.translate._GUI;

public class RemainingColumn extends ExtFileSizeColumn<AbstractNode> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public RemainingColumn() {
        super(_GUI._.RemainingColumn_RemainingColumn());
    }

    // @Override
    // public int getMaxWidth() {
    //
    // return 85;
    // }
    @Override
    protected boolean isDefaultResizable() {
        return false;
    }

    @Override
    public int getDefaultWidth() {
        return 90;
    }

    @Override
    protected long getBytes(AbstractNode o2) {
        if (o2 instanceof DownloadLink) {
            return ((DownloadLink) o2).getRemainingKB();
        } else if (o2 instanceof FilePackage) {
            FilePackageView view = ((FilePackage) o2).getView();
            return Math.max(0, view.getSize() - view.getDone());
        } else
            return -1l;

    }

    @Override
    public boolean isDefaultVisible() {
        return false;
    }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        return obj.isEnabled();
    }

}
