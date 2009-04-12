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

package jd.gui.skins.simple.premium;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.gui.skins.simple.JTabbedPanel;
import jd.gui.skins.simple.components.ChartAPIEntity;
import jd.gui.skins.simple.components.ChartAPI_GOM;
import jd.gui.skins.simple.components.PieChartAPI;
import jd.gui.skins.simple.tasks.TaskPanel;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXLabel;

public class PremiumPane extends JTabbedPanel {
    class PremiumDetailTaskPanel extends TaskPanel {

        private static final long serialVersionUID = 6542044502421479328L;

        public PremiumDetailTaskPanel(String string, ImageIcon ii) {

            super(string, ii, "premiumdetailpanel");
            setLayout(new MigLayout("ins 2, wrap 3", "[fill]15[grow, fill][]", "[][][][][][][][][][][fill,grow]"));

            this.setCollapsed(true);
        }

        public void actionPerformed(ActionEvent e) {
            // TODO Auto-generated method stub

        }

    }

    private static final long serialVersionUID = 6433012230610964105L;

    private JPanel panel;

    private static final Color ACTIVE = new Color(0x7cd622);
    private static final Color INACTIVE = new Color(0xa40604);
    private static final Color DISABLED = new Color(0xaff0000);

    private static Color FILTER_COLOR = new Color(0.0f,0.0f,0.0f,0.0f);
    private static boolean premiumActivated = true;
    private PluginForHost host;
    private JScrollPane sp;

    private ArrayList<AccountPanel> accs = new ArrayList<AccountPanel>();
    private AccountPanel lastOpenedPanel;

    private PieChartAPI freeTrafficChart;
    private PremiumDetailTaskPanel details;

    private JPanel chartContainer;

