package org.jdownloader.api.accounts;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;

import jd.controlling.AccountController;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.AccountTrafficView;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.remoteapi.APIQuery;
import org.appwork.remoteapi.RemoteAPI;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.exceptions.InternalApiException;
import org.appwork.utils.StringUtils;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.net.HTTPHeader;
import org.jdownloader.DomainInfo;
import org.jdownloader.myjdownloader.client.json.JsonMap;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.plugins.controller.host.PluginFinder;

@Deprecated
public class AccountAPIImpl implements AccountAPI {
    @Deprecated
    public List<AccountAPIStorable> queryAccounts(APIQuery queryParams) {
        List<Account> accs = AccountController.getInstance().list();
        java.util.List<AccountAPIStorable> ret = new ArrayList<AccountAPIStorable>();
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
            AccountAPIStorable accas = new AccountAPIStorable(acc);
            JsonMap infoMap = new JsonMap();
            if (queryParams.fieldRequested("username")) {
                infoMap.put("username", acc.getUser());
            }
            if (queryParams.fieldRequested("validUntil")) {
                AccountInfo ai = acc.getAccountInfo();
                if (ai != null) {
                    infoMap.put("validUntil", ai.getValidUntil());
                }
            }
            AccountTrafficView accountTrafficView = null;
            if (queryParams.fieldRequested("trafficLeft")) {
                accountTrafficView = acc.getAccountTrafficView();
                if (accountTrafficView != null) {
                    infoMap.put("trafficLeft", accountTrafficView.getTrafficLeft());
                }
            }
            if (queryParams.fieldRequested("trafficMax")) {
                if (accountTrafficView == null) {
                    accountTrafficView = acc.getAccountTrafficView();
                }
                if (accountTrafficView != null) {
                    infoMap.put("trafficMax", accountTrafficView.getTrafficMax());
                }
            }
            if (queryParams.fieldRequested("enabled")) {
                infoMap.put("enabled", acc.isEnabled());
            }
            if (queryParams.fieldRequested("valid")) {
                infoMap.put("valid", acc.isValid());
            }
            accas.setInfoMap(infoMap);
            ret.add(accas);
        }
        return ret;
    }

    @Deprecated
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

    @Deprecated
    @Override
    public JsonMap listPremiumHosterUrls() {
        final Collection<LazyHostPlugin> allPLugins = HostPluginController.getInstance().list();
        // Filter - only premium plugins should be here
        final JsonMap ret = new JsonMap();
        for (final LazyHostPlugin lhp : allPLugins) {
            if (lhp.isPremium()) {
                ret.put(lhp.getDisplayName(), AccountController.createFullBuyPremiumUrl(lhp.getPremiumUrl(), "accountmanager/webinterface"));
            }
        }
        return ret;
    }

    @Deprecated
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

    @Deprecated
    @Override
    public void premiumHosterIcon(RemoteAPIRequest request, RemoteAPIResponse response, String premiumHoster) throws InternalApiException {
        OutputStream out = null;
        try {
            /* we force content type to image/png and allow caching of the image */
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CACHE_CONTROL, "public,max-age=60", false));
            response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, "image/png", false));
            out = RemoteAPI.getOutputStream(response, request, RemoteAPI.gzip(request), false);
            LazyHostPlugin plugin = HostPluginController.getInstance().get(premiumHoster);
            if (plugin != null) {
                ImageIO.write(IconIO.toBufferedImage(DomainInfo.getInstance(plugin.getHost()).getFavIcon()), "png", out);
            }
        } catch (IOException e) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
            throw new InternalApiException(e);
        } finally {
            try {
                out.close();
            } catch (final Throwable e) {
            }
        }
    }

    @Deprecated
    public boolean removeAccounts(Long[] ids) {
        final List<Account> removeACCs = getAccountbyIDs(ids);
        for (final Account acc : removeACCs) {
            AccountController.getInstance().removeAccount(acc);
        }
        return true;
    }

    @Deprecated
    private java.util.List<Account> getAccountbyIDs(Long IDs[]) {
        final List<Long> todoIDs = new ArrayList<Long>(Arrays.asList(IDs));
        final List<Account> accs = new ArrayList<Account>();
        for (final Account lacc : AccountController.getInstance().list()) {
            if (lacc != null && todoIDs.size() > 0) {
                final Iterator<Long> it = todoIDs.iterator();
                while (it.hasNext()) {
                    final long id = it.next();
                    if (lacc.getId().getID() == id) {
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

    @Deprecated
    @Override
    public boolean enableAccounts(final List<Long> linkIds) {
        return setEnabledState(true, linkIds.toArray(new Long[linkIds.size()]));
    }

    @Deprecated
    @Override
    public boolean disableAccounts(final List<Long> linkIds) {
        return setEnabledState(false, linkIds.toArray(new Long[linkIds.size()]));
    }

    @Deprecated
    public boolean setEnabledState(boolean enabled, Long[] ids) {
        final List<Account> accs = getAccountbyIDs(ids);
        for (final Account acc : accs) {
            acc.setEnabled(enabled);
        }
        return true;
    }

    @Deprecated
    public AccountAPIStorable getAccountInfo(long id) {
        final List<Account> accs = getAccountbyIDs(new Long[] { id });
        if (accs.size() == 1) {
            return new AccountAPIStorable(accs.get(0));
        }
        return null;
    }

    @Deprecated
    @Override
    public boolean addAccount(String host, String username, String password) {
        final PluginFinder pluginFinder = new PluginFinder();
        final String pluginHost = new PluginFinder().assignHost(host);
        if (pluginHost != null) {
            final Account acc = new Account(username, password);
            acc.setHoster(pluginHost);
            pluginFinder.assignPlugin(acc, true);
            AccountController.getInstance().addAccount(acc);
            return true;
        }
        return false;
    }

    @Deprecated
    @Override
    public boolean updateAccount(Long accountId, String username, String password) {
        if (accountId == null) {
            return false;
        }
        for (final Account acc : AccountController.getInstance().list()) {
            if (accountId.equals(acc.getId().getID())) {
                if (StringUtils.isNotEmpty(username)) {
                    acc.setUser(username);
                }
                if (StringUtils.isNotEmpty(password)) {
                    acc.setPass(password);
                }
                return true;
            }
        }
        return true;
    }
}
