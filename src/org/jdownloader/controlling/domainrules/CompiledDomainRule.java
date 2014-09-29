package org.jdownloader.controlling.domainrules;

import java.util.regex.Pattern;

import jd.plugins.Account;

public class CompiledDomainRule {

    private Pattern domainPattern;

    public Pattern getDomainPattern() {
        return domainPattern;
    }

    private DomainRule rule;
    private Pattern    accountPattern;
    private Pattern    filenamePattern;

    public CompiledDomainRule(DomainRule dr) {
        this.domainPattern = dr.getDomainPattern() == null ? null : Pattern.compile(dr.getDomainPattern(), Pattern.DOTALL);
        accountPattern = dr.getAccountPattern() == null ? null : Pattern.compile(dr.getAccountPattern(), Pattern.DOTALL);
        filenamePattern = dr.getFilenamePattern() == null ? null : Pattern.compile(dr.getFilenamePattern(), Pattern.DOTALL);
        this.rule = dr;
    }

    public boolean matches(Account account, String domain, String name) {
        if (accountPattern != null) {
            if (!accountPattern.matcher(account == null ? "" : account.getUser()).matches()) {
                return false;
            }
        }
        if (filenamePattern != null) {
            if (!filenamePattern.matcher(name).matches()) {
                return false;
            }
        }
        if (domainPattern != null) {
            if (!domainPattern.matcher(domain).matches()) {
                return false;
            }
        }
        return true;
    }

    public int getMaxSimultanDownloads() {
        return rule.getMaxSimultanDownloads();
    }

    public boolean isAllowToExceedTheGlobalLimit() {
        return rule.isAllowToExceedTheGlobalLimit();
    }

}
