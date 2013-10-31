package org.jdownloader.gui.views.linkgrabber.bottombar;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

public class AutoConfirmToggleAction extends ToggleAppAction {

    public AutoConfirmToggleAction() {
        super(org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.LINKGRABBER_AUTO_CONFIRM_ENABLED, _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_autoconfirm(), _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_autoconfirm_tt());
        setIconKey(IconKey.ICON_PARALELL);
    }

}
