package org.jdownloader.gui.views.components.packagetable.columns;

import javax.swing.JPopupMenu;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.swing.exttable.columns.ExtTextAreaColumn;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.columns.FileColumn;

public class CommentColumn extends ExtTextAreaColumn<AbstractNode> {

    /**
     * 
     */
    private static final long serialVersionUID = 3276217379318150024L;

    public CommentColumn() {
        super(_GUI._.CommentColumn_CommentColumn_());
    }

    public JPopupMenu createHeaderPopup() {

        return FileColumn.createColumnPopup(this, getMinWidth() == getMaxWidth() && getMaxWidth() > 0);

    }

    @Override
    public boolean isEditable(AbstractNode obj) {
        return true;
    }

    @Override
    public boolean isEnabled(final AbstractNode obj) {
        if (obj instanceof AbstractPackageNode) { return ((AbstractPackageNode) obj).getView().isEnabled(); }
        return obj.isEnabled();
    }

    protected boolean isEditable(final AbstractNode obj, final boolean enabled) {
        /* needed so we can edit even is row is disabled */
        return isEditable(obj);
    }

    @Override
    protected void setStringValue(String value, AbstractNode object) {
        DownloadLink dl = null;
        if (object instanceof DownloadLink) {
            dl = (DownloadLink) object;
        } else if (object instanceof CrawledLink) {
            dl = ((CrawledLink) object).getDownloadLink();
        } else if (object instanceof FilePackage) {
            ((FilePackage) object).setComment(value);
            return;
        } else if (object instanceof CrawledPackage) {
            ((CrawledPackage) object).setComment(value);
            return;
        }
        if (dl != null) {
            dl.setComment(value);
            return;
        }
    }

    @Override
    public String getStringValue(AbstractNode object) {
        DownloadLink dl = null;
        if (object instanceof DownloadLink) {
            dl = (DownloadLink) object;
        } else if (object instanceof CrawledLink) {
            dl = ((CrawledLink) object).getDownloadLink();
        } else if (object instanceof FilePackage) {
            return ((FilePackage) object).getComment();
        } else if (object instanceof CrawledPackage) { return ((CrawledPackage) object).getComment(); }
        if (dl != null) { return dl.getComment(); }
        return null;
    }
}
