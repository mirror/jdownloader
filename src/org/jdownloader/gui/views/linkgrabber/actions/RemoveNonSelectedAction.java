package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;

import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.gui.translate._GUI;

public class RemoveNonSelectedAction extends AbstractDeleteCrawledLinksAppAction {

    /**
     * 
     */
    private static final long serialVersionUID = 6855083561629297363L;

    public RemoveNonSelectedAction() {

        setName(_GUI._.RemoveNonSelectedAction_RemoveNonSelectedAction_object_());
        setIconKey("ok");
    }

    public void actionPerformed(final ActionEvent e) {
        if (!isEnabled()) return;
        DownloadController.getInstance().getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                final HashSet<CrawledLink> set = new HashSet<CrawledLink>();
                set.addAll(getSelection().getChildren());
                List<CrawledLink> nodesToDelete = LinkCollector.getInstance().getChildrenByFilter(new AbstractPackageChildrenNodeFilter<CrawledLink>() {

                    @Override
                    public int returnMaxResults() {
                        return 0;
                    }

                    @Override
                    public boolean acceptNode(CrawledLink node) {
                        return !set.contains(node);
                    }
                });
                // deleteLinksRequest(new SelectionInfo<CrawledPackage, CrawledLink>(null, nodesToDelete, null, null, e,
                // DownloadsTableModel.getInstance().getTable()), _GUI._.RemoveNonSelectedAction_actionPerformed());
                return null;
            }
        });
    }

}
