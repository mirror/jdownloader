package org.jdownloader.gui.views.linkgrabber;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.bottombar.ToggleAppAction;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class LinkgrabberPropertiesToggleAction extends ToggleAppAction {

    public LinkgrabberPropertiesToggleAction() {
        super(CFG_GUI.LINKGRABBER_TAB_PROPERTIES_PANEL_VISIBLE, _GUI._.LinkgrabberPropertiesToggleAction_LinkgrabberPropertiesToggleAction(), _GUI._.LinkgrabberPropertiesToggleAction_LinkgrabberPropertiesToggleAction());
        setIconKey("bottombar");

    }

}
