//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.controlling;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeMap;

import javax.swing.Timer;

import jd.HostPluginWrapper;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.event.JDBroadcaster;
import jd.event.JDEvent;
import jd.gui.swing.components.Balloon;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

class AccountControllerBroadcaster extends JDBroadcaster<AccountControllerListener, AccountControllerEvent> {

    public boolean fireEvent(AccountControllerEvent event) {
        if (event.getID() != AccountControllerEvent.ACCOUNT_GET) {
            super.fireEvent(event);
            return false;
        } else {
            synchronized (removeList) {
                callList.removeAll(removeList);
                removeList.clear();
            }
            for (int i = callList.size() - 1; i >= 0; i--) {
                if (callList.get(i).vetoAccountGetEvent(event.getHost(), event.getAccount())) return true;
            }
            return false;
        }
    }

    // @Override
    protected void fireEvent(AccountControllerListener listener, AccountControllerEvent event) {
        listener.onAccountControllerEvent(event);
    }

}

class AccountProviderEvent extends JDEvent {
    public AccountProviderEvent(Object source, int ID) {
        super(source, ID);
    }
}

class AccountProviderBroadcaster extends JDBroadcaster<AccountProvider, AccountProviderEvent> {
    @Override
    protected void fireEvent(AccountProvider listener, AccountProviderEvent event) {
    }

    public ArrayList<Account> collectAccountsFor(PluginForHost pluginForHost) {
        ArrayList<Account> ret = new ArrayList<Account>();
        if (pluginForHost == null) return ret;
        String host = pluginForHost.getHost();
        synchronized (removeList) {
            callList.removeAll(removeList);
            removeList.clear();
        }
        for (int i = callList.size() - 1; i >= 0; i--) {
            ArrayList<Account> ret2 = callList.get(i).provideAccountsFor(host);
            if (ret2 != null) ret.addAll(ret2);
        }
        return ret;
    }
}

public class AccountController extends SubConfiguration implements ActionListener, AccountControllerListener {

    public static enum ProviderMode {
        FIFO, RR
    };

    private static final long serialVersionUID = -7560087582989096645L;

    private static TreeMap<String, ArrayList<Account>> hosteraccounts = null;

    private static AccountController INSTANCE = null;

    private AccountControllerBroadcaster broadcaster = new AccountControllerBroadcaster();

    private AccountProviderBroadcaster provider = new AccountProviderBroadcaster();

    private Timer asyncSaveIntervalTimer;

    private boolean saveinprogress = false;

    private long lastballoon = 0;

    private long waittimeAccountInfoUpdate = 15 * 60 * 1000l;

    private long ballooninterval = 30 * 60 * 1000l;
    private ProviderMode providemode = ProviderMode.RR;

    public long getUpdateTime() {
        return waittimeAccountInfoUpdate;
    }

    public void setUpdateTime(long time) {
        this.waittimeAccountInfoUpdate = time;
    }

    private static Comparator<Account> compare_RR = new Comparator<Account>() {

        @Override
        public int compare(Account o1, Account o2) {
            AccountInfo I1 = o1.getAccountInfo();
            AccountInfo I2 = o2.getAccountInfo();
            if (I1 != null && I2 != null) {
                if (I1.getTrafficLeft() < I2.getTrafficLeft()) {
                    return -1;
                } else {
                    return +1;
                }
            }
            return 0;
        }

    };

    private AccountController() {
        super("AccountController");
        hosteraccounts = loadAccounts();
        importOld();
        asyncSaveIntervalTimer = new Timer(2000, this);
        asyncSaveIntervalTimer.setInitialDelay(2000);
        asyncSaveIntervalTimer.setRepeats(false);
        broadcaster.addListener(this);
    }

    public AccountInfo UpdateAccountInfo(PluginForHost host, Account account, boolean forceupdate) {
        return UpdateAccountInfo(host.getHost(), account, forceupdate);
    }

    public AccountInfo UpdateAccountInfo(String host, Account account, boolean forceupdate) {
        String hostname = host != null ? host : getHosterName(account);
        if (hostname == null) {
            account.setAccountInfo(null);
            logger.severe("Cannot update AccountInfo, no Hostername available!");
            return null;
        }
        PluginForHost plugin = JDUtilities.getPluginForHost(hostname);
        if (plugin == null) {
            account.setAccountInfo(null);
            logger.severe("Cannot update AccountInfo, no HosterPlugin available!");
            return null;
        }
        AccountInfo ai = account.getAccountInfo();
        if (ai != null) {
            System.out.println("Update acc for " + ai.getCreateTime());
            if (!forceupdate && (System.currentTimeMillis() - ai.getCreateTime()) < waittimeAccountInfoUpdate) return ai;
        }
        try {
            System.out.println("Update acc for " + plugin.getHost());
            ai = plugin.getNewInstance().fetchAccountInfo(account);
            if (ai == null) {
                /* not every plugin has fetchAccountInfo */
                account.setAccountInfo(null);
                return null;
            }
            account.setAccountInfo(ai);
            if (ai.isExpired()) {
                System.out.println("expired acc for " + plugin.getHost());
                account.setEnabled(false);
            } else if (!ai.isValid()) {
                System.out.println("invalid acc for " + plugin.getHost());
                account.setEnabled(false);
            }
        } catch (PluginException e) {
            System.out.println("pexception disabled acc for " + plugin.getHost());
            account.setEnabled(false);
            account.setAccountInfo(null);
        } catch (Exception e) {
            System.out.println("exception disabled acc for " + plugin.getHost());
            account.setAccountInfo(null);
        }
        return ai;
    }

