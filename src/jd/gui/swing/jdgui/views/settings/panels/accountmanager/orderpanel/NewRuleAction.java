package jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import jd.config.Property;
import jd.controlling.AccountController;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.PluginForHost;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.DomainInfo;
import org.jdownloader.controlling.hosterrule.AccountUsageRule;
import org.jdownloader.controlling.hosterrule.HosterRuleController;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.AbstractAddAction;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;

public class NewRuleAction extends AbstractAddAction {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public NewRuleAction() {
        super();
    }

    public void actionPerformed(ActionEvent e) {
        ArrayList<DomainInfo> list = getAvailableDomainInfoList();
        // only one rule per hoster
        for (AccountUsageRule aur : HosterRuleController.getInstance().list()) {
            list.remove(DomainInfo.getInstance(aur.getHoster()));
        }
        ChooseHosterDialog d = new ChooseHosterDialog(_GUI._.NewRuleAction_actionPerformed_choose_hoster_message(), list.toArray(new DomainInfo[] {}));
        try {
            Dialog.getInstance().showDialog(d);
            DomainInfo di = d.getSelectedItem();
            AccountUsageRule rule = new AccountUsageRule(di.getTld());
            rule.setEnabled(true);
            HosterRuleController.getInstance().add(rule);
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
    }

    protected ArrayList<DomainInfo> getAvailableDomainInfoList() {
        final HashSet<DomainInfo> domains = new HashSet<DomainInfo>();
        for (Account acc : AccountController.getInstance().list()) {
            final AccountInfo ai = acc.getAccountInfo();
            if (ai != null) {
                final Object supportedHosts = ai.getProperty("multiHostSupport", Property.NULL);
                if (supportedHosts != null && supportedHosts instanceof List) {
                    for (Object supportedHost : (List<?>) supportedHosts) {
                        if (supportedHost != null && supportedHost instanceof String) {
                            final LazyHostPlugin plg = HostPluginController.getInstance().get((String) supportedHost);
                            if (plg != null && !plg.getClassName().endsWith("r.Offline")) {
                                domains.add(DomainInfo.getInstance(plg.getHost()));
                            }
                        }
                    }
                }
            }
            if (PluginForHost.implementsHandlePremium(acc.getPlugin())) {
                domains.add(DomainInfo.getInstance(acc.getHoster()));
            }
        }
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
        return _GUI._.NewRuleAction_getTooltipText_tt_();
    }

}
