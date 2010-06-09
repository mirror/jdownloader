//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.swing.dialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

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
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.views.settings.JDLabelListRenderer;
import jd.nutils.JDFlags;
import jd.plugins.Account;
import jd.plugins.PluginForHost;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class AccountDialog extends AbstractDialog {

    public static void showDialog(final PluginForHost pluginForHost) {
        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                AccountDialog dialog = new AccountDialog(pluginForHost);
                if (JDFlags.hasAllFlags(dialog.getReturnValue(), UserIO.RETURN_OK)) {
                    Account ac = new Account(dialog.getUsername(), dialog.getPassword());
                    AccountController.getInstance().addAccount(dialog.getHoster().getPlugin(), ac);
                }
                return null;
            }

        }.start();
    }

    private static final long serialVersionUID = -2099080199110932990L;

    private static final String JDL_PREFIX = "jd.gui.swing.components.AccountDialog.";

    private JComboBox hoster;

    private JButton link;

    private JTextField name;

    private JPasswordField pass;

    private PluginForHost plugin;

    public AccountDialog(PluginForHost pluginForHost) {
        super(UserIO.NO_COUNTDOWN | UserIO.NO_ICON, JDL.L(JDL_PREFIX + "title", "Add new Account"), JDTheme.II("gui.images.premium", 16, 16), null, null);
        this.plugin = pluginForHost;
        init();
    }

    @Override
    public JComponent contentInit() {
        JPanel panel = new JPanel(new MigLayout("ins 0, wrap 2"));
        panel.add(new JLabel(JDL.L(JDL_PREFIX + "hoster", "Hoster:")));
        ArrayList<HostPluginWrapper> plugins = JDUtilities.getPremiumPluginsForHost();
        Collections.sort(plugins, new Comparator<HostPluginWrapper>() {
            public int compare(HostPluginWrapper a, HostPluginWrapper b) {
                return a.getHost().compareToIgnoreCase(b.getHost());
            }
        });
        HostPluginWrapper[] array = plugins.toArray(new HostPluginWrapper[plugins.size()]);
        panel.add(hoster = new JComboBox(array), "w 200!");
        if (plugin != null) {
            try {
                hoster.setSelectedItem(plugin.getWrapper());
            } catch (Exception e) {
            }
        }
        hoster.setRenderer(new JDLabelListRenderer());

        panel.add(link = new JButton(ActionController.getToolBarAction("action.premium.buy")), "skip, w 200!");
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
