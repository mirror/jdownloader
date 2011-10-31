package org.jdownloader.gui.views.linkgrabber;

import jd.controlling.packagecontroller.AbstractNode;

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
        return true;
    }

    @Override
    protected void setStringValue(String value, AbstractNode object) {
        super.setStringValue(value, object);
    }

    @Override
    public String getStringValue(AbstractNode value) {
        return null;
    }

}
