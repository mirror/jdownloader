package jd.controlling.linkcrawler;

public interface LinkCrawlerFilter {

    public boolean isCrawledLinkFiltered(CrawledLink link);
}
