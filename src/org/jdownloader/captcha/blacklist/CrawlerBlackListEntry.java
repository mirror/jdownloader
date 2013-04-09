package org.jdownloader.captcha.blacklist;

import jd.controlling.linkcrawler.LinkCrawler;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.ImageCaptchaChallenge;

public class CrawlerBlackListEntry implements BlacklistEntry {

    private LinkCrawler crawler;

    public CrawlerBlackListEntry(LinkCrawler crawler) {
        this.crawler = crawler;
    }

    @Override
    public boolean canCleanUp() {
        return !crawler.isRunning();
    }

    @Override
    public boolean matches(Challenge c) {
        if (c instanceof ImageCaptchaChallenge) {
            Plugin plugin = ((ImageCaptchaChallenge) c).getPlugin();
            if (plugin instanceof PluginForDecrypt) { return ((PluginForDecrypt) plugin).getCrawler() == crawler; }
        }
        return true;
    }

}
