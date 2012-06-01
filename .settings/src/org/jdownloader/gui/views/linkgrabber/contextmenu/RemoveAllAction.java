package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.actions.ClearAction;

public class RemoveAllAction extends AppAction {
    /**
     * 
     */
    private static final long serialVersionUID = 841782078416257540L;

    public RemoveAllAction() {
        setName(_GUI._.RemoveAllAction_RemoveAllAction_object_());
        setIconKey("clear");
    }

    public void actionPerformed(ActionEvent e) {
        new ClearAction().actionPerformed(e);
    }

}
