package org.jdownloader.plugins.components;

import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.JLabel;

import jd.plugins.Account;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.utils.StringUtils;
import org.jdownloader.gui.InputChangedCallbackInterface;
import org.jdownloader.plugins.accounts.AccountBuilderInterface;

public class FilebitNetAccountFactory extends MigPanel implements AccountBuilderInterface {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private final String      APIKEYHELP       = "Enter your Licence key";
    private final JLabel      apikeyLabel;

    String correctLicenseKey(final String key) {
        if (key == null) {
            return null;
        } else {
            return key.trim();
        }
    }

    boolean isLicenseKey(final String str) {
        if (str == null) {
            return false;
        } else if (str.matches("[A-Za-z0-9]+")) {
            return true;
        } else {
            return false;
        }
    }

    private String getPassword() {
        if (this.pass == null) {
            return null;
        } else {
            return correctLicenseKey(new String(this.pass.getPassword()));
        }
    }

    public boolean updateAccount(Account input, Account output) {
        if (!StringUtils.equals(input.getUser(), output.getUser())) {
            output.setUser(input.getUser());
            return true;
        } else if (!StringUtils.equals(input.getPass(), output.getPass())) {
            output.setPass(input.getPass());
            return true;
        } else {
            return false;
        }
    }

    private final ExtPasswordField pass;

    public FilebitNetAccountFactory(final InputChangedCallbackInterface callback) {
        super("ins 0, wrap 2", "[][grow,fill]", "");
        add(new JLabel("Enter license key."));
        add(new JLabel("You can find it in your PDF file."));
        add(apikeyLabel = new JLabel("Licence key:"));
        add(this.pass = new ExtPasswordField() {
            @Override
            public void onChanged() {
                callback.onChangedInput(this);
            }
        }, "");
        pass.setHelpText(APIKEYHELP);
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    @Override
    public void setAccount(Account defaultAccount) {
        if (defaultAccount != null) {
            // name.setText(defaultAccount.getUser());
            pass.setText(defaultAccount.getPass());
        }
    }

    @Override
    public boolean validateInputs() {
        final String pw = getPassword();
        if (isLicenseKey(pw)) {
            apikeyLabel.setForeground(Color.BLACK);
            return true;
        } else {
            apikeyLabel.setForeground(Color.RED);
            return false;
        }
    }

    @Override
    public Account getAccount() {
        return new Account(null, getPassword());
    }
}