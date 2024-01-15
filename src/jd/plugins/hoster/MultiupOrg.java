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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.storage.JSonMapperException;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "multiup.io" }, urls = { "" })
public class MultiupOrg extends PluginForHost {
    private static final String          API_BASE                   = "https://multiup.io/api";
    private static MultiHosterManagement mhm                        = new MultiHosterManagement("multiup.io");
    /** 2023-01-15: Max connections per IP in total according to admin: 3 */
    private static final int             defaultMAXDOWNLOADSPremium = 3;
    private static final int             defaultMAXCHUNKS           = 1;

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    @SuppressWarnings("deprecation")
    public MultiupOrg(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://" + this.getHost() + "/en/premium");
    }

    @Override
    public String getAGBLink() {
        return "https://" + this.getHost() + "/en/terms-and-conditions";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException {
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
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        /* This should never get called. */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        /* handle premium should never be called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private void handleDL(final Account account, final DownloadLink link) throws Exception {
        final String directlinkproperty = this.getHost() + "directlink";
        final String storedDirecturl = link.getStringProperty(directlinkproperty);
        String dllink;
        if (storedDirecturl != null) {
            logger.info("Using previously generated final downloadurl: " + storedDirecturl);
            dllink = storedDirecturl;
        } else {
            logger.info("Generating fresh directurl");
            final boolean use_website_workaround = false;
            final String url = link.getDefaultPlugin().buildExternalDownloadURL(link, this);
            if (use_website_workaround) {
                loginWebsite(account);
                final String urlDoubleB64 = Encoding.Base64Encode(Encoding.Base64Encode(url));
                dllink = "https://debrid." + this.getHost() + "/" + urlDoubleB64;
            } else {
                this.loginAPI(account);
                final UrlQuery dlQuery = new UrlQuery();
                dlQuery.add("link", Encoding.urlEncode(url));
                if (link.getDownloadPassword() != null) {
                    dlQuery.add("password", Encoding.urlEncode(link.getDownloadPassword()));
                }
                /**
                 * 2019-04-24: WTF this also works without login --> According to admin, once logged in once, current IP gets unlocked for
                 * premium download. Without this happening, used is a free-user and can only download one file at the time.
                 */
                br.postPage(API_BASE + "/generate-debrid-link", dlQuery);
                final Map<String, Object> entries = this.checkErrorsAPI(br, account, link);
                dllink = (String) entries.get("debrid_link");
                if (StringUtils.isEmpty(dllink)) {
                    /* This should never happen. */
                    mhm.handleErrorGeneric(account, link, "Failed to find final downloadurl", 50, 5 * 60 * 1000l);
                }
            }
        }
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, account), defaultMAXCHUNKS);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                /* 402 - Payment required */
                if (dl.getConnection().getResponseCode() == 402) {
                    /* 2019-05-03: E.g. free account[or expired premium], only 1 download per day (?) possible */
                    /*
                     * 2019-11-03: We cannot trust this error as it may even occur on premium account download attempts. Rather wait than
                     * temp. disabling account.
                     */
                    final boolean trust_402_message = false;
                    final String errortext = "Error 402 payment required";
                    if (trust_402_message || account.getType() == AccountType.FREE) {
                        throw new AccountUnavailableException(errortext, 5 * 60 * 1000l);
                    } else {
                        mhm.handleErrorGeneric(account, link, errortext, 10, 5 * 60 * 1000l);
                    }
                }
                br.followConnection(true);
                checkErrorsAPI(this.br, account, link);
                mhm.handleErrorGeneric(account, link, "unknown_dl_error", 10, 5 * 60 * 1000l);
            }
            link.setProperty(directlinkproperty, dl.getConnection().getURL().toExternalForm());
        } catch (final Exception e) {
            if (storedDirecturl != null) {
                link.removeProperty(directlinkproperty);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Stored directurl expired", e);
            } else {
                throw e;
            }
        }
        dl.startDownload();
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        mhm.runCheck(account, link);
        handleDL(account, link);
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        final Map<String, Object> usermap = loginAPI(account);
        boolean is_premium = false;
        long validuntiltimestamp = 0;
        String validuntilStr = null;
        /**
         * TODO: 2019-04-04: Admin has updated API so this workaround is not required anymore but we will keep this boolean as a switch just
         * in case ...
         */
        final boolean website_workaround_required = false;
        if (website_workaround_required) {
            loginWebsite(account);
            if (br.getURL() == null || !br.getURL().contains("/profile/my-profile")) {
                br.getPage("https://" + account.getHoster() + "/en/profile/my-profile");
            }
            validuntilStr = br.getRegex("<td>(\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2})</td>\\s*?<td>\\d+</td>\\s*?<td>\\+\\d+ days?</td").getMatch(0);
            if (validuntilStr != null) {
                validuntiltimestamp = TimeFormatter.getMilliSeconds(validuntilStr, "dd/MM/yyyy HH:mm:ss", Locale.ENGLISH);
            }
            is_premium = validuntiltimestamp > System.currentTimeMillis();
            if (!is_premium) {
                is_premium = br.containsHTML("class=\"role\">\\s*?Premium user");
            }
        } else {
            final String account_type = (String) usermap.get("account_type");
            final String premium_days_left = usermap.get("premium_days_left").toString();
            is_premium = account_type != null && account_type.equalsIgnoreCase("premium");
            if (premium_days_left != null && premium_days_left.matches("\\d+")) {
                validuntiltimestamp = System.currentTimeMillis() + Integer.parseInt(premium_days_left) * 24 * 60 * 60 * 1000l;
            }
        }
        if (is_premium) {
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(defaultMAXDOWNLOADSPremium);
            /* Expiredate may not always be found! */
            if (validuntiltimestamp > System.currentTimeMillis()) {
                ai.setValidUntil(validuntiltimestamp, this.br);
            }
            ai.setUnlimitedTraffic();
        } else {
            account.setType(AccountType.FREE);
            /*
             * 2019-05-07: Limit according to: https://multiup.io/en/premium - once this limit is reached, website will return
             * "402 payment required" on further download attempts.
             */
            ai.setStatus("Free account (max. 1 download per day)");
            account.setMaxSimultanDownloads(1);
            // ai.setTrafficLeft(0);
        }
        /* Find list of supported hosts */
        br.getPage(API_BASE + "/get-list-hosts-debrid");
        final Map<String, Object> resp = this.checkErrorsAPI(br, account, null);
        final Object hostsO = resp.get("hosts");
        final ArrayList<String> supportedhostslist = new ArrayList<String>();
        if (hostsO instanceof Map) {
            final Map<String, Object> hostermap = (Map<String, Object>) hostsO;
            final Iterator<Entry<String, Object>> iterator = hostermap.entrySet().iterator();
            while (iterator.hasNext()) {
                final Entry<String, Object> entry = iterator.next();
                final String host = entry.getKey();
                supportedhostslist.add(host);
            }
        } else {
            final List<String> hostlist = (List<String>) hostsO;
            supportedhostslist.addAll(hostlist);
        }
        ai.setMultiHostSupport(this, supportedhostslist);
        account.setConcurrentUsePossible(true);
        return ai;
    }

    private Map<String, Object> loginAPI(final Account account) throws IOException, PluginException, InterruptedException {
        final UrlQuery loginQuery = new UrlQuery();
        loginQuery.add("username", account.getUser());
        loginQuery.add("password", account.getPass());
        br.postPage(API_BASE + "/login", loginQuery);
        return this.checkErrorsAPI(br, account, null);
    }

    /**
     * Only use this if the API fails or is buggy at some point. THIS IS ONLY A WORKAROUND!
     *
     * @throws PluginException
     */
    private void loginWebsite(final Account account) throws IOException, PluginException {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    br.getPage("https://" + getHost() + "/en/profile/my-profile");
                    /* 2019-04-24: Debug code */
                    // br.setCookie(br.getURL(), "REMEMBERME_MULTIDOMAIN", "true");
                    // br.setCookie(br.getURL(), "premium_available", "true");
                    if (isLoggedinHTML(this.br)) {
                        account.saveCookies(this.br.getCookies(br.getHost()), "");
                        return;
                    }
                }
                /* Perform full login */
                br.getPage("https://" + account.getHoster() + "/en/login");
                final Form loginform = br.getFormbyProperty("id", "frmSignIn");
                if (loginform == null) {
                    logger.warning("Failed to find loginform");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("_username", Encoding.urlEncode(account.getUser()));
                loginform.put("_password", Encoding.urlEncode(account.getPass()));
                loginform.put("_remember_me", "on");
                br.submitForm(loginform);
                if (!isLoggedinHTML(this.br)) {
                    throw new AccountInvalidException();
                }
                account.saveCookies(this.br.getCookies(br.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    private boolean isLoggedinHTML(final Browser br) {
        return br.containsHTML("/logout");
    }

    private Map<String, Object> checkErrorsAPI(final Browser br, final Account account, final DownloadLink link) throws PluginException, InterruptedException {
        Map<String, Object> entries = null;
        try {
            entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        } catch (final JSonMapperException ignore) {
            /* This should never happen. */
            throw new AccountUnavailableException("Invalid API response", 60 * 1000);
        }
        final String statuscode = (String) entries.get("status");
        if (statuscode != null && statuscode.matches("\\d+")) {
            /* Old handling */
            final int errorcode = Integer.parseInt(statuscode);
            // final String errormsg = getErrormessage(this.br);
            switch (errorcode) {
            case 0:
                break;
            case 401:
                /* Login failed */
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 400:
                /* Bad request, this should never happen */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            case 404:
                mhm.handleErrorGeneric(account, link, "hoster_offline_or_unsupported", 10, 5 * 60 * 1000l);
            case 503:
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "503 - Service unavailable");
            default:
                /* Unknown issue */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        final String statustext = (String) entries.get("error");
        if (!"success".equalsIgnoreCase(statustext)) {
            if (link != null) {
                mhm.handleErrorGeneric(account, link, statustext, 10, 5 * 60 * 1000l);
            } else {
                throw new AccountInvalidException(statustext);
            }
        }
        return entries;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}