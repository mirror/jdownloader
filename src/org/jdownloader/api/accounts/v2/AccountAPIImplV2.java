package org.jdownloader.api.accounts.v2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import jd.controlling.AccountController;
import jd.plugins.Account;
import jd.plugins.AccountInfo;

import org.jdownloader.myjdownloader.client.bindings.AccountQuery;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;

public class AccountAPIImplV2 implements AccountAPIV2 {

    public AccountAPIImplV2() {

    }

    public List<AccountAPIStorableV2> listAccounts(AccountQuery queryParams) {

        List<Account> accs = AccountController.getInstance().list();

        java.util.List<AccountAPIStorableV2> ret = new ArrayList<AccountAPIStorableV2>();

        int startWith = queryParams.getStartAt();
        int maxResults = queryParams.getMaxResults();

        if (startWith > accs.size() - 1) return ret;
        if (startWith < 0) startWith = 0;
        if (maxResults < 0) maxResults = accs.size();

        for (int i = startWith; i < Math.min(startWith + maxResults, accs.size()); i++) {
            Account acc = accs.get(i);
            AccountAPIStorableV2 accas = new AccountAPIStorableV2(acc);

            if (queryParams.isUserName()) {
                accas.setUsername(acc.getUser());
            }

            if (queryParams.isValidUntil()) {
                AccountInfo ai = acc.getAccountInfo();
                if (ai != null) {
                    accas.setValidUntil(ai.getValidUntil());
                }
            }
            if (queryParams.isTrafficLeft()) {
                AccountInfo ai = acc.getAccountInfo();
                if (ai != null) {
                    accas.setTrafficLeft(ai.getTrafficLeft());

                }
            }
            if (queryParams.isTrafficMax()) {
                AccountInfo ai = acc.getAccountInfo();
                accas.setTrafficMax(ai.getTrafficMax());

            }
            if (queryParams.isEnabled()) {
                accas.setEnabled(acc.isEnabled());
            }
            if (queryParams.isValid()) {
                accas.setValid(acc.isValid());
            }

            ret.add(accas);
        }
        return ret;
    }

    public List<String> listPremiumHoster() {

        final Collection<LazyHostPlugin> allPLugins = HostPluginController.getInstance().list();
        // Filter - only premium plugins should be here
        final java.util.List<LazyHostPlugin> plugins = new ArrayList<LazyHostPlugin>();
        List<String> ret = new ArrayList<String>();
        for (LazyHostPlugin lhp : allPLugins) {
            if (lhp.isPremium()) {
                plugins.add(lhp);
                ret.add(lhp.getDisplayName());
            }
        }
        return ret;
    }

    @Override
    public HashMap<String, String> listPremiumHosterUrls() {

        final Collection<LazyHostPlugin> allPLugins = HostPluginController.getInstance().list();
        // Filter - only premium plugins should be here
        final java.util.List<LazyHostPlugin> plugins = new ArrayList<LazyHostPlugin>();

        HashMap<String, String> ret = new HashMap<String, String>();
        for (LazyHostPlugin lhp : allPLugins) {
            if (lhp.isPremium()) {
                plugins.add(lhp);
                ret.put(lhp.getDisplayName(), AccountController.createFullBuyPremiumUrl(lhp.getPremiumUrl(), "accountmanager/webinterface"));
            }
        }
        return ret;
    }

    @Override
    public String getPremiumHosterUrl(String hoster) {
        if (hoster == null) { return null; }
        if (HostPluginController.getInstance().get(hoster) == null) { return null; }
        return AccountController.createFullBuyPremiumUrl(HostPluginController.getInstance().get(hoster).getPremiumUrl(), "captcha/webinterface");
    }

    public boolean removeAccounts(long[] ids) {
        java.util.List<Account> removeACCs = getAccountbyIDs(ids);
        for (Account acc : removeACCs) {
            AccountController.getInstance().removeAccount(acc);
        }
        return true;
    }

    private java.util.List<Account> getAccountbyIDs(long[] ids) {
        HashSet<Long> todoIDs = new HashSet<Long>();
        for (long l : ids)
            todoIDs.add(l);
        java.util.List<Account> accs = new ArrayList<Account>();
        for (Account lacc : AccountController.getInstance().list()) {
            if (lacc != null && todoIDs.size() > 0) {
                if (todoIDs.remove(lacc.getId().getID())) {
                    accs.add(lacc);
                }

            } else if (todoIDs.size() == 0) {
                break;
            }
        }
        return accs;
    }

    @Override
    public boolean enableAccounts(final long[] linkIds) {
        return setEnabledState(true, linkIds);
    }

    @Override
    public boolean disableAccounts(long[] linkIds) {
        return setEnabledState(false, linkIds);
    }

    public boolean setEnabledState(boolean enabled, long[] ids) {
        java.util.List<Account> accs = getAccountbyIDs(ids);
        for (Account acc : accs) {
            acc.setEnabled(enabled);
        }
        return true;
    }

    public AccountAPIStorableV2 getAccountInfo(long id) {
        java.util.List<Account> accs = getAccountbyIDs(new long[] { id });
        if (accs.size() == 1) { return new AccountAPIStorableV2(accs.get(0)); }
        return null;
    }

    @Override
    public boolean addAccount(String premiumHoster, String username, String password) {
        Account acc = new Account(username, password);
        acc.setHoster(premiumHoster);
        AccountController.getInstance().addAccount(acc);
        return true;
    }

    @Override
    public boolean setUserNameAndPassword(Long accountId, String username, String password) {
        if (accountId == null) return false;

        for (Account acc : AccountController.getInstance().list()) {
            if (accountId.equals(acc.getId().getID())) {
                if (username != null && !username.isEmpty()) {
                    acc.setUser(username);
                }
                if (password != null && !password.isEmpty()) {
                    acc.setPass(password);
                }
                return true;
            }
        }
        return true;
    }
}
