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
        renderer.setHorizontalAlignment(SwingConstants.CENTER);
    }

    @Override
    protected String getBadDateText(PackageLinkNode value) {
        return "";
    }

    @Override
    protected Date getDate(PackageLinkNode node, Date date) {

        if (node.getFinishedDate() <= 0) return null;
        date.setTime(node.getFinishedDate());

        return date;
    }

}
