package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.packagecontroller.AbstractNode;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class RemoveOfflineAction extends AppAction {

    private ArrayList<AbstractNode> selection;

    public RemoveOfflineAction(ArrayList<AbstractNode> selection) {
        this.selection = selection;
        setName(_GUI._.RemoveOfflineAction_RemoveOfflineAction_object_());
        setIconKey("remove_offline");
    }

    public void actionPerformed(ActionEvent e) {
    }

}
