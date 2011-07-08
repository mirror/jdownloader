package org.jdownloader.gui.views.downloads.columns;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import jd.plugins.PackageLinkNode;

import org.appwork.utils.swing.table.ExtTableHeaderRenderer;
import org.appwork.utils.swing.table.columns.ExtLongColumn;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class ListOrderIDColumn extends ExtLongColumn<PackageLinkNode> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public ListOrderIDColumn() {
        super(_GUI._.ListOrderIDColumn_ListOrderIDColumn());
    }

    public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

        final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

            private static final long serialVersionUID = 2051980596953422289L;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setIcon(NewTheme.I().getIcon("sort", 14));
                setHorizontalAlignment(CENTER);
                setText(null);
                return this;
            }

        };

        return ret;
    }

    @Override
    public boolean isDefaultVisible() {
        return false;
    }

    @Override
    public int getDefaultWidth() {
        return 30;
    }

    @Override
    public int getMaxWidth() {

        return 50;
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
