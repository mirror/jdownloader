package org.jdownloader.api.accounts.v2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import jd.controlling.AccountController;
import jd.plugins.Account;
import jd.plugins.Account.AccountError;
import jd.plugins.AccountInfo;

import org.appwork.utils.StringUtils;
import org.jdownloader.api.RemoteAPIController;
import org.jdownloader.myjdownloader.client.bindings.AccountQuery;
import org.jdownloader.myjdownloader.client.bindings.interfaces.AccountInterface;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.plugins.controller.host.PluginFinder;

public class AccountAPIImplV2 implements AccountAPIV2 {

    public AccountAPIImplV2() {
        RemoteAPIController.validateInterfaces(AccountAPIV2.class, AccountInterface.class);
    }

    public List<AccountAPIStorableV2> listAccounts(AccountQuery queryParams) {

        List<Account> accs = AccountController.getInstance().list();

        java.util.List<AccountAPIStorableV2> ret = new ArrayList<AccountAPIStorableV2>();

        int startWith = queryParams.getStartAt();
        int maxResults = queryParams.getMaxResults();

        if (startWith > accs.size() - 1) {
            return ret;
        }
        if (startWith < 0) {
            startWith = 0;
        }
        if (maxResults < 0) {
            maxResults = accs.size();
        }

        for (int i = startWith; i < Math.min(startWith + maxResults, accs.size()); i++) {
            Account acc = accs.get(i);
            if (queryParams.getUUIDList() != null && !queryParams.getUUIDList().contains(acc.getId().getID())) {
                continue;
            }
            AccountAPIStorableV2 accas = new AccountAPIStorableV2(acc);

            if (queryParams.isError()) {
                accas.setErrorString(acc.getErrorString());
                accas.setErrorType(enumToString(acc.getError()));
            }
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
                    if (ai.isUnlimitedTraffic()) {
                        accas.setTrafficLeft(-1l);
                    } else {
                        accas.setTrafficLeft(ai.getTrafficLeft());
                    }
                }
            }
            if (queryParams.isTrafficMax()) {
                AccountInfo ai = acc.getAccountInfo();
                if (ai != null) {
                    if (ai.isUnlimitedTraffic()) {
                        accas.setTrafficMax(-1l);
                    } else {
                        accas.setTrafficMax(ai.getTrafficMax());
                    }
                }

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

    private String enumToString(AccountError error) {
        if (error == null) {
            return null;
        }
        return error.name();
    }

    public List<String> listPremiumHoster() {
        final Collection<LazyHostPlugin> allPLugins = HostPluginController.getInstance().list();
        // Filter - only premium plugins should be here
        final List<String> ret = new ArrayList<String>();
        for (final LazyHostPlugin lhp : allPLugins) {
            if (lhp.isPremium()) {
                ret.add(lhp.getDisplayName());
            }
        }
        return ret;
    }

    @Override
    public HashMap<String, String> listPremiumHosterUrls() {
        final Collection<LazyHostPlugin> allPLugins = HostPluginController.getInstance().list();
        // Filter - only premium plugins should be here
        final HashMap<String, String> ret = new HashMap<String, String>();
        for (final LazyHostPlugin lhp : allPLugins) {
            if (lhp.isPremium()) {
                ret.put(lhp.getDisplayName(), AccountController.createFullBuyPremiumUrl(lhp.getPremiumUrl(), "accountmanager/webinterface"));
            }
        }
        return ret;
    }

    @Override
    public String getPremiumHosterUrl(String hoster) {
        if (hoster == null) {
            return null;
        }
        final LazyHostPlugin plugin = HostPluginController.getInstance().get(hoster);
        if (plugin == null) {
            return null;
        }
        return AccountController.createFullBuyPremiumUrl(plugin.getPremiumUrl(), "captcha/webinterface");
    }

    public void removeAccounts(long[] ids) {
        final List<Account> removeACCs = getAccountbyIDs(ids);
        for (final Account acc : removeACCs) {
            AccountController.getInstance().removeAccount(acc);
        }
    }

    private List<Account> getAccountbyIDs(long[] ids) {
        final HashSet<Long> todoIDs = new HashSet<Long>();
        for (long l : ids) {
            todoIDs.add(l);
        }
        final List<Account> accs = new ArrayList<Account>();
        for (final Account lacc : AccountController.getInstance().list()) {
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
    public void enableAccounts(final long[] linkIds) {
        setEnabledState(true, linkIds);
    }

    @Override
    public void disableAccounts(long[] linkIds) {
        setEnabledState(false, linkIds);
    }

    @Override
    public void refreshAccounts(long[] ids) {
        final List<Account> accs = getAccountbyIDs(ids);
        for (final Account acc : accs) {
            AccountController.getInstance().updateAccountInfo(acc, false);
        }
    }

    public void setEnabledState(boolean enabled, long[] ids) {
        final List<Account> accs = getAccountbyIDs(ids);
        for (final Account acc : accs) {
            acc.setEnabled(enabled);
        }
    }

    @Override
    public void addAccount(String host, String username, String password) {
        final PluginFinder pluginFinder = new PluginFinder();
        final String pluginHost = new PluginFinder().assignHost(host);
        if (pluginHost != null) {
            final Account acc = new Account(username, password);
            acc.setHoster(pluginHost);
            pluginFinder.assignPlugin(acc, true);
            AccountController.getInstance().addAccount(acc);
        }
    }

    @Override
    public boolean setUserNameAndPassword(long accountId, String username, String password) {
        for (final Account acc : AccountController.getInstance().list()) {
            if (accountId == acc.getId().getID()) {
                if (StringUtils.isNotEmpty(username)) {
                    acc.setUser(username);
                }
                if (StringUtils.isNotEmpty(password)) {
                    acc.setPass(password);
                }
                return true;
            }
        }
        return false;
    }

}
