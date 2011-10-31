package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.packagecontroller.AbstractNode;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class EnabledAction extends AppAction {
    enum State {
        ALL_ENABLED,
        ALL_DISABLED,
        MIXED;
    }

    private ArrayList<AbstractNode> selection;

    public EnabledAction(ArrayList<AbstractNode> selection) {
        this.selection = selection;
        setName(_GUI._.EnabledAction_EnabledAction_object_());
        switch (getState(selection)) {
        case MIXED:
            setIconKey("checkbox_undefined");
            break;
        case ALL_DISABLED:
            setIconKey("disabled");
            break;
        case ALL_ENABLED:
            setIconKey("enabled");
            break;
        }

    }

    private State getState(ArrayList<AbstractNode> selection2) {
        if (selection2.size() == 0) return State.ALL_DISABLED;
        boolean first = selection2.get(0).isEnabled();
        for (AbstractNode a : selection2) {
            if (a.isEnabled() != first) { return State.MIXED; }
        }
        return first ? State.ALL_ENABLED : State.ALL_DISABLED;
    }

    public void actionPerformed(ActionEvent e) {
    }

}
