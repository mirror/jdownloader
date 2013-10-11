package org.jdownloader.api.linkcrawler;

import jd.controlling.linkcrawler.LinkCrawler;

public class LinkCrawlerAPIImpl implements LinkCrawlerAPI {
    @Override
    public boolean isCrawling() {
        return LinkCrawler.isCrawling();
    }
}