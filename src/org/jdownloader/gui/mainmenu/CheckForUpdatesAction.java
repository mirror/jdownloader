package org.jdownloader.gui.mainmenu;

import java.awt.event.ActionEvent;

import jd.gui.swing.jdgui.components.toolbar.actions.UpdateAction;

import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.gui.translate._GUI;

public class CheckForUpdatesAction extends CustomizableAppAction {
    public CheckForUpdatesAction() {
        setIconKey("update");
        setName(_GUI._.CheckForUpdatesAction_CheckForUpdatesAction());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new UpdateAction().actionPerformed(e);
    }

}
