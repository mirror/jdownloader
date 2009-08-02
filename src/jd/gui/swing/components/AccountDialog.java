package jd.gui.swing.components;

import java.util.ArrayList;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import jd.HostPluginWrapper;
import jd.gui.UserIO;
import jd.gui.swing.dialog.AbstractDialog;
import jd.nutils.JDFlags;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class AccountDialog extends AbstractDialog {

    public static void showDialog() {
        AccountDialog dialog = new AccountDialog();
        if (JDFlags.hasAllFlags(dialog.getReturnValue(), UserIO.RETURN_OK)) {
            // TODO: Account wurde mit OK bestätigt und nun können über getXYZ
            // die Daten geholt werden!
        }
    }

    private static final long serialVersionUID = -2099080199110932990L;

    private static final String JDL_PREFIX = "jd.gui.swing.components.AccountDialog.";

    private JComboBox hoster;

    private JTextField name;

    private JPasswordField pass;

    public AccountDialog() {
        super(UserIO.NO_COUNTDOWN, JDL.L(JDL_PREFIX + "title", "Add new Account"), UserIO.getInstance().getIcon(UserIO.ICON_QUESTION), null, null);

        init();
    }

    @Override
    public JComponent contentInit() {
        JPanel panel = new JPanel(new MigLayout("ins 0"));

        panel.add(new JLabel(JDL.L(JDL_PREFIX + "hoster", "Hoster:")));
        ArrayList<HostPluginWrapper> plugins = JDUtilities.getPremiumPluginsForHost();
        panel.add(hoster = new JComboBox(plugins.toArray(new HostPluginWrapper[plugins.size()])), "wrap");

        panel.add(new JLabel(JDL.L(JDL_PREFIX + "name", "Name:")));
        panel.add(name = new JTextField());
        panel.add(new JLabel(JDL.L(JDL_PREFIX + "pass", "Pass:")));
        panel.add(pass = new JPasswordField());

        return panel;
    }

    public HostPluginWrapper getHoster() {
        return (HostPluginWrapper) hoster.getSelectedItem();
    }

    public String getUsername() {
        return name.getText();
    }

    public String getPassword() {
        return new String(pass.getPassword());
    }

}
