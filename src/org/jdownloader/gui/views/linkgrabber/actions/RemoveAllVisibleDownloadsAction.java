package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;

import org.jdownloader.gui.translate._GUI;

public class RemoveAllVisibleDownloadsAction extends AbstractDeleteCrawledLinksAppAction {
    /**
     * 
     */
    private static final long serialVersionUID = 841782078416257540L;

    public RemoveAllVisibleDownloadsAction() {

        setName(_GUI._.RemoveAllAction_RemoveAllAction_object());
        setIconKey("clear");
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        deleteLinksRequest(getSelection(), _GUI._.RemoveSelectionAction_actionPerformed_());
    }

}
