package org.jdownloader.extensions.jdtrayicon;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.jdownloader.actions.AppAction;

public class TrayAction extends AppAction {

    private AbstractAction action;

    public TrayAction(AppAction action) {
        this(action, action.getName());

    }

    public TrayAction(AppAction action, String name) {
        super();
        setIconKey(action.getIconKey());
        setIconSizes(24);
        setName(name);
        if (action.isToggle()) {
            setSelected(action.isSelected());
        }
        this.action = action;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        action.actionPerformed(e);
    }

}
