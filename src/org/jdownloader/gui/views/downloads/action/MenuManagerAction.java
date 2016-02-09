package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuManagerDownloadTableContext;

public class MenuManagerAction extends CustomizableTableContextAppAction {

    public MenuManagerAction() {
        super(true, true);
        setName(_GUI.T.MenuManagerAction_MenuManagerAction());
        setIconKey(IconKey.ICON_MENU);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MenuManagerDownloadTableContext.getInstance().openGui();

    }

}
