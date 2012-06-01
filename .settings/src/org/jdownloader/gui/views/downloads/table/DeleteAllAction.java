package org.jdownloader.gui.views.downloads.table;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.context.DeleteAction;

public class DeleteAllAction extends DeleteAction {

    private static final long serialVersionUID = 4828124112878883568L;

    public DeleteAllAction(SelectionInfo<FilePackage, DownloadLink> si) {
        super(si);
        setName(_GUI._.DeleteAllAction_DeleteAllAction_object_());
        setIconKey("remove");
    }

}
