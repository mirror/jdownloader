package org.jdownloader.gui.views.downloads.columns;

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
        if (object instanceof FilePackage) {
            ((FilePackage) object).setComment(value);
        } else {
            ((DownloadLink) object).setSourcePluginComment(value);
        }
    }

    @Override
    public void resetEditor() {
        super.resetEditor();
        // editor.setBorder(null);
    }

    @Override
    public String getStringValue(AbstractNode value) {
        if (value instanceof FilePackage) {
            return ((FilePackage) value).getComment();
        } else {
            return ((DownloadLink) value).getSourcePluginComment();
        }
    }
}
