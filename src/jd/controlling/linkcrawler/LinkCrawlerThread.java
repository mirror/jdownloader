package jd.controlling.linkcrawler;

import java.util.concurrent.atomic.AtomicInteger;

import jd.controlling.BrowserSettingsThread;

public class LinkCrawlerThread extends BrowserSettingsThread {
    private static AtomicInteger linkCrawlerThread = new AtomicInteger(0);
    private boolean              b                 = false;

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

}
