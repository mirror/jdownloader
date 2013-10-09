package org.jdownloader.gui.mainmenu;

import java.awt.event.ActionEvent;

import jd.gui.swing.jdgui.components.toolbar.actions.UpdateAction;

import org.jdownloader.actions.AppAction;
import org.jdownloader.actions.CachableInterface;
import org.jdownloader.gui.translate._GUI;

public class CheckForUpdatesAction extends AppAction implements CachableInterface {
    public CheckForUpdatesAction() {
        setIconKey("update");
        setName(_GUI._.CheckForUpdatesAction_CheckForUpdatesAction());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new UpdateAction(null).actionPerformed(e);
    }

    @Override
    public void setData(String data) {
    }

}
