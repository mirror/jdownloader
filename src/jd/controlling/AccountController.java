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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import jd.config.SubConfiguration;
import jd.controlling.accountchecker.AccountChecker;
import jd.controlling.accountchecker.AccountCheckerThread;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.WarnLevel;
import jd.http.Browser;
import jd.http.BrowserSettingsThread;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.Account.AccountError;
import jd.plugins.Account.AccountPropertyChangeHandler;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountProperty;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.Eventsender;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.controller.PluginClassLoader;
import org.jdownloader.plugins.controller.PluginClassLoader.PluginClassLoaderChild;
import org.jdownloader.plugins.controller.host.PluginFinder;
import org.jdownloader.settings.AccountData;
import org.jdownloader.settings.AccountSettings;
import org.jdownloader.statistics.StatsManager;

public class AccountController implements AccountControllerListener, AccountPropertyChangeHandler {
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

    public Eventsender<AccountControllerListener, AccountControllerEvent> getEventSender() {
        return broadcaster;
    }

    private AccountSettings config;
    private DelayedRunnable delayedSaver;

    private AccountController() {
        super();
        config = JsonConfig.create(AccountSettings.class);
        if (config.getListID() <= 0) {
            config.setListID(System.currentTimeMillis());
        }
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {
            @Override
            public void onShutdown(final ShutdownRequest shutdownRequest) {
                save();
            }

            @Override
            public long getMaxDuration() {
                return 0;
            }

            @Override
            public String toString() {
                return "ShutdownEvent: Save AccountController";
            }
        });
        ACCOUNTS = loadAccounts(config, true);
        MULTIHOSTER_ACCOUNTS = new HashMap<String, List<Account>>();
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
        final Collection<List<Account>> accsc = ACCOUNTS.values();
        for (final List<Account> accs : accsc) {
            for (final Account acc : accs) {
                acc.setAccountController(this);
                if (acc.getPlugin() != null) {
                    updateInternalMultiHosterMap(acc, acc.getAccountInfo());
                }
            }
        }
        broadcaster.addListener(this);
        delayedSaver.getService().scheduleWithFixedDelay(new Runnable() {
            public void run() {
                if (JsonConfig.create(AccountSettings.class).isAutoAccountRefreshEnabled()) {
                    /*
                     * this scheduleritem checks all enabled accounts every 5 mins
                     */
                    try {
                        refreshAccountStats();
                    } catch (Throwable e) {
                        LogController.CL().log(e);
                    }
                }
            }
        }, 1, 5, TimeUnit.MINUTES);
        org.jdownloader.settings.staticreferences.CFG_GENERAL.USE_AVAILABLE_ACCOUNTS.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {
            @Override
            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                if (JDGui.bugme(WarnLevel.SEVERE)) {
                    if (!newValue) {
                        final ConfirmDialog d = new ConfirmDialog(0 | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, _GUI.T.lit_are_you_sure(), _GUI.T.are_you_sure_disabled_premium(), new AbstractIcon(IconKey.ICON_QUESTION, 32), _GUI.T.lit_continue(), null);
                        try {
                            UIOManager.I().show(ConfirmDialogInterface.class, d).throwCloseExceptions();
                            return;
                        } catch (DialogClosedException e) {
                        } catch (DialogCanceledException e) {
                        }
                        // we need a new thread to throw correct change events
                        new Thread("") {
                            public void run() {
                                org.jdownloader.settings.staticreferences.CFG_GENERAL.USE_AVAILABLE_ACCOUNTS.setValue(true);
                            };
                        }.start();
                    }
                } else {
                    return;
                }
            }

