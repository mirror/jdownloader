package org.jdownloader.gui.views.components.packagetable.context;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;

import jd.controlling.IOEQ;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.appwork.utils.ImageProvider.ImageProvider;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.images.NewTheme;

public class EnabledAction<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends AppAction {
    /**
     * 
     */
    private static final long serialVersionUID = -1733286276459073749L;

    enum State {
        ALL_ENABLED,
        ALL_DISABLED,
        MIXED;
    }

    private State                                    state = State.MIXED;

    private SelectionInfo<PackageType, ChildrenType> selection;

    public EnabledAction(SelectionInfo<PackageType, ChildrenType> si) {
        if (si != null) {
            this.selection = si;
            switch (state = getState()) {
            case MIXED:
                setSmallIcon(new ImageIcon(ImageProvider.merge(NewTheme.I().getImage("select", 18), NewTheme.I().getImage("checkbox_undefined", 14), 0, 0, 9, 8)));
                setName(_GUI._.EnabledAction_EnabledAction_disable());
                break;
            case ALL_DISABLED:
                setSmallIcon(new ImageIcon(ImageProvider.merge(NewTheme.I().getImage("select", 18), NewTheme.I().getImage("disabled", 14), 0, 0, 9, 8)));
                setName(_GUI._.EnabledAction_EnabledAction_enable());
                break;
            case ALL_ENABLED:
                setSmallIcon(new ImageIcon(ImageProvider.merge(NewTheme.I().getImage("select", 18), NewTheme.I().getImage("enabled", 14), 0, 0, 9, 8)));
                setName(_GUI._.EnabledAction_EnabledAction_disable());
                break;
            }
        } else {
            setSmallIcon(new ImageIcon(ImageProvider.merge(NewTheme.I().getImage("select", 18), NewTheme.I().getImage("disabled", 14), 0, 0, 9, 8)));
            setName(_GUI._.EnabledAction_EnabledAction_empty());
        }

    }

    private State getState() {
        if (selection.isEmpty()) return State.ALL_DISABLED;
        Boolean first = null;
        for (ChildrenType a : selection.getChildren()) {

            /* check a child */
            if (first == null) first = a.isEnabled();
            if (a.isEnabled() != first) { return State.MIXED; }

        }
        return first ? State.ALL_ENABLED : State.ALL_DISABLED;
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        IOEQ.add(new Runnable() {

            public void run() {
                boolean enable = state.equals(State.ALL_DISABLED);
                for (ChildrenType a : selection.getChildren()) {

                    a.setEnabled(enable);

                }
            }
        }, true);
    }

    @Override
    public boolean isEnabled() {
        return !selection.isEmpty();
    }

}
