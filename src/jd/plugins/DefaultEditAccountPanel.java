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

import jd.http.Cookies;

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
    private static String                       EMPTYPW                     = "                 ";
    private JLabel                              usernameLabel               = null;
    private JLabel                              passwordLabel               = null;
    private boolean                             allowCookiesInPasswordField = false;

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
        this(callback, true, true);
    }

    public DefaultEditAccountPanel(final InputChangedCallbackInterface callback, boolean requiresUserName, final boolean allowCookieLogin) {
        super("ins 0, wrap 2", "[][grow,fill]", "");
        this.callback = callback;
        if (requiresUserName) {
            add(usernameLabel = new JLabel(_GUI.T.jd_gui_swing_components_AccountDialog_name()));
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
            name.setHelpText(_GUI.T.jd_gui_swing_components_AccountDialog_help_username());
        } else {
            name = null;
        }
        add(passwordLabel = new JLabel(_GUI.T.jd_gui_swing_components_AccountDialog_pass()));
        add(this.pass = new ExtPasswordField() {
            @Override
            public void onChanged() {
                callback.onChangedInput(pass);
            }

            {
                final HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(Color.yellow);
                addTextHighlighter(new ExtTextHighlighter(painter, Pattern.compile("^(\\s+)")));
                addTextHighlighter(new ExtTextHighlighter(painter, Pattern.compile("(\\s+)$")));
                applyTextHighlighter(null);
            }
        }, "");
        if (allowCookieLogin) {
            pass.setHelpText(_GUI.T.BuyAndAddPremiumAccount_layoutDialogContent_pass_or_cookies());
        } else {
            pass.setHelpText(_GUI.T.BuyAndAddPremiumAccount_layoutDialogContent_pass());
        }
        final ExtTextField dummy = new ExtTextField();
        dummy.paste();
        final String clipboard = dummy.getText();
        if (StringUtils.isNotEmpty(clipboard)) {
            /* Automatically put exported cookies json string into password field in case that's the current clipboard content. */
            if (allowCookieLogin && Cookies.parseCookiesFromJsonString(clipboard, null) != null) {
                /*
                 * Cookie login is supported and users' clipboard contains exported cookies at this moment -> Auto-fill password field with
                 * them.
                 */
                pass.setPassword(clipboard.toCharArray());
            } else if (name != null) {
                /* Auto fill username field with clipboard content if username is needed. */
                name.setText(clipboard);
            }
        }
        allowCookiesInPasswordField = allowCookieLogin;
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
        final boolean userok;
        final boolean passok;
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
        final String pw = getPassword();
        final Cookies cookies = Cookies.parseCookiesFromJsonString(pw);
        if (StringUtils.isEmpty(pw)) {
            passok = false;
        } else if (cookies != null && !allowCookiesInPasswordField) {
            /* Cookie login is not allowed but user has entered exported cookies into password field. */
            passok = false;
        } else {
            passok = true;
        }
        if (!passok) {
            passwordLabel.setForeground(Color.RED);
        } else {
            passwordLabel.setForeground(Color.BLACK);
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
