package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;

import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.Priority;
import org.jdownloader.gui.views.linkgrabber.LinkTreeUtils;

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
        ArrayList<AbstractNode> selection = LinkTreeUtils.getSelectedChildren(orgSelection, new ArrayList<AbstractNode>());
        for (AbstractNode l : selection) {
            if (l instanceof CrawledLink) {
                ((CrawledLink) l).setPriority(priority);
            } else if (l instanceof DownloadLink) {
                ((DownloadLink) l).setPriority(priority.getId());
            }
        }
    }

}