    /*
     * return hostername if account is under controll of AccountController
     */
    public String getHosterName(Account account) {
        synchronized (hosteraccounts) {
            for (String host : hosteraccounts.keySet()) {
                if (hosteraccounts.get(host).contains(account)) return host;
            }
        }
        return null;
    }

    public void setProviderMode(ProviderMode mode) {
        this.providemode = mode;
    }

    public ProviderMode getProviderMode() {
        return this.providemode;
    }

    public synchronized static AccountController getInstance() {
        if (INSTANCE == null) INSTANCE = new AccountController();
        return INSTANCE;
    }

    public void addListener(AccountControllerListener l) {
        broadcaster.addListener(l);
    }

    public void removeListener(AccountControllerListener l) {
        broadcaster.removeListener(l);
    }

    public void addAccountProvider(AccountProvider l) {
        provider.addListener(l);
    }

    public void removeAccountProvider(AccountProvider l) {
        provider.removeListener(l);
    }

    private TreeMap<String, ArrayList<Account>> loadAccounts() {
        return getGenericProperty("accountlist", new TreeMap<String, ArrayList<Account>>());
    }

    private void importOld() {
        try {
            importOldAccounts();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            importOldAccounts2();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void importOldAccounts() {
        if (getBooleanProperty("oldimported21", false)) return;
        for (HostPluginWrapper wrapper : JDUtilities.getPluginsForHost()) {
            ArrayList<Account> list = wrapper.getPluginConfig().getGenericProperty("PREMIUM", new ArrayList<Account>());
            for (Account acc : list) {
                addAccount(wrapper.getHost(), acc);
            }
        }
        setProperty("oldimported21", true);
        saveSync();
    }

    private void importOldAccounts2() {
        if (getBooleanProperty("oldimported22", false)) return;
        SubConfiguration sub = SubConfiguration.getConfig("AccountManager");
        for (HostPluginWrapper wrapper : JDUtilities.getPluginsForHost()) {
            ArrayList<Account> list = sub.getGenericProperty(wrapper.getHost(), new ArrayList<Account>());
            for (Account acc : list) {
                addAccount(wrapper.getHost(), acc);
            }
        }
        setProperty("oldimported22", true);
        saveSync();
    }

    public void addAccount(PluginForHost pluginForHost, Account account) {
        String host = pluginForHost.getHost();
        addAccount(host, account);
    }

    public boolean hasAccounts(String host) {
        return !getAllAccounts(host).isEmpty();
    }

    public ArrayList<Account> getAllAccounts(PluginForHost pluginForHost) {
        if (pluginForHost == null) return new ArrayList<Account>();
        return this.getAllAccounts(pluginForHost.getHost());
    }

    public ArrayList<Account> getAllAccounts(String host) {
        if (host == null) return new ArrayList<Account>();
        synchronized (hosteraccounts) {
            if (hosteraccounts.containsKey(host)) {
                return hosteraccounts.get(host);
            } else {
                ArrayList<Account> haccounts = new ArrayList<Account>();
                hosteraccounts.put(host, haccounts);
                return haccounts;
            }
        }
    }

    public int validAccounts() {
        int count = 0;
        synchronized (hosteraccounts) {
            for (ArrayList<Account> accs : hosteraccounts.values()) {
                for (Account acc : accs) {
                    if (acc.isEnabled()) count++;
                }
            }
        }
        return count;
    }

    private void addAccount(String host, Account account) {
        if (host == null) return;
        if (account == null) return;
        synchronized (hosteraccounts) {
            if (hosteraccounts.containsKey(host)) {
                ArrayList<Account> haccounts = hosteraccounts.get(host);
                synchronized (haccounts) {
                    boolean b = haccounts.contains(account);
                    if (!b) {
                        boolean b2 = false;
                        ArrayList<Account> temp = new ArrayList<Account>(haccounts);
                        for (Account acc : temp) {
                            if (acc.equals(account)) {
                                b2 = true;
                                break;
                            }
                        }
                        if (!b2) {
                            haccounts.add(account);
                            b = true;
                        }
                    }
                    if (b) this.broadcaster.fireEvent(new AccountControllerEvent(this, AccountControllerEvent.ACCOUNT_ADDED, host, account));
                }
            } else {
                ArrayList<Account> haccounts = new ArrayList<Account>();
                haccounts.add(account);
                hosteraccounts.put(host, haccounts);
                this.broadcaster.fireEvent(new AccountControllerEvent(this, AccountControllerEvent.ACCOUNT_ADDED, host, account));
            }
        }
    }

    public boolean removeAccount(PluginForHost pluginForHost, Account account) {
        if (pluginForHost == null) return false;
        if (account == null) return false;
        String host = pluginForHost.getHost();
        synchronized (hosteraccounts) {
            if (!hosteraccounts.containsKey(host)) return false;
            ArrayList<Account> haccounts = hosteraccounts.get(host);
            synchronized (haccounts) {
                boolean b = haccounts.remove(account);
                if (!b) {
                    ArrayList<Account> temp = new ArrayList<Account>(haccounts);
                    for (Account acc : temp) {
                        if (acc.equals(account)) {
                            account = acc;
                            b = haccounts.remove(account);
                            break;
                        }
                    }
                }
                if (b) this.broadcaster.fireEvent(new AccountControllerEvent(this, AccountControllerEvent.ACCOUNT_REMOVED, host, account));
                return b;
            }
        }
    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.getSource() == asyncSaveIntervalTimer) {
            saveSync();
        }
    }

    public void onAccountControllerEvent(AccountControllerEvent event) {
        switch (event.getID()) {
        case AccountControllerEvent.ACCOUNT_ADDED:
            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true);
            JDUtilities.getConfiguration().save();
            Balloon.show(JDL.L("gui.ballon.accountmanager.title", "Accountmanager"), JDTheme.II("gui.images.add", 32, 32), JDL.LF("gui.ballon.addaccount", "Added Account: %s(%s)", event.getHost(), event.getAccount().getUser()));
            saveAsync();
            break;
        case AccountControllerEvent.ACCOUNT_REMOVED:
            saveAsync();
            break;
        default:
            break;
        }
    }

