package org.jdownloader.gui.views.downloads;

import javax.swing.Icon;

import jd.gui.swing.jdgui.interfaces.View;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class DownloadsView extends View {

    private static final long serialVersionUID = 2624923838160423884L;

    public DownloadsView() {
        super();
        this.setContent(new DownloadsPanel());
    }

    @Override
    public Icon getIcon() {
        return NewTheme.I().getIcon("download", ICON_SIZE);
    }

    @Override
    public String getTitle() {
        return _GUI._.jd_gui_swing_jdgui_views_downloadview_tab_title();
    }

    @Override
    public String getTooltip() {
        return _GUI._.jd_gui_swing_jdgui_views_downloadview_tab_tooltip();
    }

    @Override
    protected void onHide() {
    }

    @Override
    protected void onShow() {
    }

    @Override
    public String getID() {
        return "downloadsview";
    }

}