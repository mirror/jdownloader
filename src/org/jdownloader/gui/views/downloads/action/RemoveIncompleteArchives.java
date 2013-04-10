package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

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
        setIconKey("package_open_error");
    }

    public void actionPerformed(ActionEvent e) {
    }

}