    public PremiumPane(String hostname) {

        setLayout(new MigLayout("ins 3", "[grow]"));

        this.host = JDUtilities.getPluginForHost(hostname);

        details = new PremiumDetailTaskPanel("test", JDTheme.II("gui.images.config.tip", 24, 24));
  
        freeTrafficChart = new PieChartAPI("", 500, 100, FILTER_COLOR);

        this.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, this.getBackground().darker()));

        chartContainer = new JPanel(new MigLayout("ins 0"));

        chartContainer.setOpaque(true);
        chartContainer.add(freeTrafficChart, "alignx center");
        // chartContainer.setBorder(BorderFactory.createEtchedBorder());

    }

    public void onHide() {
        this.removeAll();
    }

    private void showCollapseAccount(AccountPanel accp) {
        try {
            details.setCollapsed(true);

            if (lastOpenedPanel != null) lastOpenedPanel.setHide();

            lastOpenedPanel = accp;

            AccountInfo ai = host.getAccountInformation(accp.getAccount());
            details.removeAll();

            if (!ai.isValid()) {
                accp.setNotValid(ai.getStatus());
                return;
            }

            DateFormat formater = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
            ChartAPI_GOM freeTraffic = new ChartAPI_GOM("", 200, 100, FILTER_COLOR);
            double procent = ((double) ai.getTrafficLeft() / (double) ai.getTrafficMax() * 100);
            freeTraffic.addEntity(new ChartAPIEntity(JDUtilities.formatBytesToMB(ai.getTrafficLeft()) + " free", String.valueOf(procent), new Color(50, 200, 50)));
            freeTraffic.fetchImage();
            details.add(freeTraffic, "cell 2 0,spany,aligny center");
            details.setCollapsed(false);
            details.setTitle("Accountinformation for " + host.getHost() + " account: " + accp.getAccount().getUser());

            if (ai.getValidUntil() > -1) {
                details.add(new JLabel("Valid until"), " alignright, w 15%:pref, growx");
                details.add(getTextField(formater.format(new Date(ai.getValidUntil()))), "alignleft");
            }

            if (ai.getAccountBalance() > -1) {
                details.add(new JLabel("Balance"), " alignright, w 15%:pref, growx");
                details.add(getTextField(String.valueOf(ai.getAccountBalance() / 100) + " €"), "alignleft");
            }
            if (ai.getFilesNum() > -1) {
                details.add(new JLabel("Files stored"), " alignright, w 15%:pref, growx");
                details.add(getTextField(String.valueOf(ai.getFilesNum())), "alignleft");
            }
            if (ai.getUsedSpace() > -1) {
                details.add(new JLabel("Used Space"), " alignright, w 15%:pref, growx");
                details.add(getTextField(JDUtilities.formatBytesToMB(ai.getUsedSpace())), "alignleft");
            }
            if (ai.getPremiumPoints() > -1) {
                details.add(new JLabel("PremiumPoints"), " alignright, w 15%:pref, growx");
                details.add(getTextField(String.valueOf(ai.getPremiumPoints())), "alignleft");
            }
            if (ai.getTrafficShareLeft() > -1) {
                details.add(new JLabel("Trafficshare left"), " alignright, w 15%:pref, growx");
                details.add(getTextField(JDUtilities.formatBytesToMB(ai.getTrafficShareLeft())), "alignleft");
            }
            if (ai.getTrafficLeft() > -1) {
                details.add(new JLabel("Traffic left"), " alignright, w 15%:pref, growx");
                details.add(getTextField(JDUtilities.formatBytesToMB(ai.getTrafficLeft())), "alignleft");

            }

        } catch (Exception e) {
            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occured",e);
        }
    }

    private JTextField getTextField(String text) {
        JTextField help = new JTextField(text);
        help.setEditable(false);
        help.setOpaque(false);
        help.setBorder(null);
        return help;
    }

    private void showGlobalHostInformation() {
        details.setCollapsed(true);
        details.removeAll();

        details.setTitle("Informations for " + host.getHost());

        details.add(new JLabel("Accounts"), "newline, alignright, w 15%:pref");
        details.add(getTextField(String.valueOf(host.getPremiumAccounts().size())), "alignleft");

        details.add(this.chartContainer, "cell 0 10,spanx 2");
        new ChartRefresh().start();

        details.setCollapsed(false);
    }

    private void createPanel() {
        panel = new JPanel();
        panel.setLayout(new MigLayout("ins 5", "[right, pref!]10[100:pref, fill]0[right][90:pref, grow,fill]"));
        accs = new ArrayList<AccountPanel>();

        for (int i = 0; i < host.getPremiumAccounts().size(); i++) {
            accs.add(new AccountPanel(panel, i + 1, this, host.getPremiumAccounts().get(i)));
        }

        removeAll();
        add(sp = new JScrollPane(panel), "grow, push");
        sp.setBorder(null);
        add(details, "south");

        sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.getVerticalScrollBar().setUnitIncrement(80);
        sp.getVerticalScrollBar().setBlockIncrement(190);

        showGlobalHostInformation();
    }

    private class AccountPanel implements ChangeListener, ActionListener {
        private JCheckBox chkEnable;
        private JLabel lblUsername;
        private JTextField txtUsername;
        private JTextField txtStatus;
        private JButton btnCheck;
        private JButton btnDelete;
        private Account account;
        private boolean accvalid = true;
        private PremiumPane owner;

        public AccountPanel(JPanel panel, int nr, PremiumPane owner, Account account) {
            this.owner = owner;
            this.account = account;
            createPanel(panel, nr);
        }

        public void createPanel(JPanel panel, int nr) {
            chkEnable = new JCheckBox(JDLocale.LF("plugins.config.premium.accountnum", "<html><b>Premium Account #%s</b></html>", nr));
            if (account.isEnabled()) {
                chkEnable.setForeground(ACTIVE);
            } else {
                chkEnable.setForeground(INACTIVE);
            }
            chkEnable.setSelected(account.isEnabled());

            panel.add(chkEnable, "alignleft, newline");
            chkEnable.addChangeListener(this);

            panel.add(btnCheck = new JButton(JDLocale.L("plugins.config.premium.information.show", "Show Information")), "w pref:pref:pref, split 2");
            btnCheck.addActionListener(this);

            panel.add(btnDelete = new JButton(JDTheme.II("gui.images.delete", 14, 14)));
            btnDelete.addActionListener(this);

            panel.add(new JSeparator(), "w 30:push, growx, pushx");
            panel.add(txtStatus = new JTextField(account.getStatus()), "spanx, pushx, growx");
            txtStatus.setEditable(false);

            panel.add(lblUsername = new JXLabel(JDLocale.L("plugins.config.premium.user", "Premium User")), "gaptop 8");
            panel.add(txtUsername = new JTextField(account.getUser()));
            txtUsername.setEditable(false);

            txtStatus.setEnabled(account.isEnabled());
            btnCheck.setEnabled(account.isEnabled());
            lblUsername.setEnabled(account.isEnabled());
        }

        public void setHide() {
            btnCheck.setText("Show Information");
        }

        public Account getAccount() {
            return account;
        }

        public void setNotValid(final String status) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    txtStatus.setText(status);
                    chkEnable.setSelected(false);
                    chkEnable.setForeground(DISABLED);
                    txtStatus.setEnabled(false);
                    btnCheck.setEnabled(false);
                    lblUsername.setEnabled(false);
                    account.setEnabled(false);
                    accvalid = false;
                }
            });
        }

        public void stateChanged(ChangeEvent e) {
            boolean sel = chkEnable.isSelected();
            if (premiumActivated && accvalid) {
                chkEnable.setForeground((sel) ? ACTIVE : INACTIVE);
            } else {
                chkEnable.setForeground(DISABLED);
            }
            txtStatus.setEnabled(sel);
            btnCheck.setEnabled(sel);
            lblUsername.setEnabled(sel);
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == btnCheck) {
                if (btnCheck.getText().equals("Show Information")) {
                    owner.showCollapseAccount(this);
                    btnCheck.setText("Hide Information");
                } else if (btnCheck.getText().equals("Hide Information")) {
                    owner.showGlobalHostInformation();
                    btnCheck.setText("Show Information");
                }
            } else if (e.getSource() == btnDelete) {
                ArrayList<Account> hp = host.getPremiumAccounts();

                for (int i = 0; i < hp.size(); i++) {
                    if (hp.get(i).equals(account)) {
                        hp.remove(i);
                        break;
                    }
                }

                host.setPremiumAccounts(hp);
                owner.createPanel();
            }
        }
    }

    private class ChartRefresh extends Thread {
        public void run() {
            Long collectTraffic = new Long(0);
            freeTrafficChart.clear();
            int accCounter = 0;
            Account acc;
            for (int i = 0; i < host.getPremiumAccounts().size(); i++) {
                acc = host.getPremiumAccounts().get(i);
                if (acc != null && acc.getUser().length() > 0 && acc.getPass().length() > 0) {
                    try {
                        accCounter++;
                        AccountInfo ai = host.getAccountInformation(acc);
                        Long tleft = new Long(ai.getTrafficLeft());
                        if (tleft >= 0 && ai.isExpired() == false) {
                            freeTrafficChart.addEntity(new ChartAPIEntity(acc.getUser() + " [" + (Math.round(tleft.floatValue() / 1024 / 1024 / 1024 * 100) / 100.0) + " GB]", tleft, new Color(50, 255 - ((255 / (i + host.getPremiumAccounts().size())) * accCounter), 50)));
                            long rest = ai.getTrafficMax() - tleft;
                            if (rest > 0) collectTraffic = collectTraffic + rest;
                        } else if (!ai.isValid()) {
                            for (AccountPanel accp : accs) {
                                if (acc.getUser().equals(accp.getAccount().getUser())) {
                                    accp.setNotValid(ai.getStatus());
                                }
                            }
                        } else {
                            for (AccountPanel accp : accs) {
                                if (acc.getUser().equals(accp.getAccount().getUser())) {
                                    accp.setNotValid(JDLocale.L("plugins.config.premium.expired", "This Account is expired"));
                                }
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            }

            if (collectTraffic > 0) freeTrafficChart.addEntity(new ChartAPIEntity(JDLocale.L("plugins.config.premium.chartapi.maxTraffic", "Max. Traffic to collect") + " [" + Math.round(((collectTraffic.floatValue() / 1024 / 1024 / 1024) * 100) / 100.0) + " GB]", collectTraffic, new Color(150, 150, 150)));
            freeTrafficChart.fetchImage();
        }
    }

    @Override
    public void onDisplay() {
        createPanel();
    }
}