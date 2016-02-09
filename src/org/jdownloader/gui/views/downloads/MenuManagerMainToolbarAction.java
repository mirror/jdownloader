package org.jdownloader.gui.views.downloads;

import java.awt.event.ActionEvent;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.toolbar.MenuManagerMainToolbar;
import org.jdownloader.gui.translate._GUI;

public class MenuManagerMainToolbarAction extends AppAction {
    public MenuManagerMainToolbarAction() {
        setName(_GUI.T.MenuManagerMainToolbarAction_MenuManagerMainToolbarAction());
        setIconKey(IconKey.ICON_TOPBAR);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MenuManagerMainToolbar.getInstance().openGui();
    }

}
