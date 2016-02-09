package org.jdownloader.gui.views.linkgrabber;

import javax.swing.Icon;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

import jd.SecondLevelLaunch;
import jd.gui.swing.jdgui.interfaces.View;

public class LinkGrabberView extends View {
    public LinkGrabberView() {
        super();
        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                new EDTRunner() {
                    @Override
                    protected void runInEDT() {
                        if (getContent() == null) {
                            setContent(new LinkGrabberPanel());
                        }
                    }
                };
            }

        });
    }

    @Override
    public Icon getIcon() {
        return new AbstractIcon(IconKey.ICON_LINKGRABBER, ICON_SIZE);
    }

    @Override
    public String getTitle() {
        return _GUI.T.jd_gui_swing_jdgui_views_linkgrabberview_tab_title();
    }

    @Override
    public String getTooltip() {
        return _GUI.T.jd_gui_swing_jdgui_views_linkgrabberview_tab_tooltip();
    }

    @Override
    protected void onHide() {
    }

    @Override
    protected void onShow() {
        if (this.getContent() == null) {
            setContent(new LinkGrabberPanel());
        }
    }

    @Override
    public String getID() {
        return "linkgrabberview2";
    }
}
