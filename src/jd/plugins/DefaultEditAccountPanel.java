package jd.plugins;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.StringUtils;
import org.jdownloader.gui.InputChangedCallbackInterface;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.accounts.AccountBuilderInterface;

public class DefaultEditAccountPanel extends MigPanel implements AccountBuilderInterface {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    protected String getPassword() {
        if (this.pass == null) {
            return null;
        }
        if (EMPTYPW.equals(new String(this.pass.getPassword()))) {
            return null;
        }
        return new String(this.pass.getPassword());
    }

    protected String getUsername() {
        if (name == null) {
            return "";
        } else {
            if (_GUI.T.jd_gui_swing_components_AccountDialog_help_username().equals(this.name.getText())) {
                return null;
            }
            return this.name.getText();
        }
    }

    private final ExtTextField                  name;
    private final ExtPasswordField              pass;
    private final InputChangedCallbackInterface callback;
    private static String                       EMPTYPW = "                 ";

    public boolean updateAccount(Account input, Account output) {
        boolean changed = false;
        if (!StringUtils.equals(input.getUser(), output.getUser())) {
            output.setUser(input.getUser());
            changed = true;
        }
        if (!StringUtils.equals(input.getPass(), output.getPass())) {
            output.setPass(input.getPass());
            changed = true;
        }
        return changed;
    }

    public DefaultEditAccountPanel(final InputChangedCallbackInterface callback) {
        this(callback, true);
    }

    public DefaultEditAccountPanel(final InputChangedCallbackInterface callback, boolean requiresUserName) {
        super("ins 0, wrap 2", "[][grow,fill]", "");
        this.callback = callback;
        if (requiresUserName) {
            add(new JLabel(_GUI.T.jd_gui_swing_components_AccountDialog_name()));
            add(this.name = new ExtTextField() {
                @Override
                public void onChanged() {
                    callback.onChangedInput(name);
                }
            });
            name.setHelpText(_GUI.T.jd_gui_swing_components_AccountDialog_help_username());
            name.paste();
        } else {
            name = null;
        }
        add(new JLabel(_GUI.T.jd_gui_swing_components_AccountDialog_pass()));
        add(this.pass = new ExtPasswordField() {
            @Override
            public void onChanged() {
                callback.onChangedInput(pass);
            }
        }, "");
        pass.setHelpText(_GUI.T.BuyAndAddPremiumAccount_layoutDialogContent_pass());
    }

    public InputChangedCallbackInterface getCallback() {
        return callback;
    }

    public void setAccount(Account defaultAccount) {
        if (defaultAccount != null) {
            if (name != null) {
                name.setText(defaultAccount.getUser());
            }
            pass.setText(defaultAccount.getPass());
        }
    }

    @Override
    public boolean validateInputs() {
        if (name == null) {
            return StringUtils.isAllNotEmpty(getPassword());
        } else {
            return StringUtils.isAllNotEmpty(getPassword(), getUsername());
        }
    }

    @Override
    public Account getAccount() {
        return new Account(getUsername(), getPassword());
    }

    @Override
    public JComponent getComponent() {
        return this;
    }
}
