package org.jdownloader.gui.views.downloads.columns;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;

import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.plugins.FilePackage;

import org.appwork.swing.exttable.columns.ExtDateColumn;
import org.appwork.utils.StringUtils;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class ModifiedDateColumn extends ExtDateColumn<AbstractNode> {
    /**
     *
     */
    private static final long serialVersionUID = -8841119846403017974L;
    private final String      bad              = _GUI.T.added_date_column_invalid();

    public JPopupMenu createHeaderPopup() {
        return FileColumn.createColumnPopup(this, getMinWidth() == getMaxWidth() && getMaxWidth() > 0);
    }

    public ModifiedDateColumn() {
        super(_GUI.T.modified_date_column_title());
        rendererField.setHorizontalAlignment(SwingConstants.CENTER);
    }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        if (obj instanceof FilePackage) {
            return ((FilePackage) obj).getView().isEnabled();
        } else if (obj instanceof CrawledPackage) {
            return ((CrawledPackage) obj).getView().isEnabled();
        } else {
            return obj.isEnabled();
        }
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
    protected String getBadDateText(AbstractNode node) {
        if (node instanceof AbstractPackageNode) {
            return bad;
        } else {
            return null;
        }
    }

    protected String getDateFormatString() {
        final String custom = CFG_GUI.CFG.getDateTimeFormatDownloadListModifiedDateColumn();
        if (StringUtils.isNotEmpty(custom)) {
            return custom;
        } else {
            final DateFormat sd = SimpleDateFormat.getDateTimeInstance();
            if (sd instanceof SimpleDateFormat) {
                return ((SimpleDateFormat) sd).toPattern();
            } else {
                return _GUI.T.modified_date_column_dateformat();
            }
        }
    }

    @Override
    protected Date getDate(AbstractNode node, Date date) {
        if (node.getCreated() <= 0) {
            return null;
        } else if (node instanceof AbstractPackageNode) {
            date.setTime(((AbstractPackageNode) node).getModified());
            return date;
        } else {
            return null;
        }
    }
}
