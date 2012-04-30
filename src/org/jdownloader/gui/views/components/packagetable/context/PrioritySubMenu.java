package org.jdownloader.gui.views.components.packagetable.context;

import java.util.ArrayList;

import javax.swing.JMenu;

import jd.controlling.packagecontroller.AbstractNode;

import org.jdownloader.controlling.Priority;
import org.jdownloader.gui.translate._GUI;

public class PrioritySubMenu extends JMenu {

    private ArrayList<AbstractNode> selection;

    public PrioritySubMenu(ArrayList<AbstractNode> selection) {
        super(_GUI._.PriorityAction_PriorityAction_());
        setIcon(Priority.HIGHER.loadIcon(18));
        this.selection = selection;

        // setIconKey("priority");
        this.add(new PriorityActionEntry(Priority.LOWER, selection));
        this.add(new PriorityActionEntry(Priority.DEFAULT, selection));
        this.add(new PriorityActionEntry(Priority.HIGH, selection));

        this.add(new PriorityActionEntry(Priority.HIGHER, selection));
        this.add(new PriorityActionEntry(Priority.HIGHEST, selection));

    }

}
