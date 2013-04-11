package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class RemoveAllVisibleDownloadsAction extends AppAction {
    /**
     * 
     */
    private static final long serialVersionUID = 841782078416257540L;

    public RemoveAllVisibleDownloadsAction() {
        setName(_GUI._.RemoveAllAction_RemoveAllAction_object());
        setIconKey("clear");
    }

    public void actionPerformed(ActionEvent e) {
        new ClearDownloadListAction().actionPerformed(e);
    }

}
