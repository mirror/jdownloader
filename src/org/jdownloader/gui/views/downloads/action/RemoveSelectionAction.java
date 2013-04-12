package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;

public class RemoveSelectionAction extends AppAction {

    /**
     * 
     */
    private static final long            serialVersionUID = -3008851305036758872L;
    private java.util.List<AbstractNode> selection;
    private DownloadsTable               table;

    public RemoveSelectionAction(java.util.List<AbstractNode> selection) {
        setIconKey("remove");
        setName(_GUI._.RemoveSelectionAction_RemoveSelectionAction_object());
        this.selection = selection;

    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        final SelectionInfo<FilePackage, DownloadLink> selectionInfo = new SelectionInfo<FilePackage, DownloadLink>(selection);

        DownloadController.deleteLinksRequest(selectionInfo, _GUI._.RemoveSelectionAction_actionPerformed_());
    }

    @Override
    public boolean isEnabled() {
        return selection != null && selection.size() > 0;
    }

}
