package org.jdownloader.gui.views.linkgrabber.sidebar.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class DropHosterAction extends AppAction {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private ArrayList<String> hosterList;

    public DropHosterAction(ArrayList<String> ret) {
        super();
        setName(_GUI._.DropHosterAction_DropHosterAction_());
        setIconKey("remove");
        hosterList = ret;
    }

    public void actionPerformed(ActionEvent e) {
    }

}
