package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class RemoveAllVisibleDownloadsAction extends DeleteAppAction {
    /**
     * 
     */
    private static final long serialVersionUID = 841782078416257540L;

    public RemoveAllVisibleDownloadsAction(SelectionInfo<FilePackage, DownloadLink> si) {
        super(si);
        setName(_GUI._.RemoveAllAction_RemoveAllAction_object());
        setIconKey("clear");
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        deleteLinksRequest(getSelection(), _GUI._.RemoveSelectionAction_actionPerformed_());
    }

}
