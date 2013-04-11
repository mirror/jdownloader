package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import jd.controlling.IOEQ;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;

import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.OKCancelCloseUserIODefinition.CloseReason;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.overviewpanel.AggregatedNumbers;

public class RemoveOfflineAction extends AppAction {

    /**
     * 
     */
    private static final long serialVersionUID = -6341297356888158708L;

    public RemoveOfflineAction() {
        setName(_GUI._.RemoveOfflineAction_RemoveOfflineAction_object_());
        setIconKey("remove_offline");
    }

    public void actionPerformed(ActionEvent e) {

        final List<DownloadLink> nodesToDelete = DownloadController.getInstance().getChildrenByFilter(new AbstractPackageChildrenNodeFilter<DownloadLink>() {

            @Override
            public int returnMaxResults() {
                return 0;
            }

            @Override
            public boolean acceptNode(DownloadLink node) {
                if (node.getLinkStatus().hasStatus(LinkStatus.ERROR_FILE_NOT_FOUND)) return true;
                return node.getAvailableStatus() == AvailableStatus.FALSE;
            }
        });
        // DownloadsTableModel.getInstance().get

        AggregatedNumbers agg = new AggregatedNumbers(new SelectionInfo<FilePackage, DownloadLink>(nodesToDelete));

        final ConfirmDeleteLinksDialogInterface d = UIOManager.I().show(ConfirmDeleteLinksDialogInterface.class, new ConfirmDeleteLinksDialog(_GUI._.RemoveOfflineAction_actionPerformed(agg.getLinkCount(), DownloadController.getInstance().getChildrenCount() - agg.getLinkCount()), agg.getLoadedBytesString()));

        if (d.getCloseReason() == CloseReason.OK) {
            IOEQ.add(new Runnable() {

                public void run() {

                    DownloadController.getInstance().removeChildren(nodesToDelete);

                    if (d.isDeleteFilesFromDiskEnabled()) {
                        for (DownloadLink dl : nodesToDelete) {
                            if (dl.getLinkStatus().isFinished()) {
                                new File(dl.getFileOutput()).delete();
                            }
                        }
                    }
                }

            }, true);
        }

        // try {
        // Dialog.getInstance().showConfirmDialog(0, _GUI._.literally_are_you_sure(), _GUI._.ClearAction_actionPerformed_offline_msg(),
        // null, _GUI._.literally_yes(), _GUI._.literall_no());
        // IOEQ.add(new Runnable() {
        //
        // public void run() {
        // List<CrawledLink> offline =
        // LinkCollector.getInstance().getChildrenByFilter(new
        // AbstractPackageChildrenNodeFilter<CrawledLink>() {
        //
        // public boolean isChildrenNodeFiltered(CrawledLink node) {
        // return LinkState.OFFLINE.equals(node.getLinkState());
        // }
        //
        // public int returnMaxResults() {
        // return -1;
        // }
        //
        // });
        // LinkCollector.getInstance().removeChildren(offline);
        // }
        //
        // }, true);
        // } catch (DialogNoAnswerException e1) {
        // }
    }

}
