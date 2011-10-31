package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.packagecontroller.AbstractNode;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class RemoveNonSelectedAction extends AppAction {

    private ArrayList<AbstractNode> selection;

    public RemoveNonSelectedAction(ArrayList<AbstractNode> selection) {
        this.selection = selection;
        setName(_GUI._.RemoveNonSelectedAction_RemoveNonSelectedAction_object_());
        setIconKey("ok");
    }

    public void actionPerformed(ActionEvent e) {
    }

}
