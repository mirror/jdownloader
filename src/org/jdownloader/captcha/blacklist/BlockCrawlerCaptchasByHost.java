package org.jdownloader.captcha.blacklist;

import java.lang.ref.WeakReference;

import jd.controlling.linkcrawler.LinkCrawler;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.captcha.v2.Challenge;

public class BlockCrawlerCaptchasByHost implements BlacklistEntry {
    
    private final WeakReference<LinkCrawler> crawler;
    private final String                     host;
    
    public BlockCrawlerCaptchasByHost(LinkCrawler crawler, String host) {
        this.crawler = new WeakReference<LinkCrawler>(crawler);
        this.host = host;
    }
    
    @Override
    public boolean canCleanUp() {
        LinkCrawler lcrawler = getCrawler();
        return lcrawler == null || !lcrawler.isRunning();
    }
    
    private LinkCrawler getCrawler() {
        return crawler.get();
    }
    
    @Override
    public boolean matches(Challenge c) {
        LinkCrawler lcrawler = getCrawler();
        if (lcrawler != null && lcrawler.isRunning()) {
            Plugin plugin = Challenge.getPlugin(c);
            if (plugin instanceof PluginForDecrypt) {
                PluginForDecrypt decrypt = (PluginForDecrypt) plugin;
                return decrypt.getCrawler() == lcrawler && decrypt.getHost().equalsIgnoreCase(host);
            }
        }
        return false;
    }
}