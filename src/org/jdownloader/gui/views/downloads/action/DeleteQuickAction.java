package org.jdownloader.gui.views.downloads.action;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class DeleteQuickAction extends DeleteSelectionAction {

    private static final long serialVersionUID = -6140757831031388156L;

    public DeleteQuickAction(SelectionInfo<FilePackage, DownloadLink> si) {
        super(si);
        setName(_GUI._.DeleteQuickAction_DeleteQuickAction_object_());
    }

}
