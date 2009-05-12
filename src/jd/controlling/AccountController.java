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
import jd.plugins.Account;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

class AccountControllerBroadcaster extends JDBroadcaster<AccountControllerListener, AccountControllerEvent> {

    public boolean fireEvent(AccountControllerEvent event) {
        if (event.getID() != AccountControllerEvent.ACCOUNT_GET) {
            super.fireEvent(event);
            return false;
        } else {
            synchronized (callList) {
                synchronized (removeList) {
                    callList.removeAll(removeList);
                    removeList.clear();
                }
                for (int i = callList.size() - 1; i >= 0; i--) {
                    if (callList.get(i).vetoAccountGetEvent(event.getHost(), event.getAccount())) return true;
                }
            }
            return false;
        }
    }

    // @Override
    protected void fireEvent(AccountControllerListener listener, AccountControllerEvent event) {
        listener.onAccountControllerEvent(event);
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

    private Timer asyncSaveIntervalTimer;

    private AccountController() {
        super("AccountController");
        hosteraccounts = loadAccounts();
        importOldAccounts();
        asyncSaveIntervalTimer = new Timer(2000, this);
        asyncSaveIntervalTimer.setInitialDelay(2000);
        asyncSaveIntervalTimer.setRepeats(false);
        this.getBroadcaster().addListener(this);
    }

    private synchronized JDBroadcaster<AccountControllerListener, AccountControllerEvent> getBroadcaster() {
        if (broadcaster == null) broadcaster = new AccountControllerBroadcaster();
        return broadcaster;
    }

    public synchronized static AccountController getInstance() {
        if (INSTANCE == null) INSTANCE = new AccountController();
        return INSTANCE;
    }

    public void addListener(AccountControllerListener l) {
        getBroadcaster().addListener(l);
    }

    public void removeListener(AccountControllerListener l) {
        getBroadcaster().removeListener(l);
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

    public void addAccount(PluginForHost pluginForHost, Account account) {
        String host = pluginForHost.getHost();
        addAccount(host, account);
    }

    private void addAccount(String host, Account account) {
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
                    if (!b) System.out.println("Account already in list, do not add again!");
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
        String host = pluginForHost.getHost();
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

    public void actionPerformed(ActionEvent arg0) {
        if (arg0.getSource() == asyncSaveIntervalTimer) {
            saveSync();
        }
    }

    public void onAccountControllerEvent(AccountControllerEvent event) {
        switch (event.getID()) {
        case AccountControllerEvent.ACCOUNT_ADDED:
            System.out.println("new account added: " + event.getHost());
            saveAsync();
            break;
        case AccountControllerEvent.ACCOUNT_REMOVED:
            System.out.println("account removed: " + event.getHost());
            saveAsync();
            break;
        default:
            break;
        }
    }

    public void throwUpdateEvent(PluginForHost pluginForHost, Account account) {
        if (pluginForHost == null) return;
        this.broadcaster.fireEvent(new AccountControllerEvent(this, AccountControllerEvent.ACCOUNT_UPDATE, pluginForHost.getHost(), account));
    }

    public void saveAsync() {
        asyncSaveIntervalTimer.restart();
    }

    public synchronized void saveSync() {
        synchronized (hosteraccounts) {
            save();
        }
    }

    public boolean vetoAccountGetEvent(String host, Account account) {
        System.out.println("veto? " + host + " " + account);
        // TODO Auto-generated method stub
        return false;
    }

    public Account getValidAccount(PluginForHost pluginForHost) {
        if (!JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true)) return null;
        synchronized (hosteraccounts) {
            ArrayList<Account> accounts = getAllAccounts(pluginForHost);
            Account ret = null;
            synchronized (accounts) {
                for (int i = 0; i < accounts.size(); i++) {
                    Account next = accounts.get(i);
                    if (!next.isTempDisabled() && next.isEnabled()) {
                        ret = next;
                        break;
                    }
                }
            }
            if (ret != null) {
                if (this.broadcaster.fireEvent(new AccountControllerEvent(this, AccountControllerEvent.ACCOUNT_GET, pluginForHost.getHost(), ret))) ret = null;
            }
            return ret;
        }
    }
}
