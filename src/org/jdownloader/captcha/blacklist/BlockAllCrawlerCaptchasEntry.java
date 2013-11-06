package org.jdownloader.captcha.blacklist;

import jd.controlling.linkcrawler.LinkCrawler;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.captcha.v2.Challenge;

public class BlockAllCrawlerCaptchasEntry implements BlacklistEntry {

    private final LinkCrawler crawler;

    public BlockAllCrawlerCaptchasEntry(LinkCrawler crawler) {
        this.crawler = crawler;
    }

    @Override
    public boolean canCleanUp() {
        return crawler == null || !crawler.isRunning();
    }

    @Override
    public boolean matches(Challenge c) {
        Plugin plugin = Challenge.getPlugin(c);
        if (plugin instanceof PluginForDecrypt) { return ((PluginForDecrypt) plugin).getCrawler() == crawler; }
        return false;
    }

}
