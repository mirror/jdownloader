package org.jdownloader.controlling.domainrules;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;

//{"maxSimultanDownloads":10,"pattern":".*dummmydomain.com,"allowToExceedTheGlobalLimit":true,"enabled":false}
public class DomainRule implements Storable {
    public static void main(String[] args) {
        DomainRule dr = new DomainRule();
        dr.setAccountPattern("myUsername");
        dr.setDomainPattern(".*jdownloader\\.org");
        dr.setFilenamePattern("\\.png$");
        dr.setMaxSimultanDownloads(20);
        System.out.println("[" + JSonStorage.toString(dr).replace("\"", "\\\"") + "]");
    }

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
