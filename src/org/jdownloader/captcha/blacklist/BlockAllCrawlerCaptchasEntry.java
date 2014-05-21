package org.jdownloader.captcha.blacklist;

import java.lang.ref.WeakReference;

import jd.controlling.linkcrawler.LinkCrawler;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.captcha.v2.Challenge;

public class BlockAllCrawlerCaptchasEntry implements BlacklistEntry {

    private final WeakReference<LinkCrawler> crawler;

    public BlockAllCrawlerCaptchasEntry(LinkCrawler crawler) {
        this.crawler = new WeakReference<LinkCrawler>(crawler);
    }

    @Override
    public boolean canCleanUp() {
        LinkCrawler lcrawler = getCrawler();
        return lcrawler == null || !lcrawler.isRunning();
    }

    public LinkCrawler getCrawler() {
        return crawler.get();
    }

    @Override
    public boolean matches(Challenge c) {
        LinkCrawler lcrawler = getCrawler();
        if (lcrawler != null && lcrawler.isRunning()) {
            Plugin plugin = Challenge.getPlugin(c);
            if (plugin instanceof PluginForDecrypt) {
                return ((PluginForDecrypt) plugin).getCrawler() == lcrawler;
            }
        }
        return false;
    }

}
