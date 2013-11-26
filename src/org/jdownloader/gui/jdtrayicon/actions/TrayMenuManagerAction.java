package org.jdownloader.gui.jdtrayicon.actions;

import java.awt.event.ActionEvent;

import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.gui.jdtrayicon.MenuManagerTrayIcon;
import org.jdownloader.gui.translate._GUI;

public class TrayMenuManagerAction extends CustomizableAppAction {

    public TrayMenuManagerAction() {

        setName(_GUI._.MenuManagerAction_MenuManagerAction());
        setIconKey("menu");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MenuManagerTrayIcon.getInstance().openGui();

    }

}
