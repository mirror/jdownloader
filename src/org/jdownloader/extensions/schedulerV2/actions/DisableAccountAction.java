package org.jdownloader.extensions.schedulerV2.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JLabel;

import jd.controlling.AccountController;
import jd.gui.swing.jdgui.views.settings.components.ComboBox;
import jd.plugins.Account;

import org.jdownloader.extensions.schedulerV2.translate.T;

@ScheduleActionIDAnnotation("DISABLE_ACCOUNT")
public class DisableAccountAction extends AbstractScheduleAction<AccountActionConfig> {

    private final ComboBox<Account> cbAccounts = new ComboBox<Account>();
    private final JLabel            noAccLabel = new JLabel(T._.addScheduleEntryDialog_noAccount());
    protected boolean               setBoolean = false;
    private AtomicBoolean           update     = new AtomicBoolean(false);

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
                acc.setEnabled(setBoolean);
            }
        }
    }

    @Override
    protected void createPanel() {
        panel.put(new JLabel(T._.addScheduleEntryDialog_account() + ":"), "gapleft 10,");

        panel.put(cbAccounts, "");
        panel.put(noAccLabel, "");

        updateAccounts();
        cbAccounts.setRenderer(new AccountListRenderer());
        cbAccounts.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                if (cbAccounts.getSelectedItem() != null && !update.get()) {
                    getConfig().setHoster(cbAccounts.getSelectedItem().getHoster());
                    getConfig().setUser(cbAccounts.getSelectedItem().getUser());
                }
            }
        });

    };

    @Override
    public String getReadableParameter() {
        if (getConfig() == null) {
            return "?";
        }
        return getConfig().getHoster() + ": " + getConfig().getUser();
    }

    @Override
    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag);
        if (aFlag) {
            updateAccounts();
        }
    }

    private void updateAccounts() {

        List<Account> accs = AccountController.getInstance().list(null);
        if (accs == null || accs.size() == 0) {
            noAccLabel.setVisible(true);
            cbAccounts.setVisible(false);
        }
        update.set(true);
        cbAccounts.removeAllItems();
        for (Account acc : accs) {
            cbAccounts.addItem(acc);
        }

        // set default host
        if (getConfig() != null && getConfig().getHoster().length() > 0) {
            for (Account acc : accs) {
                if (acc.getHoster().equals(getConfig().getHoster()) && acc.getUser().equals(getConfig().getUser())) {
                    // we use user and hoster to compare, because if an account is re-added, rule should still be valid
                    cbAccounts.setSelectedItem(acc);
                }
            }
        }
        update.set(false);

        noAccLabel.setVisible(false);
        cbAccounts.setVisible(true);
    }
}
