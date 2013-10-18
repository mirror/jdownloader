package org.jdownloader.gui.views.linkgrabber.bottombar;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

public class AutoStartToggleAction extends ToggleAppAction {

    public AutoStartToggleAction() {
        super(org.jdownloader.settings.staticreferences.CFG_LINKFILTER.LINKGRABBER_AUTO_START_ENABLED, _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_autostart(), _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_autostart_tt());
        setIconKey(IconKey.ICON_MEDIA_PLAYBACK_START);
    }

}
