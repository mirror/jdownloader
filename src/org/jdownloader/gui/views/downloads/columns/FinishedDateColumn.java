package org.jdownloader.gui.views.downloads.columns;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;

import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.swing.exttable.columns.ExtDateColumn;
import org.appwork.utils.StringUtils;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.staticreferences.CFG_GUI;

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

    protected String getDateFormatString() {
        String custom = CFG_GUI.CFG.getDateTimeFormatDownloadListFinishedDateColumn();
        if (StringUtils.isNotEmpty(custom)) { return custom; }
        DateFormat sd = SimpleDateFormat.getDateTimeInstance();
        if (sd instanceof SimpleDateFormat) { return ((SimpleDateFormat) sd).toPattern(); }
        return _GUI._.added_date_column_dateformat();
    }

    @Override
    protected Date getDate(AbstractNode node, Date date) {
        if (node.getFinishedDate() <= 0) return null;
        date.setTime(node.getFinishedDate());
        return date;
    }

}
