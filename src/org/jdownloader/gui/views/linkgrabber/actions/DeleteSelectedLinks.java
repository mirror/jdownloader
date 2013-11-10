package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;

import org.jdownloader.gui.translate._GUI;

public class DeleteSelectedLinks extends AbstractDeleteCrawledLinksAppAction {

    public DeleteSelectedLinks() {

        setName(_GUI._.DeleteAllAction_DeleteAllAction_object_());
        setIconKey("remove");
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        deleteLinksRequest(getSelection(), _GUI._.RemoveSelectionAction_actionPerformed_());
    }
}
