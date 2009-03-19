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
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.gui.skins.simple.JTabbedPanel;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.JXHeader;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.border.DropShadowBorder;

public class PremiumPane extends JTabbedPanel {
    private static final long serialVersionUID = 6433012230610964105L;

    private JPanel panel;

    private static final Color ACTIVE = new Color(0x7cd622);
    private static final Color INACTIVE = new Color(0xa40604);
    private static final Color DISABLED = new Color(0xaff0000);
    private static boolean premiumActivated = true;
    private PluginForHost host;
    private JScrollPane sp;
    private JXCollapsiblePane cp;

    // private ChartAPI_PIE freeTrafficChart = new ChartAPI_PIE("", 450, 60,
    // this.getBackground());

    public PremiumPane(String hostname) {
        int n = 2;
        setLayout(new MigLayout("ins 1", "[grow]"));
        setBorder(new EmptyBorder(n, n, n, n));
        this.host = JDUtilities.getPluginForHost(hostname);
        cp = new JXCollapsiblePane();
        cp.setLayout(new MigLayout("ins 3", "[right]5[ left, grow, fill]"));
        cp.setBorder(new DropShadowBorder());
        cp.setCollapsed(true);
    }

    public void onHide() {
        this.removeAll();
    }

    // private void createDataset() {
    // new ChartRefresh().start();
    // }

