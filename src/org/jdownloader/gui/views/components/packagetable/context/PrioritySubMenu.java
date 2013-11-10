package org.jdownloader.gui.views.components.packagetable.context;

import javax.swing.JMenu;

import org.jdownloader.controlling.Priority;
import org.jdownloader.gui.translate._GUI;

public class PrioritySubMenu extends JMenu {

    public PrioritySubMenu() {
        super(_GUI._.PriorityAction_PriorityAction_());
        setIcon(Priority.HIGHER.loadIcon(18));

        // setIconKey("priority");
        this.add(new PriorityLowerAction());
        this.add(new PriorityDefaultAction());
        this.add(new PriorityHighAction());

        this.add(new PriorityHigherAction());
        this.add(new PriorityHighestAction());

    }

}
