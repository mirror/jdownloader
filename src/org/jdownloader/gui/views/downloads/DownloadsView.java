package org.jdownloader.gui.views.downloads;

import javax.swing.Icon;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

import jd.gui.swing.jdgui.interfaces.View;

public class DownloadsView extends View {

    private static final long serialVersionUID = 2624923838160423884L;

    public DownloadsView() {
        super();
        this.setContent(new DownloadsPanel());

    }

    @Override
    public Icon getIcon() {
        return new AbstractIcon(IconKey.ICON_DOWNLOAD, ICON_SIZE);
    }

    @Override
    public String getTitle() {
        return _GUI.T.jd_gui_swing_jdgui_views_downloadview_tab_title();
    }

    @Override
    public String getTooltip() {
        return _GUI.T.jd_gui_swing_jdgui_views_downloadview_tab_tooltip();
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