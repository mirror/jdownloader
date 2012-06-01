package org.jdownloader.extensions;

import java.awt.event.ActionEvent;

public class ExtensionGuiTabToggleAction extends ExtensionGuiEnableAction {

    private static final long serialVersionUID = 6997360773808826159L;

    public ExtensionGuiTabToggleAction(AbstractExtension<?, ?> plg) {
        super(plg);
        putValue(SELECTED_KEY, plg.getGUI().isActive());
    }

    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);

        plg.getGUI().setActive(!plg.getGUI().isActive());
    }
}
