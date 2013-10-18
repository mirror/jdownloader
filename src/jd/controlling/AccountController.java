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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.accountchecker.AccountChecker;
import jd.controlling.reconnect.ipcheck.BalancedWebIPCheck;
import jd.controlling.reconnect.ipcheck.IPCheckException;
import jd.controlling.reconnect.ipcheck.OfflineException;
import jd.http.Browser;
import jd.http.BrowserSettingsThread;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.Eventsender;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.controller.PluginClassLoader;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;
import org.jdownloader.plugins.controller.host.PluginFinder;
import org.jdownloader.settings.AccountData;
import org.jdownloader.settings.AccountSettings;

public class AccountController implements AccountControllerListener {

    private static final long                                                    serialVersionUID = -7560087582989096645L;

    private final HashMap<String, List<Account>>                                 ACCOUNTS;
    private final HashMap<String, List<Account>>                                 MULTIHOSTER_ACCOUNTS;

    private static AccountController                                             INSTANCE         = new AccountController();

    private final Eventsender<AccountControllerListener, AccountControllerEvent> broadcaster      = new Eventsender<AccountControllerListener, AccountControllerEvent>() {

                                                                                                      @Override
                                                                                                      protected void fireEvent(final AccountControllerListener listener, final AccountControllerEvent event) {
                                                                                                          listener.onAccountControllerEvent(event);
                                                                                                      }

                                                                                                  };

    public Eventsender<AccountControllerListener, AccountControllerEvent> getBroadcaster() {
        return broadcaster;
    }

    private AccountSettings config;

    private DelayedRunnable delayedSaver;

