package org.jdownloader.gui.views.downloads.table;

import java.awt.event.ActionEvent;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class DeleteOfflineFromListAndDiskAction extends AppAction {

    private SelectionInfo<FilePackage, DownloadLink> si;

    public DeleteOfflineFromListAndDiskAction(SelectionInfo<FilePackage, DownloadLink> si) {

        this.si = si;
        setName(_GUI._.DeleteOfflineAction_DeleteOfflineAction_object_());
        setIconKey("remove_offline");
    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }

}
