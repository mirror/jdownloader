package jd.plugins;

import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.StringUtils;
import org.jdownloader.gui.InputChangedCallbackInterface;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.accounts.AccountBuilderInterface;

import jd.gui.swing.components.linkbutton.JLink;
import jd.http.Cookies;

public class DefaultEditAccountPanelCookieLogin extends MigPanel implements AccountBuilderInterface {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    protected String getPassword() {
        if (this.pass == null) {
            return null;
        } else {
            return new String(this.pass.getPassword());
        }
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
    private JLabel                              usernameLabel = null;
    private final JLabel                        passwordCookiesLabel;

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

    public DefaultEditAccountPanelCookieLogin(final InputChangedCallbackInterface callback) {
        super("ins 0, wrap 2", "[][grow,fill]", "");
        this.callback = callback;
        add(new JLabel("Click here to get help:"));
        add(new JLink("https://support.jdownloader.org/Knowledgebase/Article/View/account-cookie-login-instructions"));
        add(usernameLabel = new JLabel(_GUI.T.jd_gui_swing_components_AccountDialog_name()));
        add(this.name = new ExtTextField() {
            @Override
            public void onChanged() {
                callback.onChangedInput(name);
            }
            // {
            // final HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(Color.yellow);
            // addTextHighlighter(new ExtTextHighlighter(painter, Pattern.compile("^(\\s+)")));
            // addTextHighlighter(new ExtTextHighlighter(painter, Pattern.compile("(\\s+)$")));
            // refreshTextHighlighter();
            // }
        });
        name.setHelpText(_GUI.T.jd_gui_swing_components_AccountDialog_help_username());
        add(passwordCookiesLabel = new JLabel("Exported cookies:"));
        add(this.pass = new ExtPasswordField() {
            @Override
            public void onChanged() {
                callback.onChangedInput(pass);
            }
            // {
            // final HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(Color.yellow);
            // addTextHighlighter(new ExtTextHighlighter(painter, Pattern.compile("^(\\s+)")));
            // addTextHighlighter(new ExtTextHighlighter(painter, Pattern.compile("(\\s+)$")));
            // applyTextHighlighter(null);
            // }
        }, "");
        pass.setHelpText(_GUI.T.BuyAndAddPremiumAccount_layoutDialogContent_cookies());
        final ExtTextField dummy = new ExtTextField();
        dummy.paste();
        final String clipboard = dummy.getText();
        if (StringUtils.isNotEmpty(clipboard)) {
            /* Automatically put exported cookies json string into password field in case that's the current clipboard content. */
            if (Cookies.parseCookiesFromJsonString(clipboard, null) != null) {
                pass.setPassword(clipboard.toCharArray());
            } else if (name != null) {
                name.setText(clipboard);
            }
        }
    }

    public InputChangedCallbackInterface getCallback() {
        return callback;
    }

    public void setAccount(final Account defaultAccount) {
        if (defaultAccount != null) {
            if (name != null) {
                name.setText(defaultAccount.getUser());
            }
            pass.setText(defaultAccount.getPass());
        }
    }

    @Override
    public boolean validateInputs() {
        final String pw = getPassword();
        final Cookies cookies = Cookies.parseCookiesFromJsonString(pw);
        final boolean userok;
        final boolean passok;
        if (cookies == null) {
            passwordCookiesLabel.setForeground(Color.RED);
            passok = false;
        } else {
            passwordCookiesLabel.setForeground(Color.BLACK);
            passok = true;
        }
        if (this.name != null) {
            if (StringUtils.isEmpty(this.getUsername())) {
                usernameLabel.setForeground(Color.RED);
                userok = false;
            } else {
                usernameLabel.setForeground(Color.BLACK);
                userok = true;
            }
        } else {
            /* No username needed */
            userok = true;
        }
        if (userok && passok) {
            return true;
        } else {
            return false;
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
