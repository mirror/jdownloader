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

    private final LinkCollectorCrawlerEventSender eventSender;

    protected LinkCollectorCrawler() {
        super(true, true);
        setDirectHTTPPermission(LinkCrawler.getConfig().getDirectHTTPPermission());
        setHostPluginBlacklist(CFG_GENERAL.CFG.getCrawlerHostPluginBlacklist());
        setCrawlerPluginBlacklist(CFG_GENERAL.CFG.getCrawlerCrawlerPluginBlacklist());
        eventSender = new LinkCollectorCrawlerEventSender();
    }

    public LinkCollectorCrawlerEventSender getEventSender() {
        return eventSender;
    }

    protected void crawl(LinkCrawlerGeneration generation, LazyCrawlerPlugin lazyC, final CrawledLink cryptedLink) {
        if (eventSender.hasListener()) {
            eventSender.fireEvent(new LinkCollectorCrawlerEvent(this, LinkCollectorCrawlerEvent.Type.CRAWLER_PLUGIN, cryptedLink));
        }
        super.crawl(generation, lazyC, cryptedLink);
    }

    @Override
    protected void processHostPlugin(LinkCrawlerGeneration generation, LazyHostPlugin pHost, CrawledLink possibleCryptedLink) {
        if (eventSender.hasListener()) {
            eventSender.fireEvent(new LinkCollectorCrawlerEvent(this, LinkCollectorCrawlerEvent.Type.HOST_PLUGIN, possibleCryptedLink));
        }
        super.processHostPlugin(generation, pHost, possibleCryptedLink);
    }

    @Override
    protected void container(LinkCrawlerGeneration generation, PluginsC oplg, CrawledLink cryptedLink) {
        if (eventSender.hasListener()) {
            eventSender.fireEvent(new LinkCollectorCrawlerEvent(this, LinkCollectorCrawlerEvent.Type.CONTAINER_PLUGIN, cryptedLink));
        }
        super.container(generation, oplg, cryptedLink);
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

    @Override
    public void onLinkCrawlerNewJob(LinkCollectingJob job) {
    }

    @Override
    public void onLinkCrawlerFinished() {
    }

}
