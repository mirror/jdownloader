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
import java.util.TreeMap;

import javax.swing.Timer;

import jd.HostPluginWrapper;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.event.JDBroadcaster;
import jd.event.JDEvent;
import jd.plugins.Account;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

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

    /**
     * 
     */
    private static final long serialVersionUID = -7560087582989096645L;

    private static TreeMap<String, ArrayList<Account>> hosteraccounts = null;

    private static AccountController INSTANCE = null;

    private AccountControllerBroadcaster broadcaster = new AccountControllerBroadcaster();

    private AccountProviderBroadcaster provider = new AccountProviderBroadcaster();

    private Timer asyncSaveIntervalTimer;

    private boolean saveinprogress = false;

    private AccountController() {
        super("AccountController");
        hosteraccounts = loadAccounts();
        importOldAccounts();
        importOldAccounts2();
        asyncSaveIntervalTimer = new Timer(2000, this);
        asyncSaveIntervalTimer.setInitialDelay(2000);
        asyncSaveIntervalTimer.setRepeats(false);
        broadcaster.addListener(this);
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

    @SuppressWarnings("unchecked")
    private TreeMap<String, ArrayList<Account>> loadAccounts() {
        return (TreeMap<String, ArrayList<Account>>) getProperty("accountlist", new TreeMap<String, ArrayList<Account>>());
    }

    @SuppressWarnings("unchecked")
    private void importOldAccounts() {
        if (getBooleanProperty("oldimported", false)) return;
        for (HostPluginWrapper wrapper : JDUtilities.getPluginsForHost()) {
            ArrayList<Account> list = (ArrayList<Account>) wrapper.getPluginConfig().getProperty("PREMIUM", new ArrayList<Account>());
            for (Account acc : list) {
                addAccount(wrapper.getHost(), acc);
            }
        }
        setProperty("oldimported", true);
        saveSync();
    }

    @SuppressWarnings("unchecked")
    private void importOldAccounts2() {
        if (getBooleanProperty("oldimported2", false)) return;
        SubConfiguration sub = SubConfiguration.getConfig("AccountManager");
        for (HostPluginWrapper wrapper : JDUtilities.getPluginsForHost()) {
            ArrayList<Account> list = (ArrayList<Account>) sub.getProperty(wrapper.getHost(), new ArrayList<Account>());
            for (Account acc : list) {
                addAccount(wrapper.getHost(), acc);
            }
        }
        setProperty("oldimported2", true);
        saveSync();
    }

    public void addAccount(PluginForHost pluginForHost, Account account) {
        String host = pluginForHost.getHost();
        addAccount(host, account);
    }

    public void resetAllAccounts(PluginForHost pluginForHost) {
        ArrayList<Account> accounts = new ArrayList<Account>();
        accounts.addAll(provider.collectAccountsFor(pluginForHost));
        accounts.addAll(getAllAccounts(pluginForHost));
        for (Account account : accounts) {
            account.setTempDisabled(false);
        }
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

    public ArrayList<Account> getAllAccounts(PluginForHost pluginForHost) {
        if (pluginForHost == null) return new ArrayList<Account>();
        return this.getAllAccounts(pluginForHost.getHost());
    }

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.getSource() == asyncSaveIntervalTimer) {
            saveSync();
        }
    }

    public void onAccountControllerEvent(AccountControllerEvent event) {
        switch (event.getID()) {
        case AccountControllerEvent.ACCOUNT_ADDED:
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
        }else{
            this.broadcaster.fireEvent(new AccountControllerEvent(this, AccountControllerEvent.ACCOUNT_UPDATE, null, account));
        }
        
    }

    public void saveAsync() {
        asyncSaveIntervalTimer.restart();
    }

    public void saveSync() {
        if (saveinprogress == true) return;
        new Thread() {
            public void run() {
                this.setName("AccountController: Saving");
                saveinprogress = true;
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
        if (!JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true)) return null;
        synchronized (hosteraccounts) {
            ArrayList<Account> accounts = new ArrayList<Account>();
            accounts.addAll(provider.collectAccountsFor(pluginForHost));
            accounts.addAll(getAllAccounts(pluginForHost));
            Account ret = null;
            synchronized (accounts) {
                for (int i = 0; i < accounts.size(); i++) {
                    Account next = accounts.get(i);
                    if (!next.isTempDisabled() && next.isEnabled()) {
                        if (!this.broadcaster.fireEvent(new AccountControllerEvent(this, AccountControllerEvent.ACCOUNT_GET, pluginForHost.getHost(), next))) ret = next;
                        break;
                    }
                }
            }
            return ret;
        }
    }
}
