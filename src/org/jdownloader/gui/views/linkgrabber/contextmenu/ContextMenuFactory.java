package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.swing.exttable.ExtColumn;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberPanel;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;

public class ContextMenuFactory {

    private LinkGrabberTable              table;
    private LinkGrabberPanel              panel;
    private LinkgrabberContextMenuManager manager;

    public ContextMenuFactory(LinkGrabberTable linkGrabberTable, LinkGrabberPanel linkGrabberPanel) {
        this.table = linkGrabberTable;
        this.panel = linkGrabberPanel;
        manager = LinkgrabberContextMenuManager.getInstance();
    }

    public JPopupMenu createPopup(AbstractNode context, java.util.List<AbstractNode> selection, ExtColumn<AbstractNode> column, MouseEvent event) {

        SelectionInfo<CrawledPackage, CrawledLink> si = new SelectionInfo<CrawledPackage, CrawledLink>(context, selection, event, null, null, table);
        si.setContextColumn(column);

        return manager.build(si);

    }
}
