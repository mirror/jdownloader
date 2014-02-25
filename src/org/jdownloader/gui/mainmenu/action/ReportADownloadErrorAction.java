package org.jdownloader.gui.mainmenu.action;

import java.awt.event.ActionEvent;

import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.vote.VoteWindowController;

public class ReportADownloadErrorAction extends CustomizableAppAction {

    public ReportADownloadErrorAction() {
        setName(_GUI._.ReportADownloadErrorAction_ReportADownloadErrorAction_label());
        setIconKey("download");
        setTooltipText(_GUI._.ReportADownloadErrorAction_ReportADownloadErrorAction_tooltip());

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        VoteWindowController.getInstance().show();

    }

}
