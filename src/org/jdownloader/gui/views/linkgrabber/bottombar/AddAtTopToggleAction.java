package org.jdownloader.gui.views.linkgrabber.bottombar;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.staticreferences.CFG_LINKFILTER;

public class AddAtTopToggleAction extends ToggleAppAction {

    public AddAtTopToggleAction() {
        super(CFG_LINKFILTER.LINKGRABBER_ADD_AT_TOP, _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_addtop(), _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_addtop_tt());
        setIconKey("go-top");
    }

}
