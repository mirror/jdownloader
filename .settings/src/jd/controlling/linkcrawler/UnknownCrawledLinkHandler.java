package jd.controlling.linkcrawler;

public interface UnknownCrawledLinkHandler {

    public void unhandledCrawledLink(CrawledLink link, LinkCrawler lc);
}
