package org.jdownloader.gui.views.downloads.columns;

import java.util.Date;

import javax.swing.SwingConstants;

import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.swing.exttable.columns.ExtDateColumn;
import org.jdownloader.gui.translate._GUI;

public class AddedDateColumn extends ExtDateColumn<AbstractNode> {

    /**
     * 
     */
    private static final long serialVersionUID = -8841119846403017974L;

    public AddedDateColumn() {
        super(_GUI._.added_date_column_title());
        rendererField.setHorizontalAlignment(SwingConstants.CENTER);

    }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        if (obj instanceof CrawledPackage) { return ((CrawledPackage) obj).getView().isEnabled(); }

        return obj.isEnabled();
    }

    @Override
    public boolean isDefaultVisible() {
        return false;
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
    protected String getBadDateText(AbstractNode value) {
        return _GUI._.added_date_column_invalid();
    }

    protected String getDateFormatString() {

        return _GUI._.added_date_column_dateformat();
    }

    @Override
    protected Date getDate(AbstractNode node, Date date) {
        if (node.getCreated() <= 0) return null;
        date.setTime(node.getCreated());
        return date;
    }
}
