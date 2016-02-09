package org.jdownloader.gui.views.linkgrabber.bottombar;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.staticreferences.CFG_LINKGRABBER;

public class AddAtTopToggleAction extends ToggleAppAction {

    public AddAtTopToggleAction() {
        super(CFG_LINKGRABBER.LINKGRABBER_ADD_AT_TOP, _GUI.T.LinkGrabberSidebar_LinkGrabberSidebar_addtop(), _GUI.T.LinkGrabberSidebar_LinkGrabberSidebar_addtop_tt());
        setIconKey(IconKey.ICON_GO_TOP);
    }

}
