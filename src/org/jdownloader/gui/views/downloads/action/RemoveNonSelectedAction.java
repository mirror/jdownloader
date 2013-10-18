package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;

public class RemoveNonSelectedAction extends AbstractDeleteSelectionFromDownloadlistAction {

    /**
     * 
     */
    private static final long serialVersionUID = 6855083561629297363L;

    public RemoveNonSelectedAction(SelectionInfo<FilePackage, DownloadLink> si) {
        super(si);

        setName(_GUI._.RemoveNonSelectedAction_RemoveNonSelectedAction_object_());
        setIconKey("ok");
    }

    public void actionPerformed(final ActionEvent e) {
        if (!isEnabled()) return;
        DownloadController.getInstance().getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                final HashSet<DownloadLink> set = new HashSet<DownloadLink>();
                set.addAll(getSelection().getChildren());
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
                DownloadTabActionUtils.deleteLinksRequest(new SelectionInfo<FilePackage, DownloadLink>(null, nodesToDelete, null, null, e, DownloadsTableModel.getInstance().getTable()), _GUI._.RemoveNonSelectedAction_actionPerformed());
                return null;
            }
        });
    }

}
