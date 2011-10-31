package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;

import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.swing.exttable.ExtColumn;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class SortAction extends AppAction {

    public SortAction(AbstractNode contextObject, ExtColumn<AbstractNode> column) {
        setIconKey("sort");
        setName(_GUI._.SortAction_SortAction_object_(column.getName()));
    }

    public void actionPerformed(ActionEvent e) {
    }

}
