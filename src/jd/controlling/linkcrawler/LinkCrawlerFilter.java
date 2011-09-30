package jd.controlling.linkcrawler;

public interface LinkCrawlerFilter {

    public boolean dropByUrl(CrawledLink link);

    public boolean dropByFileProperties(CrawledLink link);
}
