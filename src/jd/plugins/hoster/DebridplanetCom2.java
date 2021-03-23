//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.JDHash;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision$", interfaceVersion = 0, names = { "debridplanet.com" }, urls = { "" })
public class DebridplanetCom2 extends PluginForHost {
    private static final String          API_BASE               = "https://debridplanet.com/v1";
    private static MultiHosterManagement mhm                    = new MultiHosterManagement("debridplanet.com");
    private static final boolean         resume                 = true;
    private static final int             maxchunks              = -10;
    private static final String          PROPERTY_ACCOUNT_TOKEN = "LOGIN_TOKEN";

    @SuppressWarnings("deprecation")
    public DebridplanetCom2(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(API_BASE + "/premium");
    }

    @Override
    public String getAGBLink() {
        return API_BASE + "/tos";
    }

    private Browser prepBR(final Browser br) {
        br.setCookiesExclusive(true);
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        throw new AccountRequiredException();
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        mhm.runCheck(account, link);
        this.login(account, false);
        if (!attemptStoredDownloadurlDownload(link)) {
            final Map<String, Object> postdata = new HashMap<String, Object>();
            final ArrayList<String> urllist = new ArrayList<String>();
            urllist.add(link.getDefaultPlugin().buildExternalDownloadURL(link, this));
            postdata.put("listurl", urllist);
            br.postPageRaw(API_BASE + "/gen_link.php", JSonStorage.serializeToJson(postdata));
            /* TODO */
            this.checkErrors(account);
            final Object jsonO = JSonStorage.restoreFromString(br.toString(), TypeRef.OBJECT);
            if (jsonO instanceof Map) {
                /* Error happened e.g. {"success":0,"status":400,"message":"Error: You must be premium"} */
                final Map<String, Object> entries = (Map<String, Object>) jsonO;
                final String error = (String) entries.get("message");
                if (!StringUtils.isEmpty(error)) {
                    /* 2021-03-12: Handle all as generic errors for now */
                    mhm.handleErrorGeneric(account, link, error, 50);
                } else {
                    mhm.handleErrorGeneric(account, link, "Unknown error", 50);
                }
            }
            /* TODO: check how they're returning errors here. */
            final List<Object> ressourcelist = JSonStorage.restoreFromString(br.toString(), TypeRef.LIST);
            Map<String, Object> entries = (Map<String, Object>) ressourcelist.get(0);
            entries = (Map<String, Object>) entries.get("data");
            final String dllink = (String) entries.get("link");
            if (StringUtils.isEmpty(dllink)) {
                mhm.handleErrorGeneric(account, link, "dllinknull", 50, 5 * 60 * 1000l);
            }
            link.setProperty(this.getHost() + "directlink", dllink);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                mhm.handleErrorGeneric(account, link, "unknown_dl_error", 10, 5 * 60 * 1000l);
            }
        }
        this.dl.startDownload();
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link) throws Exception {
        final String url = link.getStringProperty(this.getHost() + "directlink");
        if (url == null) {
            return false;
        }
        try {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, this.getDownloadLink(), url, resume, maxchunks);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                throw new IOException();
            }
        } catch (final Throwable e) {
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            return false;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        entries = (Map<String, Object>) entries.get("user");
        final String accountType = (String) entries.get("account_type");
        ai.setStatus("Premium account");
        if (accountType.equalsIgnoreCase("free")) {
            account.setType(AccountType.FREE);
            /*
             * 2020-12-08: Free Accounts can download from some hosts (see websites' host list) but we just won't allow free account
             * downloads at all.
             */
            ai.setTrafficLeft(0);
        } else {
            /* There are also accounts without expire-date! */
            final Object expireDateO = entries.get("expire");
            if (expireDateO != null) {
                final String expireDate = (String) expireDateO;
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expireDate, "yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH), this.br);
            }
            account.setType(AccountType.PREMIUM);
            ai.setUnlimitedTraffic();
        }
        br.getPage(API_BASE + "/supportedhosts.php");
        entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        final List<Object> supportedHostsO = (List<Object>) entries.get("supportedhosts");
        for (final Object supportedHostO : supportedHostsO) {
            entries = (Map<String, Object>) supportedHostO;
            final String domain = (String) entries.get("host");
            final boolean currently_working = ((Boolean) entries.get("currently_working")).booleanValue();
            if (!currently_working) {
                logger.info("Skipping offline host: " + domain);
                continue;
            } else {
                supportedHosts.add(domain);
            }
        }
        ai.setMultiHostSupport(this, supportedHosts);
        account.setConcurrentUsePossible(true);
        return ai;
    }

    private void login(final Account account, final boolean validateLogins) throws IOException, PluginException, InterruptedException {
        synchronized (account) {
            try {
                prepBR(this.br);
                if (account.getStringProperty(PROPERTY_ACCOUNT_TOKEN) != null) {
                    logger.info("Trying to login via token");
                    br.getHeaders().put("Authorization", "Bearer " + account.getStringProperty(PROPERTY_ACCOUNT_TOKEN));
                    if (!validateLogins) {
                        logger.info("Trust token without check");
                        return;
                    } else {
                        logger.info("Validating login token...");
                        br.postPage(API_BASE + "/user-info.php", "");
                        try {
                            checkErrors(account);
                            logger.info("Token login successful");
                            return;
                        } catch (final PluginException e) {
                            logger.info("Token login failed");
                        }
                    }
                }
                logger.info("Performing full login");
                final Map<String, Object> postdata = new HashMap<String, Object>();
                postdata.put("username", account.getUser());
                postdata.put("password", JDHash.getSHA256(account.getPass()));
                br.postPageRaw(API_BASE + "/login.php", JSonStorage.serializeToJson(postdata));
                final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                final String token = (String) entries.get("token");
                if (StringUtils.isEmpty(token)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                br.getHeaders().put("Authorization", "Bearer " + token);
                account.setProperty(PROPERTY_ACCOUNT_TOKEN, token);
            } catch (PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private void checkErrors(final Account account) throws PluginException {
        final Object jsonO = JSonStorage.restoreFromString(br.toString(), TypeRef.OBJECT);
        if (jsonO == null || !(jsonO instanceof Map)) {
            return;
        }
        final Map<String, Object> entries = (Map<String, Object>) jsonO;
        final int success = ((Number) entries.get("success")).intValue();
        if (success != 1) {
            /* TODO: Add support for more error-cases */
            /* TODO: E.g. {"success":0,"status":400,"message":"Error: You must be premium"} */
            final int status = ((Number) entries.get("status")).intValue();
            if (status == 422) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new AccountUnavailableException("Unknown error happened: " + status, 5 * 60 * 1000l);
            }
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}