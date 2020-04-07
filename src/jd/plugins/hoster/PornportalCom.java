//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class PornportalCom extends PluginForHost {
    public PornportalCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://join.fakehub.com/signup/signup.php");
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://www.supportmg.com/terms-of-service";
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "babes.com" });
        ret.add(new String[] { "brazzers.com" });
        ret.add(new String[] { "digitalplayground.com" });
        ret.add(new String[] { "erito.com" });
        ret.add(new String[] { "fakehub.com" });
        ret.add(new String[] { "mofos.com" });
        ret.add(new String[] { "realitykings.com" });
        ret.add(new String[] { "sexyhub.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/dummy_not_yet_required");
        }
        return ret.toArray(new String[0]);
    }

    /*
     * Debug function: Can be used to quickly find the currently used pornportal version of all supported websites and compare against
     * previously set expected version value.
     */
    public static void checkUsedVersions(final Plugin plg) {
        final String target_version = "4.35.2";
        plg.getLogger().info("Target version: " + target_version);
        final Browser br = new Browser();
        final String[] supportedSites = getAnnotationNames();
        for (final String host : supportedSites) {
            try {
                br.getPage(getPornportalMainURL(host));
                final String usedVersion = PluginJSonUtils.getJson(br, "appVersion");
                plg.getLogger().info("***********************************");
                plg.getLogger().info("Site: " + host);
                if (StringUtils.isEmpty(usedVersion)) {
                    plg.getLogger().info("Used version: Unknown");
                } else {
                    plg.getLogger().info("Used version: " + usedVersion);
                    if (usedVersion.equals(target_version)) {
                        plg.getLogger().info("Expected version: OK");
                    } else {
                        plg.getLogger().info("Expected version: NOK");
                    }
                }
            } catch (final Throwable e) {
                plg.getLogger().info("!BROWSER ERROR!");
            }
        }
        plg.getLogger().info("***********************************");
    }

    public static String getPornportalMainURL(final String host) {
        if (host == null) {
            return null;
        }
        return "https://site-ma." + host;
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = false;
    private static final int     FREE_MAXCHUNKS               = 1;
    private static final int     FREE_MAXDOWNLOADS            = 1;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    private String               dllink                       = null;
    private boolean              server_issues                = false;

    public static Browser prepBR(final Browser br) {
        br.setAllowedResponseCodes(new int[] { 400 });
        return br;
    }

    // public void correctDownloadLink(final DownloadLink link) {
    // link.setUrlDownload(link.getDownloadURL().replaceAll("http://fakehubdecrypted", "http://"));
    // }
    /**
     * TODO: 2020-04-07: Their direct-URLs time out after some hours (only trailer download URLs are permanently valid) --> Add handling in
     * this host plugin to refresh expired directURLs
     */
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa == null) {
            link.getLinkStatus().setStatusText("Cannot check links without valid premium account");
            return AvailableStatus.UNCHECKABLE;
        }
        this.login(this.br, aa, false);
        dllink = link.getDownloadURL();
        URLConnectionAdapter con = null;
        try {
            con = br.openHeadConnection(dllink);
            if (!con.getContentType().contains("html")) {
                link.setDownloadSize(con.getLongContentLength());
                link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con)));
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private static final String PROPERTY_authorization                     = "authorization";
    private static final String PROPERTY_jwt                               = "jwt";
    private static final String PROPERTY_timestamp_website_cookies_updated = "timestamp_website_cookies_updated";
    public static final String  PROPERTY_cookiename_authCookie             = "auth_cookie";
    public static final String  PROPERTY_cookiename_instanceCookie         = "instanceCookie";
    public static final String  PROPERTY_plugin_jwt                        = "jwt";
    public static final String  PROPERTY_plugin_jwt_create_timestamp       = "jwt_create_timestamp";

    public void login(Browser br, final Account account, final boolean checkCookies) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                // checkUsedVersions(this);
                prepBR(br);
                Cookies cookies = account.loadCookies("");
                String jwt = null;
                if (cookies != null && setStoredAPIAuthHeaderAccount(br, account)) {
                    /*
                     * Try to avoid login captcha at all cost!
                     */
                    br.setCookies(account.getHoster(), cookies);
                    if (!checkCookies && System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 5 * 60 * 1000) {
                        logger.info("Trust cookies without check");
                        return;
                    }
                    br.getPage("https://site-api.project1service.com/v1/self");
                    if (br.getHttpConnection().getResponseCode() == 200) {
                        logger.info("Cookie login successful");
                        final long timestamp_headers_updated = account.getLongProperty(PROPERTY_timestamp_website_cookies_updated, 0);
                        /* Update website cookies sometimes although we really use the Website-API for most of all requests. */
                        if (System.currentTimeMillis() - timestamp_headers_updated >= 5 * 60 * 1000l) {
                            logger.info("Updating website cookies and JWT value");
                            /* Access mainpage without authorization headers but with cookies */
                            final Browser brc = prepBR(new Browser());
                            brc.setCookies(account.getHoster(), cookies);
                            brc.getPage(getPornportalMainURL(account.getHoster()));
                            /* TODO: This is very unsafe without using json parser! */
                            jwt = PluginJSonUtils.getJson(brc, "jwt");
                            if (jwt == null) {
                                logger.warning("Failed to find jwt --> Re-using old value");
                            } else {
                                account.setProperty(PROPERTY_jwt, jwt);
                                br.setCookie(getPornportalMainURL(account.getHoster()), account.getStringProperty(PROPERTY_cookiename_instanceCookie, getDefaultCookieNameInstance()), jwt);
                                account.setProperty(PROPERTY_timestamp_website_cookies_updated, System.currentTimeMillis());
                            }
                        }
                        account.saveCookies(br.getCookies(account.getHoster()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        cookies = null;
                        /* Important: Especially old Authorization headers can cause trouble! */
                        br = prepBR(new Browser());
                    }
                }
                logger.info("Performing full login");
                br.setFollowRedirects(true);
                br.getPage(getPornportalMainURL(account.getHoster()) + "/login");
                Map<String, Object> entries = getWebsiteJson(br);
                final String api_base = PluginJSonUtils.getJson(br, "dataApiUrl");
                String cookie_name_login = PluginJSonUtils.getJson(br, "authCookie");
                if (cookie_name_login == null) {
                    cookie_name_login = getDefaultCookieNameLogin();
                }
                if (StringUtils.isEmpty(api_base)) {
                    logger.warning("Failed to find api_base");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (!prepareBrAPI(this, br, account, entries)) {
                    logger.warning("Failed to prepare API headers");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                jwt = PluginJSonUtils.getJson(br, "jwt");
                entries = (LinkedHashMap<String, Object>) entries.get("domain");
                /* E.g. site-ma.fakehub.com */
                final String hostname = (String) entries.get("hostname");
                final String recaptchaSiteKey = (String) entries.get("siteKey");
                /* Prepare POST-data */
                Map<String, Object> logindata = new HashMap<String, Object>();
                final String successUrl = "https://" + hostname + "/access/success";
                final String failureUrl = "https://" + hostname + "/access/failure";
                logindata.put("username", account.getUser());
                logindata.put("password", account.getPass());
                logindata.put("failureUrl", successUrl);
                logindata.put("successUrl", failureUrl);
                final DownloadLink dlinkbefore = getDownloadLink();
                String recaptchaV2Response = null;
                /* 2020-04-03: So far, all pornportal websites required a captcha on login. */
                if (!StringUtils.isEmpty(recaptchaSiteKey)) {
                    try {
                        if (dlinkbefore == null) {
                            setDownloadLink(new DownloadLink(this, "Account", hostname, "https://" + hostname, true));
                        }
                        final CaptchaHelperHostPluginRecaptchaV2 captcha = new CaptchaHelperHostPluginRecaptchaV2(this, br, recaptchaSiteKey);
                        recaptchaV2Response = captcha.getToken();
                        logindata.put("googleReCaptchaResponse", recaptchaV2Response);
                    } finally {
                        if (dlinkbefore != null) {
                            setDownloadLink(dlinkbefore);
                        }
                    }
                }
                // final String postData = "{\"username\":\"" + account.getUser() + "\",\"password\":\"" + account.getPass() +
                // "\",\"googleReCaptchaResponse\":\"" + recaptchaV2Response + "\",\"successUrl\":\"" + successUrl +
                // "\",\"failureUrl\":\"" + failureUrl + "\"}";
                /* 2019-09-12: This action can be found in their html inside json: dataApiUrl */
                // br.postPageRaw(api_base + "/v1/authenticate/redirect", postData);
                final PostRequest postRequest = br.createPostRequest(api_base + "/v1/authenticate/redirect", JSonStorage.serializeToJson(logindata));
                br.getPage(postRequest);
                entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                final String authenticationUrl = (String) entries.get("authenticationUrl");
                if (StringUtils.isEmpty(authenticationUrl)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                /* Now continue without API */
                br.getPage(authenticationUrl);
                final Form continueform = br.getFormbyKey("response");
                if (continueform != null) {
                    /*
                     * Redirect from API to main website --> Grants us authorization cookie which can then again be used to authorize API
                     * requests
                     */
                    logger.info("Found continueform");
                    br.submitForm(continueform);
                } else {
                    logger.warning("Failed to find continueform");
                }
                final String access_token_ma = br.getCookie(br.getHost(), cookie_name_login, Cookies.NOTDELETEDPATTERN);
                if (!isLoggedIN(cookie_name_login) || access_token_ma == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.setProperty(PROPERTY_authorization, access_token_ma);
                account.setProperty(PROPERTY_jwt, jwt);
                account.setProperty(PROPERTY_timestamp_website_cookies_updated, System.currentTimeMillis());
                account.saveCookies(br.getCookies(account.getHoster()), "");
                /* Sets PROPERTY_authorization as Authorization header */
                setStoredAPIAuthHeaderAccount(br, account);
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    public static boolean prepareBrAPI(final Plugin plg, final Browser br, final Account acc) throws PluginException {
        final Map<String, Object> entries = getWebsiteJson(br);
        return prepareBrAPI(plg, br, acc, entries);
    }

    /** Sets required API headers based on data given in json. */
    public static boolean prepareBrAPI(final Plugin plg, final Browser br, final Account acc, Map<String, Object> entries) throws PluginException {
        final String plugin_host = plg.getHost();
        final String hostname;
        String ip = null;
        if (entries != null) {
            entries = (LinkedHashMap<String, Object>) entries.get("domain");
            /* E.g. site-ma.fakehub.com */
            hostname = (String) entries.get("hostname");
            ip = PluginJSonUtils.getJson(br, "ip");
        } else {
            hostname = getPornportalMainURL(plugin_host);
        }
        boolean isNewJWT = false;
        String jwt = null;
        if (acc == null) {
            /* Try to re-use old token */
            jwt = setAndGetStoredAPIAuthHeaderPlugin(br, plg);
        }
        if (jwt == null) {
            if (entries == null) {
                /* E.g. attempt to restore old tokens without having json/html with new data available. */
                return false;
            }
            jwt = PluginJSonUtils.getJson(br, "jwt");
            isNewJWT = true;
        }
        String cookie_name_instance = PluginJSonUtils.getJson(br, "instanceCookie");
        if (cookie_name_instance == null) {
            cookie_name_instance = getDefaultCookieNameInstance();
        } else if (acc != null) {
            acc.setProperty(PROPERTY_cookiename_instanceCookie, cookie_name_instance);
        }
        if (StringUtils.isEmpty(jwt) || StringUtils.isEmpty(hostname)) {
            plg.getLogger().warning("Failed to find api base data");
            return false;
        }
        br.setCookie(hostname, cookie_name_instance, jwt);
        br.getHeaders().put("Content-Type", "application/json");
        br.getHeaders().put("Referer", "https://" + hostname + "/login");
        br.getHeaders().put("sec-fetch-dest", "empty");
        br.getHeaders().put("sec-fetch-mode", "cors");
        br.getHeaders().put("sec-fetch-site", "cross-site");
        br.getHeaders().put("Origin", "https://" + hostname);
        br.getHeaders().put("Instance", jwt);
        if (!StringUtils.isEmpty(ip)) {
            br.getHeaders().put("x-forwarded-for", ip);
        }
        if (acc == null && isNewJWT) {
            plg.getPluginConfig().setProperty(PROPERTY_plugin_jwt, jwt);
            plg.getPluginConfig().setProperty(PROPERTY_plugin_jwt_create_timestamp, System.currentTimeMillis());
        }
        return true;
    }

    public static Map<String, Object> getWebsiteJson(final Browser br) {
        final String json = br.getRegex("window\\.__JUAN\\.rawInstance = (\\{.*?);\\s*\\}\\)\\(\\);").getMatch(0);
        return JSonStorage.restoreFromString(json, TypeRef.HASHMAP);
    }

    private boolean isLoggedIN(String login_cookie_name) {
        if (login_cookie_name == null) {
            login_cookie_name = getDefaultCookieNameLogin();
        }
        return br.getCookie(getHost(), login_cookie_name, Cookies.NOTDELETEDPATTERN) != null;
    }

    private static final String getDefaultCookieNameLogin() {
        /* 2020-04-03 */
        return "access_token_ma";
    }

    public static final String getDefaultCookieNameInstance() {
        /* 2020-04-03 */
        return "instance_token";
    }

    private boolean setStoredAPIAuthHeaderAccount(final Browser br, final Account account) {
        if (account == null) {
            return false;
        }
        final String jwt = account.getStringProperty(PROPERTY_jwt);
        final String authorization = account.getStringProperty(PROPERTY_authorization);
        if (jwt == null || authorization == null) {
            /* This should never happen */
            return false;
        }
        br.getHeaders().put("Instance", jwt);
        br.getHeaders().put("Authorization", authorization);
        return true;
    }

    public static String setAndGetStoredAPIAuthHeaderPlugin(final Browser br, final Plugin plg) {
        if (plg == null) {
            return null;
        }
        final String jwt = plg.getPluginConfig().getStringProperty(PROPERTY_plugin_jwt);
        final long timestamp_jwt_created = plg.getPluginConfig().getLongProperty(PROPERTY_plugin_jwt_create_timestamp, 0);
        final long jwt_age = System.currentTimeMillis() - timestamp_jwt_created;
        final long max_jwt_age_minutes = 5;
        if (jwt == null) {
            return null;
        } else if (jwt_age > max_jwt_age_minutes * 60 * 1000) {
            plg.getLogger().info("jwt is older than " + max_jwt_age_minutes + " minutes --> New jwt required");
            return null;
        }
        br.getHeaders().put("Instance", jwt);
        return jwt;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        synchronized (account) {
            try {
                login(this.br, account, true);
                final AccountInfo ai = new AccountInfo();
                if (br.getURL() == null || !br.getURL().contains("/v1/self")) {
                    br.getPage("https://site-api.project1service.com/v1/self");
                }
                final Map<String, Object> map = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                final Boolean isExpired = (Boolean) map.get("isExpired");
                final Boolean isTrial = (Boolean) map.get("isTrial");
                final Boolean isCanceled = (Boolean) map.get("isCanceled");
                if (Boolean.TRUE.equals(isTrial) || Boolean.TRUE.equals(isExpired)) {
                    account.setType(AccountType.FREE);
                    /* 2020-04-02: Free accounts can only be used to download trailers */
                    ai.setStatus("Free Account");
                } else if (isTrial) {
                    ai.setStatus("Free Account (Trial)");
                } else {
                    /* Premium accounts must not have any expire-date! */
                    final String expiryDate = (String) map.get("expiryDate");
                    if (expiryDate != null) {
                        final long expireTimestamp = TimeFormatter.getMilliSeconds(expiryDate, "yyyy'-'MM'-'dd'T'HH':'mm':'ss", null);
                        if (expireTimestamp > 0) {
                            ai.setValidUntil(expireTimestamp, br);
                        }
                    }
                    account.setType(AccountType.PREMIUM);
                    if (isCanceled) {
                        ai.setStatus("Premium Account (subscription cancelled)");
                    } else {
                        ai.setStatus("Premium Account (subscription running)");
                    }
                }
                account.setConcurrentUsePossible(true);
                ai.setUnlimitedTraffic();
                return ai;
            } catch (PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty("premium_directlink", dllink);
        dl.startDownload();
    }

    @Override
    public String getDescription() {
        return "Download videos- and pictures with the fakehub.com plugin.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_pc_1080p_6000", "Grab 1080p (mp4)?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_pc_720p_2600", "Grab 720p (mp4)?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_pc_480p_1500", "Grab 480p (mp4)?").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "GRAB_pc_368p_850", "Grab 360p (mp4)?").setDefaultValue(true));
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.PornPortal;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}