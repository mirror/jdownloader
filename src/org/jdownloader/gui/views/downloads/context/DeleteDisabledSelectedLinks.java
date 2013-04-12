package org.jdownloader.gui.views.downloads.context;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class DeleteDisabledSelectedLinks extends AppAction {

    private SelectionInfo<FilePackage, DownloadLink> si;

    public DeleteDisabledSelectedLinks(SelectionInfo<FilePackage, DownloadLink> si) {

        this.si = si;
        setName(_GUI._.DeleteDisabledLinks_DeleteDisabledLinks_object_());
        setIconKey("remove_disabled");

    }

    @Override
    public void actionPerformed(ActionEvent e) {

        List<DownloadLink> nodesToDelete = new ArrayList<DownloadLink>();
        for (DownloadLink dl : si.getChildren()) {
            if (!dl.isEnabled()) {
                nodesToDelete.add(dl);
            }
        }
        DownloadController.deleteLinksRequest(new SelectionInfo<FilePackage, DownloadLink>(null, nodesToDelete), _GUI._.DeleteDisabledLinksFromListAndDiskAction_actionPerformed_object_());
    }

}
