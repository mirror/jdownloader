package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.HashSet;
import java.util.List;

import jd.controlling.IOEQ;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.OKCancelCloseUserIODefinition.CloseReason;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.overviewpanel.AggregatedNumbers;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;

public class RemoveNonSelectedAction extends AppAction {

    /**
     * 
     */
    private static final long            serialVersionUID = 6855083561629297363L;
    private java.util.List<AbstractNode> selection;
    private DownloadsTable               table;

    public RemoveNonSelectedAction(DownloadsTable table, java.util.List<AbstractNode> selection) {
        this.selection = selection;
        this.table = table;
        setName(_GUI._.RemoveNonSelectedAction_RemoveNonSelectedAction_object_());
        setIconKey("ok");
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;

        // LinkCollector.getInstance().getChildrenByFilter(filter)

        final SelectionInfo<FilePackage, DownloadLink> selectionInfo = new SelectionInfo<FilePackage, DownloadLink>(selection);
        final HashSet<DownloadLink> set = new HashSet<DownloadLink>();
        set.addAll(selectionInfo.getSelectedChildren());
        List<DownloadLink> nodesToDelete = DownloadController.getInstance().getChildrenByFilter(new AbstractPackageChildrenNodeFilter<DownloadLink>() {

            @Override
            public int returnMaxResults() {
                return 0;
            }

            @Override
            public boolean acceptNode(DownloadLink node) {
                return !set.contains(node);
            }
        });
        // DownloadsTableModel.getInstance().get

        AggregatedNumbers agg = new AggregatedNumbers(new SelectionInfo<FilePackage, DownloadLink>(nodesToDelete));

        final ConfirmDeleteLinksDialogInterface d = UIOManager.I().show(ConfirmDeleteLinksDialogInterface.class, new ConfirmDeleteLinksDialog(_GUI._.RemoveNonSelectedAction_actionPerformed(agg.getLinkCount(), DownloadController.getInstance().getChildrenCount() - agg.getLinkCount()), agg.getLoadedBytesString()));

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
