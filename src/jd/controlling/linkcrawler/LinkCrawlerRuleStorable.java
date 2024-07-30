package jd.controlling.linkcrawler;

import org.appwork.storage.Storable;

public class LinkCrawlerRuleStorable extends LinkCrawlerRule implements Storable {
    public LinkCrawlerRuleStorable(/* Storable */) {
        super();
    }

    public LinkCrawlerRuleStorable(LinkCrawlerRule rule) {
        super(rule.getId());
        _set(rule);
    }

    public void _set(LinkCrawlerRule rule) {
        this.setEnabled(rule.isEnabled());
        this.setName(rule.getName());
        this.setPattern(rule.getPattern());
        this.setRule(rule.getRule());
        this.setMaxDecryptDepth(rule.getMaxDecryptDepth());
        this.setPackageNamePattern(rule.getPackageNamePattern());
        this.setFormPattern(rule.getFormPattern());
        this.setDeepPattern(rule.getDeepPattern());
        this.setRewriteReplaceWith(rule.getRewriteReplaceWith());
        this.setCookies(rule.getCookies());
        this.setPasswordPattern(rule.getPasswordPattern());
        this.setUpdateCookies(rule.isUpdateCookies());
        this.setLogging(rule.isLogging());
        this.setId(rule.getId());
    }
}
