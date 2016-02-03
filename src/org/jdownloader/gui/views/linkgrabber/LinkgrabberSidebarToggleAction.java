package org.jdownloader.gui.views.linkgrabber;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.bottombar.ToggleAppAction;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class LinkgrabberSidebarToggleAction extends ToggleAppAction {

    public LinkgrabberSidebarToggleAction() {
        super(CFG_GUI.LINKGRABBER_SIDEBAR_VISIBLE, _GUI._.LinkgrabberSidebarToggleAction_LinkgrabberSidebarToggleAction(), _GUI._.LinkGrabberPanel_LinkGrabberPanel_btn_showsidebar_tt_up());

        putValue(SMALL_ICON, new AbstractIcon(IconKey.ICON_SIDEBAR, -1));

    }

}
