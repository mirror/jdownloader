package org.jdownloader.gui.views.downloads.columns;

import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

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

    public boolean isPaintWidthLockIcon() {
        return false;
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
            return ((FilePackage) o2).getRemainingKB();
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
