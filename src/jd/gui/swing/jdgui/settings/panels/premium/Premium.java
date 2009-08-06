//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.gui.swing.jdgui.settings.panels.premium;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.Timer;

import jd.HostPluginWrapper;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.controlling.AccountController;
import jd.controlling.AccountControllerEvent;
import jd.controlling.AccountControllerListener;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.components.AccountDialog;
import jd.gui.swing.components.IconListRenderer;
import jd.gui.swing.components.linkbutton.JLink;
import jd.gui.swing.components.pieapi.ChartAPIEntity;
import jd.gui.swing.components.pieapi.PieChartAPI;
import jd.gui.swing.dialog.ContainerDialog;
import jd.gui.swing.jdgui.actions.ThreadedAction;
import jd.gui.swing.jdgui.settings.ConfigPanel;
import jd.gui.swing.jdgui.views.toolbar.ViewToolbar;
import jd.nutils.Formatter;
import jd.nutils.JDFlags;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXTitledSeparator;

public class Premium extends ConfigPanel implements ActionListener, AccountControllerListener {

    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.settings.panels.premium.Premium.";

    private static final long serialVersionUID = -7685744533817989161L;
    private PremiumTable internalTable;
    private JScrollPane scrollPane;
    private Timer Update_Async;
    private boolean tablerefreshinprogress = false;
    protected Logger logger = jd.controlling.JDLogger.getLogger();

    public Premium(Configuration configuration) {
        super();

        initPanel();
        load();
    }

    public String getBreadcrum() {
        return JDL.L(this.getClass().getName() + ".breadcrum", this.getClass().getSimpleName());
    }

    public static String getTitle() {
        return JDL.L(JDL_PREFIX + "title", "Premium");
    }

    public void initPanel() {
        panel.setLayout(new MigLayout("ins 5,wrap 1", "[fill,grow]", "[fill,grow]"));
        initPanel(panel);
        JTabbedPane tabbed = new JTabbedPane();
        tabbed.setOpaque(false);
        tabbed.add(getBreadcrum(), panel);
        this.add(tabbed);
    }

    private void initPanel(JPanel panel) {
        internalTable = new PremiumTable(new PremiumJTableModel(), this);
        scrollPane = new JScrollPane(internalTable);
        Update_Async = new Timer(250, this);
        Update_Async.setInitialDelay(250);
        Update_Async.setRepeats(false);
        AccountController.getInstance().addListener(this);
        initActions();

        ViewToolbar vt = new ViewToolbar() {
            /**
             * 
             */
            private static final long serialVersionUID = 583469943193290056L;

            public void setDefaults(int i, AbstractButton ab) {
                ab.setForeground(new JLabel().getForeground());

            }
        };
        // vt.setContentPainted(false);
        vt.setList(new String[] { "action.premiumview.addacc", "action.premiumview.removeacc", "action.premium.buy" });

        panel.add(vt, "dock north,gapleft 3");
        panel.add(scrollPane);
    }

    @Override
    public void load() {
        loadConfigEntries();
    }

    @Override
    public void save() {
        saveConfigEntries();

    }

    @Override
    public ConfigEntry.PropertyType hasChanges() {
        return ConfigEntry.PropertyType.NORMAL;
    }

