package org.jdownloader.controlling.domainrules;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;

//{"maxSimultanDownloads":10,"pattern":".*dummmydomain.com,"allowToExceedTheGlobalLimit":true,"enabled":false}
public class DomainRule implements Storable {
    public static void main(String[] args) {
        System.out.println(JSonStorage.toString(new DomainRule()));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public DomainRule(/* Storable */) {
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private String pattern;
    private String accountPattern;

    public String getAccountPattern() {
        return accountPattern;
    }

    public void setAccountPattern(String accountPattern) {
        this.accountPattern = accountPattern;
    }

    private boolean enabled                     = false;
    private int     maxSimultanDownloads        = 0;
    private boolean allowToExceedTheGlobalLimit = false;

    public boolean isAllowToExceedTheGlobalLimit() {
        return allowToExceedTheGlobalLimit;
    }

    public void setAllowToExceedTheGlobalLimit(boolean maxSimultanDownloadsObeysGlobalLimit) {
        this.allowToExceedTheGlobalLimit = maxSimultanDownloadsObeysGlobalLimit;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public int getMaxSimultanDownloads() {
        return maxSimultanDownloads;
    }

    public void setMaxSimultanDownloads(int maxSimultanDownloads) {
        this.maxSimultanDownloads = maxSimultanDownloads;
    }
}
