package org.jdownloader.controlling.domainrules;

import java.util.regex.Pattern;

import jd.plugins.Account;

public class CompiledDomainRule {

    private Pattern pattern;

    public Pattern getPattern() {
        return pattern;
    }

    private DomainRule rule;
    private Pattern    accountPattern;

    public CompiledDomainRule(DomainRule dr) {
        this.pattern = Pattern.compile(dr.getPattern(), Pattern.DOTALL);
        accountPattern = dr.getAccountPattern() == null ? null : Pattern.compile(dr.getAccountPattern(), Pattern.DOTALL);
        this.rule = dr;
    }

    public boolean matches(Account account, String domain) {
        if (accountPattern != null) {
            if (!accountPattern.matcher(account == null ? "" : account.getUser()).matches()) {
                return false;
            }
        }
        return pattern.matcher(domain).matches();
    }

    public int getMaxSimultanDownloads() {
        return rule.getMaxSimultanDownloads();
    }

    public boolean isAllowToExceedTheGlobalLimit() {
        return rule.isAllowToExceedTheGlobalLimit();
    }

}