    private void initActions() {
        new ThreadedAction("action.premiumview.addacc", "gui.images.premium") {

            /**
             * 
             */
            private static final long serialVersionUID = -4407938288408350792L;

            @Override
            public void initDefaults() {
                this.setToolTipText(JDL.L("action.premiumview.addacc.tooltip", "Add a new Account"));
            }

            @Override
            public void init() {
            }

            public void threadedActionPerformed(final ActionEvent e) {

                new GuiRunnable<Object>() {

                    @Override
                    public Object runSave() {
                        if (e.getSource() instanceof PluginForHost) {
                            AccountDialog.showDialog((PluginForHost) e.getSource());
                        } else {
                            AccountDialog.showDialog(null);
                        }

                        return null;
                    }
                }.start();

            }
        };
        new ThreadedAction("action.premiumview.removeacc", "gui.images.delete") {

            /**
             * 
             */
            private static final long serialVersionUID = -4407938288408350792L;

            @Override
            public void initDefaults() {
                this.setToolTipText(JDL.L("action.premiumview.removeacc.tooltip", "Remove selected Account(s)"));
            }

            @Override
            public void init() {
            }

            public void threadedActionPerformed(ActionEvent e) {
                ArrayList<Account> accs = internalTable.getSelectedAccounts();
                if (accs.size() == 0) return;
                if (JDFlags.hasSomeFlags(UserIO.getInstance().requestConfirmDialog(0, JDL.L("action.premiumview.removeacc.ask", "Remove selected ") + " (" + JDL.LF("action.premiumview.removeacc.accs", "%s Account(s)", accs.size()) + ")"), UserIO.RETURN_OK, UserIO.RETURN_DONT_SHOW_AGAIN)) {
                    for (Account acc : accs) {
                        AccountController.getInstance().removeAccount((String) null, acc);
                    }
                }
            }
        };

        new ThreadedAction("action.premium.buy", "gui.images.buy") {

            /**
             * 
             */
            private static final long serialVersionUID = -4407938288408350792L;

            @Override
            public void initDefaults() {
                this.setToolTipText(JDL.L(JDL_PREFIX + "buy.tooltip", "Buy a new premium account."));
            }

            @Override
            public void init() {
            }

            public void threadedActionPerformed(ActionEvent e) {
                new GuiRunnable<Object>() {

                    @Override
                    public Object runSave() {

                        ArrayList<HostPluginWrapper> plugins = JDUtilities.getPremiumPluginsForHost();
                        HostPluginWrapper[] data = plugins.toArray(new HostPluginWrapper[plugins.size()]);
                        int selection = UserIO.getInstance().requestComboDialog(0, JDL.L(JDL_PREFIX + "buy.title", "Buy Premium"), JDL.L(JDL_PREFIX + "buy.message", "Which hoster are you interested in?"), data, 0, null, JDL.L(JDL_PREFIX + "continue", "Continue"), null, new IconListRenderer());

                        try {
                            JLink.openURL(data[selection].getPlugin().getBuyPremiumUrl());
                        } catch (Exception ex) {
                        }

                        return null;
                    }

                }.start();

            }
        };

    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    @Override
    public void onHide() {
        super.onHide();
        Update_Async.stop();
        AccountController.getInstance().removeListener(this);
    }

    public void fireTableChanged(final boolean fast) {
        if (tablerefreshinprogress && !fast) return;
        new Thread() {
            public void run() {
                if (!fast) tablerefreshinprogress = true;
                this.setName("PremiumPanel: refresh Table");
                try {
                    internalTable.fireTableChanged();
                } catch (Exception e) {
                    logger.severe("TreeTable Exception, complete refresh!");
                    Update_Async.restart();
                }
                if (!fast) tablerefreshinprogress = false;
            }
        }.start();
    }

    @Override
    public void onShow() {
        super.onShow();
        AccountController.getInstance().addListener(this);
        fireTableChanged(true);
    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.getSource() == Update_Async) {
            fireTableChanged(false);
            return;
        }
    }

    public void onAccountControllerEvent(AccountControllerEvent event) {
        switch (event.getID()) {
        case AccountControllerEvent.ACCOUNT_ADDED:
        case AccountControllerEvent.ACCOUNT_REMOVED:
        case AccountControllerEvent.ACCOUNT_UPDATE:
        case AccountControllerEvent.ACCOUNT_EXPIRED:
        case AccountControllerEvent.ACCOUNT_INVALID:
            Update_Async.restart();
            break;
        default:
            break;
        }
    }

    public boolean vetoAccountGetEvent(String host, Account account) {
        // TODO Auto-generated method stub
        return false;
    }

