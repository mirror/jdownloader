package jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.DomainInfo;
import org.jdownloader.controlling.hosterrule.AccountUsageRule;
import org.jdownloader.controlling.hosterrule.HosterRuleController;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.AbstractAddAction;
import org.jdownloader.plugins.controller.LazyPlugin.FEATURE;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;

import jd.controlling.AccountController;
import jd.plugins.Account;
import jd.plugins.AccountInfo;

public class NewRuleAction extends AbstractAddAction {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public NewRuleAction() {
        super();
    }

    public void actionPerformed(ActionEvent e) {
        final ArrayList<DomainInfo> list = getAvailableDomainInfoList();
        /* Allow only one rule per hoster -> Remove items from list which a rule already exists for. */
        final HosterRuleController hrc = HosterRuleController.getInstance();
        for (final AccountUsageRule aur : hrc.list()) {
            list.remove(DomainInfo.getInstance(aur.getHoster(), true));
        }
        final ChooseHosterDialog d = new ChooseHosterDialog(_GUI.T.NewRuleAction_actionPerformed_choose_hoster_message(), list.toArray(new DomainInfo[] {}));
        try {
            Dialog.getInstance().showDialog(d);
            final DomainInfo di = d.getSelectedItem();
            if (di != null) {
                /* Add rule for selected item. */
                final AccountUsageRule rule = new AccountUsageRule(di.getTld());
                rule.setEnabled(true);
                hrc.add(rule);
            }
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
    }

    /** Returns list of possible domains which an AccountUsageRule can be added for. */
    protected ArrayList<DomainInfo> getAvailableDomainInfoList() {
        final HashSet<DomainInfo> domains = new HashSet<DomainInfo>();
        final HashSet<String> multihosterDomains = new HashSet<String>();
        /* Collect domains of all multihoster accounts which the user currently has. */
        for (final Account acc : AccountController.getInstance().list()) {
            if (!acc.getPlugin().hasFeature(FEATURE.MULTIHOST)) {
                continue;
            }
            final String thisMultihosterDomain = acc.getHoster();
            multihosterDomains.add(thisMultihosterDomain);
            final AccountInfo ai = acc.getAccountInfo();
            if (ai == null) {
                continue;
            }
            final List<String> supportedHosts = ai.getMultiHostSupport();
            if (supportedHosts == null) {
                continue;
            }
            for (final String supportedHost : supportedHosts) {
                if (multihosterDomains.contains(supportedHost)) {
                    /*
                     * Multihoster supports its own domain or domains of other multihosters -> Exclude those domains from usage rule
                     * selection
                     */
                    continue;
                }
                final LazyHostPlugin plg = HostPluginController.getInstance().get(supportedHost);
                if (plg != null) {
                    domains.add(DomainInfo.getInstance(plg.getHost(), true));
                }
            }
        }
        final Collection<LazyHostPlugin> plugins = HostPluginController.getInstance().list();
        /* Collect all domains which the user is allowed to create usage rules for. */
        for (final LazyHostPlugin plugin : plugins) {
            if (allowAccountUsageRuleCreation(plugin)) {
                domains.add(DomainInfo.getInstance(plugin.getHost(), true));
            }
        }
        /* Sort results. */
        final ArrayList<DomainInfo> lst = new ArrayList<DomainInfo>(domains);
        Collections.sort(lst, new Comparator<DomainInfo>() {
            @Override
            public int compare(DomainInfo o1, DomainInfo o2) {
                return o1.getTld().compareTo(o2.getTld());
            }
        });
        return lst;
    }

    @Override
    public String getTooltipText() {
        return _GUI.T.NewRuleAction_getTooltipText_tt_();
    }

    /** Returns true if new rule creation is allowed according to some factors given inside given LazyHostPlugin instance. */
    public static boolean allowAccountUsageRuleCreation(final LazyHostPlugin plg) {
        if (!plg.isPremium()) {
            return false;
        } else if (plg.hasFeature(FEATURE.MULTIHOST)) {
            /*
             * Do not allow users to create account usage rules for multihosts as those usually don't host any files thus creating a rule
             * doesn't make any sense.
             */
            return false;
        } else if (plg.hasFeature(FEATURE.USENET)) {
            /* Do not allow users to create account usage rules for Usenet services as Usenet can't be used without account anyways. */
            return false;
        } else if (plg.hasFeature(FEATURE.INTERNAL)) {
            /* Do not allow users to create account usage rules for internal plugins. */
            return false;
        } else if (plg.isOfflinePlugin()) {
            /* Do not allow users to create account usage rules for domains known to be permanently offline. */
            return false;
        } else {
            return true;
        }
    }
}
