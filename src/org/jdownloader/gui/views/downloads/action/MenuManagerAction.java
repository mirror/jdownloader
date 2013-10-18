package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import org.jdownloader.actions.AppAction;
import org.jdownloader.actions.CachableInterface;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuManagerDownloadTableContext;

public class MenuManagerAction extends AppAction implements CachableInterface {

    public MenuManagerAction() {

        setName(_GUI._.MenuManagerAction_MenuManagerAction());
        setIconKey("menu");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MenuManagerDownloadTableContext.getInstance().openGui();

    }

    @Override
    public void setData(String data) {
    }

}
