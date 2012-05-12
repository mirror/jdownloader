package org.jdownloader.gui.views.downloads.table;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.context.DeleteAction;

public class DeleteQuickAction extends DeleteAction {

    private static final long serialVersionUID = -6140757831031388156L;

    public DeleteQuickAction(SelectionInfo<FilePackage, DownloadLink> si) {
        super(si);
        setName(_GUI._.DeleteQuickAction_DeleteQuickAction_object_());

    }

}
