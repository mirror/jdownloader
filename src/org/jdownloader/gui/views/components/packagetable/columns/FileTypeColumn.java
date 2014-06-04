package org.jdownloader.gui.views.components.packagetable.columns;

import javax.swing.Icon;
import javax.swing.JPopupMenu;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ExtensionsFilterInterface;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.columns.FileColumn;

public class FileTypeColumn extends ExtTextColumn<AbstractNode> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public FileTypeColumn() {
        super(_GUI._.FileTypeColumn_FileTypeColumn_());
    }

    public JPopupMenu createHeaderPopup() {
        return FileColumn.createColumnPopup(this, getMinWidth() == getMaxWidth() && getMaxWidth() > 0);
    }

    @Override
    public boolean isDefaultVisible() {
        return false;
    }

    @Override
    protected boolean isEditable(final AbstractNode obj, final boolean enabled) {
        return false;
    }

    @Override
    public boolean isEditable(AbstractNode obj) {
        return false;
    }

    @Override
    public boolean isEnabled(final AbstractNode obj) {
        if (obj instanceof CrawledPackage) {
            return ((CrawledPackage) obj).getView().isEnabled();
        }
        if (obj instanceof FilePackage) {
            return ((FilePackage) obj).getView().isEnabled();
        }
        return obj.isEnabled();
    }

    @Override
    public String getStringValue(AbstractNode value) {
        ExtensionsFilterInterface extension = null;
        if (value instanceof DownloadLink) {
            extension = ((DownloadLink) value).getLinkInfo().getExtension();
        } else if (value instanceof CrawledLink) {
            extension = ((CrawledLink) value).getLinkInfo().getExtension();
        }
        if (extension != null) {
            return extension.name();
        }
        return null;
    }

    @Override
    protected Icon getIcon(AbstractNode value) {
        if (value instanceof DownloadLink) {
            return ((DownloadLink) value).getLinkInfo().getIcon();
        } else if (value instanceof CrawledLink) {
            return ((CrawledLink) value).getLinkInfo().getIcon();
        }
        return null;
    }

    @Override
    protected String getTooltipText(AbstractNode value) {
        ExtensionsFilterInterface extension = null;
        if (value instanceof DownloadLink) {
            extension = ((DownloadLink) value).getLinkInfo().getExtension();
        } else if (value instanceof CrawledLink) {
            extension = ((CrawledLink) value).getLinkInfo().getExtension();
        }
        if (extension != null) {
            return extension.getDesc();
        }
        return null;
    }
}