package org.jdownloader.gui.views.downloads.columns;

import javax.swing.Icon;

import jd.controlling.DownloadWatchDog;
import jd.plugins.PackageLinkNode;

import org.appwork.utils.swing.table.columns.ExtTextColumn;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class StopSignColumn extends ExtTextColumn<jd.plugins.PackageLinkNode> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public StopSignColumn() {
        super(_GUI._.StopSignColumn_StopSignColumn());

    }

    @Override
    protected Icon getIcon(PackageLinkNode value) {
        if (DownloadWatchDog.getInstance().isStopMark(value)) { return NewTheme.I().getIcon("stopsign", 16); }
        return null;
    }

    @Override
    protected String getTooltipText(PackageLinkNode obj) {
        if (DownloadWatchDog.getInstance().isStopMark(obj)) { return _GUI._.jd_gui_swing_jdgui_views_downloadview_TableRenderer_stopmark(); }
        return null;
    }

    @Override
    public int getDefaultWidth() {
        return 30;
    }

    @Override
    protected int getMaxWidth() {
        return getDefaultWidth();
    }

    @Override
    public int getMinWidth() {
        return getDefaultWidth();
    }

    @Override
    public boolean isDefaultVisible() {
        return false;
    }

    @Override
    public String getStringValue(PackageLinkNode value) {

        return "";
    }

}
