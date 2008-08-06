package jd.gui.skins.simple.config.panels;

import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

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
import net.miginfocom.swing.MigLayout;

public class PremiumPanel extends JPanel implements ChangeListener {

    private static final Color INACTIVE = new Color(0xa40604);
    private static final Color ACTIVE = new Color(0x7cd622);
    private static final long serialVersionUID = 3275917572262383770L;
    private static final Insets INSETS = new Insets(2, 5, 2, 10);
    private JCheckBox[] enables;
    private JTextField[] usernames;
    private JPasswordField[] passwords;
    private JTextField[] stati;
    private ArrayList<Account> accounts;
    private JLabel[] usernamesLabels;
    private JLabel[] passwordsLabels;
    private JLabel[] statiLabels;
    private ConfigEntry configEntry;

    public PremiumPanel(GUIConfigEntry gce) {
        this.configEntry = gce.getConfigEntry();
        this.setLayout(new MigLayout("ins 5", "[right]10[grow,fill]15[right][grow,fill]"));
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
        statiLabels= new JLabel[accountNum];
        stati = new JTextField[accountNum];

        for (int i = 1; i <= accountNum; i++) {
            
            final JCheckBox active = new JCheckBox("<html><b>Premium Account #" + i + "</b></html>");
            active.setForeground(Color.red);
            active.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (active.isSelected()) {
                        active.setForeground(ACTIVE);
                    } else {
                        active.setForeground(INACTIVE);
                    }
                }
            });
            active.setSelected(i % 2 != 0 ? true : false);
            enables[i-1]=active;
            enables[i - 1].addChangeListener(this);
            add(active, "alignleft");
            add(new JSeparator(), "spanx, pushx, growx");

            add(usernamesLabels[i-1]=new JLabel("Premium User"), "gaptop 12");
            add(usernames[i-1]=new JTextField("coalado"));

            add(passwordsLabels[i-1]=new JLabel("Passwort"));
            add(passwords[i-1]=new JPasswordField("1234565789"), "wrap");

            add(statiLabels[i-1]=new JLabel("Account Status"));
            JTextField status = new JTextField("tuxla sold me into slavery :(");
            stati[i-1]=status;
            status.setEditable(false);
            add(status, "span, gapbottom :10:push");
        }
        
        
        
        
//        int entryHeight = 5;
//        int row = 0;
//        for (int i = 1; i <= accountNum; i++) {
//
//            JDUtilities.addToGridBag(this, new JLabel("Enabled account " + i), 0, (i - 1) * entryHeight + row, 1, 1, 0, 0, INSETS, GridBagConstraints.NONE, GridBagConstraints.WEST);
//
//            enables[i - 1] = new JCheckBox();
//            enables[i - 1].setEnabled(configEntry.isEnabled());
//            enables[i - 1].addChangeListener(this);
//            JDUtilities.addToGridBag(this, enables[i - 1], 2, (i - 1) * entryHeight + row, 1, 1, 0, 0, INSETS, GridBagConstraints.BOTH, GridBagConstraints.EAST);
//
//            row++;
//            // User
//            JDUtilities.addToGridBag(this, usernamesLabels[i - 1] = new JLabel("User"), 0, (i - 1) * entryHeight + row, 1, 1, 0, 0, INSETS, GridBagConstraints.NONE, GridBagConstraints.WEST);
//            usernames[i - 1] = new JTextField();
//            usernames[i - 1].setEnabled(configEntry.isEnabled());
//            usernames[i - 1].setHorizontalAlignment(SwingConstants.RIGHT);
//            JDUtilities.addToGridBag(this, usernames[i - 1], 2, (i - 1) * entryHeight + row, 1, 1, 1, 1, INSETS, GridBagConstraints.BOTH, GridBagConstraints.EAST);
//
//            row++;
//
//            // pass
//            JDUtilities.addToGridBag(this, passwordsLabels[i - 1] = new JLabel("Pass"), 0, (i - 1) * entryHeight + row, 1, 1, 0, 0, INSETS, GridBagConstraints.NONE, GridBagConstraints.WEST);
//            passwords[i - 1] = new JPasswordField();
//
//            passwords[i - 1].setEnabled(configEntry.isEnabled());
//            passwords[i - 1].setHorizontalAlignment(SwingConstants.RIGHT);
//            JDUtilities.addToGridBag(this, passwords[i - 1], 2, (i - 1) * entryHeight + row, 1, 1, 1, 1, INSETS, GridBagConstraints.BOTH, GridBagConstraints.EAST);
//
//            row++;
//            // Status
//            stati[i - 1] = new JLabel("AccountStatus Hier steht was mit dem acocunt zuletzt so los war");
//            stati[i - 1].setHorizontalAlignment(SwingConstants.RIGHT);
//            JDUtilities.addToGridBag(this, stati[i - 1], 0, (i - 1) * entryHeight + row, 3, 1, 0, 0, INSETS, GridBagConstraints.NONE, GridBagConstraints.WEST);
//
//            row++;
//
//            if (i != accountNum) {
//
//                JDUtilities.addToGridBag(this, new JSeparator(SwingConstants.HORIZONTAL), 0, (i - 1) * entryHeight + row, 3, 1, 1, 0, INSETS, GridBagConstraints.BOTH, GridBagConstraints.WEST);
//                row++;
//            }
//
//        }

    }

    public void stateChanged(ChangeEvent e) {
        for (int i = 0; i < enables.length; i++) {
            if (e.getSource() == enables[i]) {
                passwords[i].setEnabled(enables[i].isSelected());
                usernames[i].setEnabled(enables[i].isSelected());
                stati[i].setEnabled(enables[i].isSelected());
                passwordsLabels[i].setEnabled(enables[i].isSelected());
                usernamesLabels[i].setEnabled(enables[i].isSelected());
                statiLabels[i].setEnabled(enables[i].isSelected());
            }
        }

    }

}