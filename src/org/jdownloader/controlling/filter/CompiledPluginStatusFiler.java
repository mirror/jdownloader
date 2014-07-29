package org.jdownloader.controlling.filter;

import java.util.ArrayList;

import jd.controlling.AccountController;
import jd.controlling.linkcrawler.CrawledLink;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.PluginStatusFilter;
import jd.plugins.Account;

public class CompiledPluginStatusFiler extends PluginStatusFilter {

    public CompiledPluginStatusFiler(PluginStatusFilter pluginStatusFilter) {
        super(pluginStatusFilter.getMatchType(), pluginStatusFilter.isEnabled(), pluginStatusFilter.getPluginStatus());

    }

    public boolean matches(CrawledLink link) {
        /* TODO: do we want to check all accounts for hasCaptcha!? */
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
        boolean bPremium = false;
        Boolean bAccount = AccountController.getInstance().hasAccounts(link.getHost());
        Boolean bMultiHost = AccountController.getInstance().hasMultiHostAccounts(link.getHost());
        if (bAccount || bMultiHost) {
            // There is at least one account, verify if there is at least one premium account valid
            ArrayList<Account> alAccount = AccountController.getInstance().getValidAccounts(link.getHost());
            if (alAccount.size() > 0) {
                // There is some valid account, verify if they are valid premium
                for (Account AccountToVerify : alAccount) {
                    long lgValidUntil = AccountToVerify.getAccountInfo().getValidUntil();
                    if (lgValidUntil > 0) {
                        bPremium = true;
                        break;
                    }
                }
            }
        }
        return bPremium;
    }
}
