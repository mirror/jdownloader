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
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.config.PornportalComConfig;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.plugins.controller.host.PluginFinder;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.plugins.decrypter.PornportalComCrawler;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class PornportalCom extends PluginForHost {
    public PornportalCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.pornportal.com/");
        // setConfigElements();
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 400 });
        return br;
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX, LazyPlugin.FEATURE.IMAGE_GALLERY };
    }

    @Override
    public String getAGBLink() {
        return "http://www.supportmg.com/terms-of-service";
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "pornportal.com" });
        ret.add(new String[] { "babes.com", "blackisbetter.com" });
        ret.add(new String[] { "bellesafilms.com" });
        ret.add(new String[] { "biempire.com" });
        ret.add(new String[] { "brazzers.com" });
        ret.add(new String[] { "digitalplayground.com" });
        ret.add(new String[] { "erito.com", "eritos.com" });
        ret.add(new String[] { "fakehub.com", "femalefaketaxi.com", "fakedrivingschool.com", "fakehostel.com" });
        ret.add(new String[] { "hentaipros.com" });
        ret.add(new String[] { "lilhumpers.com" });
        ret.add(new String[] { "milehighmedia.com", "sweetheartvideo.com", "realityjunkies.com" });
        ret.add(new String[] { "metrohd.com", "familyhookups.com", "kinkyspa.com" });
        ret.add(new String[] { "mofos.com", "publicpickups.com", "iknowthatgirl.com", "dontbreakme.com" });
        ret.add(new String[] { "propertysex.com" });
        ret.add(new String[] { "realitykings.com", "gfleaks.com", "inthevip.com", "mikesapartment.com", "8thstreetlatinas.com", "bignaturals.com", "cumfiesta.com", "happytugs.com", "milfhunter.com", "momsbangteens.com", "momslickteens.com", "moneytalks.com", "roundandbrown.com", "sneakysex.com", "teenslovehugecocks.com", "welivetogether.com", "blackgfs.com", "daredorm.com" });
        ret.add(new String[] { "sexyhub.com", "fitnessrooms.com" });
        ret.add(new String[] { "spankwirepremium.com" });
        ret.add(new String[] { "squirted.com" });
        ret.add(new String[] { "transangels.com" });
        ret.add(new String[] { "transsensual.com" });
        ret.add(new String[] { "trueamateurs.com" });
        ret.add(new String[] { "twistys.com", "teenpinkvideos.com" });
        ret.add(new String[] { "whynotbi.com" });
        ret.add(new String[] { "bangbros.com", "bangbrothers.com", "bangbrothers.net" });
        return ret;
    }

    /** Returns content of getPluginDomains as single dimensional Array. */
    public static ArrayList<String> getAllSupportedPluginDomainsFlat() {
        ArrayList<String> allDomains = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            for (final String singleDomain : domains) {
                allDomains.add(singleDomain);
            }
        }
        return allDomains;
    }

    /** Contains domains which will be ignored in list of supported hosts in account view / "multihoster support". */
    public static ArrayList<String> getAllBlacklistedDomains() {
        final ArrayList<String> allDomains = new ArrayList<String>();
        allDomains.add("pornportal.com");
        return allDomains;
    }

    /**
     * Basically contains sites that are allowed to be displayed in special internal multihost handling but at the same time cannot be
     * handled by this plugin.
     */
    public static ArrayList<String> getPossibleExternalSupportedSitesThatCannotBeHandledByPornPortal() {
        final ArrayList<String> extSitesSpecial = new ArrayList<String>();
        extSitesSpecial.add("pornhub.com");
        return extSitesSpecial;
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
            /* No regex needed - all items are added via crawler plugin. */
            ret.add("");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        final String host = link.getHost();
        if (getPossibleExternalSupportedSitesThatCannotBeHandledByPornPortal().contains(host)) {
            /* Do not allow hosts which are supported by internal multihoster handling but cannot be handled by this plugin. */
            return false;
        } else {
            return true;
        }
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String videoid = link.getStringProperty(PROPERTY_VIDEO_ID);
        final String videoquality = link.getStringProperty(PROPERTY_VIDEO_QUALITY);
        final String galleryid = link.getStringProperty(PROPERTY_GALLERY_ID);
        final int galleryImagePosition = link.getIntegerProperty(PROPERTY_GALLERY_IMAGE_POSITION, -1);
        final int galleryPosition = link.getIntegerProperty(PROPERTY_GALLERY_POSITION, -1);
        if (videoid != null && videoquality != null) {
            return this.getHost() + "://video" + videoid + "/" + videoquality;
        } else if (galleryid != null && galleryPosition != -1 && galleryImagePosition != -1) {
            return this.getHost() + "://photo" + galleryid + "/" + galleryPosition + "/" + galleryImagePosition;
        } else {
            return super.getLinkID(link);
        }
    }

    /*
     * Debug function: Can be used to quickly find the currently used pornportal version of all supported websites and compare against
     * previously set expected version value.
     */
    public static void checkUsedVersions(final Plugin plg) {
        final String target_version = "4.35.2";
        plg.getLogger().info("Target version: " + target_version);
        final Browser br = plg.createNewBrowserInstance();
        final String[] supportedSites = getAnnotationNames();
        for (final String host : supportedSites) {
            try {
                getPage(br, getPornportalMainURL(host));
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
            getPage(br, "https://ppp.contentdef.com/thirdparty?sid=" + sid + "&_=" + System.currentTimeMillis());
            Map<String, Object> entries = JSonStorage.restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final List<Object> domaininfos = (List<Object>) entries.get("notificationNetworks");
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
    private static final int     FREE_MAXDOWNLOADS               = 1;
    private static final boolean ACCOUNT_PREMIUM_RESUME          = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS       = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS    = 20;
    public static final String   PROPERTY_directurl              = "directurl";
    public static final String   PROPERTY_VIDEO_ID               = "videoid";
    public static final String   PROPERTY_VIDEO_QUALITY          = "quality";
    public static final String   PROPERTY_GALLERY_ID             = "galleryid";
    public static final String   PROPERTY_GALLERY_POSITION       = "gallery_position";
    public static final String   PROPERTY_GALLERY_IMAGE_POSITION = "gallery_image_position";
    public static final String   PROPERTY_GALLERY_DIRECTORY      = "gallery_directory";
    public static final String   PROPERTY_GALLERY_SIZE           = "gallery_size";
    public static Object         KEYLOCK                         = new Object();

    public static Request getPage(final Browser br, final Request request) throws Exception {
        br.getPage(request);
        String RNKEY = evalKEY(br);
        if (RNKEY != null) {
            int maxLoops = 8;// up to 3 loops in tests
            synchronized (KEYLOCK) {
                while (true) {
                    if (RNKEY == null) {
                        return br.getRequest();
                    } else if (--maxLoops > 0) {
                        br.setCookie(br.getHost(), "KEY", RNKEY);
                        Thread.sleep(1000 + ((8 - maxLoops) * 500));
                        br.getPage(request.cloneRequest());
                        RNKEY = evalKEY(br);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
            }
        } else {
            return br.getRequest();
        }
    }

    public static Request getPage(final Browser br, final String url) throws Exception {
        return getPage(br, br.createGetRequest(url));
    }

    /* Similar in: PornHubCom, PornportalCom */
    public final static String evalKEY(Browser br) throws ScriptException {
        if (br.containsHTML("document\\.cookie=\"KEY") && br.containsHTML("leastFactor")) {
            ScriptEngineManager mgr = JavaScriptEngineFactory.getScriptEngineManager(null);
            ScriptEngine engine = mgr.getEngineByName("JavaScript");
            String js = br.toString();
            js = new Regex(js, "<script[^>]*>(?:<!--)?(.*?)(?://-->)?</script>").getMatch(0);
            js = js.replace("document.cookie=", "return ");
            js = js.replaceAll("(/\\*.*?\\*/)", "");
            engine.eval(js + " var ret=go();");
            final String answer = engine.get("ret").toString();
            final String keyStr = new Regex(answer, "KEY=(.+)").getMatch(0);
            return keyStr.split(";")[0];
        } else {
            return null;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null, false);
    }

    private String getContentID(final DownloadLink link) {
        final String videoid = link.getStringProperty(PROPERTY_VIDEO_ID);
        final String galleryid = link.getStringProperty(PROPERTY_GALLERY_ID);
        if (videoid != null) {
            return videoid;
        } else if (galleryid != null) {
            return galleryid;
        } else {
            return null;
        }
    }

    private boolean isVideo(final DownloadLink link) {
        final String videoid = link.getStringProperty(PROPERTY_VIDEO_ID);
        if (videoid != null) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isImage(final DownloadLink link) {
        final String galleryid = link.getStringProperty(PROPERTY_GALLERY_ID);
        if (galleryid != null) {
            return true;
        } else {
            return false;
        }
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        this.setBrowserExclusive();
        final String dllink = link.getStringProperty(PROPERTY_directurl);
        if (dllink == null) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        URLConnectionAdapter con = null;
        String newDirecturl = null;
        try {
            con = br.openHeadConnection(dllink);
            /**
             * 403 = Generic expired </br>
             * 472 = Video-directurl expired 474 = Image directurl expired and/or any directurl is not expired but used with the wrong IP ->
             * New one needs to be obtained.
             */
            if (con.getResponseCode() == 403 || con.getResponseCode() == 472 || con.getResponseCode() == 474) {
                br.followConnection(true);
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
                final String contentID = getContentID(link);
                if (contentID == null) {
                    /* This should never happen */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                /* We should already be loggedIN at this stage! */
                this.login(this.br, account, link.getHost(), false);
                final PornportalComCrawler crawler = (PornportalComCrawler) this.getNewPluginForDecryptInstance(this.getHost());
                final ArrayList<DownloadLink> results = crawler.crawlContentAPI(this, contentID, account, null);
                final String targetLinkid = this.getLinkID(link);
                DownloadLink result = null;
                for (final DownloadLink item : results) {
                    if (StringUtils.equals(this.getLinkID(item), targetLinkid)) {
                        result = item;
                        break;
                    }
                }
                if (result == null) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to refresh expired directurl --> Content offline or session expired?");
                }
                newDirecturl = result.getStringProperty(PROPERTY_directurl);
                if (newDirecturl == null) {
                    /* This should never happen */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                logger.info("Successfully found new directurl");
                con = br.openHeadConnection(newDirecturl);
            }
            if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (!this.looksLikeDownloadableContent(con)) {
                br.followConnection(true);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Directurl did not lead to downloadable content");
            }
            if (con.getCompleteContentLength() > 0) {
                link.setDownloadSize(con.getCompleteContentLength());
            }
            /*
             * 2020-04-08: Final filename is supposed to be set in crawler. Their internal filenames are always the same e.g.
             * "scene_320p.mp4".
             */
            // link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con)));
            if (newDirecturl != null) {
                /* Only set new directurl if it is working. Keep old one until then! */
                logger.info("Successfully checked new directurl and set property");
                link.setProperty(PROPERTY_directurl, newDirecturl);
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
        this.handleDownload(link, null);
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        logger.info("Downloading in multihoster mode");
        this.handlePremium(link, account);
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

    public void login(final Browser brlogin, final Account account, final String target_domain, final boolean checkCookies) throws Exception {
        synchronized (account) {
            try {
                if (brlogin == null || account == null || target_domain == null) {
                    return;
                }
                final boolean isExternalPortalLogin;
                if (!target_domain.equalsIgnoreCase(account.getHoster())) {
                    isExternalPortalLogin = true;
                } else {
                    isExternalPortalLogin = false;
                }
                if (isExternalPortalLogin) {
                    /* Login via "Jump-URL" into other portal */
                    logger.info("External portal login: " + target_domain);
                } else {
                    /* Login to main portal */
                    logger.info("Internal portal login: " + target_domain);
                }
                brlogin.setCookiesExclusive(true);
                // checkUsedVersions(this);
                Cookies cookies = account.loadCookies(target_domain);
                String jwt = null;
                if (cookies != null && setStoredAPIAuthHeaderAccount(brlogin, account, target_domain)) {
                    /*
                     * Try to avoid login captcha at all cost!
                     */
                    brlogin.setCookies(target_domain, cookies);
                    if (!checkCookies) {
                        /* Trust cookies without check */
                        logger.info("Trust cookies without check");
                        return;
                    }
                    getPage(brlogin, getAPIBase() + "/self");
                    if (brlogin.getHttpConnection().getResponseCode() == 200) {
                        logger.info("Cookie login successful");
                        final long timestamp_headers_updated = this.getLongPropertyAccount(account, target_domain, PROPERTY_timestamp_website_cookies_updated, 0);
                        /* Update website cookies sometimes although we really use the Website-API for most of all requests. */
                        if (System.currentTimeMillis() - timestamp_headers_updated >= 5 * 60 * 1000l) {
                            logger.info("Updating website cookies and JWT value");
                            /* Access mainpage without authorization headers but with cookies */
                            final Browser brc = this.createNewBrowserInstance();
                            brc.setCookies(target_domain, cookies);
                            getPage(brc, getPornportalMainURL(account.getHoster()));
                            /* Attention: This is very unsafe without using json parser! */
                            jwt = PluginJSonUtils.getJson(brc, "jwt");
                            if (jwt == null) {
                                logger.warning("Failed to find jwt --> Re-using old value");
                            } else {
                                this.setPropertyAccount(account, target_domain, PROPERTY_jwt, jwt);
                                brlogin.setCookie(getPornportalMainURL(account.getHoster()), this.getStringPropertyAccount(account, target_domain, PROPERTY_cookiename_instanceCookie, getDefaultCookieNameInstance()), jwt);
                                this.setPropertyAccount(account, target_domain, PROPERTY_timestamp_website_cookies_updated, System.currentTimeMillis());
                            }
                        }
                        account.saveCookies(brlogin.getCookies(target_domain), target_domain);
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        /* Important: Especially old "Authorization" headers can cause trouble! */
                        brlogin.clearAll();
                    }
                }
                logger.info("Performing full login");
                brlogin.setCookiesExclusive(true);
                Map<String, Object> entries;
                String cookie_name_login = null;
                if (isExternalPortalLogin) {
                    handleExternalLoginStep(brlogin, account, target_domain);
                    /* Now we should finally land on '/postlogin' --> This would mean SUCCESS! */
                    if (!brlogin.getURL().contains("/postlogin")) {
                        logger.warning("Possible external login failure: Expected location '/postlogin' but got this instead: " + br.getURL());
                    }
                    /* Further checks will decide whether we're loggedIN or not */
                    entries = getJsonJuanEawInstance(brlogin);
                    cookie_name_login = PluginJSonUtils.getJson(brlogin, "authCookie");
                } else {
                    getPage(brlogin, getPornportalMainURL(target_domain) + "/login");
                    entries = getJsonJuanEawInstance(brlogin);
                    final String authApiUrl = PluginJSonUtils.getJson(brlogin, "authApiUrl");
                    cookie_name_login = PluginJSonUtils.getJson(brlogin, "authCookie");
                    if (cookie_name_login == null) {
                        cookie_name_login = getDefaultCookieNameLogin();
                    }
                    if (StringUtils.isEmpty(authApiUrl)) {
                        logger.warning("Failed to find api_base");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    } else if (!prepareBrAPI(this, brlogin, account, entries)) {
                        logger.warning("Failed to prepare API headers");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final Map<String, Object> domainmap = (Map<String, Object>) entries.get("domain");
                    final String hostname = domainmap.get("hostname").toString(); // E.g. site-ma.fakehub.com
                    final String recaptchaSiteKey = (String) domainmap.get("siteKey");
                    final String recaptchaInvisibleSiteKey = (String) domainmap.get("siteKeyV3");
                    final boolean allowNormalReCaptchaV2 = false;
                    final boolean allowInvisibleReCaptchaV2 = true; // 2023-11-02
                    /* Prepare POST-data */
                    final Map<String, Object> logindata = new HashMap<String, Object>();
                    logindata.put("hostname", hostname);
                    logindata.put("username", account.getUser());
                    logindata.put("password", account.getPass());
                    /* 2023-11-02: Not needed anymore TODO: Remove commented-out-code down below */
                    // logindata.put("failureUrl", "https://" + hostname + "/access/failure");
                    // logindata.put("successUrl", "https://" + hostname + "/access/success");
                    /* 2020-04-03: So far, all pornportal websites required a captcha on login. */
                    String recaptchaV2Response = null;
                    if (!StringUtils.isEmpty(recaptchaSiteKey) && allowNormalReCaptchaV2) {
                        final CaptchaHelperHostPluginRecaptchaV2 captcha = new CaptchaHelperHostPluginRecaptchaV2(this, brlogin, recaptchaSiteKey);
                        recaptchaV2Response = captcha.getToken();
                        logindata.put("googleReCaptchaResponse", recaptchaV2Response);
                    } else if (!StringUtils.isEmpty(recaptchaInvisibleSiteKey) && allowInvisibleReCaptchaV2) {
                        final CaptchaHelperHostPluginRecaptchaV2 captcha = new CaptchaHelperHostPluginRecaptchaV2(this, brlogin, recaptchaInvisibleSiteKey) {
                            @Override
                            public TYPE getType() {
                                return TYPE.INVISIBLE;
                            }
                        };
                        recaptchaV2Response = captcha.getToken();
                        logindata.put("googleReCaptchaResponse", recaptchaV2Response);
                        logindata.put("googleReCaptchaVersion", "v3");
                    }
                    final PostRequest postRequest = brlogin.createPostRequest(authApiUrl + "/v1/authenticate", JSonStorage.serializeToJson(logindata));
                    getPage(brlogin, postRequest);
                    if (brlogin.getHttpConnection().getResponseCode() == 401) {
                        throw new AccountInvalidException();
                    } else if (brlogin.getHttpConnection().getResponseCode() == 403) {
                        throw new AccountInvalidException();
                    }
                    final Map<String, Object> authinfo = restoreFromString(brlogin.getRequest().getHtmlCode(), TypeRef.MAP);
                    final String accessToken = (String) authinfo.get("access_token");
                    if (StringUtils.isEmpty(accessToken)) {
                        throw new AccountInvalidException();
                    }
                    brlogin.setCookie(hostname, cookie_name_login, accessToken);
                    final String authenticationUrl = "https://" + hostname + "/postlogin";
                    /* Now continue without API */
                    getPage(brlogin, authenticationUrl);
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
                /* Now we should e.g. be here: '/postlogin' */
                if (cookie_name_login == null) {
                    cookie_name_login = getDefaultCookieNameLogin();
                }
                /*
                 * 2020-04-18: This cookie is valid for (max.) 24 hours.
                 */
                final String login_cookie = getLoginCookie(brlogin, cookie_name_login);
                jwt = PluginJSonUtils.getJson(brlogin, "jwt");
                if (login_cookie == null) {
                    logger.info("Login failure after API login");
                    loginFailure(isExternalPortalLogin);
                } else if (StringUtils.isEmpty(jwt)) {
                    logger.info("Login failure after API login");
                    loginFailure(isExternalPortalLogin);
                }
                logger.info("Looks like successful login");
                setPropertyAccount(account, target_domain, PROPERTY_authorization, login_cookie);
                setPropertyAccount(account, target_domain, PROPERTY_jwt, jwt);
                setPropertyAccount(account, target_domain, PROPERTY_timestamp_website_cookies_updated, System.currentTimeMillis());
                account.saveCookies(brlogin.getCookies(brlogin.getHost()), target_domain);
                setStoredAPIAuthHeaderAccount(brlogin, account, target_domain);
            } catch (final PluginException e) {
                /* 2020-05-20: Never delete login cookies! */
                // if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                // account.clearCookies(target_domain);
                // }
                throw e;
            }
        }
    }

    private void handleExternalLoginStep(final Browser brlogin, final Account account, final String target_domain) throws Exception {
        String autologinURL = this.getStringPropertyAccount(account, target_domain, PROPERTY_url_external_login, null);
        if (autologinURL == null) {
            logger.warning("Property autologinURL is null");
            return;
        }
        if (autologinURL.startsWith("/")) {
            autologinURL = "https://ppp.contentdef.com/" + autologinURL;
        }
        getPage(brlogin, autologinURL);
        final String redirectURL = brlogin.getRegex("window\\.top\\.location\\s*=\\s*\\'(https?://[^<>\"\\']+)").getMatch(0);
        if (redirectURL == null) {
            logger.warning("Failed to find external login redirectURL");
            return;
        }
        /* This is already the final step for non-pornportal sites */
        getPage(brlogin, redirectURL);
        /* This is usually the final step for pornportal sites */
        final Form probillerForm = brlogin.getFormbyActionRegex(".+access/success.*?");
        if (probillerForm != null) {
            logger.info("Found proBiller Form");
            brlogin.submitForm(probillerForm);
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
            throw new AccountInvalidException();
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
            entries = (Map<String, Object>) entries.get("domain");
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
        final String json = br.getRegex("window\\.__JUAN\\.rawInstance = (\\{.*?\\});\n").getMatch(0);
        return JSonStorage.restoreFromString(json, TypeRef.MAP);
    }

    public static Map<String, Object> getJsonJuanInitialState(final Browser br) {
        final String json = br.getRegex("window\\.__JUAN\\.initialState = (\\{.*?\\});\\s+").getMatch(0);
        return JSonStorage.restoreFromString(json, TypeRef.MAP);
    }

    private static final String getDefaultCookieNameLogin() {
        /* 2020-04-03 */
        return "access_token_ma";
    }

    public static final String getDefaultCookieNameInstance() {
        /* 2020-04-03 */
        return "instance_token";
    }

    /* Sets headers required to do API requests. */
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
                account.setConcurrentUsePossible(true);
                ai.setUnlimitedTraffic();
                if (br.getURL() == null || !br.getURL().contains("/v1/self")) {
                    getPage(br, getAPIBase() + "/self");
                }
                final Map<String, Object> user = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                final String joinDate = (String) user.get("joinDate");
                if (!StringUtils.isEmpty(joinDate)) {
                    ai.setCreateTime(TimeFormatter.getMilliSeconds(joinDate, "yyyy'-'MM'-'dd'T'HH':'mm':'ss", null));
                }
                if (Boolean.TRUE.equals(user.get("isBanned"))) {
                    /*
                     * 2021-11-08: This may randomly be "true" (also via website) although the account is definitely not banned! Tested with
                     * a brazzers.com account. --> Ignore this for now!
                     */
                    final boolean trustBannedFlag = false;
                    if (trustBannedFlag) {
                        throw new AccountInvalidException("Account banned for reason: " + user.get("banReason"));
                    } else {
                        logger.info("Account might be banned??");
                    }
                }
                final Boolean isExpired = (Boolean) user.get("isExpired");
                final Boolean isTrial = (Boolean) user.get("isTrial");
                final Boolean isCanceled = (Boolean) user.get("isCanceled");
                final Number initialAmount = (Number) user.get("initialAmount");
                boolean foundValidExpireDate = false;
                if (Boolean.TRUE.equals(isExpired)) {
                    account.setType(AccountType.FREE);
                    /* Free accounts can be used to download trailers */
                    ai.setStatus("Free Account (expired premium)");
                } else if (Boolean.TRUE.equals(isTrial)) {
                    /* Free trial -> Free Account with premium capability */
                    account.setType(AccountType.PREMIUM);
                    ai.setStatus("Free Account (Trial)");
                } else if (initialAmount == null || initialAmount.longValue() <= 0) {
                    /* Free user who never (?) had a premium subscription. */
                    account.setType(AccountType.FREE);
                } else {
                    /* Premium account [well...most likely] */
                    /**
                     * Premium accounts must not have any expire-date! </br>
                     * 2021-06-05: Only set expire-date if it is still valid. Premium accounts are premium as long as "isExpired" != true.
                     */
                    account.setType(AccountType.PREMIUM);
                    if (Boolean.TRUE.equals(isCanceled)) {
                        ai.setStatus("Premium Account (subscription cancelled)");
                    } else {
                        ai.setStatus("Premium Account (subscription running)");
                    }
                    final String expiryDate = (String) user.get("expiryDate");
                    if (!StringUtils.isEmpty(expiryDate)) {
                        final long expireTimestamp = TimeFormatter.getMilliSeconds(expiryDate, "yyyy'-'MM'-'dd'T'HH':'mm':'ss", null);
                        if (expireTimestamp > System.currentTimeMillis()) {
                            ai.setValidUntil(expireTimestamp, br);
                            foundValidExpireDate = true;
                        }
                    }
                    final List<Map<String, Object>> bundles = (List<Map<String, Object>>) user.get("addons");
                    if (!foundValidExpireDate && bundles != null) {
                        /**
                         * Try to find alternative expire-date inside users' additional purchased "bundles". </br>
                         * Each bundle can have different expire-dates and also separate pricing and so on.
                         */
                        logger.info("Looking for alternative expiredate");
                        long highestExpireTimestamp = -1;
                        String titleOfBundleWithHighestExpireDate = null;
                        for (final Map<String, Object> bundle : bundles) {
                            if (!(Boolean) bundle.get("isActive")) {
                                continue;
                            }
                            final String expireDateStrTmp = (String) bundle.get("expirationDate");
                            final long expireTimestampTmp = TimeFormatter.getMilliSeconds(expireDateStrTmp, "yyyy'-'MM'-'dd'T'HH':'mm':'ss", null);
                            if (expireTimestampTmp > highestExpireTimestamp) {
                                highestExpireTimestamp = expireTimestampTmp;
                                titleOfBundleWithHighestExpireDate = (String) bundle.get("title");
                            }
                        }
                        if (highestExpireTimestamp > System.currentTimeMillis()) {
                            logger.info("Successfully found alternative expiredate");
                            ai.setValidUntil(highestExpireTimestamp, br);
                            if (!StringUtils.isEmpty(titleOfBundleWithHighestExpireDate)) {
                                ai.setStatus(ai.getStatus() + " [" + titleOfBundleWithHighestExpireDate + "]");
                            }
                        } else {
                            logger.info("Failed to find alternative expiredate");
                        }
                    }
                    /* Now check which other websites we can now use as well and add them via multihoster handling. */
                    try {
                        getPage(br, "https://site-ma." + this.getHost() + "/");
                        final Map<String, Object> initialState = getJsonJuanInitialState(br);
                        final Map<String, Object> client = (Map<String, Object>) initialState.get("client");
                        final String userAgent = (String) client.get("userAgent");
                        final String domain = (String) client.get("domain");
                        final String ip = (String) client.get("ip");
                        final String baseUrl = (String) client.get("baseUrl");
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
                        final Map<String, Object> pornportalresponse = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                        final String data = pornportalresponse.get("data").toString();
                        getPage(br, "https://ppp.contentdef.com/postlogin?data=" + Encoding.urlEncode(data));
                        /*
                         * We can authorize ourselves to these other portals through these URLs. Sadly we do not get the full domains before
                         * accessing these URLs but this would take a lot of time which is why we will try to find the full domains here
                         * without accessing these URLs.
                         */
                        final String[] autologinURLs = br.getRegex("(/autologin/[a-z0-9]+\\?sid=[^\"]+)\"").getColumn(0);
                        final String sid = UrlQuery.parse("https://" + this.getHost() + autologinURLs[0]).get("sid");
                        final Browser brContentdef = br.cloneBrowser();
                        getPage(brContentdef, String.format("https://ppp.contentdef.com/notification/list?page=1&type=1&network=1&archived=0&ajaxCounter=1&sid=%s&data=%s&_=%d", sid, Encoding.urlEncode(data), System.currentTimeMillis()));
                        final Map<String, Object> entries = restoreFromString(brContentdef.getRequest().getHtmlCode(), TypeRef.MAP);
                        final List<Object> notificationNetworks = (List<Object>) entries.get("notificationNetworks");
                        final ArrayList<String> supportedHostsTmp = new ArrayList<String>();
                        final ArrayList<String> allowedHosts = getAllSupportedPluginDomainsFlat();
                        final ArrayList<String> blacklistedHosts = getAllBlacklistedDomains();
                        final ArrayList<String> allowedHostsSpecial = getPossibleExternalSupportedSitesThatCannotBeHandledByPornPortal();
                        final ArrayList<String> supportedHostsFinal = new ArrayList<String>();
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
                                    domainWithoutTLD = domainWithoutTLD.replaceAll("( |\\')", "");
                                }
                            }
                            /* Find full domain for shortcode */
                            String domainFull = null;
                            for (final Object notificationNetworkO : notificationNetworks) {
                                final Map<String, Object> notificationNetwork = (Map<String, Object>) notificationNetworkO;
                                final String domainShortcodeTmp = (String) notificationNetwork.get("short_name");
                                final String site_url = (String) notificationNetwork.get("site_url");
                                if (StringUtils.isEmpty(domainShortcodeTmp) || StringUtils.isEmpty(site_url)) {
                                    /* Skip invalid items */
                                    continue;
                                } else if (domainShortcodeTmp.equals(domainShortcode)) {
                                    domainFull = site_url;
                                    break;
                                }
                            }
                            final String domainToAdd;
                            if (domainFull != null) {
                                domainToAdd = Browser.getHost(domainFull, false);
                            } else {
                                domainToAdd = domainWithoutTLD;
                            }
                            if (domainToAdd == null) {
                                logger.warning("Failed to find any usable domain for domain: " + domainShortcode);
                                continue;
                            }
                            supportedHostsTmp.clear();
                            supportedHostsTmp.add(domainToAdd);
                            ai.setMultiHostSupport(this, supportedHostsTmp);
                            final List<String> supportedHostsTmpReal = ai.getMultiHostSupport();
                            if (supportedHostsTmpReal == null || supportedHostsTmpReal.isEmpty()) {
                                logger.info("Failed to find any real host for: " + domainToAdd);
                                continue;
                            }
                            final String final_domain = supportedHostsTmpReal.get(0);
                            if (!allowedHosts.contains(final_domain) && !allowedHostsSpecial.contains(final_domain)) {
                                logger.info("Skipping the following host as it is not an allowed/PornPortal host or an external/unsupported host: " + final_domain);
                                continue;
                            } else if (blacklistedHosts.contains(final_domain)) {
                                /* Skip blacklisted entries */
                                continue;
                            } else if (supportedHostsFinal.contains(final_domain)) {
                                /* Avoid duplicates */
                                continue;
                            }
                            supportedHostsFinal.add(final_domain);
                            this.setPropertyAccount(account, final_domain, PROPERTY_url_external_login, autologinURL);
                        }
                        /* Remove current host - we do not want that in our list of supported hosts! */
                        supportedHostsFinal.remove(this.getHost());
                        ai.setMultiHostSupport(this, supportedHostsFinal);
                        try {
                            final String domain_pornhub = "pornhub.com";
                            Account pornhubAccount = findSpecialPornhubAccount(account);
                            if (supportedHostsFinal.contains(domain_pornhub)) {
                                /* Special pornhub handling --> Add dummy account if external login works */
                                final Browser br2 = PornHubCom.prepBr(this.createNewBrowserInstance());
                                handleExternalLoginStep(br2, account, domain_pornhub);
                                final boolean isLoggedIN = PornHubCom.isLoggedInHtmlPremium(br2);
                                /* Look for special account created by this plugin --> Add account if non existant */
                                final String targetUsername = this.getHost() + "_" + account.getUser();
                                if (!isLoggedIN) {
                                    logger.info("Pornhub external login failed");
                                    if (pornhubAccount != null) {
                                        logger.info("Mark existing pornhub account as expired");
                                        pornhubAccount.getAccountInfo().setExpired(true);
                                    }
                                } else {
                                    /*
                                     * TODO: Maybe synchronize account stuff --> Should not be required this- and the special account will
                                     * never be checked at the same time (?)
                                     */
                                    logger.info("Pornhub external login successful");
                                    boolean addNewAccount = false;
                                    final PluginForHost pornhubPlugin = getNewPluginForHostInstance(domain_pornhub);
                                    if (pornhubAccount == null) {
                                        /* Adds account if non existant */
                                        logger.info("Failed to find special pornhub account --> Creating it");
                                        pornhubAccount = new Account(targetUsername, "123456");
                                        pornhubAccount.setPlugin(pornhubPlugin);
                                        addNewAccount = true;
                                        /* TODO: Why does this not work? */
                                        // AccountController.getInstance().addAccount(pornhubAccount);
                                    }
                                    pornhubAccount.setProperty(PornHubCom.PROPERTY_ACCOUNT_is_cookie_login_only, true);
                                    pornhubAccount.setEnabled(true);
                                    pornhubAccount.setType(AccountType.PREMIUM);
                                    final AccountInfo pornhubAI = new AccountInfo();
                                    pornhubAI.setUnlimitedTraffic();
                                    pornhubAI.setStatus("Premium via " + this.getHost());
                                    if (ai.getValidUntil() != -1) {
                                        /* Set expiredate of current account on this special account as well */
                                        pornhubAI.setValidUntil(ai.getValidUntil());
                                    }
                                    pornhubAccount.setAccountInfo(pornhubAI);
                                    /*
                                     * Set a really long refresh timeout on this account so that it does not e.g. get checked when all other
                                     * accounts get checked. After all it will get checked by our pornportal plugin instance.
                                     */
                                    pornhubAccount.setRefreshTimeout(24 * 60 * 60 * 1000l);
                                    /*
                                     * Set pornhubpremium cookies with a new browser instance. Then update pornhub cookies each time, the
                                     * main account of this plugin gets refreshed.
                                     */
                                    savePornhubCookies(this, br2, pornhubAccount);
                                    if (addNewAccount) {
                                        AccountController.getInstance().addAccount(pornhubPlugin, pornhubAccount);
                                    }
                                }
                            } else if (pornhubAccount != null) {
                                logger.info("Pornhub was supported but is not supported anymore --> Removing special account");
                                AccountController.getInstance().removeAccount(pornhubAccount);
                            }
                        } catch (final Throwable ignore) {
                            logger.log(ignore);
                            logger.info("Exception occured in special pornhub handling");
                        }
                    } catch (final Throwable ignore) {
                        logger.log(ignore);
                        logger.warning("Internal Multihoster handling failed");
                    }
                }
                return ai;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    public static void savePornhubCookies(final Plugin plg, final Browser br, final Account acc) {
        boolean successFree = false;
        for (final String domain : PornHubCom.domainsFree) {
            final Cookies cookies = br.getCookies(domain);
            if (cookies != null && !cookies.isEmpty()) {
                acc.saveCookies(cookies, PornHubCom.COOKIE_ID_FREE);
                successFree = true;
                break;
            }
        }
        if (!successFree) {
            plg.getLogger().info("Failed to find any FREE pornhub cookies to save");
        }
        boolean successPremium = false;
        for (final String domain : PornHubCom.domainsPremium) {
            final Cookies cookies = br.getCookies(domain);
            if (cookies != null && !cookies.isEmpty()) {
                acc.saveCookies(cookies, PornHubCom.COOKIE_ID_PREMIUM);
                successPremium = true;
                break;
            }
        }
        if (!successPremium) {
            plg.getLogger().info("Failed to find any PREMIUM pornhub cookies to save");
        }
    }

    public static Account findSpecialPornhubAccount(final Account sourceAccount) {
        final String domain_pornhub = "pornhub.com";
        final String targetUsername = sourceAccount.getHoster() + "_" + sourceAccount.getUser();
        List<Account> pornhubAccounts = AccountController.getInstance().getValidAccounts(domain_pornhub);
        Account pornhubAccount = null;
        if (pornhubAccounts != null) {
            for (final Account pornhubAccountTmp : pornhubAccounts) {
                final String usernameTmp = pornhubAccountTmp.getUser();
                if (usernameTmp.equalsIgnoreCase(targetUsername)) {
                    pornhubAccount = pornhubAccountTmp;
                    break;
                }
            }
        }
        return pornhubAccount;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleDownload(link, account);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account, true);
        final String dllink = link.getStringProperty(PROPERTY_directurl);
        if (StringUtils.isEmpty(dllink)) {
            /* This should never happen! */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Final downloadurl did not lead to file");
        }
        dl.startDownload();
    }

    @Override
    public Class<? extends PornportalComConfig> getConfigInterface() {
        return PornportalComConfig.class;
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