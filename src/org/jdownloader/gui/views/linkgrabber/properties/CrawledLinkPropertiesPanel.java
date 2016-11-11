package org.jdownloader.gui.views.linkgrabber.properties;

import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.plugins.DownloadLink;

public class CrawledLinkPropertiesPanel extends LinkcrawlerListPropertiesPanel<CrawledLinkNodeProperties> {

    @Override
    public void onLinkCollectorDataRefresh(LinkCollectorEvent event) {
        final CrawledLinkNodeProperties current = getAbstractNodeProperties();
        if (current != null) {
            if (event.getParameter() instanceof DownloadLink) {
                final DownloadLink DownloadLink = (DownloadLink) event.getParameter();
                if (!current.isDifferent(DownloadLink)) {
                    refresh();
                }
            } else if (event.getParameter() instanceof CrawledLink) {
                final CrawledLink crawledLink = (CrawledLink) event.getParameter();
                if (!current.isDifferent(crawledLink)) {
                    refresh();
                }
            } else if (event.getParameter() instanceof CrawledPackage) {
                if (current.samePackage((AbstractPackageNode) event.getParameter())) {
                    refresh();
                }
            }
        }
    }

    @Override
    protected CrawledLinkNodeProperties createAbstractNodeProperties(AbstractNode abstractNode) {
        return new CrawledLinkNodeProperties((CrawledLink) abstractNode);
    }

}
