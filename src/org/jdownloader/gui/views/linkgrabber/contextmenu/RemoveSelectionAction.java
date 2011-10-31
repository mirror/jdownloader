package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.packagecontroller.AbstractNode;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class RemoveSelectionAction extends AppAction {

    public RemoveSelectionAction(ArrayList<AbstractNode> selection) {
        setIconKey("remove");
        setName(_GUI._.RemoveSelectionAction_RemoveSelectionAction_object_());
    }

    public void actionPerformed(ActionEvent e) {
    }

}
