package jd.plugins;

import java.awt.Color;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter.HighlightPainter;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.swing.components.ExtTextField;
import org.appwork.swing.components.ExtTextHighlighter;
import org.appwork.utils.StringUtils;
import org.jdownloader.gui.InputChangedCallbackInterface;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.accounts.AccountBuilderInterface;
import org.jdownloader.plugins.controller.LazyPlugin.FEATURE;

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
    private final JLabel                        passwordOrCookiesLabel;
    private final PluginForHost                 plg;
    private final boolean                       usernameIsEmail;
    private final boolean                       cookieLoginOnly;
    private final boolean                       cookieLoginOptional;

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

    public DefaultEditAccountPanelCookieLogin(final InputChangedCallbackInterface callback, final PluginForHost plg) {
        super("ins 0, wrap 2", "[][grow,fill]", "");
        this.plg = plg;
        this.callback = callback;
        this.usernameIsEmail = this.plg.hasFeature(FEATURE.USERNAME_IS_EMAIL);
        this.cookieLoginOnly = this.plg.hasFeature(FEATURE.COOKIE_LOGIN_ONLY);
        this.cookieLoginOptional = this.plg.hasFeature(FEATURE.COOKIE_LOGIN_OPTIONAL);
        if (cookieLoginOnly) {
            // TODO: Add translation
            add(new JLabel(_GUI.T.jd_gui_swing_components_AccountDialog_generic_instructions()));
            add(new JLink(_GUI.T.jd_gui_swing_components_AccountDialog_generic_instructions_click_here_for_instructions(), "https://support.jdownloader.org/Knowledgebase/Article/View/account-cookie-login-instructions"));
        }
        if (this.usernameIsEmail) {
            add(usernameLabel = new JLabel(_GUI.T.jd_gui_swing_components_AccountDialog_email()));
        } else {
            add(usernameLabel = new JLabel(_GUI.T.jd_gui_swing_components_AccountDialog_name()));
        }
        add(this.name = new ExtTextField() {
            @Override
            public void onChanged() {
                callback.onChangedInput(name);
            }

            {
                final HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(Color.yellow);
                addTextHighlighter(new ExtTextHighlighter(painter, Pattern.compile("^(\\s+)")));
                addTextHighlighter(new ExtTextHighlighter(painter, Pattern.compile("(\\s+)$")));
                refreshTextHighlighter();
            }
        });
        if (this.usernameIsEmail) {
            name.setHelpText(_GUI.T.jd_gui_swing_components_AccountDialog_help_email());
        } else {
            name.setHelpText(_GUI.T.jd_gui_swing_components_AccountDialog_help_username());
        }
        if (cookieLoginOnly) {
            add(passwordOrCookiesLabel = new JLabel(_GUI.T.jd_gui_swing_components_AccountDialog_cookies()));
        } else if (cookieLoginOptional) {
            add(passwordOrCookiesLabel = new JLabel(_GUI.T.jd_gui_swing_components_AccountDialog_pass_or_cookies()));
        } else {
            /* Normal username & password login */
            add(passwordOrCookiesLabel = new JLabel(_GUI.T.jd_gui_swing_components_AccountDialog_pass()));
        }
        add(this.pass = new ExtPasswordField() {
            @Override
            public void onChanged() {
                callback.onChangedInput(pass);
            }
            /* Highlighter doesn't make any sense here as user can't see password anyways. */
            // {
            // final HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(Color.yellow);
            // addTextHighlighter(new ExtTextHighlighter(painter, Pattern.compile("^(\\s+)")));
            // addTextHighlighter(new ExtTextHighlighter(painter, Pattern.compile("(\\s+)$")));
            // applyTextHighlighter(null);
            // }
        }, "");
        if (cookieLoginOnly) {
            pass.setHelpText(_GUI.T.BuyAndAddPremiumAccount_layoutDialogContent_cookies());
        } else if (cookieLoginOptional) {
            pass.setHelpText(_GUI.T.BuyAndAddPremiumAccount_layoutDialogContent_pass_or_cookies());
        } else {
            /* Normal username & password login */
            pass.setHelpText(_GUI.T.BuyAndAddPremiumAccount_layoutDialogContent_pass());
        }
        final ExtTextField dummy = new ExtTextField();
        dummy.paste();
        final String clipboard = dummy.getText();
        if (StringUtils.isNotEmpty(clipboard)) {
            /* Automatically put exported cookies json string into password field in case that's the current clipboard content. */
            final Cookies userCookies = Cookies.parseCookiesFromJsonString(clipboard, null);
            if ((cookieLoginOnly || cookieLoginOptional) && userCookies != null) {
                /*
                 * Cookie login is supported and users' clipboard contains exported cookies at this moment -> Auto-fill password field with
                 * them.
                 */
                pass.setPassword(clipboard.toCharArray());
            } else if (userCookies == null && clipboard.trim().length() > 0) {
                /* Auto fill username field with clipboard content. */
                name.setText(clipboard);
            }
        }
    }

    public InputChangedCallbackInterface getCallback() {
        return callback;
    }

    public void setAccount(final Account defaultAccount) {
        if (defaultAccount != null) {
            name.setText(defaultAccount.getUser());
            pass.setText(defaultAccount.getPass());
        }
    }

    @Override
    public boolean validateInputs() {
        final boolean userok;
        final boolean passok;
        if (StringUtils.isEmpty(this.getUsername())) {
            usernameLabel.setForeground(Color.RED);
            userok = false;
        } else if (this.usernameIsEmail && !PluginForHost.looksLikeValidEmailAddress(this.getUsername())) {
            /* E-Mail is needed but user did not enter a valid-looking e-mail address. */
            usernameLabel.setForeground(Color.RED);
            userok = false;
        } else {
            usernameLabel.setForeground(Color.BLACK);
            userok = true;
        }
        final String pw = getPassword();
        final Cookies cookies = Cookies.parseCookiesFromJsonString(pw);
        if (StringUtils.isEmpty(pw)) {
            /* Password field is never allowed to be empty/null. */
            passok = false;
        } else if (cookieLoginOnly && cookies == null) {
            /* Cookies are needed but not given. */
            passok = false;
        } else if (!cookieLoginOnly && !cookieLoginOptional && cookies != null) {
            /* Cookies are given while user is not allowed to use cookies. */
            passok = false;
        } else {
            passok = true;
        }
        if (!passok) {
            passwordOrCookiesLabel.setForeground(Color.RED);
        } else {
            passwordOrCookiesLabel.setForeground(Color.BLACK);
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
