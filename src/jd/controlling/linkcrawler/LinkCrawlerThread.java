package jd.controlling.linkcrawler;

import java.util.concurrent.atomic.AtomicInteger;

import jd.http.BrowserSettingsThread;

public class LinkCrawlerThread extends BrowserSettingsThread {
    private static AtomicInteger linkCrawlerThread = new AtomicInteger(0);
    private boolean              b                 = false;
    private LinkCrawler          crawler;

    public LinkCrawlerThread(Runnable r) {
        super(r);
        setName("LinkCrawler:" + linkCrawlerThread.incrementAndGet());
        setDaemon(true);
    }

    protected void setLinkCrawlerThreadUsedbyDecrypter(boolean b) {
        this.b = b;
    }

    protected boolean isLinkCrawlerThreadUsedbyDecrypter() {
        return b;
    }

    protected void setCurrentLinkCrawler(LinkCrawler crawler) {
        this.crawler = crawler;
    }

    public LinkCrawler getCurrentLinkCrawler() {
        return crawler;
    }

}
