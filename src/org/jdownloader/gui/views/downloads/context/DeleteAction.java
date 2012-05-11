package org.jdownloader.gui.views.downloads.context;

import java.awt.event.ActionEvent;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class DeleteAction extends AppAction {

    private static final long                        serialVersionUID = -5721724901676405104L;

    private SelectionInfo<FilePackage, DownloadLink> si;

    public DeleteAction(SelectionInfo<FilePackage, DownloadLink> si) {

        this.si = si;
        setIconKey("delete");
        setName(_GUI._.gui_table_contextmenu_deletelist2());

    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;

    }

    @Override
    public boolean isEnabled() {

        return !si.isEmpty();
    }

}