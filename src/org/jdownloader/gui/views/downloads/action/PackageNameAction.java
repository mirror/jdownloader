package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import jd.gui.UserIO;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo.PackageView;

public class PackageNameAction extends CustomizableTableContextAppAction<FilePackage, DownloadLink> {

    private static final long serialVersionUID = -5155537516674035401L;

    public PackageNameAction() {

        setName(_GUI._.gui_table_contextmenu_editpackagename());
        setIconKey("edit");
    }

    public void actionPerformed(ActionEvent e) {
        String name = UserIO.getInstance().requestInputDialog(0, _GUI._.gui_linklist_editpackagename_message(), getSelection().getContextPackage().getName());
        if (name == null) return;

        for (PackageView<FilePackage, DownloadLink> packagee : getSelection().getPackageViews()) {
            packagee.getPackage().setName(name);
        }
    }

}