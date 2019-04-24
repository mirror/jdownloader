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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map.Entry;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "multiup.org" }, urls = { "" })
public class MultiupOrg extends PluginForHost {
    private static final String          API_BASE            = "http://multiup.org/api";
    private static MultiHosterManagement mhm                 = new MultiHosterManagement("multiup.org");
    /** 2019-04-24: According to support: max con per file:3, max per account: 15 */
    private static final int             defaultMAXDOWNLOADS = 5;
    private static final int             defaultMAXCHUNKS    = -3;
    private static final boolean         defaultRESUME       = true;

    @SuppressWarnings("deprecation")
    public MultiupOrg(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://multiup.org/en/premium");
    }

    @Override
    public String getAGBLink() {
        return "https://multiup.org/en/terms-and-conditions";
    }

    private Browser newBrowser() {
        br = new Browser();
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(final DownloadLink downloadLink, final Account account) throws Exception {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        }
        return true;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        /* handle premium should never be called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private void handleDL(final Account account, final DownloadLink link) throws Exception {
        String dllink = checkDirectLink(link, this.getHost() + "directlink");
        br.setFollowRedirects(true);
        if (dllink == null) {
            final boolean use_website_workaround = false;
            if (use_website_workaround) {
                loginWebsite(account);
                br.setFollowRedirects(false);
                final String urlDoubleB64 = Encoding.Base64Encode(Encoding.Base64Encode(link.getPluginPatternMatcher()));
                dllink = "https://debrid.multiup.org/" + urlDoubleB64;
            } else {
                /** TODO: Find better way to login */
                fetchAccountInfo(account);
                final UrlQuery dlQuery = new UrlQuery();
                dlQuery.add("link", link.getPluginPatternMatcher());
                final String passCode = link.getDownloadPassword();
                if (passCode != null) {
                    dlQuery.add("password", passCode);
                }
                /**
                 * 2019-04-24: WTF this also works without login --> According to admin, once logged in once, current IP gets unlocked for
                 * premium download. Without this happening, used is a free-user and can only download one file at the time.
                 */
                br.postPage(API_BASE + "/generate-debrid-link", dlQuery);
                dllink = PluginJSonUtils.getJsonValue(br, "debrid_link");
            }
            if (StringUtils.isEmpty(dllink)) {
                mhm.handleErrorGeneric(account, link, "dllinknull", 10, 5 * 60 * 1000l);
            }
        }
        link.setProperty(this.getHost() + "directlink", dllink);
        /** 2019-04-24: Now returning 402 payment required ... */
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, defaultRESUME, defaultMAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            handleKnownErrors(this.br, account, link);
            mhm.handleErrorGeneric(account, link, "unknown_dl_error", 10, 5 * 60 * 1000l);
        }
        this.dl.startDownload();
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        this.br = newBrowser();
        mhm.runCheck(account, link);
        handleDL(account, link);
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        final long direct_url_end_time = downloadLink.getLongProperty("direct_url_end_time", 0) * 1000;
        String dllink = downloadLink.getStringProperty(property);
        if (direct_url_end_time > 0 && direct_url_end_time < System.currentTimeMillis()) {
            logger.info("Directlink expired");
            downloadLink.setProperty(property, Property.NULL);
            dllink = null;
        } else if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("html") || con.getResponseCode() == 404 || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        this.br = newBrowser();
        final AccountInfo ai = new AccountInfo();
        br.setFollowRedirects(true);
        final UrlQuery loginQuery = new UrlQuery();
        loginQuery.add("username", account.getUser());
        loginQuery.add("password", account.getPass());
        br.postPage(API_BASE + "/login", loginQuery);
        final String error = PluginJSonUtils.getJson(br, "error");
        if (!"success".equalsIgnoreCase(error)) {
            /* E.g. {"error":"bad username OR bad password"} */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        boolean is_premium = false;
        long validuntil = 0;
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
                validuntil = TimeFormatter.getMilliSeconds(validuntilStr, "dd/MM/yyyy HH:mm:ss", Locale.ENGLISH);
            }
            is_premium = validuntil > System.currentTimeMillis();
            if (!is_premium) {
                is_premium = br.containsHTML("class=\"role\">\\s*?Premium user");
            }
        } else {
            final String account_type = PluginJSonUtils.getJson(br, "account_type");
            final String premium_days_left = PluginJSonUtils.getJson(br, "premium_days_left");
            is_premium = account_type != null && account_type.equalsIgnoreCase("premium");
            if (premium_days_left != null && premium_days_left.matches("\\d+")) {
                validuntil = System.currentTimeMillis() + Integer.parseInt(premium_days_left) * 24 * 60 * 1000l;
            }
        }
        if (!is_premium) {
            account.setType(AccountType.FREE);
            ai.setStatus("Free account");
            account.setMaxSimultanDownloads(1);
            // ai.setTrafficLeft(0);
        } else {
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium account");
            account.setMaxSimultanDownloads(defaultMAXDOWNLOADS);
            /* Expiredate may not always be found! */
            if (validuntil > System.currentTimeMillis()) {
                ai.setValidUntil(validuntil, this.br);
            }
            ai.setUnlimitedTraffic();
        }
        /* Continue via API */
        this.getAPISafe(API_BASE + "/get-list-hosts-debrid", account, null);
        try {
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            final Object hostsO = entries.get("hosts");
            final ArrayList<String> supportedhostslist = new ArrayList<String>();
            if (hostsO instanceof LinkedHashMap) {
                entries = (LinkedHashMap<String, Object>) hostsO;
                final Iterator<Entry<String, Object>> iterator = entries.entrySet().iterator();
                while (iterator.hasNext()) {
                    final Entry<String, Object> entry = iterator.next();
                    final String host = entry.getKey();
                    supportedhostslist.add(host);
                }
            } else {
                final ArrayList<Object> hostlistO = (ArrayList<Object>) hostsO;
                for (final Object hostO : hostlistO) {
                    supportedhostslist.add((String) hostO);
                }
            }
            ai.setMultiHostSupport(this, supportedhostslist);
        } catch (final Throwable e) {
        }
        account.setConcurrentUsePossible(true);
        return ai;
    }

    private static Object acclock = new Object();

    /**
     * Only use this if the API fails or is buggy at some point. THIS IS ONLY A WORKAROUND!
     *
     * @throws PluginException
     */
    private void loginWebsite(final Account account) throws IOException, PluginException {
        synchronized (acclock) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    br.getPage("https://" + account.getHoster() + "/en/profile/my-profile");
                    /* 2019-04-24: Debug code */
                    // br.setCookie(br.getURL(), "REMEMBERME_MULTIDOMAIN", "true");
                    // br.setCookie(br.getURL(), "premium_available", "true");
                    if (isLoggedinHTML(this.br)) {
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
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
                    // throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    logger.info("Website login failed");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    private boolean isLoggedinHTML(final Browser br) {
        return br.containsHTML("/logout");
    }

    private void getAPISafe(final String accesslink, final Account account, final DownloadLink link) throws IOException, PluginException, InterruptedException {
        this.br.getPage(accesslink);
        handleKnownErrors(this.br, account, link);
    }

    private void handleKnownErrors(final Browser br, final Account account, final DownloadLink link) throws PluginException, InterruptedException {
        final int errorcode = getErrorcode(br);
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

    private int getErrorcode(final Browser br) {
        String status = PluginJSonUtils.getJson(br, "status");
        if (status != null) {
            /* Return errorcode */
            return Integer.parseInt(status);
        } else {
            /* Everything ok */
            return 0;
        }
    }

    private String getErrormessage(final Browser br) {
        return PluginJSonUtils.getJson(br, "details");
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}