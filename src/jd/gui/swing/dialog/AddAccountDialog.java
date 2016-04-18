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
import java.awt.event.MouseAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.plaf.basic.ComboPopup;

import jd.controlling.AccountController;
import jd.controlling.accountchecker.AccountChecker;
import jd.controlling.accountchecker.AccountChecker.AccountCheckJob;
import jd.gui.UserIO;
import jd.plugins.Account;
import jd.plugins.Account.AccountError;
import jd.plugins.AccountInfo;
import jd.plugins.PluginForHost;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.components.searchcombo.SearchComboBox;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.jdownloader.DomainInfo;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.InputChangedCallbackInterface;
import org.jdownloader.gui.sponsor.Sponsor;
import org.jdownloader.gui.sponsor.SponsorUtils;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.accounts.AccountBuilderInterface;
import org.jdownloader.plugins.controller.PluginClassLoader;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;
import org.jdownloader.plugins.controller.UpdateRequiredClassNotFoundException;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.plugins.controller.host.PluginFinder;
import org.jdownloader.statistics.StatsManager;
import org.jdownloader.translate._JDT;

public class AddAccountDialog extends AbstractDialog<Integer> implements InputChangedCallbackInterface {

    public static void showDialog(final PluginForHost pluginForHost, Account acc) {
        final AddAccountDialog dialog = new AddAccountDialog(pluginForHost, acc);

        try {
            Dialog.getInstance().showDialog(dialog);
            if (dialog.getHoster() == null) {
                return;
            }
            final Account ac = dialog.getAccount();
            ac.setHoster(dialog.getHoster().getDisplayName());

            if (!addAccount(ac)) {
                showDialog(dialog.getHoster().getPrototype(null), ac);
            }

        } catch (Throwable e) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
        }

    }

    private Account getAccount() {
        AccountBuilderInterface leditAccountPanel = accountBuilderUI;
        if (leditAccountPanel != null) {
            return leditAccountPanel.getAccount();
        }
        return null;
    }

    public static boolean addAccount(final Account ac) throws DialogNoAnswerException {
        try {
            checkAccount(ac);
        } catch (DialogNoAnswerException e) {
            throw e;
        } catch (Throwable e) {
            Dialog.getInstance().showExceptionDialog(_GUI.T.accountdialog_check_failed(), _GUI.T.accountdialog_check_failed_msg(), e);
        }
        AccountError error = ac.getError();
        String errorMessage = ac.getErrorString();
        if (StringUtils.isEmpty(errorMessage)) {
            AccountInfo ai = ac.getAccountInfo();
            if (ai != null) {
                errorMessage = ai.getStatus();
            }
        }
        if (error != null) {
            switch (error) {
            case PLUGIN_ERROR:
                if (StringUtils.isEmpty(errorMessage)) {
                    errorMessage = _JDT.T.AccountController_updateAccountInfo_status_plugin_defect();
                }
                Dialog.getInstance().showMessageDialog(_GUI.T.accountdialog_check_invalid(errorMessage));
                return false;
            case EXPIRED:
                Dialog.getInstance().showConfirmDialog(0, _GUI.T.accountdialog_check_expired_title(), _GUI.T.accountdialog_check_expired(ac.getUser()), null, _GUI.T.accountdialog_check_expired_renew(), null);
                AccountController.getInstance().addAccount(ac, false);
                return true;
            case TEMP_DISABLED:
                if (StringUtils.isEmpty(errorMessage)) {
                    errorMessage = _GUI.T.accountdialog_check_failed();
                }
                Dialog.getInstance().showMessageDialog(_GUI.T.accountdialog_check_result(errorMessage));
                AccountController.getInstance().addAccount(ac, false);
                return true;
            default:
            case INVALID:
                if (StringUtils.isEmpty(errorMessage)) {
                    errorMessage = _GUI.T.accountdialog_check_failed_msg();
                }
                Dialog.getInstance().showMessageDialog(_GUI.T.accountdialog_check_invalid(errorMessage));
                return false;
            }
        } else {
            String message = null;
            AccountInfo ai = ac.getAccountInfo();
            if (ai != null) {
                message = ai.getStatus();
            }
            if (StringUtils.isEmpty(message)) {
                message = _GUI.T.lit_yes();
            }
            Dialog.getInstance().showMessageDialog(_GUI.T.accountdialog_check_valid(message));
            AccountController.getInstance().addAccount(ac, false);
            return true;
        }
    }

    public static ProgressDialog checkAccount(final Account ac) throws Throwable {
        ProgressDialog pd = new ProgressDialog(new ProgressGetter() {

            public void run() throws Exception {
                final PluginForHost hostPlugin = new PluginFinder().assignPlugin(ac, true);
                if (hostPlugin != null) {
                    ac.setPlugin(hostPlugin);
                }
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
        }, 0, _GUI.T.accountdialog_check(), _GUI.T.accountdialog_check_msg(), DomainInfo.getInstance(ac.getHosterByPlugin()).getFavIcon());
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

    AccountBuilderInterface                accountBuilderUI;

    private JPanel                         content;

    private final PluginClassLoaderChild   cl;

    protected MouseAdapter                 mouseAdapter;

    private AddAccountDialog(final PluginForHost plugin, Account acc) {
        super(UserIO.NO_ICON, _GUI.T.jd_gui_swing_components_AccountDialog_title(), null, _GUI.T.lit_save(), null);
        this.defaultAccount = acc;
        this.plugin = plugin;
        cl = PluginClassLoader.getInstance().getChild();
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
            if (lhp.isPremium()) {
                plugins.add(lhp);
            }
        }

        if (plugins.size() == 0) {
            throw new RuntimeException("No Plugins Loaded Exception");
            // final HostPluginWrapper[] array = plugins.toArray(new
            // HostPluginWrapper[plugins.size()]);
        }

        hoster = new SearchComboBox<LazyHostPlugin>(plugins) {

            @Override
            protected Icon getIconForValue(LazyHostPlugin value) {
                if (value == null) {
                    return null;
                }
                return DomainInfo.getInstance(value.getDisplayName()).getFavIcon();
            }

            @Override
            protected boolean replaceAutoCompletePopupList() {
                return true;
            }

            @Override
            public void setSelectedIndex(int anIndex) {
                if (isPopupVisible() && anIndex >= 0) {
                    try {
                        final Object popup = getUI().getAccessibleChild(this, 0);
                        if (popup != null && popup instanceof ComboPopup) {
                            final JList jlist = ((ComboPopup) popup).getList();
                            if (anIndex < jlist.getModel().getSize()) {
                                setSelectedItem(jlist.getModel().getElementAt(anIndex));
                                return;
                            }
                        }
                    } catch (final Throwable e) {
                        org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
                    }
                }
                super.setSelectedIndex(anIndex);
            }

            @Override
            protected void searchAutoComplete(ComboBoxModel model, String txt, List<LazyHostPlugin> found, List<LazyHostPlugin> all) {
                final ArrayList<LazyHostPlugin> goodMatches = new ArrayList<LazyHostPlugin>();
                for (int i = 0; i < model.getSize(); i++) {
                    final LazyHostPlugin element = (LazyHostPlugin) model.getElementAt(i);
                    all.add(element);
                    final String text = getTextForValue(element);
                    if (matches(text, txt)) {
                        found.add(element);
                    } else {
                        if (StringUtils.isNotEmpty(txt) && txt.length() > 2 && StringUtils.isNotEmpty(text)) {
                            if (isSearchCaseSensitive() && StringUtils.containsIgnoreCase(text, txt)) {
                                goodMatches.add(element);
                            } else if (StringUtils.contains(text, txt)) {
                                goodMatches.add(element);
                            }
                        }
                    }
                }
                found.addAll(goodMatches);
            }

            @Override
            protected void sortFound(List<LazyHostPlugin> found) {
            }

            @Override
            public void onChanged() {
                super.onChanged();
                try {
                    if (getSelectedItem() == null || content == null) {
                        return;
                    }
                    PluginForHost plg = getSelectedItem().newInstance(cl);
                    if (plg != plugin) {
                        plugin = plg;
                        updatePanel();

                    }

                } catch (UpdateRequiredClassNotFoundException e1) {
                    Dialog.getInstance().showErrorDialog(_GUI.T.AddAccountDialog_actionPerformed_outdated(getSelectedItem().getHost()));
                }

            }

            @Override
            protected String getTextForValue(LazyHostPlugin value) {
                if (value == null) {
                    return "";
                }
                return value.getDisplayName();
            }
        };

        final JButton link = new JButton(new AbstractIcon(IconKey.ICON_MONEY, 16));
        link.setText(_GUI.T.gui_menu_action_premium_buy_name());
        link.setToolTipText(_GUI.T.gui_menu_action_premium_buy_name());
        link.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final PluginForHost lPlugin = plugin;
                if (lPlugin != null) {
                    StatsManager.I().openAfflink(lPlugin, null, "accountmanager/table");
                }
            }
        });
        link.setFocusable(false);

        content = new JPanel(new MigLayout("ins 0, wrap 1", "[grow,fill]"));
        content.add(header(_GUI.T.AddAccountDialog_layoutDialogContent_choosehoster_()), "gapleft 15,spanx,pushx,growx");
        content.add(this.hoster, "gapleft 32,height 24!");
        content.add(link, "height 20!,gapleft 32");
        content.add(header(_GUI.T.AddAccountDialog_layoutDialogContent_enterlogininfo()), "gapleft 15,spanx,pushx,growx,gaptop 15");

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
        updatePanel();
        return content;
    }

    public static String getPreselectedHoster() {
        Sponsor sp = SponsorUtils.getSponsor();
        String host = sp.getPreSelectedInAddAccountDialog();
        if (host != null) {
            return host;
        } else {
            return "rapidgator.net";
        }
    }

    protected int getPreferredWidth() {
        return 450;
    }

    public LazyHostPlugin getHoster() {
        return this.hoster.getSelectedItem();
    }

    protected void updatePanel() {
        try {
            if (content == null) {
                return;
            }
            PluginForHost plg = plugin;
            if (plg == null) {
                LazyHostPlugin p = HostPluginController.getInstance().get(getPreselectedHoster());
                if (p == null) {
                    Iterator<LazyHostPlugin> it = HostPluginController.getInstance().list().iterator();
                    if (it.hasNext()) {
                        p = it.next();
                    }
                }
                hoster.setSelectedItem(p);
                plg = p.newInstance(cl);
            }

            AccountBuilderInterface accountFactory = plg.getAccountFactory(this);
            if (accountBuilderUI != null) {
                defaultAccount = accountBuilderUI.getAccount();
                content.remove(accountBuilderUI.getComponent());
            }
            accountBuilderUI = accountFactory;
            content.add(accountBuilderUI.getComponent(), "gapleft 32,spanx");
            accountBuilderUI.setAccount(defaultAccount);

            onChangedInput(null);
            getDialog().pack();

        } catch (UpdateRequiredClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == okButton) {
            if (hoster != null && hoster.getSelectedItem() != null && accountBuilderUI.validateInputs()) {
                super.actionPerformed(e);
            }
        } else {
            super.actionPerformed(e);
        }
    }

    private JComponent header(String buyAndAddPremiumAccount_layoutDialogContent_get) {
        JLabel ret = SwingUtils.toBold(new JLabel(buyAndAddPremiumAccount_layoutDialogContent_get));
        ret.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ret.getForeground()));
        return ret;
    }

    @Override
    protected void packed() {
        super.packed();

    }

    @Override
    public void onChangedInput(Object component) {

        InputOKButtonAdapter.register(this, accountBuilderUI);

    }

}