package org.jdownloader.gui.views.downloads.columns;

import jd.controlling.linkcrawler.CrawledLinkInfo;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.swing.exttable.columns.ExtTextAreaColumn;
import org.jdownloader.gui.translate._GUI;

public class CommentColumn extends ExtTextAreaColumn<AbstractNode> {

    public CommentColumn() {
        super(_GUI._.CommentColumn_CommentColumn_());
    }

    @Override
    public boolean isEditable(AbstractNode obj) {
        return true;
    }

    public boolean isPaintWidthLockIcon() {
        return false;
    }

    @Override
    protected void setStringValue(String value, AbstractNode object) {
        DownloadLink dl = null;
        if (object instanceof DownloadLink) {
            dl = (DownloadLink) object;
        } else if (object instanceof CrawledLinkInfo) {
            dl = ((CrawledLinkInfo) object).getDownloadLink();
        }
        if (dl != null) {
            dl.setSourcePluginComment(value);
            return;
        }
        if (object instanceof FilePackage) {
            ((FilePackage) object).setComment(value);
        }
    }

    @Override
    public void resetEditor() {
        super.resetEditor();
        // editor.setBorder(null);
    }

    @Override
    public String getStringValue(AbstractNode object) {
        DownloadLink dl = null;
        if (object instanceof DownloadLink) {
            dl = (DownloadLink) object;
        } else if (object instanceof CrawledLinkInfo) {
            dl = ((CrawledLinkInfo) object).getDownloadLink();
        }
        if (dl != null) { return dl.getSourcePluginComment(); }
        if (object instanceof FilePackage) { return ((FilePackage) object).getComment(); }
        return null;
    }
}
