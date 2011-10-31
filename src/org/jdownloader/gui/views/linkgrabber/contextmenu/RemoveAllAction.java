package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class RemoveAllAction extends AppAction {
    public RemoveAllAction() {
        setName(_GUI._.RemoveAllAction_RemoveAllAction_object_());
        setIconKey("clear");
    }

    public void actionPerformed(ActionEvent e) {
    }

}
