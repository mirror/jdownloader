package org.jdownloader.gui.views.downloads.columns;

import jd.plugins.PackageLinkNode;

import org.appwork.swing.exttable.columns.ExtFileSizeColumn;
import org.jdownloader.gui.translate._GUI;

public class RemainingColumn extends ExtFileSizeColumn<PackageLinkNode> {

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
    protected long getBytes(PackageLinkNode o2) {

        return o2.getRemainingKB();

    }

    @Override
    public boolean isDefaultVisible() {
        return false;
    }

    @Override
    public boolean isEnabled(PackageLinkNode obj) {
        return obj.isEnabled();
    }

}
