package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuManagerDownloadTableContext;

public class MenuManagerAction extends CustomizableTableContextAppAction {

    public MenuManagerAction() {
        super(true, true);
        setName(_GUI._.MenuManagerAction_MenuManagerAction());
        setIconKey("menu");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MenuManagerDownloadTableContext.getInstance().openGui();

    }

}
