package jd.controlling.linkcrawler;

import org.appwork.storage.Storable;

public class LinkCrawlerRuleStorable extends LinkCrawlerRule implements Storable {
    public LinkCrawlerRuleStorable(/* Storable */) {
        super();
    }

    public LinkCrawlerRuleStorable(LinkCrawlerRule rule) {
        super(rule.getId());
        set(rule);
    }

    public void setId(long id) {
        this.id.setID(id);
    }

    public void set(LinkCrawlerRule rule) {
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
        this.setId(rule.getId());
    }

    public LinkCrawlerRule _getLinkCrawlerRule() {
        final LinkCrawlerRule ret = new LinkCrawlerRule(this.getId());
        ret.setMaxDecryptDepth(getMaxDecryptDepth());
        ret.setEnabled(isEnabled());
        ret.setName(getName());
        ret.setPattern(getPattern());
        ret.setRule(getRule());
        ret.setPackageNamePattern(getPackageNamePattern());
        ret.setFormPattern(getFormPattern());
        ret.setDeepPattern(getDeepPattern());
        ret.setRewriteReplaceWith(getRewriteReplaceWith());
        ret.setCookies(getCookies());
        ret.setUpdateCookies(isUpdateCookies());
        ret.setPasswordPattern(getPasswordPattern());
        return ret;
    }
}
