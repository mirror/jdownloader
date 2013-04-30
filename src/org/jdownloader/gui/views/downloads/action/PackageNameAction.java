package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import jd.gui.UserIO;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class PackageNameAction extends AppAction {

    private static final long                        serialVersionUID = -5155537516674035401L;
    private SelectionInfo<FilePackage, DownloadLink> si;

    public PackageNameAction(SelectionInfo<FilePackage, DownloadLink> si) {
        this.si = si;
        setName(_GUI._.gui_table_contextmenu_editpackagename());
        setIconKey("edit");
    }

    public void actionPerformed(ActionEvent e) {
        String name = UserIO.getInstance().requestInputDialog(0, _GUI._.gui_linklist_editpackagename_message(), si.getContextPackage().getName());
        if (name == null) return;

        for (FilePackage packagee : si.getAllPackages()) {
            packagee.setName(name);
        }
    }

}