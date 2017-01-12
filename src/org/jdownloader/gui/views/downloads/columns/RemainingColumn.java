package org.jdownloader.gui.views.downloads.columns;

import javax.swing.JPopupMenu;

import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.FilePackageView;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.exttable.columns.ExtFileSizeColumn;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.SIZEUNIT;

public class RemainingColumn extends ExtFileSizeColumn<AbstractNode> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private final SIZEUNIT    maxSizeUnit;

    public JPopupMenu createHeaderPopup() {
        return FileColumn.createColumnPopup(this, getMinWidth() == getMaxWidth() && getMaxWidth() > 0);
    }

    public RemainingColumn() {
        super(_GUI.T.RemainingColumn_RemainingColumn());
        maxSizeUnit = JsonConfig.create(GraphicalUserInterfaceSettings.class).getMaxSizeUnit();
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
            final DownloadLink link = ((DownloadLink) o2);
            final long size = link.getView().getBytesTotal();
            if (size >= 0) {
                return Math.max(0, size - link.getView().getBytesLoaded());
            } else {
                return -1;
            }
        } else if (o2 instanceof FilePackage) {
            final FilePackageView view = ((FilePackage) o2).getView();
            if (view.getUnknownFileSizes() > 0) {
                return Math.max(-1, view.getSize() - view.getDone());
            }
            return Math.max(0, view.getSize() - view.getDone());
        } else {
            return -1l;
        }
    }

    @Override
    protected String getSizeString(long fileSize) {
        return SIZEUNIT.formatValue(maxSizeUnit, formatter, fileSize);
    }

    @Override
    protected String getTooltipText(final AbstractNode value) {
        final long sizeValue = this.getBytes(value);
        if (sizeValue < 0) {
            return _GUI.T.SizeColumn_getSizeString_zero_tt();
        } else {
            return this.getSizeString(sizeValue);
        }

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
