package jd.plugins;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.swing.components.ExtTextField;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.accounts.EditAccountPanel;
import org.jdownloader.plugins.accounts.Notifier;

public class DefaultEditAccountPanel extends MigPanel implements EditAccountPanel {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private String getPassword() {
        if (this.pass == null) return null;
        if (EMPTYPW.equals(new String(this.pass.getPassword()))) return null;
        return new String(this.pass.getPassword());
    }

    private String getUsername() {
        if (_GUI._.jd_gui_swing_components_AccountDialog_help_username().equals(this.name.getText())) return null;
        return this.name.getText();
    }

    private ExtTextField  name;

    ExtPasswordField      pass;

    Notifier              notifier;
    private static String EMPTYPW = "                 ";

    public DefaultEditAccountPanel() {
        super("ins 0, wrap 2", "[][grow,fill]", "");

        add(new JLabel(_GUI._.jd_gui_swing_components_AccountDialog_name()));
        add(this.name = new ExtTextField() {

            @Override
            public void onChanged() {
                if (notifier != null) {
                    notifier.onNotify();
                }
            }

        });

        name.setHelpText(_GUI._.jd_gui_swing_components_AccountDialog_help_username());

        add(new JLabel(_GUI._.jd_gui_swing_components_AccountDialog_pass()));
        add(this.pass = new ExtPasswordField() {

            @Override
            public void onChanged() {
                if (notifier != null) {
                    notifier.onNotify();
                }
            }

        }, "");
        pass.setHelpText(_GUI._.BuyAndAddPremiumAccount_layoutDialogContent_pass());
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    @Override
    public void setAccount(Account defaultAccount) {
        if (defaultAccount != null) {

            name.setText(defaultAccount.getUser());
            pass.setText(defaultAccount.getPass());
        }

    }

    @Override
    public boolean validateInputs() {
        return getPassword() != null || getUsername() != null;
    }

    @Override
    public void setNotifyCallBack(Notifier notifier) {
        this.notifier = notifier;
    }

    @Override
    public Account getAccount() {
        return new Account(getUsername(), getPassword());
    }

}
