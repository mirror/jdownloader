package org.jdownloader.controlling.hosterrule;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.SecondLevelLaunch;
import jd.controlling.AccountController;
import jd.controlling.AccountControllerEvent;
import jd.controlling.AccountControllerListener;
import jd.controlling.downloadcontroller.AccountCache;
import jd.controlling.downloadcontroller.AccountCache.ACCOUNTTYPE;
import jd.controlling.downloadcontroller.AccountCache.CachedAccount;
import jd.controlling.downloadcontroller.DownloadSession;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel.dialog.EditHosterRuleDialog;
import jd.plugins.Account;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.queue.Queue;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.logging.LogController;

public class HosterRuleController implements AccountControllerListener {
    private static final HosterRuleController INSTANCE = new HosterRuleController();

    /**
     * get the only existing instance of HosterRuleController. This is a singleton
     * 
     * @return
     */
    public static HosterRuleController getInstance() {
        return HosterRuleController.INSTANCE;
    }

    private final HosterRuleControllerEventSender eventSender;
    private final List<AccountUsageRule>          loadedRules;
    private final DelayedRunnable                 delayedSaver;
    private final File                            configFile;
    private final LogSource                       logger;
    private final AtomicBoolean                   initDone = new AtomicBoolean(false);
    private final Queue                           queue;

