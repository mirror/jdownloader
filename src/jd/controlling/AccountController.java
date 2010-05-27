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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeMap;

import javax.swing.Timer;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.event.JDBroadcaster;
import jd.gui.swing.components.Balloon;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.PluginForHost;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class AccountController extends SubConfiguration implements ActionListener, AccountControllerListener {

    private static final long serialVersionUID = -7560087582989096645L;

    public static final String PROPERTY_ACCOUNT_SELECTION = "ACCOUNT_SELECTION";

    private static TreeMap<String, ArrayList<Account>> hosteraccounts = null;

    private static AccountController INSTANCE = new AccountController();

    private JDBroadcaster<AccountControllerListener, AccountControllerEvent> broadcaster = new JDBroadcaster<AccountControllerListener, AccountControllerEvent>() {

        @Override
        protected void fireEvent(final AccountControllerListener listener, final AccountControllerEvent event) {
            listener.onAccountControllerEvent(event);
        }

    };

    private final Timer asyncSaveIntervalTimer;

    private boolean saveinprogress = false;

    private long lastballoon = 0;

    private long waittimeAccountInfoUpdate = 15 * 60 * 1000l;

    private static final long BALLOON_INTERVAL = 30 * 60 * 1000l;

    public static final Object ACCOUNT_LOCK = new Object();

    public long getUpdateTime() {
        return waittimeAccountInfoUpdate;
    }

    public void setUpdateTime(final long time) {
        this.waittimeAccountInfoUpdate = time;
    }

    private static final Comparator<Account> COMPARE_MOST_TRAFFIC_LEFT = new Comparator<Account>() {
        public int compare(final Account o1, final Account o2) {
            final AccountInfo ai1 = o1.getAccountInfo();
            final AccountInfo ai2 = o2.getAccountInfo();
            if (ai1 != null && ai2 != null) {
                if (ai1.getTrafficLeft() == ai2.getTrafficLeft()) return 0;
                if (ai1.getTrafficLeft() < ai2.getTrafficLeft()) {
                    return -1;
                } else {
                    return 1;
                }
            } else {
                return 0;
            }
        }
    };

    private AccountController() {
        super("AccountController");
        asyncSaveIntervalTimer = new Timer(2000, this);
        // asyncSaveIntervalTimer.setInitialDelay(2000); // this.initialDelay =
        // delay;
        asyncSaveIntervalTimer.setRepeats(false);
        hosteraccounts = loadAccounts();
        broadcaster.addListener(this);
    }

    public AccountInfo updateAccountInfo(final PluginForHost host, final Account account, final boolean forceupdate) {
        return updateAccountInfo(host.getHost(), account, forceupdate);
    }

    public AccountInfo updateAccountInfo(final String host, final Account account, final boolean forceupdate) {
        final String hostname = host != null ? host : getHosterName(account);
        if (hostname == null) {
            account.setAccountInfo(null);
            logger.severe("Cannot update AccountInfo, no Hostername available!");
            return null;
        }
        final PluginForHost plugin = JDUtilities.getNewPluginForHostInstance(hostname);
        if (plugin == null) {
            account.setAccountInfo(null);
            logger.severe("Cannot update AccountInfo, no HosterPlugin available!");
            return null;
        }
        AccountInfo ai = account.getAccountInfo();
        if (!forceupdate) {
            if (account.lastUpdateTime() != 0 && ai != null && ai.isExpired()) {
                /* account is expired, no need to update */
                return ai;
            }
            if (!account.isValid() && account.lastUpdateTime() != 0) {
                /* account is invalid, no need to update */
                return ai;
            }
            if ((System.currentTimeMillis() - account.lastUpdateTime()) < waittimeAccountInfoUpdate) {
                /*
                 * account was checked before, timeout for recheck not reached,
                 * no need to update
                 */
                return ai;
            }
        }
        try {
            account.setUpdateTime(System.currentTimeMillis());
            /* not every plugin sets this info correct */
            account.setValid(true);
            ai = plugin.fetchAccountInfo(account);
            if (ai == null) {
                // System.out.println("plugin no update " + hostname);
                /* not every plugin has fetchAccountInfo */
                account.setAccountInfo(null);
                this.broadcaster.fireEvent(new AccountControllerEvent(this, AccountControllerEvent.ACCOUNT_UPDATE, hostname, account));
                return null;
            }
            synchronized (ACCOUNT_LOCK) {
                account.setAccountInfo(ai);
            }
            if (ai.isExpired()) {
                account.setEnabled(false);
                this.broadcaster.fireEvent(new AccountControllerEvent(this, AccountControllerEvent.ACCOUNT_EXPIRED, hostname, account));
            } else if (!account.isValid()) {
                account.setEnabled(false);
                this.broadcaster.fireEvent(new AccountControllerEvent(this, AccountControllerEvent.ACCOUNT_INVALID, hostname, account));
            } else {
                this.broadcaster.fireEvent(new AccountControllerEvent(this, AccountControllerEvent.ACCOUNT_UPDATE, hostname, account));
            }
        } catch (IOException e) {
            logger.severe("AccountUpdate: " + host + " failed!");
        } catch (Exception e) {
            logger.severe("AccountUpdate: " + host + " failed!");
            JDLogger.exception(e);
            account.setAccountInfo(null);
            account.setEnabled(false);
            account.setValid(false);
            this.broadcaster.fireEvent(new AccountControllerEvent(this, AccountControllerEvent.ACCOUNT_INVALID, hostname, account));
        }
        return ai;
    }

    /**
     * return hostername if account is under controll of AccountController
     */
    public String getHosterName(final Account account) {
        if (account.getHoster() != null) { return account.getHoster(); }
        synchronized (hosteraccounts) {
            for (String host : hosteraccounts.keySet()) {
                if (hosteraccounts.get(host).contains(account)) {
                    account.setHoster(host);
                    return host;
                }
            }
        }
        return null;
    }

    public static AccountController getInstance() {
        return INSTANCE;
    }

    public void addListener(final AccountControllerListener l) {
        broadcaster.addListener(l);
    }

    public void removeListener(final AccountControllerListener l) {
        broadcaster.removeListener(l);
    }

    private TreeMap<String, ArrayList<Account>> loadAccounts() {
        return getGenericProperty("accountlist", new TreeMap<String, ArrayList<Account>>());
    }

    public void addAccount(final PluginForHost pluginForHost, final Account account) {
        addAccount(pluginForHost.getHost(), account);
    }

    public boolean hasAccounts(final String host) {
        return !getAllAccounts(host).isEmpty();
    }

    public ArrayList<Account> getAllAccounts(final PluginForHost pluginForHost) {
        // if (pluginForHost == null) return new ArrayList<Account>();
        // return this.getAllAccounts(pluginForHost.getHost());
        return (pluginForHost == null) ? new ArrayList<Account>() : getAllAccounts(pluginForHost.getHost());
    }

    public ArrayList<Account> getAllAccounts(final String host) {
        ArrayList<Account> ret = new ArrayList<Account>();
        if (host == null) return ret;
        synchronized (hosteraccounts) {
            if (hosteraccounts.containsKey(host)) {
                return hosteraccounts.get(host);
            } else {
                final ArrayList<Account> haccounts = new ArrayList<Account>();
                hosteraccounts.put(host, haccounts);
                return haccounts;
            }
        }
    }

    public int validAccounts() {
        int count = 0;
        synchronized (hosteraccounts) {
            for (final ArrayList<Account> accs : hosteraccounts.values()) {
                for (final Account acc : accs) {
                    if (acc.isEnabled()) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private void addAccount(final String host, final Account account) {
        if (host != null && account != null) {
            synchronized (hosteraccounts) {
                if (hosteraccounts.containsKey(host)) {
                    final ArrayList<Account> haccounts = hosteraccounts.get(host);
                    synchronized (haccounts) {
                        boolean b = haccounts.contains(account);
                        if (!b) {
                            boolean b2 = false;
                            final ArrayList<Account> temp = new ArrayList<Account>(haccounts);
                            for (final Account acc : temp) {
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
                        if (b) {
                            this.broadcaster.fireEvent(new AccountControllerEvent(this, AccountControllerEvent.ACCOUNT_ADDED, host, account));
                        }
                    }
                } else {
                    final ArrayList<Account> haccounts = new ArrayList<Account>();
                    haccounts.add(account);
                    hosteraccounts.put(host, haccounts);
                    this.broadcaster.fireEvent(new AccountControllerEvent(this, AccountControllerEvent.ACCOUNT_ADDED, host, account));
                }
            }
        }
    }

    public boolean removeAccount(final String hostname, final Account account) {
        if (account == null) { return false; }
        final String host = (hostname == null) ? getHosterName(account) : hostname;
        if (host == null) { return false; }
        synchronized (hosteraccounts) {
            if (!hosteraccounts.containsKey(host)) { return false; }
            final ArrayList<Account> haccounts = hosteraccounts.get(host);
            synchronized (haccounts) {
                boolean b = haccounts.remove(account);
                if (!b) {
                    final ArrayList<Account> temp = new ArrayList<Account>(haccounts);
                    for (final Account acc : temp) {
                        if (acc.equals(account)) {
                            // account = acc;
                            // b = haccounts.remove(account);
                            b = haccounts.remove(acc);
                            break;
                        }
                    }
                }
                if (b) {
                    this.broadcaster.fireEvent(new AccountControllerEvent(this, AccountControllerEvent.ACCOUNT_REMOVED, host, account));
                }
                return b;
            }
        }
    }

    public boolean removeAccount(final PluginForHost pluginForHost, final Account account) {
        if (account == null) { return false; }
        if (pluginForHost == null) { return removeAccount((String) null, account); }
        return removeAccount(pluginForHost.getHost(), account);
    }

    public void actionPerformed(final ActionEvent arg0) {
        if (arg0.getSource() == asyncSaveIntervalTimer) {
            saveSync();
        }
    }

    public void onAccountControllerEvent(final AccountControllerEvent event) {
        switch (event.getID()) {
        case AccountControllerEvent.ACCOUNT_ADDED:
            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true);
            JDUtilities.getConfiguration().save();
            saveAsync();
            break;
        case AccountControllerEvent.ACCOUNT_REMOVED:
        case AccountControllerEvent.ACCOUNT_UPDATE:
        case AccountControllerEvent.ACCOUNT_EXPIRED:
        case AccountControllerEvent.ACCOUNT_INVALID:
            saveAsync();
            break;
        default:
            break;
        }
    }

    public void throwUpdateEvent(final PluginForHost pluginForHost, final Account account) {
        if (pluginForHost != null) {
            this.broadcaster.fireEvent(new AccountControllerEvent(this, AccountControllerEvent.ACCOUNT_UPDATE, pluginForHost.getHost(), account));
        } else {
            this.broadcaster.fireEvent(new AccountControllerEvent(this, AccountControllerEvent.ACCOUNT_UPDATE, null, account));
        }
    }

    public void saveAsync() {
        if (!saveinprogress) {
            asyncSaveIntervalTimer.restart();
        }
    }

    public void saveSync() {
        if (!saveinprogress) {
            new Thread() {
                @Override
                public void run() {
                    saveSyncnonThread();
                }
            }.start();
        }
    }

    public void saveSyncnonThread() {
        asyncSaveIntervalTimer.stop();
        final String id = JDController.requestDelayExit("accountcontroller");
        synchronized (hosteraccounts) {
            saveinprogress = true;
            save();
            saveinprogress = false;
        }
        JDController.releaseDelayExit(id);
    }

    public Account getValidAccount(final PluginForHost pluginForHost) {
        return getValidAccount(pluginForHost.getHost());
    }

    public Account getValidAccount(final String host) {
        Account ret = null;
        synchronized (hosteraccounts) {
            final ArrayList<Account> accounts = new ArrayList<Account>(getAllAccounts(host));
            if (getBooleanProperty(PROPERTY_ACCOUNT_SELECTION, true)) {
                Collections.sort(accounts, COMPARE_MOST_TRAFFIC_LEFT);
            }
            final int accountsSize = accounts.size();
            for (int i = 0; i < accountsSize; i++) {
                final Account next = accounts.get(i);
                if (!next.isTempDisabled() && next.isEnabled() && next.isValid()) {
                    ret = next;
                    break;
                }
            }
        }
        if (ret != null && !JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true)) {
            if (System.currentTimeMillis() - lastballoon > BALLOON_INTERVAL) {
                lastballoon = System.currentTimeMillis();
                Balloon.show(JDL.L("gui.ballon.accountmanager.title", "Accountmanager"), JDTheme.II("gui.images.accounts", 32, 32), JDL.L("gui.accountcontroller.globpremdisabled", "Premiumaccounts are globally disabled!<br/>Click <a href='http://jdownloader.org/knowledge/wiki/gui/premiummenu'>here</a> for help."));
            }
            ret = null;
        }
        return ret;
    }
}
