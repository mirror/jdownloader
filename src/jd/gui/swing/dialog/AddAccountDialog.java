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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.controlling.AccountController;
import jd.controlling.accountchecker.AccountChecker;
import jd.controlling.accountchecker.AccountChecker.AccountCheckJob;
import jd.gui.UserIO;
import jd.plugins.Account;
import jd.plugins.PluginForHost;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.components.searchcombo.SearchComboBox;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging.Log;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.jdownloader.DomainInfo;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.accounts.AccountFactory;
import org.jdownloader.plugins.accounts.EditAccountPanel;
import org.jdownloader.plugins.accounts.Notifier;
import org.jdownloader.plugins.controller.PluginClassLoader;
import org.jdownloader.plugins.controller.UpdateRequiredClassNotFoundException;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.plugins.controller.host.PluginFinder;

public class AddAccountDialog extends AbstractDialog<Integer> {

    public static void showDialog(final PluginForHost pluginForHost, Account acc) {
        final AddAccountDialog dialog = new AddAccountDialog(pluginForHost, acc);

        try {
            Dialog.getInstance().showDialog(dialog);
            if (dialog.getHoster() == null) return;
            final Account ac = dialog.getAccount();
            ac.setHoster(dialog.getHoster().getDisplayName());

            if (!addAccount(ac)) {
                showDialog(dialog.getHoster().getPrototype(null), ac);
            }

        } catch (Throwable e) {
            Log.exception(e);
        }

    }

    private Account getAccount() {
        EditAccountPanel leditAccountPanel = editAccountPanel;
        if (leditAccountPanel != null) return leditAccountPanel.getAccount();
        return null;
    }

    public static boolean addAccount(final Account ac) throws DialogClosedException, DialogCanceledException {
        try {
            checkAccount(ac);
        } catch (DialogClosedException e) {
            throw e;
        } catch (DialogCanceledException e) {
            throw e;

        } catch (Throwable e) {
            Dialog.getInstance().showExceptionDialog(_GUI._.accountdialog_check_failed(), _GUI._.accountdialog_check_failed_msg(), e);
        }

        if (ac.getAccountInfo() != null) {
            if (ac.getAccountInfo().isExpired()) {
                Dialog.getInstance().showConfirmDialog(0, _GUI._.accountdialog_check_expired_title(), _GUI._.accountdialog_check_expired(ac.getAccountInfo().getStatus()), null, _GUI._.accountdialog_check_expired_renew(), null);
                AccountController.getInstance().addAccount(ac);
                return true;

            } else if (!ac.isValid()) {

                Dialog.getInstance().showMessageDialog(_GUI._.accountdialog_check_invalid(ac.getAccountInfo().getStatus()));
                return false;
            } else {
                Dialog.getInstance().showMessageDialog(_GUI._.accountdialog_check_valid(ac.getAccountInfo().getStatus()));
                AccountController.getInstance().addAccount(ac);
                return true;

            }
        } else {
            return false;
        }
    }

    public static ProgressDialog checkAccount(final Account ac) throws Throwable {
        ProgressDialog pd = new ProgressDialog(new ProgressGetter() {

            public void run() throws Exception {
                PluginForHost hostPlugin = new PluginFinder().assignPlugin(ac, true, LogController.CL());
                if (hostPlugin != null) ac.setPlugin(hostPlugin);
                AccountCheckJob job = AccountChecker.getInstance().check(ac, true);
                job.waitChecked();
            }

            public String getString() {
                return null;
            }

            public int getProgress() {
                return -1;
            }

            @Override
            public String getLabelString() {
                return null;
            }
        }, 0, _GUI._.accountdialog_check(), _GUI._.accountdialog_check_msg(), DomainInfo.getInstance(ac.getHoster()).getFavIcon());
        try {

            Dialog.getInstance().showDialog(pd);
        } catch (DialogCanceledException e) {
            if (pd.getThrowable() == null) {
                throw e;
            } else {
                throw pd.getThrowable();
            }

        }
        return pd;
    }

    private SearchComboBox<LazyHostPlugin> hoster;

    private PluginForHost                  plugin;

    private Account                        defaultAccount;

    private EditAccountPanel               editAccountPanel;

    private JPanel                         content;

    private AddAccountDialog(final PluginForHost plugin, Account acc) {
        super(UserIO.NO_ICON, _GUI._.jd_gui_swing_components_AccountDialog_title(), null, _GUI._.lit_save(), null);
        this.defaultAccount = acc;
        this.plugin = plugin;
    }

    @Override
    protected Integer createReturnValue() {
        return this.getReturnmask();

    }

    protected void initFocus(final JComponent focus) {
    }

