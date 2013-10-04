package jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import jd.config.Property;
import jd.controlling.AccountController;
import jd.plugins.Account;
import jd.plugins.AccountInfo;

import org.appwork.utils.locale._AWU;
import org.appwork.utils.swing.dialog.ComboBoxDialog;
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
        ComboBoxDialog d = new ComboBoxDialog(0, _GUI._.NewRuleAction_actionPerformed_choose_hoster_(), _GUI._.NewRuleAction_actionPerformed_choose_hoster_message(), list.toArray(new DomainInfo[] {}), 0, null, _AWU.T.lit_continue(), null, null) {
            protected ListCellRenderer getRenderer(final ListCellRenderer orgRenderer) {
                // TODO Auto-generated method stub
                return new ListCellRenderer() {

                    @Override
                    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                        if (value == null) return (JLabel) orgRenderer.getListCellRendererComponent(list, "", index, isSelected, cellHasFocus);

                        try {

                            JLabel ret = (JLabel) orgRenderer.getListCellRendererComponent(list, ((DomainInfo) value).getTld(), index, isSelected, cellHasFocus);
                            ret.setIcon(((DomainInfo) value).getFavIcon());

                            return ret;
                        } catch (Exception e) {
                            e.printStackTrace();
                            JLabel ret = (JLabel) orgRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                            return ret;
                        }
                    }
                };
            }
        };

        try {
            Dialog.getInstance().showDialog(d);
            DomainInfo di = list.get(d.getSelectedIndex());
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
        HashSet<DomainInfo> domains = new HashSet<DomainInfo>();
        HashSet<String> plugins = new HashSet<String>();
        final AtomicBoolean refreshRequired = new AtomicBoolean();
        for (Account acc : AccountController.getInstance().list()) {

            AccountInfo ai = acc.getAccountInfo();
            if (ai != null) {
                Object supported = ai.getProperty("multiHostSupport", Property.NULL);
                if (supported != null && supported instanceof List) {
                    for (Object support : (List<?>) supported) {
                        if (support instanceof String) {

                            LazyHostPlugin plg = HostPluginController.getInstance().get((String) support);
                            if (plg != null && plugins.add(plg.getClassname())) {
                                domains.add(DomainInfo.getInstance(plg.getHost()));
                            }
                        }
                    }
                } else {
                    domains.add(DomainInfo.getInstance(acc.getHoster()));
                }
            } else {
                refreshRequired.set(true);
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
