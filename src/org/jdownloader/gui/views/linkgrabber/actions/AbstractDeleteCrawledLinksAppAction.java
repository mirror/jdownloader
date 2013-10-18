package org.jdownloader.gui.views.linkgrabber.actions;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.jdownloader.actions.AbstractSelectionContextAction;
import org.jdownloader.gui.views.SelectionInfo;

public abstract class AbstractDeleteCrawledLinksAppAction extends AbstractSelectionContextAction<CrawledPackage, CrawledLink> {

    public AbstractDeleteCrawledLinksAppAction(SelectionInfo<CrawledPackage, CrawledLink> si) {
        super(si);
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1117751339878672160L;

    public void deleteLinksRequest(final SelectionInfo<CrawledPackage, CrawledLink> si, final String msg) {
        System.out.println("Delete " + si + " msg");
    }

    @Override
    public boolean isEnabled() {
        SelectionInfo<CrawledPackage, CrawledLink> si = getSelection();
        if (si != null && si.getChildren().size() > 0) {
            if (si.isAltDown() || si.isAltGraphDown()) return false;
            return true;
        }
        return false;
    }
}
