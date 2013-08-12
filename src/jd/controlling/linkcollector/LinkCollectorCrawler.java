package jd.controlling.linkcollector;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawler;

public class LinkCollectorCrawler extends LinkCrawler implements LinkCollectorListener {

    public LinkCollectorCrawler() {
        super(true, true);
    }

    public void onLinkCollectorAbort(LinkCollectorEvent event) {
        stopCrawling();
    }

    public void onLinkCollectorFilteredLinksAvailable(LinkCollectorEvent event) {
    }

    public void onLinkCollectorFilteredLinksEmpty(LinkCollectorEvent event) {
    }

    public void onLinkCollectorDataRefresh(LinkCollectorEvent event) {
    }

    public void onLinkCollectorStructureRefresh(LinkCollectorEvent event) {
    }

    public void onLinkCollectorLinkAdded(LinkCollectorEvent event, CrawledLink parameter) {
    }

    @Override
    public void onLinkCollectorDupeAdded(LinkCollectorEvent event, CrawledLink parameter) {
    }

    @Override
    public void onLinkCollectorContentRemoved(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorContentAdded(LinkCollectorEvent event) {
    }

}
