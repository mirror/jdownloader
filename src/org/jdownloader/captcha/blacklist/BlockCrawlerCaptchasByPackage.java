package org.jdownloader.captcha.blacklist;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.captcha.v2.Challenge;

public class BlockCrawlerCaptchasByPackage implements BlacklistEntry {

    private final LinkCrawler crawler;
    private CrawledLink       origin;

    public BlockCrawlerCaptchasByPackage(LinkCrawler crawler, CrawledLink link) {
        this.crawler = crawler;
        origin = link.getOriginLink();
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
            CrawledLink link = decrypt.getCurrentLink();
            return decrypt.getCrawler() == crawler && link != null && link.getOriginLink() == origin;
        }
        return false;
    }
}