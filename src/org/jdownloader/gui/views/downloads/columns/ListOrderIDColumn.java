package org.jdownloader.gui.views.downloads.columns;

import jd.plugins.PackageLinkNode;

import org.appwork.utils.swing.table.columns.ExtLongColumn;

public class ListOrderIDColumn extends ExtLongColumn<PackageLinkNode> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public ListOrderIDColumn() {
        super("ListID");
    }

    @Override
    protected long getLong(PackageLinkNode value) {
        return value.getListOrderID();
    }

    @Override
    public boolean isSortable(PackageLinkNode obj) {
        return true;
    }

}