    public static void showAccountInformation(final PluginForHost pluginForHost, final Account account) {
        new GuiRunnable<Object>() {
            // @Override
            public Object runSave() {
                AccountInfo ai = AccountController.getInstance().updateAccountInfo(pluginForHost, account, false);
                if (ai == null) {
                    UserIO.getInstance().requestMessageDialog(JDL.LF("plugins.host.premium.info.error", "The %s plugin does not support the Accountinfo feature yet.", pluginForHost.getHost()));
                    return null;
                }
                if (!account.isValid()) {
                    UserIO.getInstance().requestMessageDialog(JDL.LF("plugins.host.premium.info.notValid", "The account for '%s' isn't valid! Please check username and password!\r\n%s", account.getUser(), ai.getStatus() != null ? ai.getStatus() : ""));
                    return null;
                }
                if (ai.isExpired()) {
                    UserIO.getInstance().requestMessageDialog(JDL.LF("plugins.host.premium.info.expired", "The account for '%s' is expired! Please extend the account or buy a new one!\r\n%s", account.getUser(), ai.getStatus() != null ? ai.getStatus() : ""));
                    return null;
                }

                String def = JDL.LF("plugins.host.premium.info.title", "Accountinformation from %s for %s", account.getUser(), pluginForHost.getHost());
                String[] label = new String[] { JDL.L("plugins.host.premium.info.validUntil", "Valid until"), JDL.L("plugins.host.premium.info.trafficLeft", "Traffic left"), JDL.L("plugins.host.premium.info.files", "Files"), JDL.L("plugins.host.premium.info.premiumpoints", "PremiumPoints"), JDL.L("plugins.host.premium.info.usedSpace", "Used Space"), JDL.L("plugins.host.premium.info.cash", "Cash"), JDL.L("plugins.host.premium.info.trafficShareLeft", "Traffic Share left"), JDL.L("plugins.host.premium.info.status", "Info") };

                DateFormat formater = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
                String validUntil = (ai.isExpired() ? JDL.L("plugins.host.premium.info.expiredInfo", "[expired]") + " " : "") + formater.format(new Date(ai.getValidUntil())) + "";
                if (ai.getValidUntil() == -1) validUntil = null;
                String premiumPoints = ai.getPremiumPoints() + ((ai.getNewPremiumPoints() > 0) ? " [+" + ai.getNewPremiumPoints() + "]" : "");
                String[] data = new String[] { validUntil, Formatter.formatReadable(ai.getTrafficLeft()), ai.getFilesNum() + "", premiumPoints, Formatter.formatReadable(ai.getUsedSpace()), ai.getAccountBalance() < 0 ? null : (ai.getAccountBalance() / 100.0) + " €", Formatter.formatReadable(ai.getTrafficShareLeft()), ai.getStatus() };

                JPanel panel = new JPanel(new MigLayout("ins 5", "[right]10[grow,fill]10[]"));
                panel.add(new JXTitledSeparator("<html><b>" + def + "</b></html>"), "spanx, pushx, growx, gapbottom 15");

                for (int j = 0; j < data.length; j++) {
                    if (data[j] != null && !data[j].equals("-1") && !data[j].equals("-1 B")) {
                        panel.add(new JLabel(label[j]), "gapleft 20");

                        JTextField tf = new JTextField(data[j]);
                        tf.setBorder(null);
                        tf.setBackground(null);
                        tf.setEditable(false);
                        tf.setOpaque(false);

                        if (label[j].equals(JDL.L("plugins.host.premium.info.trafficLeft", "Traffic left"))) {
                            PieChartAPI freeTrafficChart = new PieChartAPI("", 150, 60);
                            freeTrafficChart.addEntity(new ChartAPIEntity(JDL.L("plugins.host.premium.info.freeTraffic", "Free"), ai.getTrafficLeft(), new Color(50, 200, 50)));
                            freeTrafficChart.addEntity(new ChartAPIEntity("", ai.getTrafficMax() - ai.getTrafficLeft(), new Color(150, 150, 150)));
                            freeTrafficChart.fetchImage();

                            panel.add(tf);
                            panel.add(freeTrafficChart, "spany, wrap");
                        } else {
                            panel.add(tf, "span 2, wrap");
                        }
                    }

                }
                new ContainerDialog(UserIO.NO_CANCEL_OPTION, def, panel, null, null);

                return null;
            }
        }.start();
    }

}