            @Override
            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }
        });
    }

    protected void save() {
        final HashMap<String, ArrayList<AccountData>> ret = new HashMap<String, ArrayList<AccountData>>();
        synchronized (AccountController.this) {
            for (final Iterator<Entry<String, List<Account>>> it = ACCOUNTS.entrySet().iterator(); it.hasNext();) {
                final Entry<String, List<Account>> next = it.next();
                if (next.getValue().size() > 0) {
                    final ArrayList<AccountData> list = new ArrayList<AccountData>(next.getValue().size());
                    ret.put(next.getKey(), list);
                    for (final Account a : next.getValue()) {
                        list.add(AccountData.create(a));
                    }
                }
            }
        }
        config.setListVersion(System.currentTimeMillis());
        config.setAccounts(ret);
    }

    private void updateInternalMultiHosterMap(Account account, AccountInfo ai) {
        synchronized (AccountController.this) {
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
                final List<String> multiHostSupport = ai.getMultiHostSupport();
                if (multiHostSupport != null) {
                    isMulti = true;
                    for (String support : multiHostSupport) {
                        String host = support.toLowerCase(Locale.ENGLISH);
                        List<Account> accs = MULTIHOSTER_ACCOUNTS.get(host);
                        if (accs == null) {
                            accs = new ArrayList<Account>();
                            MULTIHOSTER_ACCOUNTS.put(host, accs);
                        }
                        accs.add(account);
                    }
                }
            }
            account.setProperty(Account.IS_MULTI_HOSTER_ACCOUNT, isMulti);
        }
    }

    public AccountInfo updateAccountInfo(final Account account, final boolean forceupdate) {
        AccountInfo ai = account.getAccountInfo();
        if (account.getPlugin() == null) {
            return ai;
        }
        final AccountError errorBefore = account.getError();
        PluginForHost plugin = null;
        final HashMap<AccountProperty.Property, AccountProperty> propertyChanges = new HashMap<AccountProperty.Property, AccountProperty>();
        try {
            final AccountPropertyChangeHandler handler = new AccountPropertyChangeHandler() {
                @Override
                public boolean fireAccountPropertyChange(AccountProperty property) {
                    if (property != null) {
                        synchronized (propertyChanges) {
                            propertyChanges.put(property.getProperty(), property);
                        }
                    }
                    return false;
                }
            };
            account.setNotifyHandler(handler);
            account.setChecking(true);
            if (!forceupdate) {
                if (account.lastUpdateTime() != 0) {
                    if (ai != null && ai.isExpired()) {
                        account.setError(AccountError.EXPIRED, -1, null);
                        /* account is expired, no need to update */
                        return ai;
                    }
                    if (!account.isValid()) {
                        account.setError(AccountError.INVALID, -1, null);
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
            final PluginClassLoaderChild cl = PluginClassLoader.getSharedChild(account.getPlugin());
            PluginClassLoader.setThreadPluginClassLoaderChild(cl, null);
            try {
                plugin = account.getPlugin().getLazyP().newInstance(cl);
                if (plugin == null) {
                    LogController.CL().severe("AccountCheck: Failed because plugin " + account.getHoster() + " is missing!");
                    account.setError(AccountError.PLUGIN_ERROR, -1, null);
                    return null;
                }
            } catch (final Throwable e) {
                LogController.CL().log(e);
                account.setError(AccountError.PLUGIN_ERROR, -1, e.getMessage());
                return null;
            }
            final String whoAmI = account.getUser() + "->" + account.getHoster();
            LogSource logger = LogController.getFastPluginLogger("accountCheck:" + plugin.getHost() + "_" + plugin.getLazyP().getClassName());
            logger.info("Account Update: " + whoAmI + "(" + plugin.getLazyP().getClassName() + "|" + plugin.getVersion() + ")");
            plugin.setLogger(logger);
            Thread currentThread = Thread.currentThread();
            BrowserSettingsThread bThread = null;
            LogInterface oldLogger = null;
            if (currentThread instanceof BrowserSettingsThread) {
                bThread = (BrowserSettingsThread) currentThread;
            }
            if (bThread != null) {
                /* set logger to browserSettingsThread */
                oldLogger = bThread.getLogger();
                bThread.setLogger(logger);
            }
            try {
                final Browser br = new Browser();
                br.setLogger(logger);
                plugin.setBrowser(br);
                plugin.init();
                /* not every plugin sets this info correct */
                account.setError(null, -1, null);
                /* get previous account info and resets info for new update */
                ai = account.getAccountInfo();
                if (ai != null) {
                    /* reset expired and setValid */
                    ai.setExpired(false);
                    ai.setValidUntil(-1);
                }
                long tempDisabledCounterBefore = account.getTmpDisabledTimeout();
                try {
                    /*
                     * make sure the current Thread uses the PluginClassLoaderChild of the Plugin in use
                     */
                    ai = plugin.fetchAccountInfo(account);
                    plugin.validateLastChallengeResponse();
                    account.setAccountInfo(ai);
                } finally {
                    account.setUpdateTime(System.currentTimeMillis());
                }
                if (account.isValid() == false) {
                    /* account is invalid */
                    logger.info("Account:" + whoAmI + "|Invalid!");
                    account.setError(AccountError.INVALID, -1, null);
                    return ai;
                } else {
                    account.setLastValidTimestamp(System.currentTimeMillis());
                }
                if (ai != null && ai.isExpired()) {
                    /* expired account */
                    logger.clear();
                    logger.info("Account:" + whoAmI + "|Expired!");
                    account.setError(AccountError.EXPIRED, -1, null);
                    return ai;
                }
                if (tempDisabledCounterBefore > 0 && account.getTmpDisabledTimeout() == tempDisabledCounterBefore) {
                    /* reset temp disabled information */
                    logger.info("no longer temp disabled!");
                    account.setTempDisabled(false);
                }
                logger.clear();
            } catch (final Throwable e) {
                logger.log(e);
                return plugin.handleAccountException(account, logger, e);
            } finally {
                try {
                    if (plugin != null) {
                        plugin.invalidateLastChallengeResponse();
                    }
                } catch (final Throwable e) {
                    logger.log(e);
                }
                if (plugin != null) {
                    try {
                        plugin.clean();
                    } catch (final Throwable e) {
                        logger.log(e);
                    }
                }
                logger.close();
                if (bThread != null) {
                    /* remove logger from browserSettingsThread */
                    bThread.setLogger(oldLogger);
                }
            }
            return ai;
        } finally {
            PluginClassLoader.setThreadPluginClassLoaderChild(null, null);
            account.setNotifyHandler(null);
            account.setChecking(false);
            getEventSender().fireEvent(new AccountControllerEvent(this, AccountControllerEvent.Types.ACCOUNT_CHECKED, account));
            checkAccountUpOrDowngrade(account);
            final AccountError errorNow = account.getError();
            if (errorBefore != errorNow) {
                AccountProperty latestChangeEvent = null;
                synchronized (propertyChanges) {
                    latestChangeEvent = propertyChanges.get(AccountProperty.Property.ERROR);
                }
                if (latestChangeEvent != null) {
                    getEventSender().fireEvent(new AccountPropertyChangedEvent(latestChangeEvent.getAccount(), latestChangeEvent));
                }
            }
        }
    }

    private final String lastKnownAccountTypeProperty         = "lastKnownAccountType";
    private final String lastKnownValidUntilTimeStampProperty = "lastKnownValidUntilTimeStamp";
    private final long   minimumExtendTime                    = 24 * 60 * 60 * 1000l;

    private void checkAccountUpOrDowngrade(final Account account) {
        final AccountInfo accountInfo = account.getAccountInfo();
        if (accountInfo != null && account.getError() == null && account.getLastValidTimestamp() > 0 && account.getPlugin() != null) {
            final String lastKnownAccountType;
            final AccountType currentAccountType = account.getType();
            if (currentAccountType != null) {
                lastKnownAccountType = account.getStringProperty(lastKnownAccountTypeProperty, currentAccountType.name());
                account.setProperty(lastKnownAccountTypeProperty, currentAccountType.name());
            } else {
                lastKnownAccountType = account.getStringProperty(lastKnownAccountTypeProperty, AccountType.UNKNOWN.name());
                account.setProperty(lastKnownAccountTypeProperty, AccountType.UNKNOWN.name());
            }
            final long currentValidUntilTimeStamp = account.getValidPremiumUntil();
            final boolean hasLastKnownPremiumValidUntilTimeStamp = account.hasProperty(lastKnownValidUntilTimeStampProperty);
            final long lastKnownPremiumValidUntilTimeStamp = account.getLongProperty(lastKnownValidUntilTimeStampProperty, currentValidUntilTimeStamp);
            final boolean isPremiumAccount = AccountType.PREMIUM.equals(currentAccountType);
            final boolean wasPremiumAccount = AccountType.PREMIUM.name().equals(lastKnownAccountType);
            final boolean isPremiumUpgraded = isPremiumAccount && !wasPremiumAccount;
            final boolean isPremiumDowngraded = !isPremiumAccount && wasPremiumAccount;
            final boolean isLimitedRenewal = (currentValidUntilTimeStamp > lastKnownPremiumValidUntilTimeStamp && (currentValidUntilTimeStamp - lastKnownPremiumValidUntilTimeStamp) > minimumExtendTime);
            final boolean isPremiumLimitedRenewal = isPremiumAccount && isLimitedRenewal;
            final boolean isUnlimitedRenewal = currentValidUntilTimeStamp != lastKnownPremiumValidUntilTimeStamp && currentValidUntilTimeStamp == -1;
            final boolean isPremiumUnlimitedRenewal = isPremiumAccount && isUnlimitedRenewal;
            if (isPremiumLimitedRenewal || isPremiumUnlimitedRenewal) {
                account.setProperty(lastKnownValidUntilTimeStampProperty, currentValidUntilTimeStamp);
            } else if (isPremiumAccount && !hasLastKnownPremiumValidUntilTimeStamp) {
                account.setProperty(lastKnownValidUntilTimeStampProperty, currentValidUntilTimeStamp);
            }
            final long renewalDuration;
            if (currentValidUntilTimeStamp > 0) {
                if (lastKnownPremiumValidUntilTimeStamp > 0 && (lastKnownPremiumValidUntilTimeStamp - System.currentTimeMillis() > 0)) {
                    renewalDuration = currentValidUntilTimeStamp - lastKnownPremiumValidUntilTimeStamp;
                } else {
                    renewalDuration = currentValidUntilTimeStamp - System.currentTimeMillis();
                }
            } else {
                renewalDuration = 0;
            }
            if (isPremiumDowngraded || isPremiumUpgraded || isPremiumLimitedRenewal || isPremiumUnlimitedRenewal) {
                getEventSender().fireEvent(new AccountUpOrDowngradeEvent(this, account) {
                    @Override
                    public boolean isPremiumAccount() {
                        return isPremiumAccount;
                    }

                    @Override
                    public boolean isPremiumUpgraded() {
                        return isPremiumUpgraded;
                    }

                    @Override
                    public boolean isPremiumDowngraded() {
                        return isPremiumDowngraded;
                    }

                    @Override
                    public boolean isPremiumLimitedRenewal() {
                        return isPremiumLimitedRenewal;
                    }

                    @Override
                    public boolean isPremiumUnlimitedRenewal() {
                        return isPremiumUnlimitedRenewal;
                    }

                    @Override
                    public long getPremiumRenewalDuration() {
                        return renewalDuration;
                    }

                    @Override
                    public long getExpireTimeStamp() {
                        return currentValidUntilTimeStamp;
                    }
                });
            }
        }
    }

    public void checkPluginUpdates() {
        /**
         * TODO: assignPlugin(see loadAccounts)
         */
        final PluginFinder pluginFinder = new PluginFinder();
        for (final Account account : list(null)) {
            final AccountInfo accountInfo = account.getAccountInfo();
            if (account.getPlugin() != null && account.isMultiHost() && accountInfo != null) {
                try {
                    accountInfo.setMultiHostSupport(account.getPlugin(), accountInfo.getMultiHostSupport(), pluginFinder);
                    getEventSender().fireEvent(new AccountControllerEvent(this, AccountControllerEvent.Types.ACCOUNT_CHECKED, account));
                } catch (final Throwable e) {
                    LogController.CL().log(e);
                }
            }
        }
    }

    public static AccountController getInstance() {
        return INSTANCE;
    }

    private synchronized HashMap<String, List<Account>> loadAccounts(AccountSettings config, boolean allowRestore) {
        HashMap<String, ArrayList<AccountData>> dat = config.getAccounts();
        if (dat == null && allowRestore) {
            try {
                dat = restore();
            } catch (final Throwable e) {
                LogController.CL().log(e);
            }
        }
        if (dat == null) {
            dat = new HashMap<String, ArrayList<AccountData>>();
        }
        final PluginFinder pluginFinder = new PluginFinder();
        final HashMap<String, List<Account>> ret = new HashMap<String, List<Account>>();
        for (Iterator<Entry<String, ArrayList<AccountData>>> it = dat.entrySet().iterator(); it.hasNext();) {
            final Entry<String, ArrayList<AccountData>> next = it.next();
            if (next.getValue().size() > 0) {
                final String nextHost = next.getKey().toLowerCase(Locale.ENGLISH);
                for (final AccountData ad : next.getValue()) {
                    final Account acc = ad.toAccount();
                    acc.setHoster(nextHost);
                    final PluginForHost plugin = pluginFinder.assignPlugin(acc, true);
                    final String accountHost;
                    if (plugin != null) {
                        accountHost = plugin.getHost();
                        acc.setPlugin(plugin);
                    } else {
                        accountHost = nextHost;
                        acc.setPlugin(null);
                    }
                    final AccountInfo ai = acc.getAccountInfo();
                    if (ai != null) {
                        if (plugin != null) {
                            try {
                                ai.setMultiHostSupport(plugin, ai.getMultiHostSupport(), pluginFinder);
                            } catch (final Throwable e) {
                                LogController.CL().log(e);
                            }
                        } else {
                            ai.setMultiHostSupport(null, null, null);
                        }
                    }
                    List<Account> accs = ret.get(accountHost);
                    if (accs == null) {
                        accs = new ArrayList<Account>();
                        ret.put(accountHost, accs);
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
                    List<Object> accList = (List<Object>) next.getValue();
                    if (accList.size() > 0) {
                        ArrayList<AccountData> list = new ArrayList<AccountData>();
                        ret.put(next.getKey(), list);
                        if (accList.get(0) instanceof Account) {
                            List<Account> accList2 = (List<Account>) next.getValue();
                            for (Account a : accList2) {
                                AccountData ac;
                                list.add(ac = new AccountData());
                                ac.setUser(a.getUser());
                                ac.setPassword(a.getPass());
                                ac.setEnabled(a.isEnabled());
                            }
                        } else if (accList.get(0) instanceof Map) {
                            List<Map<String, Object>> accList2 = (List<Map<String, Object>>) next.getValue();
                            for (Map<String, Object> a : accList2) {
                                AccountData ac;
                                list.add(ac = new AccountData());
                                ac.setUser((String) a.get("user"));
                                ac.setPassword((String) a.get("pass"));
                                ac.setEnabled(a.containsKey("enabled"));
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
        final ArrayList<Account> ret = new ArrayList<Account>();
        synchronized (AccountController.this) {
            if (host == null) {
                for (final List<Account> accounts : ACCOUNTS.values()) {
                    if (accounts != null) {
                        for (final Account acc : accounts) {
                            if (acc.getPlugin() != null) {
                                ret.add(acc);
                            }
                        }
                    }
                }
            } else {
                final List<Account> ret2 = ACCOUNTS.get(host.toLowerCase(Locale.ENGLISH));
                if (ret2 != null) {
                    for (final Account acc : ret2) {
                        if (acc.getPlugin() != null) {
                            ret.add(acc);
                        }
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
    public boolean hasAccounts(final String host) {
        if (host != null) {
            synchronized (AccountController.this) {
                final List<Account> ret = ACCOUNTS.get(host.toLowerCase(Locale.ENGLISH));
                if (ret != null && ret.size() > 0) {
                    for (final Account acc : ret) {
                        if (acc.isValid() && acc.getPlugin() != null) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public int getAccountsSize(final String host) {
        if (host != null) {
            synchronized (AccountController.this) {
                final List<Account> ret = ACCOUNTS.get(host.toLowerCase(Locale.ENGLISH));
                if (ret != null && ret.size() > 0) {
                    return ret.size();
                }
                return 0;
            }
        }
        return 0;
    }

    public void addAccount(final Account account) {
        addAccount(account, true);
    }

    public void addAccount(final Account account, boolean forceCheck) {
        if (account != null) {
            if (account.getPlugin() == null) {
                new PluginFinder().assignPlugin(account, true);
            }
            if (account.getHoster() != null) {
                Account existingAccount = null;
                synchronized (AccountController.this) {
                    final String host = account.getHoster().toLowerCase(Locale.ENGLISH);
                    List<Account> accs = ACCOUNTS.get(host);
                    if (accs == null) {
                        accs = new ArrayList<Account>();
                        ACCOUNTS.put(host, accs);
                    }
                    for (final Account acc : accs) {
                        if (acc.equals(account)) {
                            existingAccount = acc;
                            break;
                        }
                    }
                    if (existingAccount == null) {
                        account.setAccountController(this);
                        accs.add(account);
                    }
                }
                if (existingAccount == null) {
                    this.broadcaster.fireEvent(new AccountControllerEvent(this, AccountControllerEvent.Types.ADDED, account));
                } else if (existingAccount != null && (!existingAccount.isEnabled() || existingAccount.getError() != null)) {
                    // reuse properties and accountInfos from new account
                    existingAccount.setError(null, -1, null);
                    existingAccount.setAccountInfo(account.getAccountInfo());
                    existingAccount.setProperties(account.getProperties());
                    existingAccount.setEnabled(true);
                    getEventSender().fireEvent(new AccountControllerEvent(this, AccountControllerEvent.Types.ACCOUNT_CHECKED, existingAccount));
                }
            }
        }
    }

    public boolean removeAccount(final Account account) {
        if (account == null) {
            return false;
        }
        /* remove reference to AccountController */
        account.setAccountController(null);
        synchronized (AccountController.this) {
            final String host = account.getHoster().toLowerCase(Locale.ENGLISH);
            final List<Account> accs = ACCOUNTS.get(host);
            if (accs == null || !accs.remove(account)) {
                return false;
            }
            if (accs.size() == 0) {
                ACCOUNTS.remove(host);
            }
        }
        this.broadcaster.fireEvent(new AccountControllerEvent(this, AccountControllerEvent.Types.REMOVED, account));
        return true;
    }

    public void onAccountControllerEvent(final AccountControllerEvent event) {
        Account acc = event.getAccount();
        delayedSaver.resetAndStart();
        boolean forceRecheck = false;
        switch (event.getType()) {
        case ADDED:
            org.jdownloader.settings.staticreferences.CFG_GENERAL.USE_AVAILABLE_ACCOUNTS.setValue(true);
            updateInternalMultiHosterMap(acc, acc.getAccountInfo());
            break;
        case ACCOUNT_PROPERTY_UPDATE:
            AccountProperty propertyChange = ((AccountPropertyChangedEvent) event).getProperty();
            switch (propertyChange.getProperty()) {
            case ENABLED:
                if (Boolean.FALSE.equals(propertyChange.getValue())) {
                    return;
                }
                forceRecheck = true;
                break;
            case ERROR:
                if (propertyChange.getValue() != null) {
                    return;
                }
                forceRecheck = true;
                break;
            case PASSWORD:
            case USERNAME:
                forceRecheck = true;
                break;
            }
            break;
        case ACCOUNT_CHECKED:
            updateInternalMultiHosterMap(acc, acc.getAccountInfo());
            return;
        case REMOVED:
            updateInternalMultiHosterMap(acc, null);
            return;
        }
        if (acc == null || acc != null && acc.isEnabled() == false) {
            return;
        }
        if ((Thread.currentThread() instanceof AccountCheckerThread)) {
            return;
        }
        AccountChecker.getInstance().check(acc, forceRecheck);
    }

    private void refreshAccountStats() {
        synchronized (AccountController.this) {
            for (final List<Account> accounts : ACCOUNTS.values()) {
                if (accounts != null) {
                    for (final Account acc : accounts) {
                        if (acc.getPlugin() != null && acc.isEnabled() && acc.isValid() && acc.refreshTimeoutReached()) {
                            /*
                             * we do not force update here, the internal timeout will make sure accounts get fresh checked from time to time
                             */
                            AccountChecker.getInstance().check(acc, false);
                        }
                    }
                }
            }
        }
    }

    @Deprecated
    public Account getValidAccount(final PluginForHost pluginForHost) {
        return getValidAccount(pluginForHost.getHost());
    }

    protected Account getValidAccount(final String host) {
        final List<Account> ret = getValidAccounts(host);
        if (ret != null && ret.size() > 0) {
            return ret.get(0);
        }
        return null;
    }

    @Deprecated
    public Account getValidAccount(final PluginForDecrypt pluginForDecrypt) {
        return getValidAccount(pluginForDecrypt.getHost());
    }

    public ArrayList<Account> getValidAccounts(final String host) {
        if (StringUtils.isEmpty(host)) {
            return null;
        } else {
            final ArrayList<Account> ret;
            final Thread currentThread = Thread.currentThread();
            if (currentThread instanceof SingleDownloadController) {
                // requestFileInformation must use the account from DownloadLinkCandidate of SingleDownloadController
                final SingleDownloadController controller = (SingleDownloadController) currentThread;
                final Account acc = controller.getAccount();
                if (acc != null && StringUtils.equals(acc.getHosterByPlugin(), host)) {
                    ret = new ArrayList<Account>();
                    ret.add(acc);
                    return ret;
                }
            }
            synchronized (AccountController.this) {
                final List<Account> accounts = ACCOUNTS.get(host.toLowerCase(Locale.ENGLISH));
                if (accounts == null || accounts.size() == 0) {
                    return null;
                }
                ret = new ArrayList<Account>(accounts);
            }
            final ListIterator<Account> it = ret.listIterator(ret.size());
            while (it.hasPrevious()) {
                final Account next = it.previous();
                if (!next.isEnabled() || !next.isValid() || next.isTempDisabled() || next.getPlugin() == null) {
                    /* we remove every invalid/disabled/tempdisabled/blocked account */
                    it.remove();
                }
            }
            return ret;
        }
    }

    public boolean hasAccount(final String host, final Boolean isEnabled, final Boolean isValid, final Boolean isPremium, final Boolean isExpired) {
        if (StringUtils.isEmpty(host)) {
            return false;
        }
        synchronized (AccountController.this) {
            final List<Account> accounts = ACCOUNTS.get(host.toLowerCase(Locale.ENGLISH));
            if (accounts != null) {
                for (final Account account : accounts) {
                    if (account.getPlugin() != null) {
                        if (isEnabled != null && isEnabled != account.isEnabled()) {
                            continue;
                        }
                        if (isValid != null && isValid != (account.getError() == null)) {
                            continue;
                        }
                        if (isPremium != null && isPremium != AccountType.PREMIUM.equals(account.getType())) {
                            continue;
                        }
                        if (isExpired != null) {
                            final AccountInfo ai = account.getAccountInfo();
                            if (ai != null) {
                                final long validUntilTimeStamp = ai.getValidUntil();
                                final boolean expired = validUntilTimeStamp > 0 && validUntilTimeStamp - System.currentTimeMillis() < 0;
                                if (isExpired != expired) {
                                    continue;
                                }
                            }
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public List<Account> getMultiHostAccounts(final String host) {
        if (host != null) {
            synchronized (AccountController.this) {
                final List<Account> list = MULTIHOSTER_ACCOUNTS.get(host.toLowerCase(Locale.ENGLISH));
                if (list != null && list.size() > 0) {
                    return new ArrayList<Account>(list);
                }
            }
        }
        return null;
    }

    public boolean hasMultiHostAccounts(final String host) {
        if (host != null) {
            synchronized (AccountController.this) {
                return MULTIHOSTER_ACCOUNTS.containsKey(host.toLowerCase(Locale.ENGLISH));
            }
        }
        return false;
    }

    @Deprecated
    public ArrayList<Account> getAllAccounts(final String string) {
        return list(string);
    }

    public static String createFullBuyPremiumUrl(String buyPremiumUrl, String id) {
        return "http://update3.jdownloader.org/jdserv/BuyPremiumInterface/redirect?" + Encoding.urlEncode(buyPremiumUrl) + "&" + Encoding.urlEncode(id);
    }

    @Override
    public boolean fireAccountPropertyChange(jd.plugins.AccountProperty propertyChange) {
        if (propertyChange.getAccount().isChecking()) {
            return false;
        }
        getEventSender().fireEvent(new AccountPropertyChangedEvent(propertyChange.getAccount(), propertyChange));
        return true;
    }

    public List<Account> importAccounts(File f) {
        /* TODO: add cleanup to avoid memleak */
        final AccountSettings cfg = JsonConfig.create(new File(f.getParent(), "org.jdownloader.settings.AccountSettings"), AccountSettings.class);
        StatsManager.I().track("premium/import/" + cfg.getListID() + "/" + cfg.getListVersion());
        final long timeStamp = System.currentTimeMillis();
        final HashMap<String, List<Account>> accounts = loadAccounts(cfg, false);
        final ArrayList<Account> added = new ArrayList<Account>();
        for (final Entry<String, List<Account>> es : accounts.entrySet()) {
            for (final Account ad : es.getValue()) {
                final Account acc = new Account(ad.getUser(), ad.getPass());
                acc.setHoster(ad.getHoster());
                acc.setProperty(StatsManager.IMPORTED_TIMESTAMP, timeStamp);
                acc.setProperty(StatsManager.SLID, cfg.getListID());
                acc.setProperty(StatsManager.SLV, cfg.getListVersion());
                addAccount(acc);
                added.add(ad);
            }
        }
        return added;
    }
}