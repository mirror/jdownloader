package jd.controlling.linkcrawler;

import java.util.concurrent.atomic.AtomicInteger;

import jd.http.BrowserSettingsThread;
import jd.plugins.PluginForDecrypt;

public class LinkCrawlerThread extends BrowserSettingsThread {
    private static AtomicInteger linkCrawlerThread = new AtomicInteger(0);
    private boolean              b                 = false;
    private LinkCrawler          crawler;
    private PluginForDecrypt     currentPlugin     = null;

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

    /**
     * @return the currentPlugin
     */
    public PluginForDecrypt getCurrentPlugin() {
        return currentPlugin;
    }

    /**
     * @param currentPlugin the currentPlugin to set
     */
    public void setCurrentPlugin(PluginForDecrypt currentPlugin) {
        this.currentPlugin = currentPlugin;
    }

}
