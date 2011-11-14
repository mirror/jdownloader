package org.jdownloader.gui.views.linkgrabber;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;

import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.gui.translate._GUI;

public class DownloadPasswordColumn extends ExtTextColumn<AbstractNode> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public DownloadPasswordColumn() {
        super(_GUI._.DownloadPasswordColumn_DownloadPasswordColumn_object_());

    }

    @Override
    public boolean isDefaultVisible() {
        return false;
    }

    @Override
    public boolean isEditable(AbstractNode obj) {
        if (obj instanceof CrawledLink) return true;
        if (obj instanceof DownloadLink) return true;
        return false;
    }

    @Override
    protected void setStringValue(String value, AbstractNode object) {
        DownloadLink dl = null;
        if (object instanceof CrawledLink) {
            dl = ((CrawledLink) object).getDownloadLink();
        } else if (object instanceof DownloadLink) {
            dl = ((DownloadLink) object);
        }
        if (dl != null) dl.setDownloadPassword(value);
    }

    @Override
    public String getStringValue(AbstractNode value) {
        DownloadLink dl = null;
        if (value instanceof CrawledLink) {
            dl = ((CrawledLink) value).getDownloadLink();
        } else if (value instanceof DownloadLink) {
            dl = ((DownloadLink) value);
        }
        if (dl != null) return dl.getDownloadPassword();
        return null;

    }

}
