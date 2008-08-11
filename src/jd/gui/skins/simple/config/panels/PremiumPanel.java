package jd.gui.skins.simple.config.panels;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.config.ConfigEntry;
import jd.gui.skins.simple.config.GUIConfigEntry;
import jd.plugins.Account;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class PremiumPanel extends JPanel implements ChangeListener, ActionListener, FocusListener {

    private static final Color ACTIVE = new Color(0x7cd622);
    private static final Color INACTIVE = new Color(0xa40604);
    private static final long serialVersionUID = 3275917572262383770L;

    private JCheckBox[] enables;
    private JTextField[] usernames;
    private JPasswordField[] passwords;
    private JTextField[] stati;
    private ArrayList<Account> accounts;
    private JLabel[] usernamesLabels;
    private JLabel[] passwordsLabels;
    private JLabel[] statiLabels;
    private ConfigEntry configEntry;
    private JButton[] checkBtns;

    public PremiumPanel(GUIConfigEntry gce) {
        this.configEntry = gce.getConfigEntry();
        this.setLayout(new MigLayout("ins 5", "[right]10[grow,fill]0[right][grow,fill]"));
        this.createPanel();
    }

    /**
     * Muss immer ein ArrayList<Account> mit den neuen daten zurückgeben
     * 
     * @return
     */
    public Object getAccounts() {
        ArrayList<Account> accounts = new ArrayList<Account>();
        for (int i = 0; i < enables.length; i++) {
            Account a = new Account(usernames[i].getText(), new String(passwords[i].getPassword()));
            a.setEnabled(enables[i].isSelected());
            accounts.add(a);
        }
        return accounts;
    }

    /**
     * list ist immer ein ArrayList<Account> mit daten aus der config
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
            passwordsLabels[i].setEnabled(enables[i].isSelected());
            usernamesLabels[i].setEnabled(enables[i].isSelected());
            statiLabels[i].setEnabled(enables[i].isSelected());
            i++;
        }

    }

    private void createPanel() {

        int accountNum = configEntry.getEnd();
        // addInstantHelpLink();

        enables = new JCheckBox[accountNum];
        usernames = new JTextField[accountNum];
        passwords = new JPasswordField[accountNum];
        usernamesLabels = new JLabel[accountNum];
        passwordsLabels = new JLabel[accountNum];
        statiLabels = new JLabel[accountNum];
        stati = new JTextField[accountNum];
        checkBtns = new JButton[accountNum];
        for (int i = 1; i <= accountNum; i++) {

            final JCheckBox active = new JCheckBox(JDLocale.LF("plugins.config.premium.accountnum", "<html><b>Premium Account #%s</b></html>", i));
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
            active.setSelected(false);
            enables[i - 1] = active;
            enables[i - 1].addChangeListener(this);
            add(active, "alignleft");
            
            JButton bt = new JButton(JDLocale.L("plugins.config.premium.test", "Get Status"));
            checkBtns[i - 1] = bt;
            add(bt, "w pref:pref:pref, split 2");
            bt.addActionListener(this);
            add(new JSeparator(), "w 30:push, growx, pushx");
            
            add(new JSeparator(), "w 30:push, growx, pushx");
            statiLabels[i - 1] = new JLabel(JDLocale.L("plugins.config.premium.accountstatus", "Last Account Status"));
            JTextField status = new JTextField("");
            stati[i - 1] = status;
            status.setEditable(false);
            add(status, "spanx, pushx, growx");
            
            add(usernamesLabels[i - 1] = new JLabel(JDLocale.L("plugins.config.premium.user", "Premium User")), "gaptop 8");
            add(usernames[i - 1] = new JTextField(""));
            usernames[i - 1].addFocusListener(this);
            add(passwordsLabels[i - 1] = new JLabel(JDLocale.L("plugins.config.premium.password", "Password")), "gapleft 15");
// add(passwords[i - 1] = new JPasswordField(""), "wrap");
            add(passwords[i - 1] = new JPasswordField(""), "span, gapbottom 40:40:push");
            passwords[i - 1].addFocusListener(this);
// add(statiLabels[i - 1] = new
// JLabel(JDLocale.L("plugins.config.premium.accountstatus", "Last Account
// Status")));
// JTextField status = new JTextField("");
// stati[i - 1] = status;
// status.setEditable(false);
// add(status, "span, gapbottom :10:push");
        }

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
                statiLabels[i].setEnabled(enables[i].isSelected());
            }
        }

    }

    @SuppressWarnings("unchecked")
    public void actionPerformed(ActionEvent e) {
        int accountNum = configEntry.getEnd();
        ArrayList<Account> acc = (ArrayList<Account>) this.getAccounts();
        for (int i = 1; i <= accountNum; i++) {
            if (e.getSource() == this.checkBtns[i - 1]) {
                JDUtilities.getGUI().showAccountInformation(((PluginForHost) configEntry.getActionListener()), acc.get(i - 1));
            }

        }

    }

    public void focusGained(FocusEvent e) {
        e=e;
        String text = ((JTextField) e.getSource()).getText();
       ((JTextField) e.getSource()).setSelectionStart(0);
       ((JTextField) e.getSource()).setSelectionEnd(text.length());
        // TODO Auto-generated method stub
        
    }

    public void focusLost(FocusEvent e) {
        // TODO Auto-generated method stub
        
    }
}