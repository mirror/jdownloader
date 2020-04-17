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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.controller.host.PluginFinder;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
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
        ret.add(new String[] { "bellesafilms.com" });
        ret.add(new String[] { "biempire.com" });
        ret.add(new String[] { "brazzers.com" });
        ret.add(new String[] { "digitalplayground.com" });
        ret.add(new String[] { "erito.com", "eritos.com" });
        ret.add(new String[] { "fakehub.com" });
        ret.add(new String[] { "hentaipros.com" });
        ret.add(new String[] { "lilhumpers.com" });
        ret.add(new String[] { "milehighmedia.com", "sweetheartvideo.com", "realityjunkies.com" });
        ret.add(new String[] { "metrohd.com", "familyhookups.com", "kinkyspa.com" });
        ret.add(new String[] { "mofos.com", "publicpickups.com", "iknowthatgirl.com", "dontbreakme.com" });
        ret.add(new String[] { "propertysex.com" });
        ret.add(new String[] { "realitykings.com", "gfleaks.com", "inthevip.com", "mikesapartment.com", "8thstreetlatinas.com", "bignaturals.com", "cumfiesta.com", "happytugs.com", "milfhunter.com", "momsbangteens.com", "momslickteens.com", "moneytalks.com", "roundandbrown.com", "sneakysex.com", "teenslovehugecocks.com", "welivetogether.com" });
        ret.add(new String[] { "sexyhub.com" });
        ret.add(new String[] { "squirted.com" });
        ret.add(new String[] { "transangels.com" });
        ret.add(new String[] { "transsensual.com" });
        ret.add(new String[] { "trueamateurs.com" });
        ret.add(new String[] { "twistys.com" });
        ret.add(new String[] { "whynotbi.com" });
        return ret;
    }

    public static ArrayList<String> getAllSupportedPluginDomainsFlat() {
        ArrayList<String> allDomains = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            for (final String singleDomain : domains) {
                allDomains.add(singleDomain);
            }
        }
        return allDomains;
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
            ret.add("https://decrypted" + buildHostsPatternPart(domains) + "/\\d+/\\d+");
        }
        return ret.toArray(new String[0]);
    }
    // @Override
    // public void correctDownloadLink(final DownloadLink link) {
    // final String fuid = this.fuid != null ? this.fuid : getFUIDFromURL(link);
    // if (fuid != null) {
    // /* link cleanup, prefer https if possible */
    // if (link.getPluginPatternMatcher() != null &&
    // link.getPluginPatternMatcher().matches("https?://[A-Za-z0-9\\-\\.:]+/embed-[a-z0-9]{12}")) {
    // link.setContentUrl(getMainPage() + "/embed-" + fuid + ".html");
    // }
    // link.setPluginPatternMatcher(getMainPage() + "/" + fuid);
    // link.setLinkID(getHost() + "://" + fuid);
    // }
    // }

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

    /* Tries to find new websites based on current account! This only works if you are logged in an account! */
    public static void findNewPossiblySupportedSites(final Plugin plg, final Browser br) {
        try {
            final String sid = br.getCookie("ppp.contentdef.com", "ppp_session");
            if (sid == null) {
                plg.getLogger().warning("Failed to find sid");
                return;
            }
            br.getPage("https://ppp.contentdef.com/thirdparty?sid=" + sid + "&_=" + System.currentTimeMillis());
            Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            final ArrayList<Object> domaininfos = (ArrayList<Object>) entries.get("notificationNetworks");
            final PluginFinder finder = new PluginFinder();
            for (final Object domaininfo : domaininfos) {
                entries = (Map<String, Object>) domaininfo;
                final String name = (String) entries.get("name");
                final String domain = (String) entries.get("domain");
                if (StringUtils.isEmpty(name) || StringUtils.isEmpty(domain)) {
                    /* Skip invalid items */
                    continue;
                }
                final String plugin_host = finder.assignHost(domain);
                if (plugin_host == null) {
                    plg.getLogger().info("Found new host: " + plugin_host);
                }
            }
        } catch (final Throwable e) {
            plg.getLogger().log(e);
            plg.getLogger().info("Failure due to Exception");
        }
    }

    public static String getPornportalMainURL(final String host) {
        if (host == null) {
            return null;
        }
        /*
         * TODO: Move away from static method to e.g. support sites like: https://bbw-channel.pornportal.com/,
         * https://ebony-channel.pornportal.com/, https://latina-channel.pornportal.com/, https://cosplay-channel.pornportal.com/login,
         * https://stepfamily-channel.pornportal.com, https://3dxstar-channel.pornportal.com, https://realitygang-channel.pornportal.com,
         * https://lesbian-channel.pornportal.com, https://anal-channel.pornportal.com, https://milf-channel.pornportal.com,
         * https://teen-channel.pornportal.com/login
         */
        return "https://site-ma." + host;
    }

    public static String getAPIBase() {
        return "https://site-api.project1service.com/v1";
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
    public static final String   PROPERTY_directurl           = "directurl";

    public static Browser prepBR(final Browser br) {
        br.setAllowedResponseCodes(new int[] { 400 });
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        dllink = link.getStringProperty(PROPERTY_directurl);
        if (dllink == null) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        URLConnectionAdapter con = null;
        String newDirecturl = null;
        try {
            con = br.openHeadConnection(dllink);
            if (con.getResponseCode() == 472) {
                /* Directurl needs to be refreshed */
                logger.info("Directurl needs to be refreshed");
                if (account == null) {
                    /* We need an account! */
                    return AvailableStatus.UNCHECKABLE;
                } else if (!isDownload) {
                    /* Only refresh directurls in download mode - account will only be available in download mode anyways! */
                    return AvailableStatus.UNCHECKABLE;
                }
                logger.info("Trying to refresh directurl");
                final String videoID = link.getStringProperty("videoid");
                final String quality = link.getStringProperty("quality");
                if (videoID == null || quality == null) {
                    /* This should never happen */
                    logger.info("DownloadLink property videoid or quality missing");
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                /* We should already be loggedIN at this stage! */
                this.login(this.br, account, this.getHost(), false);
                final LinkedHashMap<String, DownloadLink> qualities = jd.plugins.decrypter.PornportalComCrawler.crawlContentAPI(this, this.br, videoID, true);
                final Iterator<Entry<String, DownloadLink>> iteratorQualities = qualities.entrySet().iterator();
                while (iteratorQualities.hasNext()) {
                    final DownloadLink video = iteratorQualities.next().getValue();
                    final String videoIDTmp = video.getStringProperty("videoid");
                    final String qualityTmp = video.getStringProperty("quality");
                    if (videoID.equals(videoIDTmp) && quality.equals(qualityTmp)) {
                        newDirecturl = video.getStringProperty(PROPERTY_directurl);
                        break;
                    }
                }
                if (newDirecturl == null) {
                    logger.warning("Failed to find fresh directurl --> Content offline?");
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to refresh expired directurl --> Content offline?");
                }
                logger.info("Successfully found new directurl");
                con = br.openHeadConnection(newDirecturl);
            }
            if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (con.isContentDisposition()) {
                link.setDownloadSize(con.getLongContentLength());
                /*
                 * 2020-04-08: Final filename is supposed to be set in crawler. Their internal filenames are always the same e.g.
                 * "scene_320p.mp4".
                 */
                // link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con)));
                if (newDirecturl != null) {
                    /* Only set new directurl if it is working. Keep old one until then! */
                    logger.info("Successfully checked new directurl and set property");
                    link.setProperty(PROPERTY_directurl, newDirecturl);
                    this.dllink = newDirecturl;
                }
            } else {
                this.server_issues = true;
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
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link, null, true);
        doFree(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    // @Override
    // public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
    // logger.info("Downloading in multihoster mode");
    // this.handlePremium(link, account);
    // }
    private void doFree(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        throw new AccountRequiredException();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    /* Account properties */
    private static final String PROPERTY_authorization                     = "authorization";
    private static final String PROPERTY_jwt                               = "jwt";
    private static final String PROPERTY_timestamp_website_cookies_updated = "timestamp_website_cookies_updated";
    public static final String  PROPERTY_cookiename_authCookie             = "auth_cookie";
    public static final String  PROPERTY_cookiename_instanceCookie         = "instanceCookie";
    public static final String  PROPERTY_url_external_login                = "url_external_login";
    /* Plugin properties */
    public static final String  PROPERTY_plugin_jwt                        = "jwt";
    public static final String  PROPERTY_plugin_jwt_create_timestamp       = "jwt_create_timestamp";

    public void login(Browser brlogin, final Account account, final String target_domain, final boolean checkCookies) throws Exception {
        synchronized (account) {
            try {
                if (brlogin == null || account == null || target_domain == null) {
                    return;
                }
                final boolean isExternalPortalLogin = !target_domain.equalsIgnoreCase(account.getHoster());
                if (isExternalPortalLogin) {
                    /* Login via "Jump-URL" into other portal */
                    logger.info("External portal login: " + target_domain);
                } else {
                    /* Login to main portal */
                    logger.info("Internal portal login: " + target_domain);
                }
                brlogin.setCookiesExclusive(true);
                // checkUsedVersions(this);
                prepBR(brlogin);
                Cookies cookies = account.loadCookies(target_domain);
                String jwt = null;
                if (cookies != null && setStoredAPIAuthHeaderAccount(brlogin, account, target_domain)) {
                    /*
                     * Try to avoid login captcha at all cost!
                     */
                    brlogin.setCookies(account.getHoster(), cookies);
                    if (!checkCookies && System.currentTimeMillis() - account.getCookiesTimeStamp(target_domain) <= 5 * 60 * 1000) {
                        logger.info("Trust cookies without check");
                        return;
                    }
                    brlogin.getPage(getAPIBase() + "/self");
                    if (brlogin.getHttpConnection().getResponseCode() == 200) {
                        logger.info("Cookie login successful");
                        final long timestamp_headers_updated = this.getLongPropertyAccount(account, target_domain, PROPERTY_timestamp_website_cookies_updated, 0);
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
                                this.setPropertyAccount(account, target_domain, PROPERTY_jwt, jwt);
                                brlogin.setCookie(getPornportalMainURL(account.getHoster()), this.getStringPropertyAccount(account, target_domain, PROPERTY_cookiename_instanceCookie, getDefaultCookieNameInstance()), jwt);
                                this.setPropertyAccount(account, target_domain, PROPERTY_timestamp_website_cookies_updated, System.currentTimeMillis());
                            }
                        }
                        account.saveCookies(brlogin.getCookies(account.getHoster()), target_domain);
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        cookies = null;
                        /* Important: Especially old Authorization headers can cause trouble! */
                        brlogin = prepBR(new Browser());
                    }
                }
                logger.info("Performing full login");
                brlogin.setCookiesExclusive(true);
                final Browser newBR = prepBR(new Browser());
                brlogin.setHeaders(newBR.getHeaders());
                brlogin.setFollowRedirects(true);
                brlogin.getPage(getPornportalMainURL(target_domain) + "/login");
                Map<String, Object> entries = getJsonJuanEawInstance(brlogin);
                final String api_base = PluginJSonUtils.getJson(brlogin, "dataApiUrl");
                String cookie_name_login = PluginJSonUtils.getJson(brlogin, "authCookie");
                if (cookie_name_login == null) {
                    cookie_name_login = getDefaultCookieNameLogin();
                }
                if (isExternalPortalLogin) {
                    String autologinURL = this.getStringPropertyAccount(account, target_domain, PROPERTY_url_external_login, null);
                    if (autologinURL == null) {
                        logger.warning("Property autologinURL is null");
                        loginFailure(isExternalPortalLogin);
                    }
                    if (autologinURL.startsWith("/")) {
                        autologinURL = "https://ppp.contentdef.com/" + autologinURL;
                    }
                    br.getPage(autologinURL);
                    final String redirectURL = br.getRegex("window\\.top\\.location\\s*=\\s*\\'(https?://[^<>\"\\']+)").getMatch(0);
                    if (redirectURL == null) {
                        logger.warning("Failed to find external login redirectURL");
                        loginFailure(isExternalPortalLogin);
                    }
                    br.getPage(redirectURL);
                    /* Now we should finally land on '/postlogin' --> This would mean SUCCESS! */
                    if (!br.getURL().contains("/postlogin")) {
                        logger.warning("External login failed - expected location /postlogin but got: " + br.getURL());
                        loginFailure(isExternalPortalLogin);
                    }
                    /* Further checks will decide whether we're loggedIN or not */
                } else {
                    if (StringUtils.isEmpty(api_base)) {
                        logger.warning("Failed to find api_base");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    if (!prepareBrAPI(this, brlogin, account, entries)) {
                        logger.warning("Failed to prepare API headers");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
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
                            final CaptchaHelperHostPluginRecaptchaV2 captcha = new CaptchaHelperHostPluginRecaptchaV2(this, brlogin, recaptchaSiteKey);
                            recaptchaV2Response = captcha.getToken();
                            logindata.put("googleReCaptchaResponse", recaptchaV2Response);
                        } finally {
                            if (dlinkbefore != null) {
                                setDownloadLink(dlinkbefore);
                            }
                        }
                    }
                    final PostRequest postRequest = brlogin.createPostRequest(api_base + "/v1/authenticate/redirect", JSonStorage.serializeToJson(logindata));
                    brlogin.getPage(postRequest);
                    entries = JSonStorage.restoreFromString(brlogin.toString(), TypeRef.HASHMAP);
                    final String authenticationUrl = (String) entries.get("authenticationUrl");
                    if (StringUtils.isEmpty(authenticationUrl)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    /* Now continue without API */
                    brlogin.getPage(authenticationUrl);
                    final Form continueform = brlogin.getFormbyKey("response");
                    if (continueform != null) {
                        /*
                         * Redirect from API to main website --> Grants us authorization cookie which can then again be used to authorize
                         * API requests
                         */
                        logger.info("Found continueform");
                        brlogin.submitForm(continueform);
                    } else {
                        logger.warning("Failed to find continueform");
                    }
                }
                /* Now we should e.g. be here: */
                final String login_cookie = getLoginCookie(brlogin, cookie_name_login);
                jwt = PluginJSonUtils.getJson(brlogin, "jwt");
                if (login_cookie == null || StringUtils.isEmpty(jwt)) {
                    logger.info("Login failure after API login");
                    loginFailure(isExternalPortalLogin);
                }
                setPropertyAccount(account, target_domain, PROPERTY_authorization, login_cookie);
                setPropertyAccount(account, target_domain, PROPERTY_jwt, jwt);
                setPropertyAccount(account, target_domain, PROPERTY_timestamp_website_cookies_updated, System.currentTimeMillis());
                account.saveCookies(brlogin.getCookies(brlogin.getHost()), target_domain);
                /* Sets PROPERTY_authorization as Authorization header */
                setStoredAPIAuthHeaderAccount(brlogin, account, target_domain);
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies(target_domain);
                }
                throw e;
            }
        }
    }

    private void loginFailure(final boolean isExternalLogin) throws PluginException {
        if (isExternalLogin) {
            /*
             * Never throw exceptions which affect accounts in here as we do not e.g. want to (temp.) disable a erito.com account just
             * because external login for fakehub.com fails here!
             */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "External portal login failed");
        } else {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    private void setPropertyAccount(final Account account, final String target_domain, final String key, final Object value) {
        account.setProperty(key + "_" + target_domain, value);
    }

    private String getStringPropertyAccount(final Account account, final String target_domain, final String key, final String fallback) {
        return account.getStringProperty(key + "_" + target_domain, fallback);
    }

    private long getLongPropertyAccount(final Account account, final String target_domain, final String key, final long fallback) {
        return account.getLongProperty(key + "_" + target_domain, fallback);
    }

    // private boolean isLoggedIN(String login_cookie_name) {
    // return getLoginCookie(this.br, login_cookie_name) != null;
    // }
    private String getLoginCookie(final Browser br, String login_cookie_name) {
        if (login_cookie_name == null) {
            login_cookie_name = getDefaultCookieNameLogin();
        }
        return br.getCookie(br.getHost(), login_cookie_name, Cookies.NOTDELETEDPATTERN);
    }

    public static boolean prepareBrAPI(final Plugin plg, final Browser br, final Account acc) throws PluginException {
        final Map<String, Object> entries = getJsonJuanEawInstance(br);
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

    public static Map<String, Object> getJsonJuanEawInstance(final Browser br) {
        final String json = br.getRegex("window\\.__JUAN\\.rawInstance = (\\{.*?);\\s*\\}\\)\\(\\);").getMatch(0);
        return JSonStorage.restoreFromString(json, TypeRef.HASHMAP);
    }

    public static Map<String, Object> getJsonJuanInitialState(final Browser br) {
        final String json = br.getRegex("window\\.__JUAN\\.initialState = (\\{.*?);\\s*\\}\\)\\(\\);").getMatch(0);
        return JSonStorage.restoreFromString(json, TypeRef.HASHMAP);
    }

    private static final String getDefaultCookieNameLogin() {
        /* 2020-04-03 */
        return "access_token_ma";
    }

    public static final String getDefaultCookieNameInstance() {
        /* 2020-04-03 */
        return "instance_token";
    }

    private boolean setStoredAPIAuthHeaderAccount(final Browser br, final Account account, final String target_domain) {
        if (br == null || account == null || target_domain == null) {
            return false;
        }
        final String jwt = this.getStringPropertyAccount(account, target_domain, PROPERTY_jwt, null);
        final String authorization = this.getStringPropertyAccount(account, target_domain, PROPERTY_authorization, null);
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
                login(this.br, account, this.getHost(), true);
                final AccountInfo ai = new AccountInfo();
                if (br.getURL() == null || !br.getURL().contains("/v1/self")) {
                    br.getPage(getAPIBase() + "/self");
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
                if (DebugMode.TRUE_IN_IDE_ELSE_FALSE && account.getType() == AccountType.PREMIUM) {
                    /* Now check which other websites we can now use as well and add them via multihoster handling. */
                    try {
                        br.getPage("https://site-ma." + this.getHost() + "/");
                        Map<String, Object> initialState = getJsonJuanInitialState(br);
                        initialState = (Map<String, Object>) initialState.get("client");
                        final String userAgent = (String) initialState.get("userAgent");
                        final String domain = (String) initialState.get("domain");
                        final String ip = (String) initialState.get("ip");
                        final String baseUrl = (String) initialState.get("baseUrl");
                        if (StringUtils.isEmpty(userAgent) || StringUtils.isEmpty(domain) || StringUtils.isEmpty(ip) || StringUtils.isEmpty(baseUrl)) {
                            /* Failure */
                        }
                        final Map<String, Object> portalmap = new HashMap<String, Object>();
                        portalmap.put("accountUrlPath", "/account");
                        portalmap.put("baseUri", "/");
                        portalmap.put("baseUrl", baseUrl);
                        portalmap.put("domain", domain);
                        portalmap.put("homeUrlPath", "/");
                        portalmap.put("logoutUrlPath", "/logout");
                        portalmap.put("postLoginUrlPath", "/postlogin");
                        portalmap.put("resetPpMember", true);
                        portalmap.put("userBrowserId", userAgent);
                        portalmap.put("userIp", ip);
                        // final PostRequest postRequest = br.createPostRequest(getAPIBase() + "/pornportal",
                        // JSonStorage.serializeToJson(portalmap));
                        // br.getPage(postRequest);
                        br.postPageRaw(getAPIBase() + "/pornportal", JSonStorage.serializeToJson(portalmap));
                        final String data = PluginJSonUtils.getJson(br, "data");
                        br.getPage("https://ppp.contentdef.com/postlogin?data=" + Encoding.urlEncode(data));
                        /*
                         * We can authorize ourselves to these other portals through these URLs. Sadly we do not get the full domains before
                         * accessing these URLs but this would take a lot of time which is why we will try to find the full domains here
                         * without accessing these URLs.
                         */
                        final String[] autologinURLs = br.getRegex("(/autologin/[a-z0-9]+\\?sid=[^\"]+)\"").getColumn(0);
                        final String sid = new UrlQuery().parse("https://" + this.getHost() + autologinURLs[0]).get("sid");
                        final Browser brContentdef = br.cloneBrowser();
                        brContentdef.getPage(String.format("https://ppp.contentdef.com/notification/list?page=1&type=1&network=1&archived=0&ajaxCounter=1&sid=%s&data=%s&_=%d", sid, Encoding.urlEncode(data), System.currentTimeMillis()));
                        Map<String, Object> entries = JSonStorage.restoreFromString(brContentdef.toString(), TypeRef.HASHMAP);
                        final ArrayList<Object> notificationNetworks = (ArrayList<Object>) entries.get("notificationNetworks");
                        ArrayList<String> supportedHostsTmp = new ArrayList<String>();
                        final ArrayList<String> allowedSupportedHosts = getAllSupportedPluginDomainsFlat();
                        ArrayList<String> supportedHostsFinal = new ArrayList<String>();
                        for (final String autologinURL : autologinURLs) {
                            String domainWithoutTLD = null;
                            final String domainShortcode = new Regex(autologinURL, "autologin/([a-z0-9]+)").getMatch(0);
                            if (domainShortcode == null) {
                                logger.warning("WTF failed to find domainShortcode for autologinURL: " + autologinURL);
                                continue;
                            }
                            final String fullHTML = br.getRegex("<li class=\"pp-menu-list-item-active " + domainShortcode + "\">(.*?)</li>").getMatch(0);
                            if (fullHTML != null) {
                                domainWithoutTLD = new Regex(fullHTML, "ContinueToProduct-(.*?)\">").getMatch(0);
                                if (domainWithoutTLD != null) {
                                    domainWithoutTLD = Encoding.htmlDecode(domainWithoutTLD);
                                    /* E.g. Hentai Pros --> HentaiPros */
                                    domainWithoutTLD = domainWithoutTLD.replace(" ", "");
                                }
                            }
                            /* Find full domain for shortcode */
                            String domainFull = null;
                            for (final Object notificationNetworkO : notificationNetworks) {
                                Map<String, Object> notificationNetwork = (Map<String, Object>) notificationNetworkO;
                                final String domainShortcodeTmp = (String) notificationNetwork.get("short_name");
                                final String site_url = (String) notificationNetwork.get("site_url");
                                if (StringUtils.isEmpty(domainShortcodeTmp) || StringUtils.isEmpty(site_url)) {
                                    /* Skip invalid items */
                                    continue;
                                }
                                if (domainShortcodeTmp.equals(domainShortcode)) {
                                    domainFull = site_url;
                                    break;
                                }
                            }
                            final String domain_to_add;
                            if (domainFull != null) {
                                domain_to_add = Browser.getHost(domainFull, false);
                            } else {
                                domain_to_add = domainWithoutTLD;
                            }
                            if (domain_to_add == null) {
                                logger.warning("Failed to find any usable domain for domain: " + domainShortcode);
                                continue;
                            }
                            supportedHostsTmp.clear();
                            supportedHostsTmp.add(domain_to_add);
                            ai.setMultiHostSupport(this, supportedHostsTmp);
                            final List<String> supportedHostsTmpReal = ai.getMultiHostSupport();
                            if (supportedHostsTmpReal == null || supportedHostsTmpReal.isEmpty()) {
                                logger.info("Failed to find any real host for: " + domain_to_add);
                                continue;
                            }
                            final String final_host = supportedHostsTmpReal.get(0);
                            if (!allowedSupportedHosts.contains(final_host)) {
                                logger.info("Skipping the following host as it is not an allowed/PornPortal host: " + final_host);
                                continue;
                            }
                            supportedHostsFinal.add(final_host);
                            this.setPropertyAccount(account, domainFull, PROPERTY_url_external_login, autologinURL);
                        }
                        /*
                         * TODO: Add special handling for pornhubpremium.
                         */
                        /* Remove current host - we do not want that in our list of supported hosts! */
                        supportedHostsFinal.remove(this.getHost());
                        ai.setMultiHostSupport(this, supportedHostsFinal);
                    } catch (final Throwable e) {
                        logger.log(e);
                        logger.warning("Internal Multihoster handling failed");
                    }
                }
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
        requestFileInformation(link, account, true);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
        }
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