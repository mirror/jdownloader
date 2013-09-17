package jd.controlling.linkcollector.event;

import java.util.EventListener;

import jd.controlling.linkcollector.LinkCollectorCrawler;
import jd.controlling.linkcrawler.CrawledLink;

public interface LinkCollectorCrawlerListener extends EventListener {

    void onProcessingCrawlerPlugin(LinkCollectorCrawler caller, CrawledLink parameter);

    void onProcessingHosterPlugin(LinkCollectorCrawler caller, CrawledLink parameter);

    void onProcessingContainerPlugin(LinkCollectorCrawler caller, CrawledLink parameter);

}