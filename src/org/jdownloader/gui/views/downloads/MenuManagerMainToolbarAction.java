package org.jdownloader.gui.views.downloads;

import java.awt.event.ActionEvent;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.toolbar.MenuManagerMainToolbar;
import org.jdownloader.gui.translate._GUI;

public class MenuManagerMainToolbarAction extends AppAction {
    public MenuManagerMainToolbarAction() {
        setName(_GUI._.MenuManagerMainToolbarAction_MenuManagerMainToolbarAction());
        setIconKey("topbar");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MenuManagerMainToolbar.getInstance().openGui();
    }

}
