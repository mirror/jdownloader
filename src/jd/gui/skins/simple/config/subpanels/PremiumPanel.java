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

package jd.gui.skins.simple.config.subpanels;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.controlling.JDLogger;
import jd.gui.skins.simple.components.ChartAPIEntity;
import jd.gui.skins.simple.components.JLinkButton;
import jd.gui.skins.simple.components.PieChartAPI;
import jd.gui.skins.simple.config.GUIConfigEntry;
import jd.http.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.JXTitledSeparator;

public class PremiumPanel extends JPanel {

    private static final long serialVersionUID = 3275917572262383770L;

    private static final Color ACTIVE = new Color(0x7cd622);
    private static final Color INACTIVE = new Color(0xa40604);
    private static final Color DISABLED = new Color(0xaff0000);

    private static final Color BG_COLOR = new Color(0.0f, 0.0f, 0.0f, 0.0f);
    private static boolean premiumActivated = true;

    private PluginForHost host;

    private ArrayList<AccountPanel> accs;

    private PieChartAPI freeTrafficChart = new PieChartAPI("", 450, 60, BG_COLOR);

    private ConfigEntry ce;

    private JButton add;

    private ArrayList<Account> list;

    public PremiumPanel(GUIConfigEntry gce) {
        ce = gce.getConfigEntry();
        host = (PluginForHost) gce.getConfigEntry().getActionListener();

        setLayout(new MigLayout("ins 5", "[fill, grow]", "[fill, grow]"));
        premiumActivated = JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true);

    }

    /**
     * Gibt alle aktuellen Accounts zurück
     * 
     * @return
     */
    public ArrayList<Account> getAccounts() {
        ArrayList<Account> accounts = new ArrayList<Account>();
        if (accs != null) {
            for (AccountPanel acc : accs) {
                accounts.add(acc.getAccount());
            }
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
        this.list = accounts;
        createPanel(accounts.size());
        for (int i = 0; i < accs.size(); i++) {
            if (i >= accounts.size()) break;
            if (accounts.get(i) != null) accs.get(i).setAccount(accounts.get(i));
        }
        createDataset();
    }

    private void createDataset() {
        new ChartRefresh().start();
    }

    private void createPanel(int j) {
        accs = new ArrayList<AccountPanel>();
        JPanel panel = new JPanel(new MigLayout("ins 0, wrap 2", "[grow, fill][grow, fill]", "[grow, fill]"));
        removeAll();
        JScrollPane sp;
        add(sp = new JScrollPane(panel));
        sp.setBorder(null);
        for (int i = 0; i < j; i++) {
            AccountPanel p = new AccountPanel(i);

            panel.add(p, "spanx");
            accs.add(p);
        }

        add = createButton(JDLocale.L("plugins.premium.add", "Add new account"), JDTheme.II("gui.images.add", 16, 16));
        add.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                list.add(new Account("", new String("")));
                setAccounts(list);
            }

        });

        final String premiumUrl = host.getBuyPremiumUrl();
        JButton buy = createButton(JDLocale.L("plugins.premium.premiumbutton", "Get Premium Account"), JDTheme.II("gui.images.buy", 16, 16));
        buy.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                try {
                    JLinkButton.openURL(new URL("http://jdownloader.org/r.php?u=" + Encoding.urlEncode(premiumUrl)));
                } catch (MalformedURLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        });

        panel.add(add, "alignx left");
        panel.add(freeTrafficChart, "spany 2");
        panel.add(buy, "alignx left");
        this.getParent().invalidate();
        this.getParent().repaint();
    }

    public JButton createButton(String string, Icon i) {
        JButton bt;
        if (i != null) {
            bt = new JButton(string, i);
        } else {
            bt = new JButton(string);
        }

        bt.setContentAreaFilled(false);
        bt.setCursor(Cursor.getPredefinedCursor(12));
        bt.setFocusPainted(false);
        bt.setBorderPainted(false);
        bt.setHorizontalAlignment(JButton.LEFT);

        bt.addMouseListener(new MouseAdapter() {

            private Font originalFont;

            @SuppressWarnings("unchecked")
            @Override
            public void mouseEntered(MouseEvent evt) {
                JButton src = (JButton) evt.getSource();

                originalFont = src.getFont();
                if (src.isEnabled()) {
                    Map attributes = originalFont.getAttributes();
                    attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
                    src.setFont(originalFont.deriveFont(attributes));
                }

            }

            @Override
            public void mouseExited(MouseEvent evt) {
                JButton src = (JButton) evt.getSource();
                src.setFont(originalFont);
            }
        });
        return bt;
    }

    private class AccountPanel extends JPanel implements ChangeListener, ActionListener, FocusListener {

        private JCheckBox chkEnable;
        private JLabel lblUsername;
        private JLabel lblPassword;
        private JTextField txtUsername;
        private JDPasswordField txtPassword;
        private JTextField txtStatus;
        private JButton btnCheck;
        private JButton btnDelete;
        private Account account;
        private int panelID;
        private JXCollapsiblePane info;

        public AccountPanel(int nr) {
            this.panelID = nr;
            createPanel(nr);
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

        public void createPanel(int nr) {
            this.setLayout(new MigLayout("ins 5, wrap 4", "[shrink][grow 20][shrink][grow 20]"));
            if (premiumActivated) {
                chkEnable = new JCheckBox(JDLocale.LF("plugins.config.premium.accountnum", "<html><b>Premium Account #%s</b></html>", nr));
                chkEnable.setForeground(INACTIVE);
            } else {
                chkEnable = new JCheckBox(JDLocale.LF("plugins.config.premium.globaldeactiv", "<html><b>Global disabled</b></html>", nr));
                chkEnable.setForeground(DISABLED);
            }
            add(chkEnable, "alignx left");
            chkEnable.addChangeListener(this);

            add(btnCheck = new JButton(JDLocale.L("plugins.config.premium.test", "Get Status")), "split 3,spanx 3");
            btnCheck.addActionListener(this);
            btnCheck.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            add(btnDelete = new JButton(JDTheme.II("gui.images.undo", 16, 16)), "shrinkx");
            btnDelete.addActionListener(this);
            btnDelete.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnDelete.setToolTipText(JDLocale.L("plugins.config.premium.delete", "Remove this account"));

            add(txtStatus = new JTextField(""), "spanx, pushx, growx,gapleft 20");
            txtStatus.setBorder(null);
            txtStatus.setBackground(null);
            txtStatus.setOpaque(false);
            txtStatus.setEditable(false);

            add(lblUsername = new JLabel(JDLocale.L("plugins.config.premium.user", "Premium User")), "newline,alignx right");
            add(txtUsername = new JTextField(""), "spanx 1, growx");

            txtUsername.addFocusListener(this);

            add(lblPassword = new JLabel(JDLocale.L("plugins.config.premium.password", "Password")), "alignx right,gapleft 15");

            add(txtPassword = new JDPasswordField(), "growx");
            txtPassword.addFocusListener(this);

            this.account = new Account(txtUsername.getText(), new String(txtPassword.getPassword()));
            chkEnable.setSelected(false);
            txtPassword.setEnabled(false);
            txtUsername.setEnabled(false);
            txtStatus.setEnabled(false);
            btnCheck.setEnabled(false);
            lblPassword.setEnabled(false);
            lblUsername.setEnabled(false);
            account.setEnabled(chkEnable.isSelected());
            info = new JXCollapsiblePane();
            info.setCollapsed(false);
       
            add(info, "spanx,growx,newline");
        }

        public void stateChanged(ChangeEvent e) {
            boolean sel = chkEnable.isSelected();

            if (this.account.isEnabled() != sel) {
                ce.setChanges(true);
            }
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
                info.setCollapsed(false);
                AccountInfo ai;
                try {
                    ai = host.getAccountInformation(account);

                    info.getContentPane().setLayout(new MigLayout("ins 22", "[right]10[grow,fill]40"));
                    String def = String.format(JDLocale.L("plugins.host.premium.info.title", "Accountinformation from %s for %s"), account.getUser(), host.getHost());
                    String[] label = new String[] { JDLocale.L("plugins.host.premium.info.validUntil", "Valid until"), JDLocale.L("plugins.host.premium.info.trafficLeft", "Traffic left"), JDLocale.L("plugins.host.premium.info.files", "Files"), JDLocale.L("plugins.host.premium.info.premiumpoints", "PremiumPoints"), JDLocale.L("plugins.host.premium.info.usedSpace", "Used Space"), JDLocale.L("plugins.host.premium.info.cash", "Cash"), JDLocale.L("plugins.host.premium.info.trafficShareLeft", "Traffic Share left"), JDLocale.L("plugins.host.premium.info.status", "Info") };

                    DateFormat formater = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
                    String validUntil = (ai.isExpired() ? "[expired] " : "") + formater.format(new Date(ai.getValidUntil())) + "";
                    if (ai.getValidUntil() == -1) validUntil = null;
                    String premiumPoints = ai.getPremiumPoints() + ((ai.getNewPremiumPoints() > 0) ? " [+" + ai.getNewPremiumPoints() + "]" : "");
                    String[] data = new String[] { validUntil, JDUtilities.formatBytesToMB(ai.getTrafficLeft()), ai.getFilesNum() + "", premiumPoints, JDUtilities.formatBytesToMB(ai.getUsedSpace()), ai.getAccountBalance() < 0 ? null : (ai.getAccountBalance() / 100.0) + " €", JDUtilities.formatBytesToMB(ai.getTrafficShareLeft()), ai.getStatus() };
                    info.getContentPane().add(new JXTitledSeparator(def), "spanx, pushx, growx, gapbottom 15");
                    PieChartAPI freeTrafficChart = new PieChartAPI("", 125, 60, BG_COLOR);
                    freeTrafficChart.addEntity(new ChartAPIEntity("Free", ai.getTrafficLeft(), new Color(50, 200, 50)));
                    freeTrafficChart.addEntity(new ChartAPIEntity("", ai.getTrafficMax() - ai.getTrafficLeft(), new Color(150, 150, 150)));
                    freeTrafficChart.fetchImage();

                    for (int j = 0; j < data.length; j++) {
                        if (data[j] != null && !data[j].equals("-1")) {
                            info.getContentPane().add(new JLabel(label[j]), "gapleft 20");
                            if (label[j].equals(JDLocale.L("plugins.host.premium.info.trafficLeft", "Traffic left"))) {
                                JPanel panel2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                                JTextField tf;
                                panel2.add(tf = new JTextField(data[j]));
                                tf.setBorder(null);
                                tf.setBackground(null);
                                tf.setEditable(false);
                                tf.setOpaque(false);
                                panel2.add(freeTrafficChart);
                                info.getContentPane().add(panel2, "wrap");
                            } else {
                                JTextField tf;
                                info.getContentPane().add(tf = new JTextField(data[j]), "wrap");
                                tf.setBorder(null);
                                tf.setBackground(null);
                                tf.setEditable(false);
                                tf.setOpaque(false);

                            }
                        }

                    }
                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }

            } else if (e.getSource() == btnDelete) {
                list.remove(panelID);
                ce.setChanges(true);
                setAccounts(list);

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
                if (acc != null && acc.getUser().length() > 0 && acc.getPass().length() > 0) {
                    try {
                        accCounter++;
                        AccountInfo ai = host.getAccountInformation(acc);
                        Long tleft = new Long(ai.getTrafficLeft());
                        if (tleft >= 0 && ai.isExpired() == false) {
                            freeTrafficChart.addEntity(new ChartAPIEntity(acc.getUser() + " [" + (Math.round(tleft.floatValue() / 1024 / 1024 / 1024 * 100) / 100.0) + " GB]", tleft, new Color(50, 255 - ((255 / (accs.size() + 1)) * accCounter), 50)));
                            long rest = ai.getTrafficMax() - tleft;
                            if (rest > 0) collectTraffic = collectTraffic + rest;
                        }
                    } catch (Exception e) {
                        JDLogger.getLogger().finest("Not able to load Traffic-Limit for ChartAPI");
                    }
                }
            }

            if (collectTraffic > 0) freeTrafficChart.addEntity(new ChartAPIEntity(JDLocale.L("plugins.config.premium.chartapi.maxTraffic", "Max. Traffic to collect") + " [" + Math.round(((collectTraffic.floatValue() / 1024 / 1024 / 1024) * 100) / 100.0) + " GB]", collectTraffic, new Color(150, 150, 150)));
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