package org.jdownloader.controlling.domainrules;

import org.appwork.storage.Storable;

public class DomainRule implements Storable {
    private String  filenamePattern;
    private String  accountPattern;
    private String  pluginPattern;
    private boolean enabled                     = false;
    private int     maxSimultanDownloads        = 0;
    private int     maxChunks                   = null;
    private boolean allowToExceedTheGlobalLimit = false;

    public DomainRule(/* Storable */) {
    }

    public boolean isEnabled() {
        return enabled;
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

    public String getFilenamePattern() {
        return filenamePattern;
    }

    public void setFilenamePattern(String filenamePattern) {
        this.filenamePattern = filenamePattern;
    }

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

    public boolean isAllowToExceedTheGlobalLimit() {
        return allowToExceedTheGlobalLimit;
    }

    public void setAllowToExceedTheGlobalLimit(boolean maxSimultanDownloadsObeysGlobalLimit) {
        this.allowToExceedTheGlobalLimit = maxSimultanDownloadsObeysGlobalLimit;
    }

    public int getMaxSimultanDownloads() {
        return maxSimultanDownloads;
    }

    public void setMaxSimultanDownloads(int maxSimultanDownloads) {
        this.maxSimultanDownloads = maxSimultanDownloads;
    }

    public Integer getMaxChunks() {
        return maxChunks;
    }

    public void setMaxChunks(Integer maxChunks) {
        this.maxChunks = maxChunks;
    }
}