    private AccountController() {
        super();
        config = JsonConfig.create(AccountSettings.class);
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public void onShutdown(final ShutdownRequest shutdownRequest) {
                save();
            }

            @Override
            public String toString() {
                return "save accounts...";
            }
        });
        ACCOUNTS = loadAccounts();
        MULTIHOSTER_ACCOUNTS = new HashMap<String, List<Account>>();
        final Collection<List<Account>> accsc = ACCOUNTS.values();
        for (final java.util.List<Account> accs : accsc) {
            for (final Account acc : accs) {
                acc.setAccountController(this);
                if (acc.getPlugin() != null) {
                    updateInternalMultiHosterMap(acc, acc.getAccountInfo());
                }
            }
        }
        delayedSaver = new DelayedRunnable(5000, 30000) {

            @Override
            public String getID() {
                return "AccountController";
            }

            @Override
            public void delayedrun() {
                save();
            }
        };
        broadcaster.addListener(this);
    }

    protected void save() {
        HashMap<String, ArrayList<AccountData>> ret = new HashMap<String, ArrayList<AccountData>>();
        synchronized (ACCOUNTS) {
            for (Iterator<Entry<String, java.util.List<Account>>> it = ACCOUNTS.entrySet().iterator(); it.hasNext();) {
                Entry<String, java.util.List<Account>> next = it.next();
                if (next.getValue().size() > 0) {
                    ArrayList<AccountData> list = new ArrayList<AccountData>();
                    ret.put(next.getKey(), list);
                    for (Account a : next.getValue()) {
                        list.add(AccountData.create(a));
                    }
                }
            }
        }
        config.setAccounts(ret);
    }

    private void updateInternalMultiHosterMap(Account account, AccountInfo ai) {
        synchronized (MULTIHOSTER_ACCOUNTS) {
            Iterator<Entry<String, List<Account>>> it = MULTIHOSTER_ACCOUNTS.entrySet().iterator();
            while (it.hasNext()) {
                Entry<String, List<Account>> next = it.next();
                List<Account> accs = next.getValue();
                if (accs.remove(account) && accs.size() == 0) {
                    it.remove();
                }
            }
            boolean isMulti = false;
            if (ai != null) {
                Object supported = ai.getProperty("multiHostSupport", Property.NULL);
                if (supported != null && supported instanceof List) {
                    isMulti = true;
                    for (Object support : (List<?>) supported) {
                        if (support instanceof String) {
                            String host = ((String) support).toLowerCase(Locale.ENGLISH);
                            List<Account> accs = MULTIHOSTER_ACCOUNTS.get(host);
                            if (accs == null) {
                                accs = new ArrayList<Account>();
                                MULTIHOSTER_ACCOUNTS.put(host, accs);
                            }
                            accs.add(account);
                        }
                    }
                }
            }
            account.setProperty(Account.IS_MULTI_HOSTER_ACCOUNT, isMulti);
        }
    }

    public AccountInfo updateAccountInfo(final Account account, final boolean forceupdate) {
        AccountInfo ai = account.getAccountInfo();
        if (!forceupdate) {
            if (account.lastUpdateTime() != 0) {
                if (ai != null && ai.isExpired()) {
                    account.setEnabled(false);
                    this.broadcaster.fireEvent(new AccountControllerEvent(this, AccountControllerEvent.Types.EXPIRED, account));
                    /* account is expired, no need to update */
                    return ai;
                }
                if (!account.isValid()) {
                    account.setEnabled(false);
                    this.broadcaster.fireEvent(new AccountControllerEvent(this, AccountControllerEvent.Types.INVALID, account));
                    /* account is invalid, no need to update */
                    return ai;
                }
            }
            if ((System.currentTimeMillis() - account.lastUpdateTime()) < account.getRefreshTimeout()) {
                /*
                 * account was checked before, timeout for recheck not reached, no need to update
                 */
                return ai;
            }
        }
        PluginClassLoaderChild cl;
        Thread.currentThread().setContextClassLoader(cl = PluginClassLoader.getInstance().getChild());
        PluginForHost plugin = null;
        try {
            plugin = account.getPlugin().getLazyP().newInstance(cl);
            if (plugin == null) {
                LogController.CL().severe("AccountCheck: Failed because plugin " + account.getHoster() + " is missing!");
                account.setEnabled(false);
                return null;
            }
        } catch (final Throwable e) {
            LogController.CL().log(e);
            account.setEnabled(false);
            return null;
        }
        String whoAmI = account.getUser() + "->" + account.getHoster();
        LogSource logger = LogController.getInstance().getLogger(plugin);
        logger.info("Account Update: " + whoAmI);
        logger.setAllowTimeoutFlush(false);
        plugin.setLogger(logger);
        Thread currentThread = Thread.currentThread();
        BrowserSettingsThread bThread = null;
        Logger oldLogger = null;
        if (currentThread instanceof BrowserSettingsThread) {
            bThread = (BrowserSettingsThread) currentThread;
        }
        if (bThread != null) {
            /* set logger to browserSettingsThread */
            oldLogger = bThread.getLogger();
            bThread.setLogger(logger);
        }
        try {
            Browser br = new Browser();
            br.setLogger(logger);
            plugin.setBrowser(br);
            /* not every plugin sets this info correct */
            account.setValid(true);
            /* get previous account info and resets info for new update */
            ai = account.getAccountInfo();
            if (ai != null) {
                /* reset expired and setValid */
                ai.setExpired(false);
                ai.setValidUntil(-1);
            }
            ClassLoader oldClassLoader = currentThread.getContextClassLoader();
            long tempDisabledBefore = account.getTmpDisabledTimeout();
            try {
                /*
                 * make sure the current Thread uses the PluginClassLoaderChild of the Plugin in use
                 */
                ai = plugin.fetchAccountInfo(account);
                account.setAccountInfo(ai);
            } finally {
                account.setUpdateTime(System.currentTimeMillis());
                currentThread.setContextClassLoader(oldClassLoader);
            }
            if (account.isValid() == false) {
                /* account is invalid */
                account.setEnabled(false);
                LogController.CL().info("Account " + whoAmI + " is invalid!");
                this.broadcaster.fireEvent(new AccountControllerEvent(this, AccountControllerEvent.Types.INVALID, account));
                return ai;
            } else {
                account.setLastValidTimestamp(System.currentTimeMillis());

            }
            if (ai != null && ai.isExpired()) {
                /* expired account */
                account.setValid(false);
                account.setEnabled(false);
                logger.clear();
                LogController.CL().info("Account " + whoAmI + " is expired!");
                this.broadcaster.fireEvent(new AccountControllerEvent(this, AccountControllerEvent.Types.EXPIRED, account));
                return ai;
            }
            if (tempDisabledBefore > 0 && account.getTmpDisabledTimeout() == tempDisabledBefore) {
                /* reset temp disabled information */
                logger.info("no longer temp disabled!");
                account.setTempDisabled(false);
            }
            logger.clear();
            this.broadcaster.fireEvent(new AccountControllerEvent(this, AccountControllerEvent.Types.UPDATE, account));
        } catch (final Throwable e) {
            if (e instanceof PluginException) {
                PluginException pe = (PluginException) e;
                ai = account.getAccountInfo();
                if (ai == null) {
                    ai = new AccountInfo();
                    account.setAccountInfo(ai);
                }
                if ((pe.getLinkStatus() == LinkStatus.ERROR_PREMIUM)) {
                    if (pe.getValue() == PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE) {
                        logger.clear();
                        LogController.CL().info("Account " + whoAmI + " traffic limit reached!");
                        if (!StringUtils.isEmpty(pe.getErrorMessage())) {
                            ai.setStatus(pe.getErrorMessage());
                        } else {
                            ai.setStatus("Traffic limit reached");
                        }
                        account.setTempDisabled(true);
                        account.getAccountInfo().setTrafficLeft(0);
                        this.broadcaster.fireEvent(new AccountControllerEvent(this, AccountControllerEvent.Types.UPDATE, account));
                        return ai;
                    } else if (pe.getValue() == PluginException.VALUE_ID_PREMIUM_DISABLE) {
                        account.setEnabled(false);
                        account.setValid(false);
                        if (!StringUtils.isEmpty(pe.getErrorMessage())) {
                            ai.setStatus(pe.getErrorMessage());
                        } else {
                            ai.setStatus("Invalid Account!");
                        }
                        logger.clear();
                        LogController.CL().info("Account " + whoAmI + " is invalid!");
                        this.broadcaster.fireEvent(new AccountControllerEvent(this, AccountControllerEvent.Types.INVALID, account));
                        return ai;
                    }
                }
            } else if (e instanceof IOException) {
                /* network exception, lets temp disable the account */
                BalancedWebIPCheck onlineCheck = new BalancedWebIPCheck(true);
                try {
                    onlineCheck.getExternalIP();
                } catch (final OfflineException e2) {
                    /*
                     * we are offline, so lets just return without any account update
                     */
                    logger.clear();
                    LogController.CL().info("It seems Computer is currently offline, skipped Accountcheck for " + whoAmI);
                    return ai;
                } catch (final IPCheckException e2) {
                }
            }
            logger.severe("AccountCheck: Failed because of exception, temp disable it!");
            logger.log(e);
            /* move download log into global log */
            account.setProperty(Account.PROPERTY_TEMP_DISABLED_TIMEOUT, config.getTempDisableOnErrorTimeout() * 60 * 1000l);
            account.setTempDisabled(true);
            this.broadcaster.fireEvent(new AccountControllerEvent(this, AccountControllerEvent.Types.UPDATE, account));
        } finally {
            logger.close();
            if (bThread != null) {
                /* remove logger from browserSettingsThread */
                bThread.setLogger(oldLogger);
            }
        }
        return ai;
    }

    public static AccountController getInstance() {
        return INSTANCE;
    }

    private synchronized HashMap<String, java.util.List<Account>> loadAccounts() {
        HashMap<String, ArrayList<AccountData>> dat = config.getAccounts();
        if (dat == null) {
            try {
                dat = restore();
            } catch (final Throwable e) {
                LogController.CL().log(e);
            }
        }
        if (dat == null) {
            dat = new HashMap<String, ArrayList<AccountData>>();
        }
        PluginFinder pluginFinder = new PluginFinder();
        HashMap<String, java.util.List<Account>> ret = new HashMap<String, java.util.List<Account>>();
        for (Iterator<Entry<String, ArrayList<AccountData>>> it = dat.entrySet().iterator(); it.hasNext();) {
            Entry<String, ArrayList<AccountData>> next = it.next();
            if (next.getValue().size() > 0) {
                for (AccountData ad : next.getValue()) {
                    String host = next.getKey();
                    host = host.toLowerCase(Locale.ENGLISH);
                    Account acc = ad.toAccount();
                    acc.setHoster(host);
                    PluginForHost plugin = pluginFinder.assignPlugin(acc, true, null);
                    if (plugin != null) {
                        host = plugin.getHost();
                        acc.setPlugin(plugin);
                    } else {
                        acc.setPlugin(null);
                    }
                    List<Account> accs = ret.get(host);
                    if (accs == null) {
                        accs = new ArrayList<Account>();
                        ret.put(host, accs);
                    }
                    accs.add(acc);
                }
            }
        }
        return ret;
    }

    /**
     * Restores accounts from old database
     * 
     * @return
     */
    private HashMap<String, ArrayList<AccountData>> restore() {
        SubConfiguration sub = SubConfiguration.getConfig("AccountController", true);
        HashMap<String, ArrayList<AccountData>> ret = new HashMap<String, ArrayList<AccountData>>();
        Object mapRet = sub.getProperty("accountlist");
        if (mapRet != null && mapRet instanceof Map) {
            Map<String, Object> tree = (Map<String, Object>) mapRet;
            for (Iterator<Entry<String, Object>> it = tree.entrySet().iterator(); it.hasNext();) {
                Entry<String, Object> next = it.next();
                if (next.getValue() instanceof ArrayList) {
                    java.util.List<Object> accList = (java.util.List<Object>) next.getValue();
                    if (accList.size() > 0) {
                        ArrayList<AccountData> list = new ArrayList<AccountData>();
                        ret.put(next.getKey(), list);
                        if (accList.get(0) instanceof Account) {
                            java.util.List<Account> accList2 = (java.util.List<Account>) next.getValue();
                            for (Account a : accList2) {
                                AccountData ac;
                                list.add(ac = new AccountData());
                                ac.setUser(a.getUser());
                                ac.setPassword(a.getPass());
                                ac.setValid(a.isEnabled());
                                ac.setEnabled(a.isEnabled());
                            }
                        } else if (accList.get(0) instanceof Map) {
                            java.util.List<Map<String, Object>> accList2 = (java.util.List<Map<String, Object>>) next.getValue();
                            for (Map<String, Object> a : accList2) {
                                AccountData ac;
                                list.add(ac = new AccountData());
                                ac.setUser((String) a.get("user"));
                                ac.setPassword((String) a.get("pass"));
                                ac.setEnabled(a.containsKey("enabled"));
                                ac.setValid(ac.isEnabled());
                            }
                        }
                    }
                }
            }
        }
        config.setAccounts(ret);
        return ret;
    }

    @Deprecated
    public void addAccount(final PluginForHost pluginForHost, final Account account) {
        account.setHoster(pluginForHost.getHost());
        addAccount(account);
    }

    /* returns a list of all available accounts for given host */
    public ArrayList<Account> list(String host) {
        ArrayList<Account> ret = new ArrayList<Account>();
        synchronized (ACCOUNTS) {
            if (StringUtils.isEmpty(host)) {
                for (String hoster : ACCOUNTS.keySet()) {
                    java.util.List<Account> ret2 = ACCOUNTS.get(hoster);
                    if (ret2 != null) {
                        for (Account acc : ret2) {
                            if (acc.getPlugin() == null) continue;
                            ret.add(acc);
                        }
                    }
                }
            } else {
                java.util.List<Account> ret2 = ACCOUNTS.get(host);
                if (ret2 != null) {
                    for (Account acc : ret2) {
                        if (acc.getPlugin() == null) continue;
                        ret.add(acc);
                    }
                }
            }
        }
        return ret;
    }

    /* returns a list of all available accounts */
    public List<Account> list() {
        return list(null);
    }

    /* do we have accounts for this host */
    public boolean hasAccounts(String host) {
        if (StringUtils.isEmpty(host)) return false;
        java.util.List<Account> ret = null;
        host = host.toLowerCase(Locale.ENGLISH);
        synchronized (ACCOUNTS) {
            ret = ACCOUNTS.get(host);
            if (ret != null) {
                for (Account acc : ret) {
                    if (acc.getPlugin() == null) continue;
                    if (acc.isValid()) return true;
                }
            }
        }
        return false;
    }

    public void addAccount(final Account account) {
        if (account == null) return;
        if (account.getPlugin() == null) {
            PluginForHost plugin = new PluginFinder().assignPlugin(account, true, null);
            if (plugin != null) {
                account.setPlugin(plugin);
            } else {
                account.setPlugin(null);
            }
        }

        synchronized (ACCOUNTS) {
            String host = account.getHoster();
            host = host.toLowerCase(Locale.ENGLISH);
            java.util.List<Account> accs = ACCOUNTS.get(host);
            if (accs == null) {
                accs = new ArrayList<Account>();
                ACCOUNTS.put(host, accs);
            }
            for (final Account acc : accs) {
                if (acc.equals(account)) return;
            }
            accs.add(account);
            account.setAccountController(this);
        }
        this.broadcaster.fireEvent(new AccountControllerEvent(this, AccountControllerEvent.Types.ADDED, account));
    }

    public boolean removeAccount(final Account account) {
        if (account == null) { return false; }
        /* remove reference to AccountController */
        account.setAccountController(null);
        synchronized (ACCOUNTS) {
            String host = account.getHoster();
            host = host.toLowerCase(Locale.ENGLISH);
            java.util.List<Account> accs = ACCOUNTS.get(host);
            if (accs == null || !accs.remove(account)) return false;
            if (accs.size() == 0) ACCOUNTS.remove(host);
        }
        this.broadcaster.fireEvent(new AccountControllerEvent(this, AccountControllerEvent.Types.REMOVED, account));
        return true;
    }

    public void onAccountControllerEvent(final AccountControllerEvent event) {
        Account acc = null;
        delayedSaver.resetAndStart();
        switch (event.getType()) {
        case ADDED:
            updateInternalMultiHosterMap(event.getParameter(), event.getParameter().getAccountInfo());
            org.jdownloader.settings.staticreferences.CFG_GENERAL.USE_AVAILABLE_ACCOUNTS.setValue(true);
            break;
        case REMOVED:
        case EXPIRED:
        case INVALID:
            updateInternalMultiHosterMap(event.getParameter(), null);
            return;
        case UPDATE:
            updateInternalMultiHosterMap(event.getParameter(), event.getParameter().getAccountInfo());
            break;
        }
        if (event.isRecheckRequired()) {
            /* event tells us to recheck the account */
            acc = event.getParameter();
            if (acc != null) AccountChecker.getInstance().check(acc, true);
        }
    }

    @Deprecated
    public Account getValidAccount(final PluginForHost pluginForHost) {
        ArrayList<Account> ret = getValidAccounts(pluginForHost.getHost());
        if (ret != null && ret.size() > 0) return ret.get(0);
        return null;
    }

    public ArrayList<Account> getValidAccounts(String host) {
        if (StringUtils.isEmpty(host)) return null;
        host = host.toLowerCase(Locale.ENGLISH);
        ArrayList<Account> ret = null;
        synchronized (ACCOUNTS) {
            final java.util.List<Account> accounts = ACCOUNTS.get(host);
            if (accounts == null || accounts.size() == 0) return null;
            ret = new ArrayList<Account>(accounts);
        }
        Iterator<Account> it = ret.iterator();
        while (it.hasNext()) {
            Account next = it.next();
            if (!next.isEnabled() || !next.isValid() || next.isTempDisabled()) {
                /* we remove every invalid/disabled/tempdisabled/blocked account */
                it.remove();
            }
        }
        return ret;
    }

    public List<Account> getMultiHostAccounts(String host) {
        if (StringUtils.isEmpty(host)) return null;
        host = host.toLowerCase(Locale.ENGLISH);
        synchronized (MULTIHOSTER_ACCOUNTS) {
            List<Account> ret = MULTIHOSTER_ACCOUNTS.get(host);
            if (ret == null) return ret;
            return new ArrayList<Account>(ret);
        }
    }

    public boolean hasMultiHostAccounts(String host) {
        if (StringUtils.isEmpty(host)) return false;
        host = host.toLowerCase(Locale.ENGLISH);
        synchronized (MULTIHOSTER_ACCOUNTS) {
            return MULTIHOSTER_ACCOUNTS.containsKey(host);
        }
    }

    @Deprecated
    public ArrayList<Account> getAllAccounts(String string) {
        return list(string);
    }

    public static String createFullBuyPremiumUrl(String buyPremiumUrl, String id) {
        return "http://update3.jdownloader.org/jdserv/BuyPremiumInterface/redirect?" + Encoding.urlEncode(buyPremiumUrl) + "&" + Encoding.urlEncode(id);
    }

}