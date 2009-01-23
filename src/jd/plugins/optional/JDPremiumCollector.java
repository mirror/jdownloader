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

package jd.plugins.optional;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import jd.HostPluginWrapper;
import jd.Main;
import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.MenuItem;
import jd.config.SubConfiguration;
import jd.event.ControlEvent;
import jd.gui.skins.simple.SimpleGUI;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class JDPremiumCollector extends PluginOptional {

    private SubConfiguration subConfig = JDUtilities.getSubConfig("JDPREMIUMCOLLECTOR");

    private static final String PROPERTY_API_URL = "PROPERTY_API_URL";
    private static final String PROPERTY_LOGIN_USER = "PROPERTY_LOGIN_USER";
    private static final String PROPERTY_LOGIN_PASS = "PROPERTY_LOGIN_PASS";

    private static final String PROPERTY_FETCHONSTARTUP = "PROPERTY_FETCHONSTARTUP";
    private static final String PROPERTY_ACCOUNTS = "PROPERTY_ACCOUNTS";
    private static final String PROPERTY_OVERWRITE = "PROPERTY_OVERWRITE";

    private JFrame guiFrame;

    public JDPremiumCollector(PluginWrapper wrapper) {
        super(wrapper);
        initConfigEntries();
    }

    private void fetchAccounts() {
        try {
            String post = "type=list";
            post += "&apiuser=" + subConfig.getStringProperty(PROPERTY_LOGIN_USER);
            post += "&apipassword=" + subConfig.getStringProperty(PROPERTY_LOGIN_PASS);
            br.postPage(subConfig.getStringProperty(PROPERTY_API_URL), post);
        } catch (IOException e1) {
            e1.printStackTrace();
            JOptionPane.showMessageDialog(guiFrame, JDLocale.L("plugins.optional.premiumcollector.error.url", "Probably wrong URL! See log for more infos!"), JDLocale.L("plugins.optional.premiumcollector.error", "Error!"), JOptionPane.ERROR_MESSAGE);
        }

        if (br.containsHTML("Login faild")) {
            JOptionPane.showMessageDialog(guiFrame, JDLocale.L("plugins.optional.premiumcollector.error.userpass", "Wrong username/password!"), JDLocale.L("plugins.optional.premiumcollector.error", "Error!"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        new Thread(new Runnable() {

            public void run() {
                String[][] accs = br.getRegex("<premacc id=\"(.*?)\">.*?<login>(.*?)</login>.*?<password>(.*?)</password>.*?<hoster>(.*?)</hoster>.*?</premacc>").getMatches();

                int accountsFound = 0;
                for (HostPluginWrapper plg : JDUtilities.getPluginsForHost()) {
                    if (!plg.isPremiumEnabled()) continue;

                    ArrayList<Account> accounts = new ArrayList<Account>();
                    for (String[] acc : accs) {
                        if (acc[3].equalsIgnoreCase(plg.getHost())) {
                            Account account = new Account(acc[1], acc[2]);
                            if (subConfig.getBooleanProperty(PROPERTY_ACCOUNTS, true)) {
                                try {
                                    AccountInfo accInfo = plg.getPlugin().getAccountInformation(account);
                                    if (accInfo != null && accInfo.isValid() && !accInfo.isExpired() && accInfo.getTrafficLeft() != 0) {
                                        accounts.add(account);
                                    } else {
                                        logger.finer(plg.getHost() + " : account " + account.getUser() + " is not valid; not added to list");
                                    }
                                } catch (Exception e1) {
                                    e1.printStackTrace();
                                }
                            } else {
                                accounts.add(account);
                            }
                        }
                    }

                    if (accounts.size() == 0) continue;
                    if (!subConfig.getBooleanProperty(PROPERTY_OVERWRITE, true)) {
                        if (JOptionPane.showConfirmDialog(guiFrame, JDLocale.LF("plugins.optional.premiumcollector.accountsFound.message", "Found %s accounts for %s plugin! Replace old saved accounts with new accounts?", accounts.size(), plg.getHost()), JDLocale.L("plugins.optional.premiumcollector.accountsFound", "Accounts found!"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) continue;
                    }

                    plg.getPlugin().setPremiumAccounts(accounts);
                    logger.finer(plg.getHost() + " : " + accounts.size() + " accounts inserted");
                    accountsFound += accounts.size();
                }

                SimpleGUI.CURRENTGUI.statusBarHandler.changeTxt(JDLocale.L("plugins.optional.premiumcollector.name", "PremiumCollector") + ": " + JDLocale.LF("plugins.optional.premiumcollector.inserted", "Successfully inserted %s accounts!", accountsFound), 10000, true);
                logger.info("totally : " + accountsFound + " accounts inserted");
            }

        }).start();

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof MenuItem) {
            MenuItem mi = (MenuItem) e.getSource();
            if (mi.getActionID() == 0) {
                fetchAccounts();
            } else if (mi.getActionID() == 1) {
                SimpleGUI.showConfigDialog(SimpleGUI.CURRENTGUI.getFrame(), getConfig());
            }
        }
    }

    @Override
    public String getRequirements() {
        return "JRE 1.5+";
    }

    @Override
    public boolean initAddon() {
        JDUtilities.getController().addControlListener(this);
        return true;
    }

    public void controlEvent(ControlEvent event) {
        if (event.getID() == ControlEvent.CONTROL_INIT_COMPLETE && event.getSource() instanceof Main) {
            guiFrame = SimpleGUI.CURRENTGUI.getFrame();
            if (subConfig.getBooleanProperty(PROPERTY_FETCHONSTARTUP, false)) {
                fetchAccounts();
            }
            return;
        }
        super.controlEvent(event);
    }

    private void initConfigEntries() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, subConfig, PROPERTY_API_URL, JDLocale.L("plugins.optional.premiumcollector.apiurl", "API-URL")).setDefaultValue("http://www.yourservicehere.org/api.php"));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, subConfig, PROPERTY_LOGIN_USER, JDLocale.L("plugins.optional.premiumcollector.username", "Username")).setDefaultValue("YOUR_USER"));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, subConfig, PROPERTY_LOGIN_PASS, JDLocale.L("plugins.optional.premiumcollector.password", "Password")).setDefaultValue("YOUR_PASS"));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_FETCHONSTARTUP, JDLocale.L("plugins.optional.premiumcollector.autoFetch", "Automatically fetch accounts on start-up")).setDefaultValue(true));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_ACCOUNTS, JDLocale.L("plugins.optional.premiumcollector.onlyValid", "Accept only valid and non-expired accounts")).setDefaultValue(true));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_OVERWRITE, JDLocale.L("plugins.optional.premiumcollector.overwrite", "Automatically overwrite accounts")).setDefaultValue(true));
    }

    @Override
    public void onExit() {
        JDUtilities.getController().removeControlListener(this);
    }

    @Override
    public ArrayList<MenuItem> createMenuitems() {
        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();

        menu.add(new MenuItem(MenuItem.NORMAL, JDLocale.L("plugins.optional.premiumcollector.fetchAccounts", "Fetch Accounts"), 0).setActionListener(this));
        menu.add(new MenuItem(MenuItem.SEPARATOR));
        menu.add(new MenuItem(MenuItem.NORMAL, JDLocale.L("gui.btn_settings", "Einstellungen"), 1).setActionListener(this));

        return menu;
    }

    @Override
    public String getHost() {
        return JDLocale.L("plugins.optional.premiumcollector.name", "PremiumCollector");
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    public static int getAddonInterfaceVersion() {
        return 2;
    }
}