package org.jdownloader.gui.views.linkgrabber;

import javax.swing.Icon;

import jd.gui.swing.jdgui.interfaces.View;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class LinkGrabberView extends View {
    public LinkGrabberView() {
        super();
        this.setContent(new LinkGrabberPanel());
    }

    @Override
    public Icon getIcon() {
        return NewTheme.I().getIcon("linkgrabber", ICON_SIZE);
    }

    @Override
    public String getTitle() {
        return _GUI._.jd_gui_swing_jdgui_views_linkgrabberview_tab_title();
    }

    @Override
    public String getTooltip() {
        return _GUI._.jd_gui_swing_jdgui_views_linkgrabberview_tab_tooltip();
    }

    @Override
    protected void onHide() {
    }

    @Override
    protected void onShow() {
    }

    @Override
    public String getID() {
        return "linkgrabberview2";
    }
}
