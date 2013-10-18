package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import jd.gui.UserIO;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.actions.AbstractSelectionContextAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class PackageNameAction extends AbstractSelectionContextAction<FilePackage, DownloadLink> {

    private static final long serialVersionUID = -5155537516674035401L;

    public PackageNameAction(SelectionInfo<FilePackage, DownloadLink> si) {
        super(si);
        setName(_GUI._.gui_table_contextmenu_editpackagename());
        setIconKey("edit");
    }

    public void actionPerformed(ActionEvent e) {
        String name = UserIO.getInstance().requestInputDialog(0, _GUI._.gui_linklist_editpackagename_message(), getSelection().getContextPackage().getName());
        if (name == null) return;

        for (FilePackage packagee : getSelection().getAllPackages()) {
            packagee.setName(name);
        }
    }

}