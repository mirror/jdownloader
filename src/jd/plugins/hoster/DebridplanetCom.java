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

import org.appwork.storage.JSonMapperException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Exceptions;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.JDHash;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "debridplanet.com" }, urls = { "" })
public class DebridplanetCom extends PluginForHost {
    private static final String          API_BASE               = "https://debridplanet.com/v1";
    private static MultiHosterManagement mhm                    = new MultiHosterManagement("debridplanet.com");
    private static final boolean         resume                 = true;
    private static final int             maxchunks              = -10;
    private static final String          PROPERTY_ACCOUNT_TOKEN = "LOGIN_TOKEN";

    @SuppressWarnings("deprecation")
    public DebridplanetCom(PluginWrapper wrapper) {
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
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account == null) {
            return false;
        } else {
            mhm.runCheck(account, link);
            return true;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST };
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
            this.checkErrors(account, link);
            final List<Object> ressourcelist = JSonStorage.restoreFromString(br.getRequest().getHtmlCode(), TypeRef.LIST);
            /* 2021-03-24: Sometimes they just return "[]" -> wtf */
            if (ressourcelist.size() == 0) {
                mhm.handleErrorGeneric(account, this.getDownloadLink(), "API returned empty array", 50, 5 * 60 * 1000l);
            }
            Map<String, Object> entries = (Map<String, Object>) ressourcelist.get(0);
            handleErrorMap(account, link, entries);
            entries = (Map<String, Object>) entries.get("data");
            final String dllink = (String) (entries != null ? entries.get("link") : null);
            if (StringUtils.isEmpty(dllink)) {
                mhm.handleErrorGeneric(account, link, "dllinknull", 50, 5 * 60 * 1000l);
            }
            link.setProperty(this.getHost() + "directlink", dllink);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                mhm.handleErrorGeneric(account, link, "unknown_dl_error", 10, 5 * 60 * 1000l);
            }
        }
        this.dl.startDownload();
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link) throws Exception {
        final String directurlproperty = this.getHost() + "directlink";
        final String url = link.getStringProperty(directurlproperty);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        boolean valid = false;
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, resume, maxchunks);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                valid = true;
                return true;
            } else {
                link.removeProperty(directurlproperty);
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            logger.log(e);
            return false;
        } finally {
            if (!valid) {
                try {
                    dl.getConnection().disconnect();
                } catch (Throwable ignore) {
                }
                dl = null;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        Map<String, Object> entries = JSonStorage.restoreFromString(br.getRequest().getHtmlCode(), TypeRef.HASHMAP);
        entries = (Map<String, Object>) entries.get("user");
        final String accountType = (String) entries.get("account_type");
        if (accountType.equalsIgnoreCase("free")) {
            account.setType(AccountType.FREE);
            ai.setExpired(true);
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
        entries = JSonStorage.restoreFromString(br.getRequest().getHtmlCode(), TypeRef.HASHMAP);
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        final List<Object> supportedHostsO = (List<Object>) entries.get("supportedhosts");
        for (final Object supportedHostO : supportedHostsO) {
            entries = (Map<String, Object>) supportedHostO;
            final String domain = entries.get("host").toString();
            if (!((Boolean) entries.get("currently_working")).booleanValue()) {
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
                            checkErrors(account, null);
                            logger.info("Token login successful");
                            return;
                        } catch (final PluginException e) {
                            logger.exception("Token login failed", e);
                        }
                    }
                }
                logger.info("Performing full login");
                final Map<String, Object> postdata = new HashMap<String, Object>();
                postdata.put("username", account.getUser());
                postdata.put("password", JDHash.getSHA256(account.getPass()));
                br.postPageRaw(API_BASE + "/login.php", JSonStorage.serializeToJson(postdata));
                final Map<String, Object> entries = JSonStorage.restoreFromString(br.getRequest().getHtmlCode(), TypeRef.HASHMAP);
                final String token = (String) entries.get("token");
                if (StringUtils.isEmpty(token)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                br.getHeaders().put("Authorization", "Bearer " + token);
                account.setProperty(PROPERTY_ACCOUNT_TOKEN, token);
            } catch (PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.removeProperty(PROPERTY_ACCOUNT_TOKEN);
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private Object checkErrors(final Account account, final DownloadLink link) throws PluginException, InterruptedException {
        Object jsonO = null;
        try {
            jsonO = JSonStorage.restoreFromString(br.getRequest().getHtmlCode(), TypeRef.OBJECT);
            if (jsonO == null || !(jsonO instanceof Map)) {
                return jsonO;
            }
            handleErrorMap(account, link, (Map<String, Object>) jsonO);
        } catch (final JSonMapperException jme) {
            if (this.getDownloadLink() != null) {
                mhm.handleErrorGeneric(account, this.getDownloadLink(), "Bad API answer", 50, 5 * 60 * 1000l);
            } else {
                throw Exceptions.addSuppressed(new AccountUnavailableException("Bad API answer", 1 * 60 * 1000l), jme);
            }
        }
        return jsonO;
    }

    private void handleErrorMap(final Account account, final DownloadLink link, final Map<String, Object> entries) throws PluginException, InterruptedException {
        final int success = ((Number) entries.get("success")).intValue();
        if (success == 1) {
            /* No error */
            return;
        }
        /* TODO: Add support for more error-cases */
        /*
         * TODO: E.g. {"success":0,"status":400,"message":"Error: You must be premium"} --> 2021-03-24: Seems like they've removed that
         */
        /*
         * 2022-03-05: Response doesn't always contain "status" field e.g.
         * [{"success":0,"message":"Error 11a2 : Can't download file, please check your url again."}]
         */
        final Number statusO = ((Number) entries.get("status"));
        final String message = entries.get("message").toString();
        if (statusO != null) {
            final int status = statusO.intValue();
            if (status == 401) {
                /* Wrong auth token -> Session expired? */
                throw new AccountUnavailableException(message, 5 * 60 * 1000l);
            } else if (status == 422) {
                /* E.g. {"success":0,"status":422,"message":"Invalid Credential!"} */
                throw new AccountInvalidException();
            } else {
                /* Unknown error */
                if (link != null) {
                    mhm.handleErrorGeneric(account, link, message, 50, 5 * 60 * 1000l);
                } else {
                    throw new AccountUnavailableException("Unknown error happened: " + message, 5 * 60 * 1000l);
                }
            }
        } else {
            if (this.getDownloadLink() != null) {
                mhm.handleErrorGeneric(account, link, message, 50, 1 * 60 * 1000l);
            } else {
                throw new AccountUnavailableException("Unknown error happened: " + message, 5 * 60 * 1000l);
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