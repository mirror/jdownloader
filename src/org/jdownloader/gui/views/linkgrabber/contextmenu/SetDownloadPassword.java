package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.packagecontroller.AbstractNode;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class SetDownloadPassword extends AppAction {

    private ArrayList<AbstractNode> selection;

    public SetDownloadPassword(ArrayList<AbstractNode> selection) {
        this.selection = selection;
        setName(_GUI._.SetDownloadPassword_SetDownloadPassword_());
        setIconKey("password");
    }

    public void actionPerformed(ActionEvent e) {
    }

}
