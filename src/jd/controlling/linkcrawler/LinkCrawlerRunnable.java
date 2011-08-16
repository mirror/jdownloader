package jd.controlling.linkcrawler;

public abstract class LinkCrawlerRunnable implements Runnable {

    private final LinkCrawler crawler;

    protected LinkCrawlerRunnable(LinkCrawler crawler) {
        if (crawler == null) throw new IllegalArgumentException("crawler==null?");
        this.crawler = crawler;
    }

    public LinkCrawler getLinkCrawler() {
        return crawler;
    }

}
