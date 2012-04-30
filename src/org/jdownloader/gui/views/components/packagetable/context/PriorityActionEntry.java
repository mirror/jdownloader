package org.jdownloader.gui.views.components.packagetable.context;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.IOEQ;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;

import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.Priority;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;

public class PriorityActionEntry extends AppAction {

    /**
     * 
     */
    private static final long       serialVersionUID = 1L;
    private Priority                priority;
    private ArrayList<AbstractNode> orgSelection;

    public PriorityActionEntry(Priority priority, ArrayList<AbstractNode> selection) {
        setName(priority._());
        setSmallIcon(priority.loadIcon(18));
        this.priority = priority;
        this.orgSelection = selection;
    }

    public void actionPerformed(ActionEvent e) {
        if (orgSelection == null) return;
        IOEQ.add(new Runnable() {

            @Override
            public void run() {
                ArrayList<AbstractNode> selection = LinkTreeUtils.getSelectedChildren(orgSelection, new ArrayList<AbstractNode>());
                boolean linkGrabber = false;
                boolean downloadList = false;
                for (AbstractNode l : selection) {
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