    /**
     * Create a new instance of HosterRuleController. This is a singleton class. Access the only existing instance by using {@link #getInstance()}.
     */
    private HosterRuleController() {
        eventSender = new HosterRuleControllerEventSender();
        configFile = Application.getResource("cfg/accountUsageRules.json");
        if (!configFile.getParentFile().exists()) configFile.getParentFile().mkdirs();
        logger = LogController.getInstance().getLogger(HosterRuleController.class.getName());
        queue = new Queue("HosterRuleController") {
            @Override
            public void killQueue() {
                logger.log(new Throwable("You cannot kill me!"));
            };
        };
        loadedRules = new CopyOnWriteArrayList<AccountUsageRule>();
        delayedSaver = new DelayedRunnable(5000, 30000) {

            @Override
            public String getID() {
                return "HosterRuleController";
            }

            @Override
            public void delayedrun() {
                save();
            }
        };
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public void onShutdown(ShutdownRequest shutdownRequest) {
                save();
            }
        });
        SecondLevelLaunch.ACCOUNTLIST_LOADED.executeWhenReached(new Runnable() {

            @Override
            public void run() {
                try {
                    load();
                    AccountController.getInstance().getBroadcaster().addListener(HosterRuleController.this);
                } finally {
                    initDone.set(true);
                }
            }
        });

    }

    private void load() {
        if (configFile.exists()) {
            try {
                ArrayList<AccountRuleStorable> loaded = JSonStorage.restoreFromString(IO.readFileToString(configFile), new TypeRef<ArrayList<AccountRuleStorable>>() {
                }, null);
                if (loaded != null && loaded.size() > 0) {
                    ArrayList<Account> availableAccounts = AccountController.getInstance().list(null);
                    List<AccountUsageRule> rules = new ArrayList<AccountUsageRule>();
                    for (AccountRuleStorable ars : loaded) {
                        try {
                            AccountUsageRule rule = ars.restore(availableAccounts);
                            rule.setOwner(this);
                            rules.add(rule);
                        } catch (Throwable e) {
                            logger.log(e);
                        }
                    }
                    this.loadedRules.addAll(rules);
                    validateRules();
                }
            } catch (Throwable e) {
                logger.log(e);
            }
        }
    }

    private void validateRules() {
        queue.add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                for (AccountUsageRule hr : loadedRules) {
                    validateRule(hr);
                }
                return null;
            }
        });
    }

    public AccountCache getAccountCache(String host, final DownloadSession session) {
        if (StringUtils.isEmpty(host)) return null;
        final String finalHost = host.toLowerCase(Locale.ENGLISH);
        return queue.addWait(new QueueAction<AccountCache, RuntimeException>() {

            @Override
            protected AccountCache run() throws RuntimeException {
                for (AccountUsageRule hr : loadedRules) {
                    if (!hr.isEnabled()) continue;
                    if (finalHost.equalsIgnoreCase(hr.getHoster())) {
                        int lastCacheSize = 0;
                        ArrayList<CachedAccount> newCache = new ArrayList<CachedAccount>();
                        ArrayList<AccountGroup.Rules> rules = new ArrayList<AccountGroup.Rules>();
                        for (AccountGroup ag : hr.getAccounts()) {
                            if (lastCacheSize != newCache.size()) {
                                lastCacheSize = newCache.size();
                                /* add null as seperator for different AccountGroups */
                                rules.add(null);
                            }
                            for (AccountReference acr : ag.getChildren()) {
                                if (!acr.isEnabled()) continue;
                                CachedAccount cachedAccount = null;
                                if (FreeAccountReference.isFreeAccount(acr)) {
                                    cachedAccount = new CachedAccount(finalHost, null, ACCOUNTTYPE.NONE, session.getPlugin(finalHost));
                                } else {
                                    Account acc = acr.getAccount();
                                    if (acc != null) {
                                        if (acc.isMulti()) {
                                            cachedAccount = new CachedAccount(finalHost, acc, ACCOUNTTYPE.MULTI, session.getPlugin(acc.getHoster()));
                                        } else {
                                            cachedAccount = new CachedAccount(finalHost, acc, ACCOUNTTYPE.ORIGINAL, session.getPlugin(finalHost));
                                        }
                                    }
                                }
                                if (cachedAccount != null) {
                                    newCache.add(cachedAccount);
                                    rules.add(ag.getRule());
                                }

                            }
                        }
                        return new AccountCache(newCache, rules);
                    }
                }
                return null;
            }
        });
    }

    protected void validateRule(AccountUsageRule hr) {
        final String host = hr.getHoster();
        HashSet<Account> accountsInRule = new HashSet<Account>();
        AccountGroup onlyRealAccounts = null;
        AccountGroup freeAccountGroup = null;
        AccountGroup onlyMultiAccounts = null;
        AccountReference free = null;

        for (Iterator<AccountGroup> it1 = hr.getAccounts().iterator(); it1.hasNext();) {
            AccountGroup ag = it1.next();
            boolean onlyReal = ag.getChildren().size() > 0;
            boolean onlyMulti = ag.getChildren().size() > 0;

            for (Iterator<AccountReference> it = ag.getChildren().iterator(); it.hasNext();) {
                AccountReference ar = it.next();

                if (FreeAccountReference.isFreeAccount(ar)) {
                    free = ar;
                    onlyMulti = false;
                    onlyReal = false;
                    freeAccountGroup = ag;
                    continue;
                }
                if (ar.getAccount() == null) {
                    logger.info("Removed " + ar + " from " + ag);
                    it.remove();
                } else {
                    if (!ar.getAccount().isMulti()) {
                        onlyMulti = false;
                    } else {
                        onlyReal = false;
                    }
                    accountsInRule.add(ar.getAccount());
                }
            }
            // remove empty groups
            if (ag.getChildren().size() == 0) {
                it1.remove();
            }
            if (onlyReal) {
                onlyRealAccounts = ag;
            }
            if (onlyMulti) {
                onlyMultiAccounts = ag;
            }
        }

        HashSet<Account> missingRealAccounts = new HashSet<Account>();
        HashSet<Account> missingMultiAccounts = new HashSet<Account>();
        for (Account acc : AccountController.getInstance().list(host)) {
            if (accountsInRule.add(acc)) {
                missingRealAccounts.add(acc);
            }
        }
        for (Account acc : AccountController.getInstance().getMultiHostAccounts(host)) {
            if (accountsInRule.add(acc)) {
                missingMultiAccounts.add(acc);
            }
        }

        if (missingRealAccounts.size() > 0) {
            ArrayList<AccountReference> refList = new ArrayList<AccountReference>();
            for (Account acc : missingRealAccounts) {
                refList.add(new AccountReference(acc));
            }
            if (onlyRealAccounts != null) {
                onlyRealAccounts.getChildren().addAll(refList);
            } else {
                hr.getAccounts().add(0, new AccountGroup(refList, _GUI._.HosterRuleController_validateRule_single_hoster_account()));
            }

        }
        if (missingMultiAccounts.size() > 0) {
            ArrayList<AccountReference> refList = new ArrayList<AccountReference>();
            for (Account acc : missingMultiAccounts) {
                refList.add(new AccountReference(acc));
            }
            if (onlyMultiAccounts != null) {
                onlyMultiAccounts.getChildren().addAll(refList);
            } else {
                int index = freeAccountGroup == null ? hr.getAccounts().size() : hr.getAccounts().indexOf(freeAccountGroup);
                if (index < 0) index = hr.getAccounts().size();
                hr.getAccounts().add(index, new AccountGroup(refList, _GUI._.HosterRuleController_validateRule_multi_hoster_account()));
            }
        }
        if (free == null) {
            ArrayList<AccountReference> refList = new ArrayList<AccountReference>();
            refList.add(new FreeAccountReference(host));
            hr.getAccounts().add(new AccountGroup(refList, _GUI._.HosterRuleController_validateRule_free()));
        }
    }

    protected void save() {
        if (SecondLevelLaunch.ACCOUNTLIST_LOADED.isReached() && initDone.get()) {
            try {
                ArrayList<AccountRuleStorable> saveList = new ArrayList<AccountRuleStorable>();
                for (AccountUsageRule hr : loadedRules) {
                    saveList.add(new AccountRuleStorable(hr));
                }
                IO.secureWrite(configFile, JSonStorage.serializeToJson(saveList).getBytes("UTF-8"));
            } catch (Exception e) {
                logger.log(e);
            }
        }
    }

    public HosterRuleControllerEventSender getEventSender() {
        return eventSender;
    }

    @Override
    public void onAccountControllerEvent(AccountControllerEvent event) {
        validateRules();
    }

    public List<AccountUsageRule> list() {
        return loadedRules;
    }

    public void add(final AccountUsageRule rule) {
        if (rule == null) return;
        queue.add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                validateRule(rule);
                rule.setOwner(HosterRuleController.this);
                loadedRules.add(rule);
                delayedSaver.delayedrun();
                eventSender.fireEvent(new HosterRuleControllerEvent(this, HosterRuleControllerEvent.Type.ADDED, rule));
                return null;
            }
        });

    }

    public void fireUpdate(final AccountUsageRule rule) {
        if (rule == null) return;
        queue.add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                delayedSaver.delayedrun();
                try {
                    validateRule(rule);
                } finally {
                    eventSender.fireEvent(new HosterRuleControllerEvent(this, HosterRuleControllerEvent.Type.DATA_UPDATE, rule));
                }
                return null;
            }
        });
    }

    public void showEditPanel(final AccountUsageRule editing) {
        if (editing == null) return;
        EditHosterRuleDialog d = new EditHosterRuleDialog(editing);
        try {
            Dialog.getInstance().showDialog(d);
            final AccountUsageRule newRule = d.getRule();
            queue.add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    validateRule(newRule);
                    editing.set(newRule.isEnabled(), newRule.getAccounts());
                    return null;
                }
            });

        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }
    }

    public void remove(final AccountUsageRule rule) {
        if (rule == null) return;
        queue.add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                if (loadedRules.remove(rule)) {
                    rule.setOwner(null);
                    delayedSaver.delayedrun();
                    eventSender.fireEvent(new HosterRuleControllerEvent(this, HosterRuleControllerEvent.Type.REMOVED, rule));
                }
                return null;
            }
        });

    }

}
