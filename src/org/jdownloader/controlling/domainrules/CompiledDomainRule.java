package org.jdownloader.controlling.domainrules;

import java.util.regex.Pattern;

import jd.plugins.Account;

public class CompiledDomainRule {

    private final DomainRule rule;
    private final Pattern    domainPattern;
    private final Pattern    accountPattern;
    private final Pattern    filenamePattern;
    private final Pattern    pluginPattern;

    public CompiledDomainRule(DomainRule dr) {
        this.domainPattern = dr.getDomainPattern() == null ? null : Pattern.compile(dr.getDomainPattern(), Pattern.DOTALL);
        this.accountPattern = dr.getAccountPattern() == null ? null : Pattern.compile(dr.getAccountPattern(), Pattern.DOTALL);
        this.pluginPattern = dr.getPluginPattern() == null ? null : Pattern.compile(dr.getPluginPattern(), Pattern.DOTALL);
        this.filenamePattern = dr.getFilenamePattern() == null ? null : Pattern.compile(dr.getFilenamePattern(), Pattern.DOTALL);
        this.rule = dr;
    }

    public boolean matches(final Account account, final String domain, final String plugin, final String filename) {
        if (accountPattern != null) {
            if (!accountPattern.matcher(account == null ? "" : account.getUser()).matches()) {
                return false;
            }
        }
        if (filenamePattern != null) {
            if (!filenamePattern.matcher(filename == null ? "" : filename).matches()) {
                return false;
            }
        }
        if (domainPattern != null) {
            if (!domainPattern.matcher(domain == null ? "" : domain).matches()) {
                return false;
            }
        }
        if (pluginPattern != null) {
            if (!pluginPattern.matcher(plugin == null ? "" : plugin).matches()) {
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
