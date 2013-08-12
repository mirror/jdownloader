package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;

public class RemoveNonSelectedAction extends DeleteAppAction {

    /**
     * 
     */
    private static final long        serialVersionUID = 6855083561629297363L;
    private final List<AbstractNode> nodeSelection;

    public RemoveNonSelectedAction(java.util.List<AbstractNode> selection) {
        super(null);
        this.nodeSelection = selection;
        setName(_GUI._.RemoveNonSelectedAction_RemoveNonSelectedAction_object_());
        setIconKey("ok");
    }

    public void actionPerformed(final ActionEvent e) {
        if (!isEnabled()) return;
        final SelectionInfo<FilePackage, DownloadLink> selectionInfo = new SelectionInfo<FilePackage, DownloadLink>(null, nodeSelection, null, null, e, DownloadsTableModel.getInstance().getTable());
        DownloadController.getInstance().getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                final HashSet<DownloadLink> set = new HashSet<DownloadLink>();
                set.addAll(selectionInfo.getChildren());
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
                deleteLinksRequest(new SelectionInfo<FilePackage, DownloadLink>(null, nodesToDelete, null, null, e, DownloadsTableModel.getInstance().getTable()), _GUI._.RemoveNonSelectedAction_actionPerformed());
                return null;
            }
        });
    }

    @Override
    public boolean isEnabled() {
        return nodeSelection != null;
    }

}
