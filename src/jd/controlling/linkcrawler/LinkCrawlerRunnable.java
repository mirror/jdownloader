package jd.controlling.linkcrawler;

public abstract class LinkCrawlerRunnable implements Runnable {

    private final LinkCrawler crawler;
    private final int         generation;

    protected LinkCrawlerRunnable(LinkCrawler crawler, int generation) {
        if (crawler == null) throw new IllegalArgumentException("crawler==null?");
        this.crawler = crawler;
        this.generation = generation;
    }

    public LinkCrawler getLinkCrawler() {
        return crawler;
    }

    public void run() {
        try {
            if (crawler.getCrawlerGeneration(false) == this.generation) {
                crawling();
            }
        } finally {
            crawler.checkFinishNotify();
        }
    }

    abstract void crawling();

}
