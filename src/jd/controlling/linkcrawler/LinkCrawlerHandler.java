package jd.controlling.linkcrawler;

public interface LinkCrawlerHandler {

    void linkCrawlerStarted();

    void linkCrawlerStopped();

    void handleFinalLink(CrawledLinkInfo link);

}
