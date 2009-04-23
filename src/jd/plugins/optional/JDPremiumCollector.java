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
import java.util.Collections;

import jd.HostPluginWrapper;
import jd.Main;
import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.MenuItem;
import jd.config.SubConfiguration;
import jd.controlling.AccountManager;
import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.event.ControlEvent;
import jd.nutils.jobber.JDRunnable;
import jd.nutils.jobber.Jobber;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class JDPremiumCollector extends PluginOptional {

    private SubConfiguration subConfig = null;

    private static final String PROPERTY_API_URL = "PROPERTY_API_URL";
    private static final String PROPERTY_LOGIN_USER = "PROPERTY_LOGIN_USER";
    private static final String PROPERTY_LOGIN_PASS = "PROPERTY_LOGIN_PASS";

    private static final String PROPERTY_ACCOUNTS = "PROPERTY_ACCOUNTS";
    private static final String PROPERTY_ACCOUNTS2 = "PROPERTY_ACCOUNTS2";

    private Thread t;

    public JDPremiumCollector(PluginWrapper wrapper) {
        super(wrapper);
        subConfig = SubConfiguration.getConfig("JDPREMIUMCOLLECTOR");
        initConfigEntries();
    }

    private void fetchAccounts() {
        if (t != null && t.isAlive()) return;
        try {
            String post = "type=list";
            post += "&apiuser=" + subConfig.getStringProperty(PROPERTY_LOGIN_USER);
            post += "&apipassword=" + subConfig.getStringProperty(PROPERTY_LOGIN_PASS);
            br.postPage(subConfig.getStringProperty(PROPERTY_API_URL), post);
        } catch (IOException e1) {
            logger.severe(JDLocale.L("plugins.optional.premiumcollector.error.url", "Probably wrong URL! See log for more infos!"));
        }

        if (br.containsHTML("Login faild")) {
            logger.severe(JDLocale.L("plugins.optional.premiumcollector.error.userpass", "Wrong username/password!"));
            return;
        }

        t = new Thread(new Runnable() {

            public void run() {
                final String[][] accs = br.getRegex("<premacc id=\"(.*?)\">.*?<login>(.*?)</login>.*?<password>(.*?)</password>.*?<hoster>(.*?)</hoster>.*?</premacc>").getMatches();
                Jobber accountJobbers = new Jobber(4);
                ProgressController pc = new ProgressController("PremiumCollector");
                pc.setRange(1);
                for (HostPluginWrapper plg : JDUtilities.getPluginsForHost()) {
                    if (!plg.isPremiumEnabled()) continue;
                    AccountThread aT = new AccountThread(accs, plg, pc);
                    accountJobbers.add(aT);
                }
                accountJobbers.start();
                int todo = accountJobbers.getJobsAdded();
                accountJobbers.start();
                pc.setRange(todo);
                while (accountJobbers.getJobsFinished() != todo) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        accountJobbers.stop();
                        return;
                    }
                }
                pc.finalize();
            }

        });
        t.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof MenuItem && ((MenuItem) e.getSource()).getActionID() == 0) {
            fetchAccounts();
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

    @Override
    public void controlEvent(ControlEvent event) {
        if (event.getID() == ControlEvent.CONTROL_INIT_COMPLETE && event.getSource() instanceof Main) {
            fetchAccounts();
            JDUtilities.getController().removeControlListener(this);
            return;
        }
        super.controlEvent(event);
    }

    private void initConfigEntries() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, subConfig, PROPERTY_API_URL, JDLocale.L("plugins.optional.premiumcollector.apiurl", "API-URL")).setDefaultValue("http://www.yourservicehere.org/api.php"));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, subConfig, PROPERTY_LOGIN_USER, JDLocale.L("plugins.optional.premiumcollector.username", "Username")).setDefaultValue("YOUR_USER"));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, subConfig, PROPERTY_LOGIN_PASS, JDLocale.L("plugins.optional.premiumcollector.password", "Password")).setDefaultValue("YOUR_PASS"));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_ACCOUNTS, JDLocale.L("plugins.optional.premiumcollector.onlyValid", "Accept only valid and non-expired accounts")).setDefaultValue(true));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_ACCOUNTS2, JDLocale.L("plugins.optional.premiumcollector.onlyValid2", "Remove invalid and expired accounts")).setDefaultValue(true));
    }

    @Override
    public void onExit() {
        JDUtilities.getController().removeControlListener(this);
    }

    @Override
    public ArrayList<MenuItem> createMenuitems() {
        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();

        menu.add(new MenuItem(MenuItem.NORMAL, JDLocale.L("plugins.optional.premiumcollector.fetchAccounts", "Fetch Accounts"), 0).setActionListener(this));

        return menu;
    }

    @Override
    public String getIconKey() {
        return "gui.images.taskpanes.premium";
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

    private class AccountThread extends Thread implements JDRunnable {
        private String[][] accs;
        private HostPluginWrapper plg;
        private ProgressController pc;

        public AccountThread(String[][] accs, HostPluginWrapper plg, ProgressController pc) {
            this.accs = accs;
            this.plg = plg;
            this.pc = pc;
        }

        @Override
        public void run() {
            ArrayList<Account> accounts = new ArrayList<Account>();
            for (String[] acc : accs) {
                if (acc[3].equalsIgnoreCase(plg.getHost())) {
                    Account account = new Account(acc[1], acc[2]);
                    account.setProperty("PREMCOLLECTOR", true);
                    if (subConfig.getBooleanProperty(PROPERTY_ACCOUNTS, true)) {
                        try {
                            AccountInfo accInfo = plg.getPlugin().getAccountInformation(account);
                            if (accInfo != null && accInfo.isValid() && !accInfo.isExpired() && accInfo.getTrafficLeft() != 0) {
                                accounts.add(account);
                            } else {
                                logger.finer(plg.getHost() + " : account " + account.getUser() + " is not valid; not added to list");
                            }
                        } catch (Exception e1) {
                            JDLogger.exception(e1);
                        }
                    } else {
                        accounts.add(account);
                    }
                }
            }

            if (accounts.size() == 0) {
                pc.increase(1);
                return;
            }
            Collections.shuffle(accounts);
            ArrayList<Account> oldaccounts = plg.getPlugin().getPremiumAccounts();
            for (Account newacc : accounts) {
                boolean found = false;
                for (Account oldacc : oldaccounts) {
                    if (newacc.getUser().trim().equalsIgnoreCase(oldacc.getUser().trim())) {
                        oldacc.setPass(newacc.getPass().trim());
                        oldacc.setProperty("PREMCOLLECTOR", true);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    oldaccounts.add(newacc);
                }
            }
            if (subConfig.getBooleanProperty(PROPERTY_ACCOUNTS, true)) {
                accounts = new ArrayList<Account>();
                for (Account acc : oldaccounts) {
                    try {
                        AccountInfo accInfo = plg.getPlugin().getAccountInformation(acc);
                        if (accInfo != null && accInfo.isValid() && !accInfo.isExpired() && accInfo.getTrafficLeft() != 0) {
                            accounts.add(acc);
                        } else {
                            if (subConfig.getBooleanProperty(PROPERTY_ACCOUNTS2, true)) {
                                logger.finer(plg.getHost() + " : account " + acc.getUser() + " is not valid; removed from list");
                            } else {
                                acc.setEnabled(false);
                                accounts.add(acc);
                            }
                        }
                    } catch (Exception e1) {

                    }
                }

                AccountManager.getInstance().setAccountsForHost(plg.getPlugin(), accounts);
                logger.finer(plg.getHost() + " : " + accounts.size() + " accounts inserted");
            } else {
                AccountManager.getInstance().setAccountsForHost(plg.getPlugin(), oldaccounts);
                logger.finer(plg.getHost() + " : " + oldaccounts.size() + " accounts inserted");
            }
            pc.increase(1);
        }

        public void go() throws Exception {
            run();
        }
    }
}