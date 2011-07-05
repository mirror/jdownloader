package org.jdownloader.gui.views.downloads.columns;

import jd.plugins.PackageLinkNode;

import org.appwork.utils.swing.table.columns.ExtLongColumn;
import org.jdownloader.gui.translate._GUI;

public class ListOrderIDColumn extends ExtLongColumn<PackageLinkNode> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public ListOrderIDColumn() {
        super(_GUI._.ListOrderIDColumn_ListOrderIDColumn());
    }

    @Override
    protected long getLong(PackageLinkNode value) {
        return value.getListOrderID();
    }

    public boolean isSortable(PackageLinkNode obj) {
        return true;
    }

    @Override
    public boolean isEnabled(PackageLinkNode obj) {
        return obj.isEnabled();
    }

}
