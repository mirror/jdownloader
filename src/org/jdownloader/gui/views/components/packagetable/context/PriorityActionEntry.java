package org.jdownloader.gui.views.components.packagetable.context;

import java.awt.event.ActionEvent;

import jd.controlling.IOEQ;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.plugins.DownloadLink;

import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.Priority;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;

public class PriorityActionEntry<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends AppAction {

    /**
     * 
     */
    private static final long                        serialVersionUID = 1L;
    private Priority                                 priority;
    private SelectionInfo<PackageType, ChildrenType> si;

    public PriorityActionEntry(Priority priority, SelectionInfo<PackageType, ChildrenType> si) {
        setName(priority._());
        setSmallIcon(priority.loadIcon(18));
        this.priority = priority;
        this.si = si;
    }

    public void actionPerformed(ActionEvent e) {
        if (si.isEmpty()) return;
        IOEQ.add(new Runnable() {

            @Override
            public void run() {

                boolean linkGrabber = false;
                boolean downloadList = false;
                for (AbstractNode l : si.getChildren()) {
                    if (l instanceof CrawledLink) {
                        linkGrabber = true;
                        ((CrawledLink) l).setPriority(priority);
                    } else if (l instanceof DownloadLink) {
                        downloadList = true;
                        ((DownloadLink) l).setPriority(priority.getId());
                    }
                }
                if (linkGrabber) LinkGrabberTableModel.getInstance().setPriorityColumnVisible(true);
                if (downloadList) DownloadsTableModel.getInstance().setPriorityColumnVisible(true);
            }
        }, true);

    }

}
