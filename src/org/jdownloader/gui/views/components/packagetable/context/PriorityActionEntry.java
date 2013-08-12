package org.jdownloader.gui.views.components.packagetable.context;

import java.awt.event.ActionEvent;

import jd.controlling.TaskQueue;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.plugins.DownloadLink;

import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.actions.SelectionAppAction;
import org.jdownloader.controlling.Priority;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;

public class PriorityActionEntry<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends SelectionAppAction<PackageType, ChildrenType> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private Priority          priority;

    public PriorityActionEntry(Priority priority, SelectionInfo<PackageType, ChildrenType> si) {
        super(si);
        setName(priority._());
        setSmallIcon(priority.loadIcon(18));
        this.priority = priority;

    }

    public void actionPerformed(ActionEvent e) {
        if (getSelection().isEmpty()) return;
        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                boolean linkGrabber = false;
                boolean downloadList = false;
                for (AbstractNode l : getSelection().getChildren()) {
                    if (l instanceof CrawledLink) {
                        linkGrabber = true;
                        ((CrawledLink) l).setPriority(priority);
                    } else if (l instanceof DownloadLink) {
                        downloadList = true;
                        ((DownloadLink) l).setPriorityEnum(priority);
                    }
                }
                if (linkGrabber) LinkGrabberTableModel.getInstance().setPriorityColumnVisible(true);
                if (downloadList) DownloadsTableModel.getInstance().setPriorityColumnVisible(true);
                return null;
            }
        });
    }

}
