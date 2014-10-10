package org.jdownloader.extensions.schedulerV2.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;

import jd.controlling.AccountController;
import jd.gui.swing.jdgui.views.settings.components.ComboBox;
import jd.plugins.Account;

import org.jdownloader.extensions.schedulerV2.translate.T;

@ScheduleActionIDAnnotation("DISABLE_ACCOUNT")
public class DisableAccountAction extends AbstractScheduleAction<AccountActionConfig> {

    public DisableAccountAction(String configJson) {
        super(configJson);
    }

    @Override
    public String getReadableName() {
        return T._.action_disableAccount();
    }

    @Override
    public void execute() {
        ArrayList<Account> accounts = AccountController.getInstance().list(getConfig().getHoster());
        for (Account acc : accounts) {
            if (acc.getUser().equals(getConfig().getUser())) {
                // we use user and hoster to compare, because if an account is re-added, rule should still be valid
                acc.setEnabled(false);
            }
        }
    }

    @Override
    protected void createPanel() {
        panel.put(new JLabel(T._.addScheduleEntryDialog_account() + ":"), "gapleft 10,");

        List<Account> accs = AccountController.getInstance().list(null);
        if (accs == null || accs.size() == 0) {
            panel.put(new JLabel(T._.addScheduleEntryDialog_noAccount()), "");
            return;
        }

        final ComboBox<Account> cbAccounts = new ComboBox<Account>(accs.toArray(new Account[accs.size()]));

        cbAccounts.setRenderer(new AccountListRenderer());

        if (getConfig().getHoster().length() > 0) {
            for (Account acc : accs) {
                if (acc.getHoster().equals(getConfig().getHoster()) && acc.getUser().equals(getConfig().getUser())) {
                    // we use user and hoster to compare, because if an account is re-added, rule should still be valid
                    cbAccounts.setSelectedItem(acc);
                }
            }
        }

        cbAccounts.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                getConfig().setHoster(cbAccounts.getSelectedItem().getHoster());
                getConfig().setUser(cbAccounts.getSelectedItem().getUser());
            }
        });

        panel.put(cbAccounts, "");
    };

    @Override
    public String getReadableParameter() {
        return getConfig().getHoster() + ": " + getConfig().getUser();
    }
}
