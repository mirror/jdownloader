package org.jdownloader.gui.views.linkgrabber.actions;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.jdownloader.controlling.contextmenu.CustomizableSelectionAppAction;
import org.jdownloader.gui.KeyObserver;
import org.jdownloader.gui.views.SelectionInfo;

public abstract class AbstractDeleteCrawledLinksAppAction extends CustomizableSelectionAppAction<CrawledPackage, CrawledLink> {

    public AbstractDeleteCrawledLinksAppAction() {

    }

    /**
     * 
     */
    private static final long serialVersionUID = 1117751339878672160L;

    public void deleteLinksRequest(final SelectionInfo<CrawledPackage, CrawledLink> si, final String msg) {

    }

    @Override
    public void requestUpdate(Object requestor) {
        super.requestUpdate(requestor);
        if (hasSelection()) {
            if (KeyObserver.getInstance().isAltOrAltGraphDown()) {
                setEnabled(false);
            } else {
                setEnabled(true);
            }
        } else {
            setEnabled(false);
        }
    }
}
