package jd.gui.swing.components;

import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import jd.HostPluginWrapper;
import jd.controlling.AccountController;
import jd.gui.UserIO;
import jd.gui.swing.dialog.AbstractDialog;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.nutils.JDFlags;
import jd.plugins.Account;
import jd.plugins.PluginForHost;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class AccountDialog extends AbstractDialog {

    public static void showDialog(PluginForHost pluginForHost) {
        AccountDialog dialog = new AccountDialog(pluginForHost);
        if (JDFlags.hasAllFlags(dialog.getReturnValue(), UserIO.RETURN_OK)) {
            Account ac = new Account(dialog.getUsername(), dialog.getPassword());
            AccountController.getInstance().addAccount(dialog.getHoster().getPlugin(), ac);
        }
    }

    private static final long serialVersionUID = -2099080199110932990L;

    private static final String JDL_PREFIX = "jd.gui.swing.components.AccountDialog.";

    private JComboBox hoster;

    private JButton link;

    private JTextField name;

    private JPasswordField pass;

    private PluginForHost plugin;

    public AccountDialog(PluginForHost pluginForHost) {
        super(UserIO.NO_COUNTDOWN, JDL.L(JDL_PREFIX + "title", "Add new Account"), JDTheme.II("gui.images.premium", 16, 16), null, null);
        this.plugin = pluginForHost;
        init();
    }

    @Override
    public JComponent contentInit() {
        JPanel panel = new JPanel(new MigLayout("ins 0, wrap 2"));
        panel.add(new JLabel(JDL.L(JDL_PREFIX + "hoster", "Hoster:")));
        ArrayList<HostPluginWrapper> plugins = JDUtilities.getPremiumPluginsForHost();
        HostPluginWrapper[] array = plugins.toArray(new HostPluginWrapper[plugins.size()]);
        panel.add(hoster = new JComboBox(array), "w 200!");
        if (plugin != null) {
            try {
                hoster.setSelectedItem(plugin.getWrapper());
            } catch (Exception e) {
            }
        }
        hoster.setRenderer(new IconListRenderer());

        panel.add(new JLabel());
        panel.add(link = new JButton(ActionController.getToolBarAction("action.premium.buy")), "w 200!");
        link.setIcon(JDTheme.II("gui.images.buy", 16, 16));

        panel.add(new JLabel(JDL.L(JDL_PREFIX + "name", "Name:")));
        panel.add(name = new JTextField(), "w 200!");

        panel.add(new JLabel(JDL.L(JDL_PREFIX + "pass", "Pass:")));
        panel.add(pass = new JPasswordField(), "w 200!");
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
