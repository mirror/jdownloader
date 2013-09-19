package jd.controlling.linkcollector;

import jd.controlling.linkcollector.event.LinkCollectorCrawlerEvent;
import jd.controlling.linkcollector.event.LinkCollectorCrawlerEventSender;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.plugins.PluginsC;

import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;

public class LinkCollectorCrawler extends LinkCrawler implements LinkCollectorListener {

    private LinkCollectorCrawlerEventSender eventSender;

    public LinkCollectorCrawler() {
        super(true, true);
        setDirectHttpEnabled(CFG_GENERAL.CFG.isDirectHTTPCrawlerEnabled());
        eventSender = new LinkCollectorCrawlerEventSender();
    }

    public LinkCollectorCrawlerEventSender getEventSender() {
        return eventSender;
    }

    protected void crawl(LazyCrawlerPlugin lazyC, final CrawledLink cryptedLink) {
        eventSender.fireEvent(new LinkCollectorCrawlerEvent(this, LinkCollectorCrawlerEvent.Type.CRAWLER_PLUGIN, cryptedLink));
        super.crawl(lazyC, cryptedLink);

    }

    @Override
    protected void processHostPlugin(LazyHostPlugin pHost, CrawledLink possibleCryptedLink) {
        eventSender.fireEvent(new LinkCollectorCrawlerEvent(this, LinkCollectorCrawlerEvent.Type.HOST_PLUGIN, possibleCryptedLink));
        super.processHostPlugin(pHost, possibleCryptedLink);
    }

    @Override
    protected void container(PluginsC oplg, CrawledLink cryptedLink) {
        eventSender.fireEvent(new LinkCollectorCrawlerEvent(this, LinkCollectorCrawlerEvent.Type.CONTAINER_PLUGIN, cryptedLink));
        super.container(oplg, cryptedLink);
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

    @Override
    public void onLinkCrawlerAdded(LinkCollectorCrawler parameter) {
    }

    @Override
    public void onLinkCrawlerStarted(LinkCollectorCrawler parameter) {
    }

    @Override
    public void onLinkCrawlerStopped(LinkCollectorCrawler parameter) {
    }

}
