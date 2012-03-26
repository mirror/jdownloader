package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractNode;

import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.Priority;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;
import org.jdownloader.gui.views.linkgrabber.LinkTreeUtils;

public class PriorityActionEntry extends AppAction {

    /**
     * 
     */
    private static final long      serialVersionUID = 1L;
    private ArrayList<CrawledLink> selection;
    private Priority               priority;

    public PriorityActionEntry(Priority priority, ArrayList<AbstractNode> selection) {
        setName(priority._());
        setSmallIcon(priority.loadIcon(18));
        this.selection = LinkTreeUtils.getSelectedChildren(selection, new ArrayList<CrawledLink>());
        this.priority = priority;
    }

    public void actionPerformed(ActionEvent e) {
        for (CrawledLink l : selection) {
            l.setPriority(priority);
        }

        LinkGrabberTableModel.getInstance().recreateModel(false);
    }

}
