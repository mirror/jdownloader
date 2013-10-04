package org.jdownloader.controlling.hosterrule;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import jd.SecondLevelLaunch;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.controlling.AccountControllerEvent;
import jd.controlling.AccountControllerListener;
import jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel.dialog.EditHosterRuleDialog;
import jd.plugins.Account;
import jd.plugins.AccountInfo;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
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
    private List<AccountUsageRule>                rules;
    private DelayedRunnable                       delayedSaver;
    private File                                  file;
    private LogSource                             logger;

    /**
     * Create a new instance of HosterRuleController. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private HosterRuleController() {
        eventSender = new HosterRuleControllerEventSender();
        file = Application.getResource("cfg/accountUsageRules.json");
        file.getParentFile().mkdirs();
        logger = LogController.getInstance().getLogger(HosterRuleController.class.getName());

        rules = new ArrayList<AccountUsageRule>();
        SecondLevelLaunch.ACCOUNTLIST_LOADED.executeWhenReached(new Runnable() {

            @Override
            public void run() {

                load();
                AccountController.getInstance().getBroadcaster().addListener(HosterRuleController.this);
            }
        });
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

    }

    private void load() {
        if (file.exists()) {
            try {
                ArrayList<AccountRuleStorable> loaded = JSonStorage.restoreFromString(IO.readFileToString(file), new TypeRef<ArrayList<AccountRuleStorable>>() {
                }, null);
                if (loaded != null) {
                    synchronized (rules) {
                        for (AccountRuleStorable ars : loaded) {
                            AccountUsageRule add = ars.restore();
                            add.setOwner(this);
                            rules.add(add);
                        }
                    }
                }
                validateRules();
            } catch (Exception e) {
                logger.log(e);
            }
        }

    }

    private void validateRules() {

        synchronized (rules) {
            for (AccountUsageRule hr : rules) {
                validateRule(hr);
            }
        }

    }

    protected void validateRule(AccountUsageRule hr) {
        HashSet<Account> accountsInRule = new HashSet<Account>();
        AccountGroup onlyRealAccounts = null;
        AccountGroup onlyMultiAccounts = null;
        AccountReference free = null;
        for (AccountGroup ag : hr.getAccounts()) {
            boolean onlyReal = ag.getChildren().size() > 0;
            boolean onlyMulti = ag.getChildren().size() > 0;

            for (Iterator<AccountReference> it = ag.getChildren().iterator(); it.hasNext();) {
                AccountReference ar = it.next();

                if (FreeAccountReference.isFreeAccount(ar)) {
                    free = ar;
                    continue;
                }
                if (ar.getAccount() == null) {
                    logger.info("Removed " + ar + " from " + ag);
                    it.remove();
                } else {
                    if (ar.getAccount().isMulti()) {
                        onlyMulti = false;
                    } else {
                        onlyReal = false;
                    }
                    accountsInRule.add(ar.getAccount());
                }
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
        for (Account acc : AccountController.getInstance().list(null)) {
            if (acc.getHoster().equalsIgnoreCase(hr.getHoster())) {
                if (!accountsInRule.contains(acc)) {
                    missingRealAccounts.add(acc);
                }
            } else {
                AccountInfo ai = acc.getAccountInfo();
                if (ai == null) continue;
                Object supported = null;
                synchronized (ai) {
                    /*
                     * synchronized on accountinfo because properties are not threadsafe
                     */
                    supported = ai.getProperty("multiHostSupport", Property.NULL);
                }
                if (Property.NULL == supported || supported == null) continue;
                synchronized (supported) {
                    /*
                     * synchronized on list because plugins can change the list in runtime
                     */
                    if (supported instanceof ArrayList) {
                        for (String sup : (java.util.List<String>) supported) {
                            if (sup.equalsIgnoreCase(hr.getHoster())) {
                                if (!accountsInRule.contains(acc)) {
                                    missingMultiAccounts.add(acc);
                                }
                                break;
                            }
                        }
                    }
                }
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
                hr.getAccounts().add(new AccountGroup(refList, _GUI._.HosterRuleController_validateRule_multi_hoster_account()));
            }

        }
        if (free == null) {
            ArrayList<AccountReference> refList = new ArrayList<AccountReference>();
            refList.add(new FreeAccountReference(hr.getHoster()));
            hr.getAccounts().add(new AccountGroup(refList, _GUI._.HosterRuleController_validateRule_free()));
        }
        // missing
        System.out.println(hr);
    }

    protected void save() {

        ArrayList<AccountRuleStorable> saveList = new ArrayList<AccountRuleStorable>();
        synchronized (rules) {
            for (AccountUsageRule hr : rules) {
                saveList.add(new AccountRuleStorable(hr));
            }
        }
        try {
            IO.secureWrite(file, JSonStorage.serializeToJson(saveList).getBytes("UTF-8"));
        } catch (Exception e) {
            logger.log(e);
        }
    }

    public HosterRuleControllerEventSender getEventSender() {
        return eventSender;
    }

    @Override
    public void onAccountControllerEvent(AccountControllerEvent event) {
        validateRules();
    }

    // public List<AccountGroup> getPriority(DomainInfo domainInfo) {

    // }

    public List<AccountUsageRule> list() {
        return Collections.unmodifiableList(rules);
    }

    public void add(AccountUsageRule rule) {
        synchronized (rules) {
            rule.setOwner(this);
            validateRule(rule);
            rules.add(rule);
        }
        validateRules();
        eventSender.fireEvent(new HosterRuleControllerEvent(this, HosterRuleControllerEvent.Type.ADDED, rule));
        delayedSaver.delayedrun();
    }

    public void fireUpdate() {
        delayedSaver.delayedrun();
        eventSender.fireEvent(new HosterRuleControllerEvent(this, HosterRuleControllerEvent.Type.DATA_UPDATE));
    }

    public void showEditPanel(AccountUsageRule editing) {
        EditHosterRuleDialog d = new EditHosterRuleDialog(editing);
        try {
            Dialog.getInstance().showDialog(d);
            AccountUsageRule newRule = d.getRule();
            editing.setEnabled(newRule.isEnabled());
            editing.setAccounts(newRule.getAccounts());

            validateRules();
            fireUpdate();
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }
    }

    public void remove(AccountUsageRule rule) {

        synchronized (rules) {

            rules.remove(rule);
        }
        validateRules();
        eventSender.fireEvent(new HosterRuleControllerEvent(this, HosterRuleControllerEvent.Type.REMOVED, rule));
        delayedSaver.delayedrun();
    }

}
