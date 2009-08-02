package jd.gui.swing.components;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import jd.HostPluginWrapper;
import jd.JDInit;
import jd.gui.UserIO;
import jd.gui.swing.components.linkbutton.JLink;
import jd.gui.swing.dialog.AbstractDialog;
import jd.nutils.JDFlags;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class AccountDialog extends AbstractDialog implements ItemListener {

    public static void showDialog() {
        AccountDialog dialog = new AccountDialog();
        if (JDFlags.hasAllFlags(dialog.getReturnValue(), UserIO.RETURN_OK)) {
            // TODO: Account wurde mit OK bestätigt und nun können über getXYZ
            // die Daten geholt werden!
        }
    }

    public static void main(String[] args) {
        new JDInit().loadPluginForHost();
        new AccountDialog();
    }

    private static final long serialVersionUID = -2099080199110932990L;

    private static final String JDL_PREFIX = "jd.gui.swing.components.AccountDialog.";

    private JComboBox hoster;

    private JLink link;

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
        panel.add(hoster = new JComboBox(plugins.toArray(new HostPluginWrapper[plugins.size()])));
        hoster.addItemListener(this);
        panel.add(link = new JLink(JDL.L(JDL_PREFIX + "buy", "Buy Account")), "wrap");
        itemStateChanged(null);

        panel.add(new JLabel(JDL.L(JDL_PREFIX + "name", "Name:")));
        panel.add(name = new JTextField(), "w 150");
        panel.add(new JLabel(JDL.L(JDL_PREFIX + "pass", "Pass:")));
        panel.add(pass = new JPasswordField(), "w 150");

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

    public void itemStateChanged(ItemEvent arg0) {
        try {
            String agb = getHoster().getPlugin().getAGBLink();
            if (agb != null) {
                link.setUrl(new URL(agb));
            }
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }
        link.setEnabled(false);
    }

}
