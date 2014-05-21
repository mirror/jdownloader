package org.jdownloader.captcha.blacklist;

import java.lang.ref.WeakReference;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.captcha.v2.Challenge;

public class BlockCrawlerCaptchasByPackage implements BlacklistEntry {

    private final WeakReference<LinkCrawler> crawler;
    private final CrawledLink                origin;

    public BlockCrawlerCaptchasByPackage(LinkCrawler crawler, CrawledLink link) {
        this.crawler = new WeakReference<LinkCrawler>(crawler);
        origin = link.getOriginLink();
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
                PluginForDecrypt decrypt = (PluginForDecrypt) plugin;
                CrawledLink link = decrypt.getCurrentLink();
                return decrypt.getCrawler() == lcrawler && link != null && link.getOriginLink() == origin;
            }
        }
        return false;
    }
}