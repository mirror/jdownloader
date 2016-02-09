package org.jdownloader.gui.views.linkgrabber.bottombar;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

public class AutoStartToggleAction extends ToggleAppAction {

    public AutoStartToggleAction() {
        super(org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.LINKGRABBER_AUTO_START_ENABLED, _GUI.T.LinkGrabberSidebar_LinkGrabberSidebar_autostart(), _GUI.T.LinkGrabberSidebar_LinkGrabberSidebar_autostart_tt());
        setIconKey(IconKey.ICON_MEDIA_PLAYBACK_START);
    }

}
