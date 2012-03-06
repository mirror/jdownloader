package org.jdownloader.gui.views.components.packagetable.context;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.IOEQ;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class EnabledAction extends AppAction {
    /**
     * 
     */
    private static final long serialVersionUID = -1733286276459073749L;

    enum State {
        ALL_ENABLED,
        ALL_DISABLED,
        MIXED;
    }

    private ArrayList<AbstractNode> selection;
    private State                   state = State.MIXED;

    public EnabledAction(ArrayList<AbstractNode> selection) {
        this.selection = selection;
        setName(_GUI._.EnabledAction_EnabledAction_object_());
        switch (state = getState(selection)) {
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
        if (selection2 == null || selection2.size() == 0) return State.ALL_DISABLED;
        Boolean first = null;
        for (AbstractNode a : selection2) {
            if (a instanceof AbstractPackageNode) {
                /* check children of this package */
                synchronized (a) {
                    @SuppressWarnings("unchecked")
                    AbstractPackageNode<AbstractPackageChildrenNode<?>, ?> pkg = (AbstractPackageNode<AbstractPackageChildrenNode<?>, ?>) a;
                    List<AbstractPackageChildrenNode<?>> children = pkg.getChildren();
                    for (AbstractPackageChildrenNode<?> child : children) {
                        if (first == null) first = child.isEnabled();
                        if (child.isEnabled() != first) { return State.MIXED; }
                    }
                }
            } else {
                /* check a child */
                if (first == null) first = a.isEnabled();
                if (a.isEnabled() != first) { return State.MIXED; }
            }
        }
        return first ? State.ALL_ENABLED : State.ALL_DISABLED;
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        IOEQ.add(new Runnable() {

            public void run() {
                boolean enable = state.equals(State.ALL_DISABLED);
                for (AbstractNode a : selection) {
                    if (a instanceof AbstractPackageNode) {
                        List<AbstractPackageChildrenNode<?>> children = null;
                        synchronized (a) {
                            @SuppressWarnings("unchecked")
                            AbstractPackageNode<AbstractPackageChildrenNode<?>, ?> pkg = (AbstractPackageNode<AbstractPackageChildrenNode<?>, ?>) a;
                            children = new ArrayList<AbstractPackageChildrenNode<?>>(pkg.getChildren());
                        }
                        for (AbstractPackageChildrenNode<?> child : children) {
                            child.setEnabled(enable);
                        }
                    } else {
                        a.setEnabled(enable);
                    }
                }
            }
        }, true);
    }

    @Override
    public boolean isEnabled() {
        return selection != null && selection.size() > 0;
    }

}
