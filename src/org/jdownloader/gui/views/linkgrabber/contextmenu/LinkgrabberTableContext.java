package org.jdownloader.gui.views.linkgrabber.contextmenu;

import javax.swing.JPopupMenu;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.swing.exttable.ExtColumn;
import org.jdownloader.gui.menu.MenuContext;
import org.jdownloader.gui.views.SelectionInfo;

public class LinkgrabberTableContext extends MenuContext<JPopupMenu> {

    private ExtColumn<AbstractNode>                    clickedColumn;
    private SelectionInfo<CrawledPackage, CrawledLink> selectionInfo;

    public SelectionInfo<CrawledPackage, CrawledLink> getSelectionInfo() {
        return selectionInfo;
    }

    public LinkgrabberTableContext(JPopupMenu p, SelectionInfo<CrawledPackage, CrawledLink> si, ExtColumn<AbstractNode> column) {
        super(p);
        clickedColumn = column;
        this.selectionInfo = si;
    }

    public ExtColumn<AbstractNode> getClickedColumn() {
        return clickedColumn;
    }

}
