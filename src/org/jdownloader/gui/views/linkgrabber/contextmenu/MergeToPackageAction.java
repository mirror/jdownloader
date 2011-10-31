package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.packagecontroller.AbstractNode;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class MergeToPackageAction extends AppAction {

    private ArrayList<AbstractNode> selection;

    public MergeToPackageAction(ArrayList<AbstractNode> selection) {
        setName(_GUI._.MergeToPackageAction_MergeToPackageAction_());
        setIconKey("package_new");
        this.selection = selection;
    }

    public void actionPerformed(ActionEvent e) {
    }

}
