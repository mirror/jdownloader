package org.jdownloader.controlling.filter;

import java.util.List;

import jd.controlling.AccountController;
import jd.controlling.linkcrawler.CrawledLink;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.PluginStatusFilter;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;

public class CompiledPluginStatusFilter extends PluginStatusFilter {

    public CompiledPluginStatusFilter(PluginStatusFilter pluginStatusFilter) {
        super(pluginStatusFilter.getMatchType(), pluginStatusFilter.isEnabled(), pluginStatusFilter.getPluginStatus());

    }

    public boolean matches(CrawledLink link) {
        switch (getMatchType()) {
        case IS:
            switch (getPluginStatus()) {
            case PREMIUM:
                return VerifyPremium(link);
            case AUTOCAPTCHA:
                return link.hasAutoCaptcha() || !link.hasCaptcha(null);
            case NO_DIRECT_HTTP:
                return !link.isDirectHTTP();
            }
            return false;
        case ISNOT:
            switch (getPluginStatus()) {
            case PREMIUM:
                return !VerifyPremium(link);
            case AUTOCAPTCHA:
                return !link.hasAutoCaptcha() && link.hasCaptcha(null);
            case NO_DIRECT_HTTP:
                return link.isDirectHTTP();
            }
        }
        return false;
    }

    /**
     * Verify if there is at least one premium account valid
     * 
     * @param link
     *            Link that we want to werify
     * @return true if a premium account is associated, false otherwise.
     */
    private boolean VerifyPremium(CrawledLink link) {
        if (AccountController.getInstance().hasAccounts(link.getHost())) {
            final List<Account> accounts = AccountController.getInstance().getValidAccounts(link.getHost());
            if (accounts != null) {
                for (final Account accountToVerify : accounts) {
                    if (AccountType.PREMIUM.equals(accountToVerify.getType())) {
                        long lgValidUntil = accountToVerify.getValidPremiumUntil();
                        if (lgValidUntil < 0 || lgValidUntil > System.currentTimeMillis()) {
                            return true;
                        }
                    }
                }
            }
        }
        if (AccountController.getInstance().hasMultiHostAccounts(link.getHost())) {
            final List<Account> accounts = AccountController.getInstance().getMultiHostAccounts(link.getHost());
            if (accounts != null) {
                for (final Account accountToVerify : accounts) {
                    if (AccountType.PREMIUM.equals(accountToVerify.getType())) {
                        long lgValidUntil = accountToVerify.getValidPremiumUntil();
                        if (lgValidUntil < 0 || lgValidUntil > System.currentTimeMillis()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
