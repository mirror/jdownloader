package org.jdownloader.extensions.streaming.gui.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.packagecontroller.AbstractNode;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class RemoveIncompleteArchives extends AppAction {

    /**
     * 
     */
    private static final long serialVersionUID = 2816227528827363428L;

    public RemoveIncompleteArchives(java.util.List<AbstractNode> selection) {
        setName(_GUI._.RemoveIncompleteArchives_RemoveIncompleteArchives_object_());
        setIconKey("remove_packages");
    }

    public void actionPerformed(ActionEvent e) {
    }

}
