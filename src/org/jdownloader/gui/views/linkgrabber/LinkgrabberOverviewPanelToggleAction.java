package org.jdownloader.gui.views.linkgrabber;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.bottombar.ToggleAppAction;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class LinkgrabberOverviewPanelToggleAction extends ToggleAppAction {

    public LinkgrabberOverviewPanelToggleAction() {
        super(CFG_GUI.LINKGRABBER_TAB_OVERVIEW_VISIBLE, _GUI._.LinkgrabberOverviewPanelToggleAction_LinkgrabberOverviewPanelToggleAction(), null);
        setIconKey("bottombar");

    }

}
