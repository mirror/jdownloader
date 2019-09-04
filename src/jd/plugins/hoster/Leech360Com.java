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

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
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

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "leech360.com" }, urls = { "" })
public class Leech360Com extends PluginForHost {
    /* Connection limits */
    private static final boolean         ACCOUNT_PREMIUM_RESUME       = true;
    private static final int             ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final String          PROPERTY_API_TOKEN           = "api_login_token";
    private static final String          PROPERTY_API_TOKEN_VALID     = "api_login_token_valid_until";
    private static final String          PROPERTY_LOGIN_METHOD        = "login_method";
    private static final boolean         USE_API                      = true;
    private static final boolean         api_supports_free_accounts   = false;
    private static final boolean         allow_free_account_downloads = false;
    private String                       currAPIToken                 = null;
    private static MultiHosterManagement mhm                          = new MultiHosterManagement("leech360.com");

    public Leech360Com(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://leech360.com/payment.html");
    }

    @Override
    public String getAGBLink() {
        return "https://leech360.com/terms-of-service.html";
    }

    private Browser prepBR(final Browser br) {
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        }
        return true;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        /* handle premium should never be called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        this.br = prepBR(this.br);
        mhm.runCheck(account, link);
        final String method = login(account, false);
        String dllink = getDllink(account, link, method);
        if (StringUtils.isEmpty(dllink)) {
            /* E.g. (website) "error_message":"some.filehost current offline or not support this time!" */
            mhm.handleErrorGeneric(account, link, "dllinknull", 50, 3 * 60 * 1000l);
        }
        handleDL(account, link, dllink);
    }

    private String getDllink(final Account account, final DownloadLink link, final String method) throws IOException, PluginException, InterruptedException {
        String dllink = checkDirectLink(link, this.getHost() + "directlink");
        if (dllink == null) {
            if (StringUtils.equalsIgnoreCase("website", method)) {
                dllink = getDllinkWebsite(account, link);
            } else if (StringUtils.equalsIgnoreCase("api", method)) {
                dllink = getDllinkAPI(account, link);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        return dllink;
    }

    private String getDllinkAPI(final Account account, final DownloadLink link) throws IOException, PluginException, InterruptedException {
        this.getAPISafe("https://" + this.getHost() + "/generate?token=" + Encoding.urlEncode(this.currAPIToken) + "&link=" + Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this)), account, link);
        String dllink = PluginJSonUtils.getJsonValue(this.br, "download_url");
        return dllink;
    }

    private String getDllinkWebsite(final Account account, final DownloadLink link) throws IOException, PluginException, InterruptedException {
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        this.postAPISafe("https://" + this.getHost() + "/generate", "link_password=&link=" + Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this)), account, link);
        String dllink = PluginJSonUtils.getJsonValue(this.br, "download_url");
        return dllink;
    }

    private void handleDL(final Account account, final DownloadLink link, final String dllink) throws Exception {
        link.setProperty(this.getHost() + "directlink", dllink);
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            if (dl.getConnection().getContentType().contains("text") || !dl.getConnection().isOK() || dl.getConnection().getLongContentLength() == -1) {
                br.followConnection();
                handleAPIErrors(this.br, account, link);
                mhm.handleErrorGeneric(account, link, "unknowndlerror", 50, 3 * 60 * 1000l);
            }
            this.dl.startDownload();
        } catch (final Exception e) {
            link.setProperty(this.getHost() + "directlink", Property.NULL);
            throw e;
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("text") || !con.isOK() || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                logger.log(e);
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return dllink;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        this.br = prepBR(this.br);
        final AccountInfo ai;
        if (USE_API) {
            ai = fetchAccountInfoAPI(account);
        } else {
            ai = fetchAccountInfoWebsite(account);
        }
        return ai;
    }

    public AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        /*
         * 2017-11-29: Lifetime premium not (yet) supported via website mode! But by the time we might need the website version again, they
         * might have stopped premium lifetime sales already as that has never been a good idea for any (M)OCH.
         */
        final AccountInfo ai = new AccountInfo();
        loginWebsite(account, true);
        /*
         * Use 2nd browser object as we already got the page containing our supported hosts inside the login process and we do not want to
         * access it twice.
         */
        final Browser br2 = br.cloneBrowser();
        br2.getPage("/my-account.html");
        final String expire = br2.getRegex("Premium until: <strong class=\\'[^\2\\']+\\'>([^<>\"]+ (AM|PM))<").getMatch(0);
        if (expire != null) {
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium Account");
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "MMM dd yyyy hh:mm a", Locale.US), br);
        } else {
            account.setType(AccountType.FREE);
            ai.setStatus("Registered (free) account");
        }
        final Regex trafficRegex = br.getRegex("id=\"total_traffic_used\">([^<>]+)</span> / ([^<>]+)<");
        final String trafficUsedStr = trafficRegex.getMatch(0);
        final String trafficMaxStr = trafficRegex.getMatch(1);
        if (trafficUsedStr != null && trafficMaxStr != null) {
            final long trafficUsed = SizeFormatter.getSize(trafficUsedStr);
            final long trafficMax = SizeFormatter.getSize(trafficMaxStr);
            final long trafficLeft = trafficMax - trafficUsed;
            ai.setTrafficLeft(trafficLeft);
            ai.setTrafficMax(trafficMax);
        } else if (account.getType() == AccountType.PREMIUM) {
            /* Fallback */
            ai.setUnlimitedTraffic();
        } else {
            /* Set traffic for ZERO for free accounts if we failed to find it */
            logger.info("Failed to find traffic_left --> Setting free account traffic to ZERO");
            ai.setTrafficLeft(0);
        }
        if (account.getType() == AccountType.FREE && !allow_free_account_downloads) {
            logger.info("Free account downloads are impossible --> Setting free account traffic to ZERO");
            ai.setStatus(ai.getStatus() + " [Downloads only possibla via browser]");
            ai.setTrafficLeft(0);
        }
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        final boolean userHasPremium = userOwnsPremiumAccount(account);
        final String supportedHostsTable = this.br.getRegex("<tbody>(.*?)</tbody>").getMatch(0);
        final String tableLines[] = supportedHostsTable.split("<tr>");
        for (final String lineHTML : tableLines) {
            final String domain = new Regex(lineHTML, "class=\"bold\\-text\">([^<>\"]+)</td>").getMatch(0);
            if (domain == null) {
                continue;
            }
            final boolean individualLimitReached;
            final Regex individualLimitRegex = new Regex(lineHTML, ">([^<>\"]*?)</span> / (?:<span>)?([^<>\"]*?)<");
            final String individualTrafficUsedStr = individualLimitRegex.getMatch(0);
            final String individualTrafficLimitStr = individualLimitRegex.getMatch(1);
            if (individualTrafficUsedStr == null || individualTrafficLimitStr == null) {
                /* Do not fail because of this missing data. */
                individualLimitReached = false;
            } else if (individualTrafficLimitStr.contains("infin")) {
                individualLimitReached = false;
            } else {
                final long individualTrafficUsed = SizeFormatter.getSize(individualTrafficUsedStr);
                final long individualTrafficLimit = SizeFormatter.getSize(individualTrafficLimitStr);
                if (individualTrafficUsed >= individualTrafficLimit) {
                    individualLimitReached = true;
                } else {
                    individualLimitReached = false;
                }
            }
            final boolean hosterIsActive = lineHTML.contains("class='green-text'>ON<");
            final boolean hosterIsPremiumOnly = lineHTML.contains("class='orange-text'>PREMIUM<");
            if (!hosterIsActive) {
                logger.info("Skipping host (inactive): " + domain);
            } else if (hosterIsPremiumOnly && !userHasPremium) {
                logger.info("Skipping host (premiumonly): " + domain);
            } else if (individualLimitReached) {
                logger.info("Skipping host (limitReached): " + domain);
            } else {
                supportedHosts.add(domain);
            }
        }
        ai.setMultiHostSupport(this, supportedHosts);
        return ai;
    }

    @SuppressWarnings("unchecked")
    public AccountInfo fetchAccountInfoAPI(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        final String method = login(account, false);
        if (StringUtils.equalsIgnoreCase("website", method)) {
            logger.info("User added FREE account which is not supported via API");
            return fetchAccountInfoWebsite(account);
        }
        this.getAPISafe("https://" + account.getHoster() + "/api/get_userinfo?token=" + Encoding.urlEncode(this.currAPIToken), account, null);
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        entries = (LinkedHashMap<String, Object>) entries.get("data");
        /*
         * For lifetime, this will return 'lifetime', for premium this will contain expire date in this form: 'Dec 16 2017 08:48 AM' (yes,
         * this is confusing!)
         */
        final String accountType = (String) entries.get("status");
        final String date_registered = (String) entries.get("joindate");
        if (!StringUtils.isEmpty(date_registered)) {
            ai.setCreateTime(TimeFormatter.getMilliSeconds(date_registered, "MMM dd yyyy hh:mm a", Locale.US));
        }
        final long premium_expire = JavaScriptEngineFactory.toLong(entries.get("premium_expire"), 0);
        final long traffic_used = JavaScriptEngineFactory.toLong(entries.get("total_used"), 0);
        /* TODO: 2017-11-16 (daily) limit hardcoded value. Ask admin to put this in the API response. */
        final long traffic_max = 536870912000l;
        if (accountType.equalsIgnoreCase("lifetime")) {
            /* Lifetime of course has not expire date */
            account.setType(AccountType.LIFETIME);
        } else if (premium_expire > 0) {
            // accountType == 'premium'
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium Account");
            ai.setValidUntil(premium_expire * 1000, br);
        } else {
            /* The only possibility left is free */
            account.setType(AccountType.FREE);
            ai.setStatus("Registered (free) account");
        }
        ai.setTrafficLeft(traffic_max - traffic_used);
        ai.setTrafficMax(traffic_max);
        this.getAPISafe("/api/get_support?token=" + Encoding.urlEncode(this.currAPIToken), account, null);
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        entries = (LinkedHashMap<String, Object>) entries.get("data");
        final Iterator<Entry<String, Object>> it = entries.entrySet().iterator();
        LinkedHashMap<String, Object> host_info;
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        final boolean userHasPremium = userOwnsPremiumAccount(account);
        /* TODO: Ask admin to move traffic info for indivdual hosts from account info to this API call. */
        while (it.hasNext()) {
            final Entry<String, Object> entry = it.next();
            host_info = (LinkedHashMap<String, Object>) entry.getValue();
            final String domain = (String) host_info.get("hostname");
            if (StringUtils.isEmpty(domain)) {
                continue;
            }
            final String status = (String) host_info.get("status");
            final boolean premiumOnlyHost = "vip".equals(status);
            final boolean hostAvailable = "online".equals(status) || premiumOnlyHost;
            if (!hostAvailable) {
                logger.info("Skipping host (inactive): " + domain);
            } else if (premiumOnlyHost && !userHasPremium) {
                logger.info("Skipping host (premiumonly): " + domain);
            } else {
                supportedHosts.add(domain);
            }
        }
        ai.setMultiHostSupport(this, supportedHosts);
        return ai;
    }

    private boolean userOwnsPremiumAccount(final Account account) {
        final AccountType acctype = account.getType();
        return acctype.equals(AccountType.PREMIUM) || acctype.equals(AccountType.LIFETIME);
    }

    private String login(final Account account, final boolean force) throws Exception {
        /* Load cookies */
        br.setCookiesExclusive(true);
        this.br = prepBR(this.br);
        String method = null;
        if (USE_API) {
            method = loginAPI(account, force);
        }
        if (method == null) {
            method = loginWebsite(account, force);
        }
        return method;
    }

    private String loginWebsite(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                final Cookies cookies = account.loadCookies("");
                boolean isLoggedin = false;
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    /*
                     * Even though login is forced first check if our cookies are still valid --> If not, force login!
                     */
                    br.getPage("https://" + this.getHost());
                    isLoggedin = this.isLoggedIN();
                }
                if (!isLoggedin) {
                    logger.info("Performing full website-login");
                    br.getPage("https://" + this.getHost() + "/sign-in.html");
                    String postData = "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass());
                    this.postAPISafe("https://" + this.getHost() + "/sign-in.html", postData, account, null);
                    if (!this.isLoggedIN()) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.setProperty(PROPERTY_LOGIN_METHOD, "website");
                account.saveCookies(this.br.getCookies(this.getHost()), "");
                return "website";
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.removeProperty(PROPERTY_LOGIN_METHOD);
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedIN() {
        return br.containsHTML("id=\"linkpass\"");
    }

    private String loginAPI(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                this.currAPIToken = account.getStringProperty(PROPERTY_API_TOKEN);
                final long token_valid_until = account.getLongProperty(PROPERTY_API_TOKEN_VALID, 0);
                if (this.currAPIToken != null && token_valid_until > System.currentTimeMillis()) {
                    account.setProperty(PROPERTY_LOGIN_METHOD, "api");
                    /* Token should still be valid, let's blindly trust it! */
                    return "api";
                }
                /* Full login (generate new token) */
                this.getAPISafe("https://" + account.getHoster() + "/api/get_token?user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()), account, null);
                this.currAPIToken = PluginJSonUtils.getJson(this.br, "token");
                if (isFreeAccountsForbiddenViaAPI()) {
                    account.removeProperty(PROPERTY_LOGIN_METHOD);
                    account.removeProperty(PROPERTY_API_TOKEN);
                    /* Special state */
                    return null;
                } else if (StringUtils.isEmpty(this.currAPIToken)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.setProperty(PROPERTY_LOGIN_METHOD, "api");
                account.setProperty(PROPERTY_API_TOKEN, this.currAPIToken);
                /* 2017-11-16: According to API docmentation, these tokens are valid for 10 minutes so let's trust them for 8 minutes. */
                /*
                 * TODO: Ask admin to return timestamp here. This way, he can adjust the validity of the token without us needing to change
                 * our code.
                 */
                account.setProperty(PROPERTY_API_TOKEN_VALID, System.currentTimeMillis() + 8 * 60 * 1000);
                account.saveCookies(this.br.getCookies(this.getHost()), "");
                return "api";
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                    account.removeProperty(PROPERTY_LOGIN_METHOD);
                    account.removeProperty(PROPERTY_API_TOKEN);
                    account.removeProperty(PROPERTY_API_TOKEN_VALID);
                }
                throw e;
            }
        }
    }

    private boolean isFreeAccountsForbiddenViaAPI() {
        return br.containsHTML("The caller does not have permission, API only support for PREMIUM\\.");
    }

    private void getAPISafe(final String accesslink, final Account account, final DownloadLink link) throws IOException, PluginException, InterruptedException {
        br.setAllowedResponseCodes(new int[] { 400 });
        br.getPage(accesslink);
        handleAPIErrors(this.br, account, link);
    }

    private void postAPISafe(final String accesslink, final String postdata, final Account account, final DownloadLink link) throws IOException, PluginException, InterruptedException {
        br.postPage(accesslink, postdata);
        handleAPIErrors(this.br, account, link);
    }

    /**
     * Keep this for possible future API implementation
     *
     * @throws InterruptedException
     */
    private void handleAPIErrors(final Browser br, final Account account, final DownloadLink link) throws PluginException, InterruptedException {
        final String errorBooleanStr = PluginJSonUtils.getJson(br, "error");
        final String errormsg = PluginJSonUtils.getJson(br, "error_message");
        if ("true".equalsIgnoreCase(errorBooleanStr) && !StringUtils.isEmpty(errormsg)) {
            if ("Invalid token".equalsIgnoreCase(errormsg)) {
                /* TODO_ check */
                /* Reset token and retry via full login. */
                link.setProperty(PROPERTY_API_TOKEN, Property.NULL);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "API token invalid", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            } else if (errormsg.equalsIgnoreCase("Invalid Username or Password.")) {
                /* Invalid logindata */
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else if (errormsg.matches("Sorry our .+ has reach bandwidth limit, please try again after few hours")) {
                /* 2019-07-23: e.g. "Sorry our filer.net has reach bandwidth limit, please try again after few hours" */
                mhm.putError(account, link, 10 * 60 * 1000l, "Bandwidth limit reached");
            } else if (isFreeAccountsForbiddenViaAPI()) {
                /* Ignore this one as it is handled elsewhere */
            } else {
                /* E.g. "error_message":"Could not connect to download server, please try again!" */
                mhm.handleErrorGeneric(account, link, "unknown_error_state", 50, 3 * 60 * 1000l);
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