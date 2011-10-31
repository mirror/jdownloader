package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.packagecontroller.AbstractNode;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class RemoveIncompleteArchives extends AppAction {

    public RemoveIncompleteArchives(ArrayList<AbstractNode> selection) {
        setName(_GUI._.RemoveIncompleteArchives_RemoveIncompleteArchives_object_());
        setIconKey("remove_packages");
    }

    public void actionPerformed(ActionEvent e) {
    }

}
