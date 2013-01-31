package org.jdownloader.controlling.filter;

import jd.controlling.AccountController;
import jd.controlling.linkcrawler.CrawledLink;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.PluginStatusFilter;

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
                if (AccountController.getInstance().hasAccounts(link.getHost())) return true;
                if (AccountController.getInstance().hasMultiHostAccounts(link.getHost())) return true;
                return false;
            case AUTOCAPTCHA:
                return link.hasAutoCaptcha() || !link.hasCaptcha(null);
            }
            return false;
        case ISNOT:
            switch (getPluginStatus()) {
            case PREMIUM:
                if (!AccountController.getInstance().hasAccounts(link.getHost()) && !AccountController.getInstance().hasMultiHostAccounts(link.getHost())) return true;
                return false;
            case AUTOCAPTCHA:
                return !link.hasAutoCaptcha() && link.hasCaptcha(null);
            }
        }
        return false;
    }
}
