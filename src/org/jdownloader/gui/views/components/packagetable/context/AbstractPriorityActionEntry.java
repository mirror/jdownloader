package org.jdownloader.gui.views.components.packagetable.context;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.TaskQueue;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.controlling.Priority;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;

public abstract class AbstractPriorityActionEntry<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends CustomizableTableContextAppAction<PackageType, ChildrenType> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private final Priority    priority;

    public AbstractPriorityActionEntry(Priority priority) {
        super();
        setName(priority._());
        setSmallIcon(priority.loadIcon(18));
        this.priority = priority;

    }

    public void actionPerformed(ActionEvent e) {
        if (getSelection().isEmpty()) {
            return;
        }
        final List<AbstractNode> finalSelection = new ArrayList<AbstractNode>(getSelection().getRawSelection());
        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                boolean linkGrabber = false;
                boolean downloadList = false;
                for (AbstractNode l : finalSelection) {
                    if (l instanceof CrawledLink) {
                        linkGrabber = true;
                        ((CrawledLink) l).setPriority(priority);
                    } else if (l instanceof DownloadLink) {
                        downloadList = true;
                        ((DownloadLink) l).setPriorityEnum(priority);
                    } else if (l instanceof CrawledPackage) {
                        linkGrabber = true;
                        ((CrawledPackage) l).setPriorityEnum(priority);
                    } else if (l instanceof FilePackage) {
                        downloadList = true;
                        ((FilePackage) l).setPriorityEnum(priority);
                    }
                }
                if (linkGrabber) {
                    LinkGrabberTableModel.getInstance().setPriorityColumnVisible(true);
                }
                if (downloadList) {
                    DownloadsTableModel.getInstance().setPriorityColumnVisible(true);
                }
                return null;
            }
        });
    }

}
