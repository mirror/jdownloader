package jd.controlling.linkcrawler;

public class LinkCrawlerAbort {

    private final long        generation;
    private final LinkCrawler crawler;

    public LinkCrawlerAbort(final long generation, LinkCrawler linkCrawler) {
        this.generation = generation;
        this.crawler = linkCrawler;
    }

    public boolean isAbort() {
        return this.generation != crawler.getCrawlerGeneration(false);
    }

}
