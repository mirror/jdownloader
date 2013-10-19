package org.jdownloader.gui.views.downloads.bottombar;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.bottombar.ToggleAppAction;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class DownloadsOverviewPanelToggleAction extends ToggleAppAction {
    public DownloadsOverviewPanelToggleAction() {
        super(CFG_GUI.DOWNLOAD_TAB_OVERVIEW_VISIBLE, _GUI._.DownloadsOverviewPanelToggleAction_DownloadsOverviewPanelToggleAction(), _GUI._.DownloadsOverviewPanelToggleAction_DownloadsOverviewPanelToggleAction());
        setIconKey("bottombar");

    }

}
