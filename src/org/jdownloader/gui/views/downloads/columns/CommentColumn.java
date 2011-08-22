package org.jdownloader.gui.views.downloads.columns;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PackageLinkNode;

import org.appwork.swing.exttable.columns.ExtTextAreaColumn;
import org.jdownloader.gui.translate._GUI;

public class CommentColumn extends ExtTextAreaColumn<PackageLinkNode> {

    public CommentColumn() {
        super(_GUI._.CommentColumn_CommentColumn_());
    }

    @Override
    public boolean isEditable(PackageLinkNode obj) {
        return true;
    }

    public boolean isPaintWidthLockIcon() {
        return false;
    }

    @Override
    protected void setStringValue(String value, PackageLinkNode object) {
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
    public String getStringValue(PackageLinkNode value) {
        if (value instanceof FilePackage) {
            return ((FilePackage) value).getComment();
        } else {
            return ((DownloadLink) value).getSourcePluginComment();
        }
    }
}
