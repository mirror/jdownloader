package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class DeleteSelectionAction extends AbstractDeleteSelectionFromDownloadlistAction {

    private static final long serialVersionUID = -5721724901676405104L;

    public DeleteSelectionAction(SelectionInfo<FilePackage, DownloadLink> si) {
        super(si);
        setIconKey("delete");
        setName(_GUI._.gui_table_contextmenu_deletelist2());
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        DownloadTabActionUtils.deleteLinksRequest(getSelection(), _GUI._.RemoveSelectionAction_actionPerformed_());
    }

}