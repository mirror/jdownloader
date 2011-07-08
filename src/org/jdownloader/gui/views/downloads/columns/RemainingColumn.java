package org.jdownloader.gui.views.downloads.columns;

import jd.plugins.PackageLinkNode;

import org.appwork.utils.swing.table.columns.ExtFileSizeColumn;
import org.jdownloader.gui.translate._GUI;

public class RemainingColumn extends ExtFileSizeColumn<PackageLinkNode> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public RemainingColumn() {
        super(_GUI._.RemainingColumn_RemainingColumn());
    }

    @Override
    public int getMaxWidth() {

        return 85;
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
