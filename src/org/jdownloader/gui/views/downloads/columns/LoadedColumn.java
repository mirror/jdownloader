package org.jdownloader.gui.views.downloads.columns;

import javax.swing.JPopupMenu;

import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.swing.exttable.columns.ExtFileSizeColumn;
import org.jdownloader.gui.translate._GUI;

public class LoadedColumn extends ExtFileSizeColumn<AbstractNode> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public LoadedColumn() {
        super(_GUI._.LoadedColumn_LoadedColumn(), null);
    }

    public JPopupMenu createHeaderPopup() {

        return FileColumn.createColumnPopup(this, getMinWidth() == getMaxWidth() && getMaxWidth() > 0);

    }

    @Override
    protected boolean isDefaultResizable() {
        return false;
    }

    @Override
    public int getDefaultWidth() {
        return 90;
    }

    @Override
    protected long getBytes(AbstractNode o2) {
        if (o2 instanceof DownloadLink) {
            return ((DownloadLink) o2).getDownloadCurrent();
        } else {
            return ((FilePackage) o2).getView().getDone();
        }
    }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        return obj.isEnabled();
    }

}