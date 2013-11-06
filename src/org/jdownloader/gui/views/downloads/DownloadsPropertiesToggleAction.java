package org.jdownloader.gui.views.downloads;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.bottombar.ToggleAppAction;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class DownloadsPropertiesToggleAction extends ToggleAppAction {

    public DownloadsPropertiesToggleAction() {
        super(CFG_GUI.DOWNLOADS_TAB_PROPERTIES_PANEL_VISIBLE, _GUI._.LinkgrabberPropertiesToggleAction_LinkgrabberPropertiesToggleAction(), _GUI._.LinkgrabberPropertiesToggleAction_LinkgrabberPropertiesToggleAction());
        setIconKey("bottombar");

    }

}
