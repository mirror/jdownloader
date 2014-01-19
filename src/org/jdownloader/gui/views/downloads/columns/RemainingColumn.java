package org.jdownloader.gui.views.downloads.columns;

import javax.swing.JPopupMenu;

import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.FilePackageView;

import org.appwork.swing.exttable.columns.ExtFileSizeColumn;
import org.jdownloader.gui.translate._GUI;

public class RemainingColumn extends ExtFileSizeColumn<AbstractNode> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public JPopupMenu createHeaderPopup() {

        return FileColumn.createColumnPopup(this, getMinWidth() == getMaxWidth() && getMaxWidth() > 0);

    }

    public RemainingColumn() {
        super(_GUI._.RemainingColumn_RemainingColumn());
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
    protected String getInvalidValue() {
        return "~";
    }

    @Override
    protected long getBytes(AbstractNode o2) {
        if (o2 instanceof DownloadLink) {
            DownloadLink link = ((DownloadLink) o2);
            long size = link.getView().getBytesTotal();
            if (size >= 0) {
                return Math.max(-1, size - link.getView().getBytesLoaded());
            } else
                return -1;
        } else if (o2 instanceof FilePackage) {
            FilePackageView view = ((FilePackage) o2).getView();
            return Math.max(0, view.getSize() - view.getDone());
        } else
            return -1l;

    }

    @Override
    public boolean isDefaultVisible() {
        return false;
    }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        return obj.isEnabled();
    }

}
