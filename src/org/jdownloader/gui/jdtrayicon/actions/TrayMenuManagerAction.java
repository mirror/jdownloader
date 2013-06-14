package org.jdownloader.gui.jdtrayicon.actions;

import java.awt.event.ActionEvent;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.jdtrayicon.TrayIconMenuManager;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class TrayMenuManagerAction extends AppAction {

    private SelectionInfo<FilePackage, DownloadLink> si;

    public TrayMenuManagerAction(SelectionInfo<FilePackage, DownloadLink> si) {

        this.si = si;
        setName(_GUI._.MenuManagerAction_MenuManagerAction());
        setIconKey("menu");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        TrayIconMenuManager.getInstance().openGui();

    }

}
