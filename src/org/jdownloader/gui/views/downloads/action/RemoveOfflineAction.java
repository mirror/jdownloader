package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.plugins.FinalLinkState;

public class RemoveOfflineAction extends AbstractDeleteSelectionFromDownloadlistAction {

    /**
     * 
     */
    private static final long serialVersionUID = -6341297356888158708L;

    public RemoveOfflineAction() {
        super(null);
        setName(_GUI._.RemoveOfflineAction_RemoveOfflineAction_object_());
        setIconKey("remove_offline");
    }

    public void actionPerformed(final ActionEvent e) {
        DownloadController.getInstance().getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                final List<DownloadLink> nodesToDelete = DownloadController.getInstance().getChildrenByFilter(new AbstractPackageChildrenNodeFilter<DownloadLink>() {

                    @Override
                    public int returnMaxResults() {
                        return 0;
                    }

                    @Override
                    public boolean acceptNode(DownloadLink node) {
                        if (node.getFinalLinkState() == FinalLinkState.OFFLINE) return true;
                        return false;
                    }
                });
                DownloadTabActionUtils.deleteLinksRequest(new SelectionInfo<FilePackage, DownloadLink>(null, nodesToDelete, null, null, e, DownloadsTableModel.getInstance().getTable()), _GUI._.RemoveOfflineAction_actionPerformed());

                return null;
            }
        });
    }
}
