package org.jdownloader.gui.views.linkgrabber.sidebar.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class KeepOnlyAction extends AppAction {

    private ArrayList<String> hosterList;

    public KeepOnlyAction(ArrayList<String> ret) {

        super();
        this.hosterList = ret;
        setName(_GUI._.KeepOnlyAction_KeepOnlyAction_());
        setIconKey("filter");

    }

    public void actionPerformed(ActionEvent e) {
    }

}
