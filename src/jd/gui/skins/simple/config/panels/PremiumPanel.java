//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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

import jd.config.ConfigEntry;
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

public class PremiumPanel extends JPanel implements ChangeListener, ActionListener, FocusListener {
    class ChartRefresh extends Thread {
        public void run() {
            Long collectTraffic = new Long(0);
            freeTrafficChart.clear();
            int accCounter = 0;
            for (Account acc : getAccounts()) {
                if (acc.getUser().length() > 0 && acc.getPass().length() > 0) {
                    try {
                        accCounter++;
                        AccountInfo ai = ((PluginForHost) configEntry.getActionListener()).getAccountInformation(acc);
                        Long tleft = new Long(ai.getTrafficLeft());
                        if (tleft >= 0 && ai.isExpired() == false) {
                            freeTrafficChart.addEntity(new ChartAPI_Entity(acc.getUser() + " [" + (Math.round(tleft.floatValue() / 1024 / 1024 / 1024 * 100) / 100.0) + " GB]", tleft, new Color(50, 255 - ((255 / (accounts.size() + 1)) * accCounter), 50)));
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

    private static final Color ACTIVE = new Color(0x7cd622);
    private static final Color INACTIVE = new Color(0xa40604);
    private static final long serialVersionUID = 3275917572262383770L;

    private JCheckBox[] enables;
    private JTextField[] usernames;
    private JDPasswordField[] passwords;
    private JTextField[] stati;
    private ArrayList<Account> accounts;
    private JLabel[] usernamesLabels;
    private JLabel[] passwordsLabels;
    private ConfigEntry configEntry;
    private JButton[] checkBtns;
    private JButton[] delete;
    private JLinkButton buybutton;

    private ChartAPI_PIE freeTrafficChart = new ChartAPI_PIE("", 450, 60, this.getBackground());
    private ChartRefresh loader;

    public PremiumPanel(GUIConfigEntry gce) {
        this.configEntry = gce.getConfigEntry();
        this.setLayout(new MigLayout("ins 5", "[right, pref!]10[100:pref, grow,fill]0[right][100:pref, grow,fill]"));
        this.createPanel();
    }

    /**
     * Gibt alle aktuellen Accounts zurück
     * 
     * @return
     */
    public ArrayList<Account> getAccounts() {
        ArrayList<Account> accounts = new ArrayList<Account>();
        for (int i = 0; i < enables.length; i++) {
            Account a = new Account(usernames[i].getText(), new String(passwords[i].getPassword()));
            a.setEnabled(enables[i].isSelected());
            accounts.add(a);
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
        this.accounts = (ArrayList<Account>) list;
        int i = 0;
        for (Account account : accounts) {
            if (i >= enables.length) break;
            enables[i].setSelected(account.isEnabled());
            usernames[i].setText(account.getUser());
            passwords[i].setText(account.getPass());
            stati[i].setText(account.getStatus());
            passwords[i].setEnabled(account.isEnabled());
            usernames[i].setEnabled(account.isEnabled());
            stati[i].setEnabled(account.isEnabled());
            checkBtns[i].setEnabled(account.isEnabled());
            passwordsLabels[i].setEnabled(account.isEnabled());
            usernamesLabels[i].setEnabled(account.isEnabled());
            i++;
        }
        createDataset();
    }

    /**
     * Creates the Dataset.
     * 
     * @param nothing
     * 
     * @return nothing
     */
    private void createDataset() {
        loader = new ChartRefresh();
        loader.start();
    }

    private void createPanel() {

        int accountNum = configEntry.getEnd();
        enables = new JCheckBox[accountNum];
        usernames = new JTextField[accountNum];
        passwords = new JDPasswordField[accountNum];
        usernamesLabels = new JLabel[accountNum];
        passwordsLabels = new JLabel[accountNum];
        stati = new JTextField[accountNum];
        checkBtns = new JButton[accountNum];
        delete = new JButton[accountNum];
        ArrayList<Account> list = new ArrayList<Account>();
        String premiumurl = null;
        if (((PluginForHost) configEntry.getActionListener()).getBuyPremiumUrl() != null) {
            premiumurl = "http://jdownloader.org/r.php?u=" + Encoding.urlEncode(((PluginForHost) configEntry.getActionListener()).getBuyPremiumUrl());
        }
        try {
            if (premiumurl != null) buybutton = new JLinkButton(JDLocale.L("plugins.premium.premiumbutton", "Get Premium Account"), new URL(premiumurl));
        } catch (MalformedURLException e1) {
            premiumurl = null;
        }

        JPanel panel = this;
        JTabbedPane tab = new JTabbedPane();
        for (int i = 0; i < accountNum; i++) {
            if (i % 5 == 0) {
                tab.add(panel = new JPanel());
                panel.setLayout(new MigLayout("ins 5", "[right, pref!]10[100:pref, grow,fill]0[right][100:pref, grow,fill]"));
            }
            list.add(new Account("", ""));
            final JCheckBox active = new JCheckBox(JDLocale.LF("plugins.config.premium.accountnum", "<html><b>Premium Account #%s</b></html>", i + 1));
            active.setForeground(INACTIVE);
            active.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (active.isSelected()) {
                        active.setForeground(ACTIVE);
                    } else {
                        active.setForeground(INACTIVE);
                    }
                }
            });
            active.setSelected(true);
            enables[i] = active;
            enables[i].addChangeListener(this);
            panel.add(active, "alignleft");

            panel.add(checkBtns[i] = new JButton(JDLocale.L("plugins.config.premium.test", "Get Status")), "w pref:pref:pref, split 2");
            checkBtns[i].addActionListener(this);

            panel.add(delete[i] = new JButton(JDUtilities.getScaledImageIcon(JDTheme.V("gui.images.exit"), -1, 14)));
            delete[i].addActionListener(this);

            panel.add(new JSeparator(), "w 30:push, growx, pushx");
            panel.add(stati[i] = new JTextField(""), "spanx, pushx, growx");
            stati[i].setEditable(false);

            panel.add(usernamesLabels[i] = new JLabel(JDLocale.L("plugins.config.premium.user", "Premium User")), "gaptop 8");
            panel.add(usernames[i] = new JTextField(""));
            usernames[i].addFocusListener(this);

            panel.add(passwordsLabels[i] = new JLabel(JDLocale.L("plugins.config.premium.password", "Password")), "gapleft 15");
            panel.add(passwords[i] = new JDPasswordField(), "span, gapbottom 10:10:push");
            passwords[i].addFocusListener(this);

            enables[i].setSelected(false);
        }
        int i;
        for (i = 0; i < tab.getTabCount() - 1; i++) {
            tab.setTitleAt(i, JDLocale.L("plugins.menu.accounts", "Accounts") + ": " + (i * 5 + 1) + " - " + ((i + 1) * 5));
        }
        tab.setTitleAt(i, JDLocale.L("plugins.menu.accounts", "Accounts") + ": " + ((i * 5 + 1 == accountNum) ? accountNum : (i * 5 + 1) + " - " + accountNum));
        if (accountNum > 5) add(tab, "span");
        if (premiumurl != null) add(buybutton, "span, alignright");
        add(freeTrafficChart, "spanx, spany");

    }

    public void stateChanged(ChangeEvent e) {
        for (int i = 0; i < enables.length; i++) {
            if (e.getSource() == enables[i]) {
                passwords[i].setEnabled(enables[i].isSelected());
                usernames[i].setEnabled(enables[i].isSelected());
                stati[i].setEnabled(enables[i].isSelected());
                checkBtns[i].setEnabled(enables[i].isSelected());
                passwordsLabels[i].setEnabled(enables[i].isSelected());
                usernamesLabels[i].setEnabled(enables[i].isSelected());
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        int accountNum = configEntry.getEnd();
        ArrayList<Account> acc = this.getAccounts();
        for (int i = 0; i < accountNum; i++) {
            if (e.getSource() == this.checkBtns[i]) {
                JDUtilities.getGUI().showAccountInformation(((PluginForHost) configEntry.getActionListener()), acc.get(i));
            } else if (e.getSource() == this.delete[i]) {
                usernames[i].setText("");
                passwords[i].setText("");
                stati[i].setText("");
                enables[i].setSelected(false);
                createDataset();
            }
        }
    }

    public void focusGained(FocusEvent e) {
        ((JTextField) e.getSource()).selectAll();
    }

    public void focusLost(FocusEvent e) {
        createDataset();
    }

    private class JDPasswordField extends JPasswordField implements ClipboardOwner {

        private static final long serialVersionUID = -7981118302661369727L;

        public JDPasswordField() {
            super();
        }

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

        public void copy() {

            StringSelection stringSelection = new StringSelection(String.valueOf(this.getSelectedText()));
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, this);

        }

        public void lostOwnership(Clipboard arg0, Transferable arg1) {
        }

    }

}