    @Override
    public JComponent layoutDialogContent() {
        final Collection<LazyHostPlugin> allPLugins = HostPluginController.getInstance().list();
        // Filter - only premium plugins should be here
        final java.util.List<LazyHostPlugin> plugins = new ArrayList<LazyHostPlugin>();

        for (LazyHostPlugin lhp : allPLugins) {
            if (lhp.isPremium()) plugins.add(lhp);
        }

        if (plugins.size() == 0) throw new RuntimeException("No Plugins Loaded Exception");
        // final HostPluginWrapper[] array = plugins.toArray(new
        // HostPluginWrapper[plugins.size()]);

        hoster = new SearchComboBox<LazyHostPlugin>(plugins) {

            @Override
            protected Icon getIconForValue(LazyHostPlugin value) {
                if (value == null) return null;
                return DomainInfo.getInstance(value.getDisplayName()).getFavIcon();
            }

            @Override
            public void onChanged() {
                super.onChanged();

                try {
                    if (getSelectedItem() == null || content == null) return;
                    PluginForHost plg = ((LazyHostPlugin) getSelectedItem()).newInstance(PluginClassLoader.getInstance().getChild());
                    if (plg != plugin) {
                        plugin = plg;
                        updatePanel();
                        checkOK();
                    }

                } catch (UpdateRequiredClassNotFoundException e1) {
                    Dialog.getInstance().showErrorDialog(_GUI._.AddAccountDialog_actionPerformed_outdated(((LazyHostPlugin) getSelectedItem()).getHost()));
                }

            }

            @Override
            protected String getTextForValue(LazyHostPlugin value) {
                if (value == null) return "";
                return value.getDisplayName();
            }
        };

        final JButton link = new JButton(NewTheme.I().getIcon("money", 16));
        link.setText(_GUI._.gui_menu_action_premium_buy_name());
        link.setToolTipText(_GUI._.gui_menu_action_premium_buy_name());
        link.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                if (plugin == null || StringUtils.isEmpty(plugin.getLazyP().getPremiumUrl())) return;
                CrossSystem.openURLOrShowMessage(AccountController.createFullBuyPremiumUrl(plugin.getLazyP().getPremiumUrl(), "accountmanager/table"));
            }
        });
        link.setFocusable(false);

        content = new JPanel(new MigLayout("ins 0, wrap 1", "[grow,fill]"));
        content.add(header(_GUI._.AddAccountDialog_layoutDialogContent_choosehoster_()), "gapleft 15,spanx,pushx,growx");
        content.add(this.hoster, "gapleft 32,height 24!");
        content.add(link, "height 20!,gapleft 32");
        content.add(header(_GUI._.AddAccountDialog_layoutDialogContent_enterlogininfo()), "gapleft 15,spanx,pushx,growx,gaptop 15");

        LazyHostPlugin lazyp = null;
        if (this.plugin != null) {
            lazyp = plugin.getLazyP();
        } else {
            lazyp = HostPluginController.getInstance().get(getPreselectedHoster());
        }
        if (lazyp != null) {
            try {
                hoster.setPrototypeDisplayValue(lazyp);
            } catch (final Throwable e) {
            }
            try {
                hoster.setSelectedItem(lazyp);
            } catch (final Exception e) {
            }
        }
        getDialog().addWindowFocusListener(new WindowFocusListener() {

            @Override
            public void windowLostFocus(final WindowEvent windowevent) {
                // TODO Auto-generated method stub

            }

            @Override
            public void windowGainedFocus(final WindowEvent windowevent) {
                final Component focusOwner = getDialog().getFocusOwner();
                if (focusOwner != null) {
                    // dialog component has already focus...
                    return;
                }
                /* we only want to force focus on first window open */
                getDialog().removeWindowFocusListener(this);
                hoster.requestFocus();
            }
        });
        return content;
    }

    public static String getPreselectedHoster() {
        if ("Europe/Berlin".equalsIgnoreCase(Calendar.getInstance().getTimeZone().getID())) {
            return "uploaded.to";
        } else {
            return "rapidgator.net";
        }
    }

    protected int getPreferredWidth() {
        return 450;
    }

    public LazyHostPlugin getHoster() {
        return (LazyHostPlugin) this.hoster.getSelectedItem();
    }

    protected void updatePanel() {
        try {
            if (content == null) return;
            PluginForHost plg = plugin;
            if (plg == null) {
                LazyHostPlugin p = HostPluginController.getInstance().get(getPreselectedHoster());
                if (p == null) {
                    Iterator<LazyHostPlugin> it = HostPluginController.getInstance().list().iterator();
                    if (it.hasNext()) p = it.next();
                }
                hoster.setSelectedItem(p);
                plg = p.newInstance(PluginClassLoader.getInstance().getChild());
            }

            AccountFactory accountFactory = plg.getAccountFactory();
            if (editAccountPanel != null) {
                defaultAccount = editAccountPanel.getAccount();
                content.remove(editAccountPanel.getComponent());
            }
            editAccountPanel = accountFactory.getPanel();
            content.add(editAccountPanel.getComponent(), "gapleft 32,spanx");
            editAccountPanel.setAccount(defaultAccount);
            editAccountPanel.setNotifyCallBack(new Notifier() {

                @Override
                public void onNotify() {
                    checkOK();
                }

            });
            checkOK();
            getDialog().pack();

        } catch (UpdateRequiredClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private JComponent header(String buyAndAddPremiumAccount_layoutDialogContent_get) {
        JLabel ret = SwingUtils.toBold(new JLabel(buyAndAddPremiumAccount_layoutDialogContent_get));
        ret.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ret.getForeground()));
        return ret;
    }

    private void checkOK() {

        this.okButton.setEnabled(hoster != null && hoster.getSelectedItem() != null && editAccountPanel.validateInputs());
    }

    @Override
    protected void packed() {
        super.packed();

    }

}