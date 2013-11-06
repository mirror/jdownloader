package org.jdownloader.captcha.blacklist;

import jd.controlling.linkcrawler.LinkCrawler;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.captcha.v2.Challenge;

public class BlockCrawlerCaptchasByHost implements BlacklistEntry {

    private final LinkCrawler crawler;
    private final String      host;

    public BlockCrawlerCaptchasByHost(LinkCrawler crawler, String host) {
        this.crawler = crawler;
        this.host = host;
    }

    @Override
    public boolean canCleanUp() {
        return crawler == null || !crawler.isRunning();
    }

    @Override
    public boolean matches(Challenge c) {
        Plugin plugin = Challenge.getPlugin(c);
        if (plugin instanceof PluginForDecrypt) {
            PluginForDecrypt decrypt = (PluginForDecrypt) plugin;
            return decrypt.getCrawler() == crawler && decrypt.getHost().equalsIgnoreCase(host);
        }
        return false;
    }
}