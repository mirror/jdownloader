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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.views.settings.JDLabelListRenderer;
import jd.nutils.JDFlags;
import jd.plugins.Account;
import jd.plugins.PluginForHost;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;

public class AccountDialog extends AbstractDialog<Integer> {

    private static final long   serialVersionUID = -2099080199110932990L;

    private static final String JDL_PREFIX       = "jd.gui.swing.components.AccountDialog.";

    public static void showDialog(final PluginForHost pluginForHost) {
        final AccountDialog dialog = new AccountDialog(pluginForHost);

        if (JDFlags.hasAllFlags(Dialog.getInstance().showDialog(dialog), Dialog.RETURN_OK)) {
            final Account ac = new Account(dialog.getUsername(), dialog.getPassword());
            AccountController.getInstance().addAccount(dialog.getHoster().getPlugin(), ac);
        }
    }

    private JComboBox           hoster;

    private JTextField          name;

    private JPasswordField      pass;

    private final PluginForHost plugin;

    private AccountDialog(final PluginForHost plugin) {
        super(UserIO.NO_COUNTDOWN | UserIO.NO_ICON, JDL.L(AccountDialog.JDL_PREFIX + "title", "Add new Account"), null, null, null);

        this.plugin = plugin;
    }

    @Override
    protected Integer createReturnValue() {
        return this.getReturnmask();
    }

    public HostPluginWrapper getHoster() {
        return (HostPluginWrapper) this.hoster.getSelectedItem();
    }

    public String getPassword() {
        return new String(this.pass.getPassword());
    }

    public String getUsername() {
        return this.name.getText();
    }

    @Override
    public JComponent layoutDialogContent() {
        final ArrayList<HostPluginWrapper> plugins = JDUtilities.getPremiumPluginsForHost();
        Collections.sort(plugins, new Comparator<HostPluginWrapper>() {
            public int compare(final HostPluginWrapper a, final HostPluginWrapper b) {
                return a.getHost().compareToIgnoreCase(b.getHost());
            }
        });
        final HostPluginWrapper[] array = plugins.toArray(new HostPluginWrapper[plugins.size()]);
        this.hoster = new JComboBox(array);
        if (this.plugin != null) {
            try {
                this.hoster.setSelectedItem(this.plugin.getWrapper());
            } catch (final Exception e) {
            }
        }
        this.hoster.setRenderer(new JDLabelListRenderer());

        final JButton link = new JButton(JDTheme.II("gui.images.buy", 16, 16));
        link.setToolTipText(JDL.L("gui.menu.action.premium.buy.name", "action.premium.buy"));
        link.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                ActionController.getToolBarAction("action.premium.buy").actionPerformed(new ActionEvent(AccountDialog.this.getHoster(), 0, "buyaccount"));
            }
        });

        final JPanel panel = new JPanel(new MigLayout("ins 0, wrap 2", "[][grow,fill]"));
        panel.add(new JLabel(JDL.L(AccountDialog.JDL_PREFIX + "hoster", "Hoster:")));
        panel.add(this.hoster, "split 2");
        panel.add(link);

        panel.add(new JLabel(JDL.L(AccountDialog.JDL_PREFIX + "name", "Name:")));
        panel.add(this.name = new JTextField());

        panel.add(new JLabel(JDL.L(AccountDialog.JDL_PREFIX + "pass", "Pass:")));
        panel.add(this.pass = new JPasswordField());
        return panel;
    }

}
