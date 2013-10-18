package org.jdownloader.gui.views.linkgrabber.bottombar;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

public class LinkFilterToggleAction extends ToggleAppAction {

    public LinkFilterToggleAction() {
        super(org.jdownloader.settings.staticreferences.CFG_LINKFILTER.LINK_FILTER_ENABLED, _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_globfilter(), _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_globfilter_tt());
        setIconKey(IconKey.ICON_FILTER);
    }

}
