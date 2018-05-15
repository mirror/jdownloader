package org.jdownloader.controlling.domainrules;

import org.appwork.storage.Storable;

//{"maxSimultanDownloads":10,"pattern":".*dummmydomain.com,"allowToExceedTheGlobalLimit":true,"enabled":false}
public class DomainRule implements Storable {
    public boolean isEnabled() {
        return enabled;
    }

    public DomainRule(/* Storable */) {
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private String domainPattern;

    public String getDomainPattern() {
        return domainPattern;
    }

    public void setDomainPattern(String domainPattern) {
        this.domainPattern = domainPattern;
    }

    private String filenamePattern;

    public String getFilenamePattern() {
        return filenamePattern;
    }

    public void setFilenamePattern(String filenamePattern) {
        this.filenamePattern = filenamePattern;
    }

    private String accountPattern;
    private String pluginPattern;

    public String getPluginPattern() {
        return pluginPattern;
    }

    public void setPluginPattern(String pluginPattern) {
        this.pluginPattern = pluginPattern;
    }

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

    /**
     * @deprecated remove xmas 2014 we keep it to convert settings
     * @param pattern
     */
    public void setPattern(String pattern) {
        this.domainPattern = pattern;
    }

    public int getMaxSimultanDownloads() {
        return maxSimultanDownloads;
    }

    public void setMaxSimultanDownloads(int maxSimultanDownloads) {
        this.maxSimultanDownloads = maxSimultanDownloads;
    }
}