    private void showCollapseAccount(Account acc) {
        try {
            cp.setCollapsed(true);

            AccountInfo ai = host.getAccountInformation(acc);
            cp.removeAll();

            DateFormat formater = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);

            JXHeader h = new JXHeader();
            h.setTitle("Accountinformation for " + acc.getUser());
            h.setIconPosition(JXHeader.IconPosition.LEFT);
            h.setFont(new Font(null, 0, 22));
            h.setIcon(JDUtilities.getScaledImageIcon(JDTheme.V("gui.images.config.tip"), 28, 28));
            cp.add(h, "growx, spanx");

            if (ai.getValidUntil() > -1) {
                cp.add(new JLabel("Valid until"), "newline, alignright, w 15%:pref");
                cp.add(getTextField(formater.format(new Date(ai.getValidUntil()))), "alignleft");
            }
            if (ai.getAccountBalance() > -1) {
                cp.add(new JLabel("Balance"), "newline, alignright, w 15%:pref");
                cp.add(getTextField(String.valueOf(ai.getAccountBalance())), "alignleft");
            }
            if (ai.getFilesNum() > -1) {
                cp.add(new JLabel("Files stored"), "newline, alignright, w 15%:pref");
                cp.add(getTextField(String.valueOf(ai.getFilesNum())), "alignleft");
            }
            if (ai.getUsedSpace() > -1) {
                cp.add(new JLabel("Used Space"), "newline, alignright, w 15%:pref");
                cp.add(getTextField(JDUtilities.formatBytesToMB(ai.getUsedSpace())), "alignleft");
            }
            if (ai.getPremiumPoints() > -1) {
                cp.add(new JLabel("PremiumPoints"), "newline, alignright, w 15%:pref");
                cp.add(getTextField(String.valueOf(ai.getPremiumPoints())), "alignleft");
            }
            if (ai.getTrafficLeft() > -1) {
                cp.add(new JLabel("Traffic left"), "newline, alignright, w 15%:pref");
                cp.add(getTextField(JDUtilities.formatBytesToMB(ai.getTrafficLeft())), "alignleft");
            }
            if (ai.getTrafficShareLeft() > -1) {
                cp.add(new JLabel("Trafficshare left"), "newline, alignright, w 15%:pref");
                cp.add(getTextField(JDUtilities.formatBytesToMB(ai.getTrafficShareLeft())), "alignleft");
            }

            cp.setCollapsed(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JTextField getTextField(String text) {
        JTextField help = new JTextField(text);
        help.setEditable(false);
        return help;
    }

    private void showGlobalHostInformation() {
        cp.setCollapsed(true);
        // cp.removeAll();
    }

    private void createPanel() {
        panel = new JPanel();
        panel.setLayout(new MigLayout("ins 5", "[right, pref!]10[100:pref, fill]0[right][90:pref, grow,fill]"));

        for (int i = 0; i < host.getPremiumAccounts().size(); i++) {
            new AccountPanel(panel, i + 1, this, host.getPremiumAccounts().get(i));
        }

        removeAll();
        add(sp = new JScrollPane(panel), "grow, push");

        add(cp, "south");

        sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.getVerticalScrollBar().setUnitIncrement(80);
        sp.getVerticalScrollBar().setBlockIncrement(190);
    }

    private class AccountPanel implements ChangeListener, ActionListener {
        private JCheckBox chkEnable;
        private JXLabel lblUsername;
        private JTextField txtUsername;
        private JTextField txtStatus;
        private JXButton btnCheck;
        private JXButton btnDelete;
        private Account account;
        private PremiumPane owner;

        public AccountPanel(JPanel panel, int nr, PremiumPane owner, Account account) {
            this.owner = owner;
            this.account = account;
            createPanel(panel, nr);
        }

        public void createPanel(JPanel panel, int nr) {
            if (premiumActivated) {
                chkEnable = new JCheckBox(JDLocale.LF("plugins.config.premium.accountnum", "<html><b>Premium Account #%s</b></html>", nr));
                chkEnable.setForeground(INACTIVE);
            } else {
                chkEnable = new JCheckBox(JDLocale.LF("plugins.config.premium.globaldeactiv", "<html><b>Global disabled</b></html>", nr));
                chkEnable.setForeground(DISABLED);
            }
            chkEnable.setSelected(account.isEnabled());

            panel.add(chkEnable, "alignleft, newline");
            chkEnable.addChangeListener(this);

            panel.add(btnCheck = new JXButton(JDLocale.L("plugins.config.premium.information.show", "Show Information")), "w pref:pref:pref, split 2");
            btnCheck.addActionListener(this);

            panel.add(btnDelete = new JXButton(JDUtilities.getScaledImageIcon(JDTheme.V("gui.images.delete"), -1, 14)));
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

        public void stateChanged(ChangeEvent e) {
            boolean sel = chkEnable.isSelected();
            if (premiumActivated) {
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
                    owner.showCollapseAccount(account);
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

    // private class ChartRefresh extends Thread {
    // @Override
    // public void run() {
    // Long collectTraffic = new Long(0);
    // freeTrafficChart.clear();
    // int accCounter = 0;
    // for (Account acc : getAccounts()) {
    // if (acc!=null && acc.getUser().length() > 0 && acc.getPass().length() >
    // 0) {
    // try {
    // accCounter++;
    // AccountInfo ai = host.getAccountInformation(acc);
    // Long tleft = new Long(ai.getTrafficLeft());
    // if (tleft >= 0 && ai.isExpired() == false) {
    // freeTrafficChart.addEntity(new ChartAPI_Entity(acc.getUser() + " [" +
    // (Math.round(tleft.floatValue() / 1024 / 1024 / 1024 * 100) / 100.0) +
    // " GB]", tleft, new Color(50, 255 - ((255 / (accountNum + 1)) *
    // accCounter), 50)));
    // long rest = ai.getTrafficMax() - tleft;
    // if (rest > 0) collectTraffic = collectTraffic + rest;
    // }
    // } catch (Exception e) {
    // JDUtilities.getLogger().finest("Not able to load Traffic-Limit for ChartAPI");
    // }
    // }
    // }
    //
    // if (collectTraffic > 0) freeTrafficChart.addEntity(new
    // ChartAPI_Entity(JDLocale.L("plugins.config.premium.chartapi.maxTraffic",
    // "Max. Traffic to collect") + " [" +
    // Math.round(((collectTraffic.floatValue() / 1024 / 1024 / 1024) * 100) /
    // 100.0) + " GB]", collectTraffic, new Color(150, 150, 150)));
    // freeTrafficChart.fetchImage();
    // }
    // }

    @Override
    public void onDisplay() {
        createPanel();
    }
}