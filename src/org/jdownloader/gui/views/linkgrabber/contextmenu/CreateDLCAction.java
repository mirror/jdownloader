package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.packagecontroller.AbstractNode;

import org.jdownloader.actions.AppAction;
import org.jdownloader.dlc.DLCFactory;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;

public class CreateDLCAction extends AppAction {

    private ArrayList<AbstractNode> selection;

    public CreateDLCAction(ArrayList<AbstractNode> selection) {
        setName(_GUI._.gui_table_contextmenu_dlc());
        setIconKey("dlc");
        this.selection = selection;
    }

    public void actionPerformed(ActionEvent e) {

        new DLCFactory().createDLCByCrawledLinks(LinkTreeUtils.getChildren(selection));
    }

}
