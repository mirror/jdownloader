package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;
import java.io.File;

import jd.controlling.IOEQ;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.OKCancelCloseUserIODefinition.CloseReason;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.overviewpanel.AggregatedNumbers;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;

public class RemoveSelectionAction extends AppAction {

    /**
     * 
     */
    private static final long            serialVersionUID = -3008851305036758872L;
    private java.util.List<AbstractNode> selection;
    private DownloadsTable               table;

    public RemoveSelectionAction(DownloadsTable table, java.util.List<AbstractNode> selection) {
        setIconKey("remove");
        setName(_GUI._.RemoveSelectionAction_RemoveSelectionAction_object());
        this.selection = selection;
        this.table = table;
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        final SelectionInfo<FilePackage, DownloadLink> selectionInfo = new SelectionInfo<FilePackage, DownloadLink>(selection);
        AggregatedNumbers agg = new AggregatedNumbers(selectionInfo);
        final ConfirmDeleteLinksDialogInterface d = UIOManager.I().show(ConfirmDeleteLinksDialogInterface.class, new ConfirmDeleteLinksDialog(_GUI._.RemoveSelectionAction_actionPerformed_(agg.getLinkCount(), DownloadController.getInstance().getChildrenCount() - agg.getLinkCount()), agg.getLoadedBytesString()));

        if (d.getCloseReason() == CloseReason.OK) {
            IOEQ.add(new Runnable() {

                public void run() {

                    DownloadController.getInstance().removeChildren(selectionInfo.getSelectedChildren());

                    if (d.isDeleteFilesFromDiskEnabled()) {
                        for (DownloadLink dl : selectionInfo.getSelectedChildren()) {
                            if (dl.getLinkStatus().isFinished()) {
                                new File(dl.getFileOutput()).delete();
                            }
                        }
                    }
                }

            }, true);
        }
    }

    @Override
    public boolean isEnabled() {
        return selection != null && selection.size() > 0;
    }

}
