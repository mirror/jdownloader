package org.jdownloader.gui.views.downloads.columns;

import javax.swing.JPopupMenu;

import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.exttable.columns.ExtFileSizeColumn;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.SIZEUNIT;

public class LoadedColumn extends ExtFileSizeColumn<AbstractNode> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private final SIZEUNIT    maxSizeUnit;

    public LoadedColumn() {
        super(_GUI.T.LoadedColumn_LoadedColumn(), null);
        maxSizeUnit = JsonConfig.create(GraphicalUserInterfaceSettings.class).getMaxSizeUnit();
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
            return ((DownloadLink) o2).getView().getBytesLoaded();
        } else {
            return ((FilePackage) o2).getView().getDone();
        }
    }

    @Override
    public boolean isEnabled(AbstractNode obj) {
        return obj.isEnabled();
    }

    @Override
    protected String getSizeString(final long fileSize) {
        switch (maxSizeUnit) {
        case TiB:
            if (fileSize >= 1024 * 1024 * 1024 * 1024l) {
                return this.formatter.format(fileSize / (1024 * 1024 * 1024 * 1024.0)).concat(" TiB");
            }
        case GiB:
            if (fileSize >= 1024 * 1024 * 1024l) {
                return this.formatter.format(fileSize / (1024 * 1024 * 1024.0)).concat(" GiB");
            }
        case MiB:
            if (fileSize >= 1024 * 1024l) {
                return this.formatter.format(fileSize / (1024 * 1024.0)).concat(" MiB");
            }
        case KiB:
            if (fileSize >= 1024l) {
                return this.formatter.format(fileSize / 1024.0).concat(" KiB");
            }
        default:
            if (fileSize == 0) {
                return "0 B";
            }
            if (fileSize < 0) {
                return zeroString;
            }
            return fileSize + " B";
        }
    }

}