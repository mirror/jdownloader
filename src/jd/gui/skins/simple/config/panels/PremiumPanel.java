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

package jd.gui.skins.simple.config.panels;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.config.Configuration;
import jd.gui.skins.simple.components.ChartAPI_Entity;
import jd.gui.skins.simple.components.ChartAPI_PIE;
import jd.gui.skins.simple.components.JLinkButton;
import jd.gui.skins.simple.config.GUIConfigEntry;
import jd.http.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class PremiumPanel extends JPanel {

    private static final long serialVersionUID = 3275917572262383770L;

    private static final Color ACTIVE = new Color(0x7cd622);
    private static final Color INACTIVE = new Color(0xa40604);
    private static final Color DISABLED = new Color(0xaff0000);
    private static boolean premiumActivated = true;
    private boolean specialCharsWarningDisplayed = false;
    private PluginForHost host;
    private int accountNum;
    private AccountPanel[] accs;

    private ChartAPI_PIE freeTrafficChart = new ChartAPI_PIE("", 450, 60, this.getBackground());

    public PremiumPanel(GUIConfigEntry gce) {
        host = (PluginForHost) gce.getConfigEntry().getActionListener();
        accountNum = gce.getConfigEntry().getEnd();
        setLayout(new MigLayout("ins 5", "[right, pref!]10[100:pref, grow,fill]0[right][100:pref, grow,fill]"));
        premiumActivated = JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true);
        createPanel();
    }

    /**
     * Gibt alle aktuellen Accounts zurück
     * 
     * @return
     */
    public ArrayList<Account> getAccounts() {
        ArrayList<Account> accounts = new ArrayList<Account>();
        for (AccountPanel acc : accs) {
            accounts.add(acc.getAccount());
        }
        return accounts;
    }

    /**
     * List ist immer eine ArrayList<Account> mit Daten aus der config
     * 
     * @param list
     */
    @SuppressWarnings("unchecked")
    public void setAccounts(Object list) {
        ArrayList<Account> accounts = (ArrayList<Account>) list;
        for (int i = 0; i < accountNum; i++) {
            if (i >= accounts.size()) break;
            if(accounts.get(i)!=null)
            accs[i].setAccount(accounts.get(i));
        }
        createDataset();
    }

    private void createDataset() {
        new ChartRefresh().start();
    }

    private void createPanel() {
        accs = new AccountPanel[accountNum];

        JPanel panel = this;
        JTabbedPane tab = new JTabbedPane();
        for (int i = 0; i < accountNum; i++) {
            if (i % 5 == 0 && accountNum > 5) {
                tab.add(panel = new JPanel());
                panel.setLayout(new MigLayout("ins 5", "[right, pref!]10[100:pref, grow,fill]0[right][100:pref, grow,fill]"));
            }
            accs[i] = new AccountPanel(panel, i + 1);
        }

        if (accountNum > 5) {
            int i;
            for (i = 0; i < tab.getTabCount() - 1; i++) {
                tab.setTitleAt(i, JDLocale.L("plugins.menu.accounts", "Accounts") + ": " + (i * 5 + 1) + " - " + ((i + 1) * 5));
            }
            tab.setTitleAt(i, JDLocale.L("plugins.menu.accounts", "Accounts") + ": " + ((i * 5 + 1 == accountNum) ? accountNum : (i * 5 + 1) + " - " + accountNum));
            this.add(tab, "span");
        }

        String premiumUrl = host.getBuyPremiumUrl();
        if (premiumUrl != null) {
            try {
                this.add(new JLinkButton(JDLocale.L("plugins.premium.premiumbutton", "Get Premium Account"), new URL("http://jdownloader.org/r.php?u=" + Encoding.urlEncode(premiumUrl))), "span, alignright");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        this.add(freeTrafficChart, "spanx, spany");
    }

    private class AccountPanel implements ChangeListener, ActionListener, FocusListener {

        private JCheckBox chkEnable;
        private JLabel lblUsername;
        private JLabel lblPassword;
        private JTextField txtUsername;
        private JDPasswordField txtPassword;
        private JTextField txtStatus;
        private JButton btnCheck;
        private JButton btnDelete;
        private Account account;

        public AccountPanel(JPanel panel, int nr) {
            createPanel(panel, nr);
        }

        public void setAccount(Account account) {
            boolean sel = account.isEnabled();
            this.account = account;
            txtUsername.setText(account.getUser());
            txtPassword.setText(account.getPass());
            txtStatus.setText(account.getStatus());
            chkEnable.setSelected(sel);
            lblUsername.setEnabled(sel);
            lblPassword.setEnabled(sel);
            txtUsername.setEnabled(sel);
            txtPassword.setEnabled(sel);
            txtStatus.setEnabled(sel);
            btnCheck.setEnabled(sel);
        }

        public Account getAccount() {
            String pass = new String(txtPassword.getPassword());
            if (account == null) return null;
            if (!account.getUser().equals(txtUsername.getText()) || !account.getPass().equals(pass)) {
                account.setUser(txtUsername.getText());
                account.setPass(pass);
               account.getProperties().clear();
          
            }

            account.setEnabled(chkEnable.isSelected());
            return account;
        }

        public void createPanel(JPanel panel, int nr) {
            if (premiumActivated) {
                chkEnable = new JCheckBox(JDLocale.LF("plugins.config.premium.accountnum", "<html><b>Premium Account #%s</b></html>", nr));
                chkEnable.setForeground(INACTIVE);
            } else {
                chkEnable = new JCheckBox(JDLocale.LF("plugins.config.premium.globaldeactiv", "<html><b>Global disabled</b></html>", nr));
                chkEnable.setForeground(DISABLED);
            }
            panel.add(chkEnable, "alignleft");
            chkEnable.addChangeListener(this);

            panel.add(btnCheck = new JButton(JDLocale.L("plugins.config.premium.test", "Get Status")), "w pref:pref:pref, split 2");
            btnCheck.addActionListener(this);

            panel.add(btnDelete = new JButton(JDUtilities.getScaledImageIcon(JDTheme.V("gui.images.exit"), -1, 14)));
            btnDelete.addActionListener(this);

            panel.add(new JSeparator(), "w 30:push, growx, pushx");
            panel.add(txtStatus = new JTextField(""), "spanx, pushx, growx");
            txtStatus.setEditable(false);

            panel.add(lblUsername = new JLabel(JDLocale.L("plugins.config.premium.user", "Premium User")), "gaptop 8");
            panel.add(txtUsername = new JTextField(""));
            KeyListener k = new KeyListener(){

                public void keyPressed(KeyEvent e) {
                    // TODO Auto-generated method stub
                    
                }

                public void keyReleased(KeyEvent e) {
                    if(!specialCharsWarningDisplayed && (""+e.getKeyChar()).matches("[^0-9a-zA-Z]*"))
                    {
                        JDUtilities.getGUI().showMessageDialog(JDLocale.LF("plugins.config.premium.specialCharsWarning", "Special chars may not work with %s", host.getHost()));
                        specialCharsWarningDisplayed=true;
                    }
                        
                    
                }

                public void keyTyped(KeyEvent e) {
                    // TODO Auto-generated method stub
                    
                }};
            if(!host.premiumSpecialCharsAllowed())
            txtUsername.addKeyListener(k);
            txtUsername.addFocusListener(this);

            panel.add(lblPassword = new JLabel(JDLocale.L("plugins.config.premium.password", "Password")), "gapleft 15");
            panel.add(txtPassword = new JDPasswordField(), "span, gapbottom 10:10:push");
            txtPassword.addFocusListener(this);
            if(!host.premiumSpecialCharsAllowed())
                txtPassword.addKeyListener(k);
            this.account = new Account(txtUsername.getText(), new String(txtPassword.getPassword()));
            chkEnable.setSelected(false);
            txtPassword.setEnabled(false);
            txtUsername.setEnabled(false);
            txtStatus.setEnabled(false);
            btnCheck.setEnabled(false);
            lblPassword.setEnabled(false);
            lblUsername.setEnabled(false);
            account.setEnabled(chkEnable.isSelected());
        }

        public void stateChanged(ChangeEvent e) {
            boolean sel = chkEnable.isSelected();
            if (premiumActivated) {
                chkEnable.setForeground((sel) ? ACTIVE : INACTIVE);
            } else {
                chkEnable.setForeground(DISABLED);
            }
            txtPassword.setEnabled(sel);
            txtUsername.setEnabled(sel);
            txtStatus.setEnabled(sel);
            btnCheck.setEnabled(sel);
            lblPassword.setEnabled(sel);
            lblUsername.setEnabled(sel);
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == btnCheck) {
                JDUtilities.getGUI().showAccountInformation(host, getAccount());
            } else if (e.getSource() == btnDelete) {
                txtUsername.setText("");
                txtPassword.setText("");
                txtStatus.setText("");
                chkEnable.setSelected(false);
                createDataset();
            }
        }

        public void focusGained(FocusEvent e) {
            ((JTextField) e.getSource()).selectAll();
        }

        public void focusLost(FocusEvent e) {
            createDataset();
        }
    }

    private class ChartRefresh extends Thread {
        @Override
        public void run() {
            Long collectTraffic = new Long(0);
            freeTrafficChart.clear();
            int accCounter = 0;
            for (Account acc : getAccounts()) {
                if (acc!=null && acc.getUser().length() > 0 && acc.getPass().length() > 0) {
                    try {
                        accCounter++;
                        AccountInfo ai = host.getAccountInformation(acc);
                        Long tleft = new Long(ai.getTrafficLeft());
                        if (tleft >= 0 && ai.isExpired() == false) {
                            freeTrafficChart.addEntity(new ChartAPI_Entity(acc.getUser() + " [" + (Math.round(tleft.floatValue() / 1024 / 1024 / 1024 * 100) / 100.0) + " GB]", tleft, new Color(50, 255 - ((255 / (accountNum + 1)) * accCounter), 50)));
                            long rest = ai.getTrafficMax() - tleft;
                            if (rest > 0) collectTraffic = collectTraffic + rest;
                        }
                    } catch (Exception e) {
                        JDUtilities.getLogger().finest("Not able to load Traffic-Limit for ChartAPI");
                    }
                }
            }

            if (collectTraffic > 0) freeTrafficChart.addEntity(new ChartAPI_Entity(JDLocale.L("plugins.config.premium.chartapi.maxTraffic", "Max. Traffic to collect") + " [" + Math.round(((collectTraffic.floatValue() / 1024 / 1024 / 1024) * 100) / 100.0) + " GB]", collectTraffic, new Color(150, 150, 150)));
            freeTrafficChart.fetchImage();
        }
    }

    private class JDPasswordField extends JPasswordField implements ClipboardOwner {

        private static final long serialVersionUID = -7981118302661369727L;

        public JDPasswordField() {
            super();
        }

        @Override
        public void cut() {
            StringSelection stringSelection = new StringSelection(String.valueOf(this.getSelectedText()));
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, this);

            String text = String.valueOf(this.getPassword());
            int position = this.getSelectionStart();
            String s1 = text.substring(0, position);
            String s2 = text.substring(this.getSelectionEnd(), text.length());
            this.setText(s1 + s2);

            this.setSelectionStart(position);
            this.setSelectionEnd(position);
        }

        @Override
        public void copy() {
            StringSelection stringSelection = new StringSelection(String.valueOf(this.getSelectedText()));
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, this);
        }

        public void lostOwnership(Clipboard arg0, Transferable arg1) {
        }

    }

}