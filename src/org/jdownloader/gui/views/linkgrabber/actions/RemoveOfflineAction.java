package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.gui.translate._GUI;

public class RemoveOfflineAction extends AbstractDeleteCrawledLinksAppAction {

    /**
     * 
     */
    private static final long serialVersionUID = -6341297356888158708L;

    public RemoveOfflineAction() {

        setName(_GUI._.RemoveOfflineAction_RemoveOfflineAction_object_());
        setIconKey("remove_offline");
    }

    public void actionPerformed(final ActionEvent e) {
        DownloadController.getInstance().getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                final List<CrawledLink> nodesToDelete = LinkCollector.getInstance().getChildrenByFilter(new AbstractPackageChildrenNodeFilter<CrawledLink>() {

                    @Override
                    public int returnMaxResults() {
                        return 0;
                    }

                    @Override
                    public boolean acceptNode(CrawledLink cl) {
                        DownloadLink dl = cl.getDownloadLink();
                        if (dl != null) {
                            AvailableStatus status = dl.getAvailableStatus();
                            if (status != null) {
                                if (status == DownloadLink.AvailableStatus.FALSE) { return true; }
                            }

                        }
                        return false;
                    }
                });
                // deleteLinksRequest(new SelectionInfo<CrawledPackage, CrawledLink>(null, nodesToDelete, null, null, e,
                // DownloadsTableModel.getInstance().getTable()), _GUI._.RemoveOfflineAction_actionPerformed());

                return null;
            }
        });
    }
}
