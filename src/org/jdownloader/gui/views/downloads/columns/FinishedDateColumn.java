package org.jdownloader.gui.views.downloads.columns;

import java.util.Date;

import javax.swing.SwingConstants;

import jd.plugins.PackageLinkNode;

import org.appwork.utils.swing.table.columns.ExtDateColumn;
import org.jdownloader.gui.translate._GUI;

public class FinishedDateColumn extends ExtDateColumn<PackageLinkNode> {

    /**
     * 
     */
    private static final long serialVersionUID = -8841119846403017974L;

    public FinishedDateColumn() {
        super(_GUI._.FinishedDateColumn_FinishedDateColumn());
        rendererField.setHorizontalAlignment(SwingConstants.CENTER);
    }

    @Override
    protected String getBadDateText(PackageLinkNode value) {
        return "";
    }

    @Override
    public boolean isDefaultVisible() {
        return false;
    }

    // @Override
    // public int getMaxWidth() {
    //
    // return 110;
    // }

    @Override
    public boolean isEnabled(PackageLinkNode obj) {
        return obj.isEnabled();
    }

    @Override
    protected Date getDate(PackageLinkNode node, Date date) {

        if (node.getFinishedDate() <= 0) return null;
        date.setTime(node.getFinishedDate());

        return date;
    }

}
