package org.jdownloader.gui.views.downloads.columns;

import java.util.Date;

import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;

import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.swing.exttable.columns.ExtDateColumn;
import org.jdownloader.gui.translate._GUI;

public class FinishedDateColumn extends ExtDateColumn<AbstractNode> {

    /**
     * 
     */
    private static final long serialVersionUID = -8841119846403017974L;

    public FinishedDateColumn() {
        super(_GUI._.FinishedDateColumn_FinishedDateColumn());
        rendererField.setHorizontalAlignment(SwingConstants.CENTER);
    }

    public JPopupMenu createHeaderPopup() {

        return FileColumn.createColumnPopup(this, getMinWidth() == getMaxWidth() && getMaxWidth() > 0);

    }

    @Override
    protected String getBadDateText(AbstractNode value) {
        return "";
    }

    @Override
    protected boolean isDefaultResizable() {
        return false;
    }

    @Override
    public int getDefaultWidth() {
        return 95;
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
    public boolean isEnabled(AbstractNode obj) {
        return obj.isEnabled();
    }

    @Override
    protected Date getDate(AbstractNode node, Date date) {
        if (node.getFinishedDate() <= 0) return null;
        date.setTime(node.getFinishedDate());
        return date;
    }

}
