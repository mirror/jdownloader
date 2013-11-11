package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.exceptions.WTFException;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.gui.translate._GUI;

public class NewPackageAction extends CustomizableTableContextAppAction<FilePackage, DownloadLink> {

    private static final long serialVersionUID = -8544759375428602013L;

    public NewPackageAction() {
        super();
        setIconKey("package_new");
        setName(_GUI._.gui_table_contextmenu_newpackage());
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        throw new WTFException("fixme 22023");

    }

}