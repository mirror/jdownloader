package org.jdownloader.api.accounts;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;

import jd.controlling.AccountController;
import jd.plugins.Account;
import jd.plugins.AccountInfo;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.remoteapi.APIQuery;
import org.appwork.remoteapi.QueryResponseMap;
import org.appwork.remoteapi.RemoteAPI;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.exceptions.InternalApiException;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.logging.Log;
import org.appwork.utils.net.HTTPHeader;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;

public class AccountAPIImpl implements AccountAPI {

    public List<AccountAPIStorable> queryAccounts(APIQuery queryParams) {

        List<Account> accs = AccountController.getInstance().list();

        java.util.List<AccountAPIStorable> ret = new ArrayList<AccountAPIStorable>();

        int startWith = queryParams.getStartAt();
        int maxResults = queryParams.getMaxResults();

        if (startWith > accs.size() - 1) return ret;
        if (startWith < 0) startWith = 0;
        if (maxResults < 0) maxResults = accs.size();

        for (int i = startWith; i < Math.min(startWith + maxResults, accs.size()); i++) {
            Account acc = accs.get(i);
            AccountAPIStorable accas = new AccountAPIStorable(acc);
            QueryResponseMap infoMap = new QueryResponseMap();

            if (queryParams.fieldRequested("username")) infoMap.put("username", acc.getUser());
            if (queryParams.fieldRequested("validUntil")) {
                AccountInfo ai = acc.getAccountInfo();
                if (ai != null) infoMap.put("validUntil", ai.getValidUntil());
            }
            if (queryParams.fieldRequested("trafficLeft")) {
                AccountInfo ai = acc.getAccountInfo();
                if (ai != null) infoMap.put("trafficLeft", ai.getTrafficLeft());
            }
            if (queryParams.fieldRequested("trafficMax")) {
                AccountInfo ai = acc.getAccountInfo();
                if (ai != null) infoMap.put("trafficMax", ai.getTrafficLeft());
            }
            if (queryParams.fieldRequested("enabled")) {
                infoMap.put("enabled", acc.isEnabled());
            }

            accas.setInfoMap(infoMap);
            ret.add(accas);
        }
        return ret;
    }

    public List<String> listPremiumHoster() {

        final List<LazyHostPlugin> allPLugins = HostPluginController.getInstance().list();
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
    public QueryResponseMap listPremiumHosterUrls() {

        final List<LazyHostPlugin> allPLugins = HostPluginController.getInstance().list();
        // Filter - only premium plugins should be here
        final java.util.List<LazyHostPlugin> plugins = new ArrayList<LazyHostPlugin>();

        QueryResponseMap ret = new QueryResponseMap();
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
        return AccountController.createFullBuyPremiumUrl(HostPluginController.getInstance().get(hoster).getPremiumUrl(), "captcha/webinterface");
    }

    @Override
    public void premiumHosterIcon(RemoteAPIRequest request, RemoteAPIResponse response, String premiumHoster) throws InternalApiException {
        OutputStream out = null;
        try {
            /* we force content type to image/png and allow caching of the image */
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CACHE_CONTROL, "public,max-age=60", false));
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE, "image/png", false));
            out = RemoteAPI.getOutputStream(response, request, RemoteAPI.gzip(request), false);

            LazyHostPlugin plugin = HostPluginController.getInstance().get(premiumHoster);
            if (plugin != null) ImageIO.write(IconIO.toBufferedImage(plugin.getIcon().getImage()), "png", out);

        } catch (IOException e) {
            Log.exception(e);
            throw new InternalApiException(e);
        } finally {
            try {
                out.close();
            } catch (final Throwable e) {
            }
        }
    }

    public boolean removeAccounts(Long[] ids) {
        java.util.List<Account> removeACCs = getAccountbyIDs(ids);
        for (Account acc : removeACCs) {
            AccountController.getInstance().removeAccount(acc);
        }
        return true;
    }

    private java.util.List<Account> getAccountbyIDs(Long IDs[]) {
        java.util.List<Long> todoIDs = new ArrayList<Long>(Arrays.asList(IDs));
        java.util.List<Account> accs = new ArrayList<Account>();
        for (Account lacc : AccountController.getInstance().list()) {
            if (lacc != null && todoIDs.size() > 0) {
                Iterator<Long> it = todoIDs.iterator();
                while (it.hasNext()) {
                    long id = it.next();
                    if (lacc.getID().getID() == id) {
                        accs.add(lacc);
                        it.remove();
                    }
                }
            } else if (todoIDs.size() == 0) {
                break;
            }
        }
        return accs;
    }

    @Override
    public boolean enableAccounts(final List<Long> linkIds) {
        return setEnabledState(true, linkIds.toArray(new Long[linkIds.size()]));
    }

    @Override
    public boolean disableAccounts(final List<Long> linkIds) {
        return setEnabledState(false, linkIds.toArray(new Long[linkIds.size()]));
    }

    public boolean setEnabledState(boolean enabled, Long[] ids) {
        java.util.List<Account> accs = getAccountbyIDs(ids);
        for (Account acc : accs) {
            acc.setEnabled(enabled);
        }
        return true;
    }

    public AccountAPIStorable getAccountInfo(long id) {
        java.util.List<Account> accs = getAccountbyIDs(new Long[] { id });
        if (accs.size() == 1) { return new AccountAPIStorable(accs.get(0)); }
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
    public boolean updateAccount(Long accountId, String username, String password) {
        if (accountId == null) return false;

        for (Account acc : AccountController.getInstance().list()) {
            if (accountId.equals(acc.getID().getID())) {
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