    public void throwUpdateEvent(PluginForHost pluginForHost, Account account) {
        if (pluginForHost != null) {
            this.broadcaster.fireEvent(new AccountControllerEvent(this, AccountControllerEvent.ACCOUNT_UPDATE, pluginForHost.getHost(), account));
        } else {
            this.broadcaster.fireEvent(new AccountControllerEvent(this, AccountControllerEvent.ACCOUNT_UPDATE, null, account));
        }
    }

    public void saveAsync() {
        asyncSaveIntervalTimer.restart();
    }

    public void saveSync() {
        if (saveinprogress == true) return;
        saveinprogress = true;
        new Thread() {
            public void run() {
                this.setName("AccountController: Saving");
                synchronized (hosteraccounts) {
                    save();
                }
                saveinprogress = false;
            }
        }.start();
    }

    public void saveSyncnonThread() {
        synchronized (hosteraccounts) {
            save();
        }
    }

    public boolean vetoAccountGetEvent(String host, Account account) {
        return false;
    }

    public Account getValidAccount(PluginForHost pluginForHost) {
        synchronized (hosteraccounts) {
            ArrayList<Account> accounts = new ArrayList<Account>();
            accounts.addAll(provider.collectAccountsFor(pluginForHost));
            accounts.addAll(getAllAccounts(pluginForHost));
            Account ret = null;
            switch (this.providemode) {
            case FIFO:
                break;
            case RR:
                Collections.sort(accounts, compare_RR);
                break;
            default:
                break;
            }
            synchronized (accounts) {
                for (int i = 0; i < accounts.size(); i++) {
                    Account next = accounts.get(i);
                    if (!next.isTempDisabled() && next.isEnabled()) {
                        if (!this.broadcaster.fireEvent(new AccountControllerEvent(this, AccountControllerEvent.ACCOUNT_GET, pluginForHost.getHost(), next))) {
                            ret = next;
                            break;
                        }
                    }
                }
            }
            if (ret != null && !JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true)) {
                if (System.currentTimeMillis() - lastballoon > ballooninterval) {
                    lastballoon = System.currentTimeMillis();
                    Balloon.show(JDL.L("gui.ballon.accountmanager.title", "Accountmanager"), JDTheme.II("gui.images.accounts", 32, 32), JDL.L("gui.accountcontroller.globpremdisabled", "Premiumaccounts are globally disabled!<br/>Click <a href='http://jdownloader.org/knowledge/wiki/gui/premiummenu'>here</a> for help."));
                }
                ret = null;
            }
            return ret;
        }
    }
}
