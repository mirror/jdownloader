package jd.controlling.linkcrawler;

public interface LinkCrawlerHandler {

    void linkCrawlerStarted();

    void linkCrawlerStopped();

    void handleFinalLink(CrawledLink link);

    void handleFilteredLink(CrawledLink link);

}
