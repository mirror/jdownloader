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

import java.awt.AWTEvent;
import java.awt.AlphaComposite;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import jd.controlling.AccountController;
import jd.controlling.accountchecker.AccountChecker;
import jd.controlling.accountchecker.AccountChecker.AccountCheckJob;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.JDGui;
import jd.plugins.Account;
import jd.plugins.Account.AccountError;
import jd.plugins.AccountInfo;
import jd.plugins.PluginForHost;
import net.miginfocom.swing.MigLayout;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.ListFocusTraversalPolicy;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.appwork.utils.swing.dialog.dimensor.RememberLastDialogDimension;
import org.appwork.utils.swing.dialog.locator.RememberRelativeDialogLocator;
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
        final AccountBuilderInterface leditAccountPanel = accountBuilderUI;
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

    private HosterChooserTable           hoster;
    private PluginForHost                plugin;
    private Account                      defaultAccount;
    AccountBuilderInterface              accountBuilderUI;
    private JPanel                       content;
    private final PluginClassLoaderChild cl;
    protected MouseAdapter               mouseAdapter;
    private ExtTextField                 filter;
    private JLabel                       header2;
    private JButton                      link;

    private AddAccountDialog(final PluginForHost plugin, Account acc) {
        super(UserIO.NO_ICON, _GUI.T.jd_gui_swing_components_AccountDialog_title(), null, _GUI.T.lit_save(), null);
        this.defaultAccount = acc;
        this.plugin = plugin;
        cl = PluginClassLoader.getInstance().getChild();
        setLocator(new RememberRelativeDialogLocator("AddAccountDialog2", JDGui.getInstance().getMainFrame()));
        setDimensor(new RememberLastDialogDimension("AddAccountDialog2"));
    }

    @Override
    protected Integer createReturnValue() {
        return this.getReturnmask();
    }

    protected void initFocus(final JComponent focus) {
    }

    private LazyHostPlugin  lazyHostPlugin = null;
    private List<Component> inputComponents;

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
        filter = new ExtTextField() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                Composite comp = g2.getComposite();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
                new AbstractIcon(IconKey.ICON_SEARCH, 16).paintIcon(this, g2, 3, 3);
                g2.setComposite(comp);
            }
        };
        filter.addFocusListener(new FocusListener() {
            @Override
            public void focusLost(FocusEvent e) {
            }

            @Override
            public void focusGained(FocusEvent e) {
                filter.selectAll();
            }
        });
        filter.setHelpText("Search Plugins");
        filter.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    hoster.onKeyDown();
                }
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    hoster.onKeyUp();
                }
            }
        });
        // filterText.setOpaque(false);
        // filterText.putClientProperty("Synthetica.opaque", Boolean.FALSE);
        // filterText.setBorder(null);
        filter.setBorder(BorderFactory.createCompoundBorder(filter.getBorder(), BorderFactory.createEmptyBorder(0, 20, 0, 0)));
        hoster = new HosterChooserTable(plugins) {
            @Override
            protected void processEvent(AWTEvent e) {
                if (e instanceof KeyEvent) {
                    if (((KeyEvent) e).getKeyCode() == KeyEvent.VK_TAB) {
                        content.dispatchEvent(e);
                        return;
                    }
                }
                super.processEvent(e);
            }

            @Override
            protected boolean processKeyBinding(KeyStroke stroke, KeyEvent evt, int condition, boolean pressed) {
                return super.processKeyBinding(stroke, evt, condition, pressed);
            }

            public void valueChanged(javax.swing.event.ListSelectionEvent e) {
                super.valueChanged(e);
                if (e.getValueIsAdjusting() || getModel().isTableSelectionClearing()) {
                    return;
                }
                try {
                    scrollToSelection(0);
                    final PluginForHost plg = getSelectedPlugin().newInstance(cl);
                    if (plg != null && (lazyHostPlugin == null || !lazyHostPlugin.equals(plg.getLazyP()))) {
                        final PluginForHost ret = updatePanel(plg);
                        if (ret != null) {
                            lazyHostPlugin = ret.getLazyP();
                        }
                    }
                } catch (UpdateRequiredClassNotFoundException e1) {
                    e1.printStackTrace();
                }
            }
        };
        filter.getDocument().addDocumentListener(new DocumentListener() {
            private DelayedRunnable delayedRefresh = new DelayedRunnable(200, 1000) {
                String lastText = null;

                @Override
                public String getID() {
                    return "AddAccountDialog";
                }

                @Override
                public void delayedrun() {
                    new EDTRunner() {
                        @Override
                        protected void runInEDT() {
                            final String text = filter.getText();
                            if (!StringUtils.equals(lastText, text)) {
                                lastText = text;
                                hoster.refresh(text);
                            }
                        }
                    }.waitForEDT();
                }
            };

            @Override
            public void removeUpdate(DocumentEvent e) {
                delayedRefresh.resetAndStart();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                delayedRefresh.resetAndStart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                delayedRefresh.resetAndStart();
            }
        });
        link = new JButton(new AbstractIcon(IconKey.ICON_MONEY, 16));
        link.setText(_GUI.T.gui_menu_action_premium_buy_name());
        link.setToolTipText(_GUI.T.gui_menu_action_premium_buy_name());
        link.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                try {
                    final PluginForHost plugin = hoster.getSelectedPlugin().newInstance(cl);
                    if (plugin != null) {
                        AccountController.openAfflink(plugin, null, "accountmanager/table");
                    }
                } catch (UpdateRequiredClassNotFoundException e1) {
                    e1.printStackTrace();
                }
            }
        });
        link.setFocusable(false);
        content = new JPanel(new MigLayout("ins 0, wrap 1", "[grow,fill]"));
        content.add(header(_GUI.T.AddAccountDialog_layoutDialogContent_choosehoster_()), "gapleft 15,spanx,pushx,growx");
        content.add(filter, "gapleft 32,pushx,growx");
        JScrollPane sp;
        content.add(sp = new JScrollPane(hoster), "gapleft 32,pushy,growy");
        sp.setFocusable(false);
        sp.getVerticalScrollBar().setFocusable(false);
        sp.getViewport().setFocusable(false);
        content.add(link, "height 20!,gapleft 32");
        content.add(header2 = header(_GUI.T.AddAccountDialog_layoutDialogContent_enterlogininfo()), "gapleft 15,spanx,pushx,growx,gaptop 15");
        final LazyHostPlugin lazyp;
        if (this.plugin != null) {
            lazyp = plugin.getLazyP();
        } else {
            lazyp = HostPluginController.getInstance().get(getPreselectedHoster());
        }
        if (lazyp != null) {
            hoster.setSelectedPlugin(lazyp);
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
                focusFirstInputComponent();
            }
        });
        getDialog().setMinimumSize(new Dimension(400, 300));
        return content;
    }

    public static String getPreselectedHoster() {
        final Sponsor sp = SponsorUtils.getSponsor();
        final String host = sp.getPreSelectedInAddAccountDialog();
        if (host != null) {
            return host;
        } else {
            return "rapidgator.net";
        }
    }

    protected int getPreferredWidth() {
        return 600;
    }

    @Override
    protected int getPreferredHeight() {
        return 450;
    }

    public LazyHostPlugin getHoster() {
        return this.hoster.getSelectedPlugin();
    }

    private PluginForHost updatePanel(PluginForHost plugin) {
        try {
            if (content == null) {
                return null;
            }
            if (plugin == null) {
                LazyHostPlugin p = HostPluginController.getInstance().get(getPreselectedHoster());
                if (p == null) {
                    Iterator<LazyHostPlugin> it = HostPluginController.getInstance().list().iterator();
                    if (it.hasNext()) {
                        p = it.next();
                    }
                }
                plugin = p.newInstance(cl);
            }
            link.setText(_GUI.T.gui_menu_action_premium_buy_name2(plugin.getHost()));
            header2.setText(_GUI.T.AddAccountDialog_layoutDialogContent_enterlogininfo2(plugin.getHost()));
            final AccountBuilderInterface accountFactory = plugin.getAccountFactory(this);
            if (accountBuilderUI != null) {
                defaultAccount = accountBuilderUI.getAccount();
                content.remove(accountBuilderUI.getComponent());
            }
            accountBuilderUI = accountFactory;
            final JComponent comp;
            content.add(comp = accountBuilderUI.getComponent(), "gapleft 32,spanx");
            accountBuilderUI.setAccount(defaultAccount);
            final ArrayList<Component> focusOrder = new ArrayList<Component>();
            focusOrder.add(filter);
            focusOrder.addAll(inputComponents = ListFocusTraversalPolicy.getFocusableComponents(comp));
            focusOrder.add(okButton);
            focusOrder.add(cancelButton);
            dialog.setFocusTraversalPolicyProvider(true);
            dialog.setFocusTraversalPolicy(new ListFocusTraversalPolicy(focusOrder));
            // content.setFocusTraversalPolicy(new FocusTraversalPolicy() {
            //
            // @Override
            // public Component getLastComponent(Container aContainer) {
            // return comp;
            // }
            //
            // @Override
            // public Component getFirstComponent(Container aContainer) {
            // return filter;
            // }
            //
            // @Override
            // public Component getDefaultComponent(Container aContainer) {
            // return filter;
            // }
            //
            // @Override
            // public Component getComponentBefore(Container aContainer, Component aComponent) {
            //
            // return null;
            // }
            //
            // @Override
            // public Component getComponentAfter(Container aContainer, Component aComponent) {
            // if (aComponent == filter) {
            // return hoster;
            // } else if (aComponent == hoster || aComponent instanceof JViewport) {
            // return comp.getComponent(0);
            // }
            // return filter;
            // }
            // });
            onChangedInput(null);
            // getDialog().pack();
            return plugin;
        } catch (UpdateRequiredClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == okButton) {
            if (hoster != null && hoster.getSelectedPlugin() != null && accountBuilderUI.validateInputs()) {
                super.actionPerformed(e);
            }
        } else {
            super.actionPerformed(e);
        }
    }

    private JLabel header(String buyAndAddPremiumAccount_layoutDialogContent_get) {
        JLabel ret = SwingUtils.toBold(new JLabel(buyAndAddPremiumAccount_layoutDialogContent_get));
        ret.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ret.getForeground()));
        return ret;
    }

    @Override
    protected void packed() {
        super.packed();
    }

    @Override
    public void windowGainedFocus(WindowEvent e) {
        super.windowGainedFocus(e);
        focusFirstInputComponent();
    }

    @Override
    public void onChangedInput(Object component) {
        InputOKButtonAdapter.register(this, accountBuilderUI);
    }

    protected void focusFirstInputComponent() {
        // if (focusOnUserName) {
        // Component f = inputComponents.get(0);
        // f.requestFocus();
        //
        // } else {
        filter.requestFocus();
        // }
    }
}