package jd.plugins;

import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.utils.StringUtils;
import org.jdownloader.gui.InputChangedCallbackInterface;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.accounts.AccountBuilderInterface;

import jd.gui.swing.components.linkbutton.JLink;

/** Use this for plugins which need API key login instead of username/password. */
public class DefaultEditAccountPanelAPIKeyLogin extends MigPanel implements AccountBuilderInterface {
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

    private final ExtPasswordField pass;
    private final JLabel           idLabel;
    private final PluginForHost    plg;

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

    public DefaultEditAccountPanelAPIKeyLogin(final InputChangedCallbackInterface callback, final PluginForHost plg) {
        super("ins 0, wrap 2", "[][grow,fill]", "");
        this.plg = plg;
        add(new JLabel(_GUI.T.jd_gui_swing_components_AccountDialog_generic_instructions()));
        add(new JLink(_GUI.T.jd_gui_swing_components_AccountDialog_generic_instructions_click_here_for_instructions(), plg.getAPILoginHelpURL()));
        this.add(this.idLabel = new JLabel(_GUI.T.jd_gui_swing_components_AccountDialog_api_key()));
        add(this.pass = new ExtPasswordField() {
            @Override
            public void onChanged() {
                callback.onChangedInput(this);
            }
        }, "");
        pass.setHelpText(_GUI.T.jd_gui_swing_components_AccountDialog_api_key_help());
    }

    public void setAccount(final Account defaultAccount) {
        if (defaultAccount != null) {
            pass.setText(defaultAccount.getPass());
        }
    }

    @Override
    public boolean validateInputs() {
        final String password = getPassword();
        if (this.plg.looksLikeValidAPIKey(password)) {
            idLabel.setForeground(Color.BLACK);
            return true;
        } else {
            idLabel.setForeground(Color.RED);
            return false;
        }
    }

    @Override
    public Account getAccount() {
        return new Account(null, getPassword());
    }

    @Override
    public JComponent getComponent() {
        return this;
    }
}
