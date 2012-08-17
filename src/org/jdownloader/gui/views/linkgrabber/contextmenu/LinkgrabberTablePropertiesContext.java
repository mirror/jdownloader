package org.jdownloader.gui.views.linkgrabber.contextmenu;

import javax.swing.JMenuItem;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.swing.exttable.ExtColumn;
import org.jdownloader.gui.menu.MenuContext;
import org.jdownloader.gui.views.SelectionInfo;

public class LinkgrabberTablePropertiesContext extends MenuContext<java.util.List<JMenuItem>> {

    private ExtColumn<AbstractNode>                    clickedColumn;
    private SelectionInfo<CrawledPackage, CrawledLink> selectionInfo;

    public LinkgrabberTablePropertiesContext(java.util.List<JMenuItem> ret, SelectionInfo<CrawledPackage, CrawledLink> si, ExtColumn<AbstractNode> column) {
        super(ret);
        selectionInfo = si;
        clickedColumn = column;
    }

    public SelectionInfo<CrawledPackage, CrawledLink> getSelectionInfo() {
        return selectionInfo;
    }

    public ExtColumn<AbstractNode> getClickedColumn() {
        return clickedColumn;
    }

}