package jd.plugins.hoster;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.Time;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.components.config.PluralsightComConfig;
import org.jdownloader.plugins.components.config.PluralsightComConfig.WaitMode;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;
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
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.decrypter.PluralsightComDecrypter;

/**
 *
 * @author Neokyuubi
 *
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 1, names = { "pluralsight.com" }, urls = { "https://app\\.pluralsight\\.com/course-player\\?clipId=[a-f0-9\\-]+" })
public class PluralsightCom extends antiDDoSForHost {
    private static WeakHashMap<Account, List<Long>> map100PerHour                            = new WeakHashMap<Account, List<Long>>();
    private static WeakHashMap<Account, List<Long>> map200Per4Hours                          = new WeakHashMap<Account, List<Long>>();
    public static final String                      PROPERTY_DURATION_SECONDS                = "duration";
    public static final String                      PROPERTY_CLIP_ID                         = "clipID";
    public static final String                      PROPERTY_CLIP_VERSION                    = "version";
    public static final String                      PROPERTY_MODULE_ORDER_ID                 = "module";
    public static final String                      PROPERTY_CLIP_ORDER_ID                   = "module_clip_order_id";
    @Deprecated
    public static final String                      PROPERTY_ORDERING                        = "ordering";
    @Deprecated
    public static final String                      PROPERTY_SUPPORTS_WIDESCREEN_FORMATS     = "supportsWideScreenVideoFormats";
    public static final String                      PROPERTY_TYPE                            = "type";
    public static final String                      PROPERTY_FORCED_RESOLUTION               = "forced_resolution";
    public static final String                      PROPERTY_DIRECTURL                       = "directurl";
    public static final String                      TYPE_SUBTITLE                            = "subtitle";
    /* Packagizer properties */
    public static final String                      PROPERTY_MODULE_TITLE                    = "module_title";
    public static final String                      PROPERTY_MODULE_CLIP_TITLE               = "module_clip_title";
    private final String                            PROPERTY_ACCOUNT_COOKIE_LOGIN_HINT_SHOWN = "cookie_login_hint_shown";
    public static final String                      WEBSITE_BASE_APP                         = "https://app.pluralsight.com";
    private static final AtomicLong                 timestampLastDownloadStarted             = new AtomicLong(0);
    private static final AtomicInteger              durationSecondsOfLastDownloadedClip      = new AtomicInteger(0);

    public PluralsightCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.pluralsight.com/pricing");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.VIDEO_STREAMING, LazyPlugin.FEATURE.COOKIE_LOGIN_OPTIONAL };
    }

    @Override
    public String getAGBLink() {
        return "https://www.pluralsight.com/terms";
    }

    public static Browser prepBR(final Browser br) {
        final PluralsightComConfig cfg = PluginJsonConfig.get(PluralsightComConfig.class);
        final String customUserAgent = cfg.getUserAgent();
        if (!StringUtils.isEmpty(customUserAgent) && !StringUtils.equalsIgnoreCase(customUserAgent, "JDDEFAULT")) {
            br.getHeaders().put("User-Agent", customUserAgent);
        }
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        if (link == null) {
            return null;
        } else {
            return "pluralsight://" + link.getStringProperty(PROPERTY_CLIP_ID) + "_" + link.getStringProperty(PROPERTY_CLIP_VERSION) + "_" + link.getStringProperty(PROPERTY_TYPE);
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        login(account, true);
        // Old API request for obtaining subscription data: /web-analytics/api/v1/users/current
        getRequest(br, this, br.createGetRequest(WEBSITE_BASE_APP + "/subscription-lifecycle/api/bootstrap"));
        final Map<String, Object> root = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        List<Map<String, Object>> subscriptions = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(root, "data/applicationData/subscriptionAccount/subscriptions");
        if (subscriptions == null) {
            subscriptions = (List<Map<String, Object>>) root.get("subscriptions");
        }
        final AccountInfo ai = new AccountInfo();
        if (subscriptions == null) {
            account.setType(AccountType.UNKNOWN);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Something went wrong with account verification type.");
        }
        boolean isPremium = false;
        /* Check if this is a premium account by looking through all subscription packages. */
        for (final Map<String, Object> subscription : subscriptions) {
            final Object expiresAtDateO = subscription.get("termEndDate");
            final String expiresAtDateStr = expiresAtDateO != null ? expiresAtDateO.toString() : null;
            if (expiresAtDateStr != null) {
                final long validUntil = TimeFormatter.getMilliSeconds(expiresAtDateStr.replace("Z", "+0000"), "yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ENGLISH);
                if (validUntil > System.currentTimeMillis()) {
                    isPremium = true;
                    account.setType(AccountType.PREMIUM);
                    ai.setStatus("Premium Account | Auto renew: " + subscription.get("autoRenew"));
                    ai.setValidUntil(validUntil, br);
                    break;
                }
            }
        }
        if (!isPremium) {
            account.setType(AccountType.FREE);
            ai.setStatus("Free (Expired) Account");
        }
        account.setMaxSimultanDownloads(1);
        account.setConcurrentUsePossible(true);
        return ai;
    }

    public void login(final Account account, final boolean revalidate) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                prepBR(br);
                final Cookies cookies = account.loadCookies("");
                /* 2022-02-16: Added cookie login as possible workaround for login issues caused by Cloudflare */
                final Cookies userCookies = account.loadUserCookies();
                if (cookies != null || userCookies != null) {
                    if (userCookies != null) {
                        br.setCookies(userCookies);
                    } else {
                        br.setCookies(cookies);
                    }
                    if (!revalidate) {
                        /* Do not validate cookies */
                        return;
                    } else {
                        logger.info("Attempting cookie login");
                        getRequest(br, this, br.createGetRequest(WEBSITE_BASE_APP + "/web-analytics/api/v1/users/current"));
                        final Request request = br.getRequest();
                        if (request.getHttpConnection().getResponseCode() != 200 || !StringUtils.containsIgnoreCase(request.getHttpConnection().getContentType(), "json")) {
                            /* Full login required */
                            logger.info("Cookie login failed");
                            br.clearCookies(null);
                            if (userCookies != null) {
                                if (account.hasEverBeenValid()) {
                                    throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                                } else {
                                    throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                                }
                            }
                        } else {
                            logger.info("Cookie login successful");
                            if (userCookies == null) {
                                account.saveCookies(br.getCookies(this.getHost()), "");
                            }
                            return;
                        }
                    }
                }
                try {
                    logger.info("Performing full login");
                    getRequest(br, this, br.createGetRequest(WEBSITE_BASE_APP + "/id/"));
                    final Form form = br.getFormbyKey("Username");
                    if (form == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final boolean isCaptchaVisible = br.getRegex("<input\\s+id=\"ReCaptchaSiteKey\"\\s+[\\w\\s\\d=\"]+(type=\"hidden\")[\\w\\s\\d=\"]+\\/>").getMatch(0) == null;
                    if (br.containsHTML("ReCaptchaSiteKey") && isCaptchaVisible) {
                        final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, "6LeVIgoTAAAAAIhx_TOwDWIXecbvzcWyjQDbXsaV").getToken();
                        form.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                    }
                    form.put("Username", URLEncoder.encode(account.getUser(), "UTF-8"));
                    form.put("Password", URLEncoder.encode(account.getPass(), "UTF-8"));
                    getRequest(br, this, br.createFormRequest(form));
                    if (br.containsHTML("(?i)>\\s*Invalid user name or password\\s*<")) {
                        throw new AccountInvalidException("Invalid user name or password");
                    } else if (br.getHostCookie("PsJwt-production", Cookies.NOTDELETEDPATTERN) == null) {
                        throw new AccountInvalidException();
                    }
                    getRequest(br, this, br.createGetRequest(WEBSITE_BASE_APP + "/web-analytics/api/v1/users/current"));
                    final Request request = br.getRequest();
                    if (request.getHttpConnection().getResponseCode() == 401) {
                        throw new AccountInvalidException();
                    } else if (request.getHttpConnection().getResponseCode() == 429) {
                        throw new AccountUnavailableException("Unfortunately the site is currently unavilable. We expect everything back in order shortly. If you continue to experience problems, let us know.", 5 * 60 * 1000);
                    } else if (request.getHttpConnection().getResponseCode() != 200) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    account.saveCookies(br.getCookies(this.getHost()), "");
                } catch (final Exception e) {
                    boolean displayCookieLoginHint = false;
                    if (e instanceof AccountInvalidException) {
                        displayCookieLoginHint = true;
                    } else if (e instanceof PluginException) {
                        if (((PluginException) e).getLinkStatus() == LinkStatus.ERROR_PLUGIN_DEFECT) {
                            displayCookieLoginHint = true;
                        }
                    }
                    if (displayCookieLoginHint) {
                        /* Tell user to try again using cookie login method. Only do this once per account. */
                        if (!account.hasProperty(PROPERTY_ACCOUNT_COOKIE_LOGIN_HINT_SHOWN)) {
                            showCookieLoginInfo();
                            account.setProperty(PROPERTY_ACCOUNT_COOKIE_LOGIN_HINT_SHOWN, true);
                        }
                        throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_required());
                    } else {
                        throw e;
                    }
                }
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    public boolean antiAccountBlockProtection(final Account account) {
        final boolean check1 = antiAccountBlockProtection(account, map100PerHour, 50, 60 * 60 * 1000l);
        final boolean check2 = antiAccountBlockProtection(account, map200Per4Hours, 200, 4 * 60 * 60 * 1000l);
        return check1 || check2;
    }

    public boolean antiAccountBlockProtection(final Account account, final Map<Account, List<Long>> map, final int maxWindow, final long window) {
        synchronized (map) {
            List<Long> list = map.get(account);
            if (list == null) {
                list = new ArrayList<Long>();
                map.put(account, list);
            }
            final long now = System.currentTimeMillis();
            list.add(now);
            if (list.size() > maxWindow) {
                final Iterator<Long> it = list.iterator();
                while (it.hasNext()) {
                    final Long next = it.next();
                    if (now - next.longValue() > window) {
                        it.remove();
                    }
                }
            }
            return list.size() > maxWindow;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        if (account != null) {
            if (antiAccountBlockProtection(account)) {
                throw new AccountUnavailableException("Account block protection, please wait!", 5 * 60 * 1000l);
            }
            login(account, false);
            return fetchFileInformation(link, account, false);
        } else {
            return fetchFileInformation(link, null, false);
        }
    }

    public static enum QUALITY {
        HIGH_WIDESCREEN(1280, 720),
        HIGH(1024, 768),
        MEDIUM(848, 640),
        LOW(640, 480);

        private final int x;
        private final int y;

        private QUALITY(final int x, final int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        @Override
        public String toString() {
            return getX() + "x" + getY();
        }
    }

    public static String getFileExtension(final DownloadLink link) {
        if (isSubtitle(link)) {
            return ".webvtt";
        } else {
            return ".mp4";
        }
    }

    public static boolean isSubtitle(final DownloadLink link) {
        if (StringUtils.equalsIgnoreCase(link.getStringProperty(PROPERTY_TYPE), TYPE_SUBTITLE)) {
            return true;
        } else {
            return false;
        }
    }

    public static void setFinalFilename(final DownloadLink link) {
        final int modulePosition = link.getIntegerProperty(PROPERTY_MODULE_ORDER_ID, -1);
        final int clipPosition = link.getIntegerProperty(PROPERTY_CLIP_ORDER_ID, -1);
        final String moduleTitle = link.getStringProperty(PROPERTY_MODULE_TITLE);
        final String title = link.getStringProperty(PROPERTY_MODULE_CLIP_TITLE);
        String fullName = String.format("%02d", modulePosition) + "-" + String.format("%02d", clipPosition) + " - " + moduleTitle + " -- " + title;
        link.setFinalFileName(fullName + getFileExtension(link));
    }

    private String getDirecturl(final Browser br, final DownloadLink link) throws Exception {
        final String storedDirecturl = link.getStringProperty(PROPERTY_DIRECTURL);
        if (storedDirecturl != null) {
            return storedDirecturl;
        } else if (!isSubtitle(link)) {
            return this.getStreamURL(br, link, null);
        } else {
            return null;
        }
    }

    /** Returns video streaming URL for downloading. */
    private String getStreamURL(final Browser br, final DownloadLink link, QUALITY quality) throws Exception {
        /* 2020-04-21: Try and error. Browser does the same lol */
        final String[] resolutions = new String[] { "1280x720", "1024x768" };
        List<Map<String, Object>> urls = null;
        /* Re-use previously working resolution in case there is one. */
        String existent_resolution = link.getStringProperty(PROPERTY_FORCED_RESOLUTION);
        for (String resolution : resolutions) {
            if (existent_resolution != null) {
                logger.info("Override quality-check of " + resolution + " with " + existent_resolution);
                resolution = existent_resolution;
            } else {
                logger.info("Checking resolution: " + resolution);
            }
            final String clipID = link.getStringProperty(PROPERTY_CLIP_ID);
            final String version = link.getStringProperty(PROPERTY_CLIP_VERSION);
            if (StringUtils.isEmpty(clipID)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (quality == null) {
                quality = link.getBooleanProperty(PROPERTY_SUPPORTS_WIDESCREEN_FORMATS, false) ? QUALITY.HIGH_WIDESCREEN : QUALITY.HIGH;
            }
            final Map<String, Object> params = new HashMap<String, Object>();
            final boolean useAPIv3 = true;
            final PostRequest request;
            boolean resolutionExists = false;
            if (useAPIv3) {
                params.put("boundedContext", "course");
                params.put("clipId", clipID);
                params.put("mediaType", "mp4");
                params.put("quality", resolution);
                params.put("online", true);
                if (version != null) {
                    params.put("versionId", version);
                } else {
                    params.put("versionId", "");
                }
                request = br.createJSonPostRequest(WEBSITE_BASE_APP + "/video/clips/v3/viewclip", params);
                request.setContentType("application/json");
                request.getHeaders().put("x-team", "video-services");
                request.getHeaders().put("Origin", WEBSITE_BASE_APP);
                request.getHeaders().put("Referer", link.getPluginPatternMatcher());
                getRequest(br, this, request);
                if (br.getHttpConnection().getResponseCode() == 403) {
                    throw new AccountRequiredException();
                }
                /*
                 * 2020-04-21: E.g.
                 * {"success":false,"error":{"message":"1280x720.mp4 encoding not found"},"meta":{"statusCode":404},"trace":[{"service":
                 * "videoservices_clip","version":"1.0.450","latency":29,"fn":"viewClipV3"}]}
                 */
                if (br.containsHTML(resolution + ".mp4 encoding not found")) {
                    resolutionExists = false;
                } else {
                    final Map<String, Object> response = restoreFromString(br.toString(), TypeRef.MAP);
                    urls = (List<Map<String, Object>>) response.get("urls");
                    existent_resolution = resolution;
                    logger.info("Found working resolution: " + resolution);
                }
            } else {
                /* Old handling */
                final UrlQuery urlParams = UrlQuery.parse(link.getPluginPatternMatcher());
                final String author = urlParams.get("author");
                final String course = urlParams.get("course");
                final String clip = urlParams.get("clip");
                if (StringUtils.isEmpty(author) || StringUtils.isEmpty(course) || StringUtils.isEmpty(clip)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                params.put("query", "query viewClip { viewClip(input: { author: \"" + author + "\", clipIndex: " + clip + ", courseName: \"" + course + "\", includeCaptions: false, locale: \"en\", mediaType: \"mp4\", moduleName: \"" + urlParams.get("name") + "\" , quality: \"" + quality + "\"}) { urls { url cdn rank source }, status } }");
                params.put("variables", "{}");
                request = br.createJSonPostRequest("https://app.pluralsight.com/player/api/graphql", params);
                request.setContentType("application/json;charset=UTF-8");
                request.getHeaders().put("Origin", "https://app.pluralsight.com");
                getRequest(br, this, request);
                final Map<String, Object> response = restoreFromString(br.toString(), TypeRef.MAP);
                urls = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(response, "data/viewClip/urls");
                existent_resolution = resolution;
                break;
            }
            if (!resolutionExists) {
                logger.info("Resolution does not exist: " + resolution);
                if (existent_resolution != null) {
                    logger.info("Forced resolution is given, stopping anyways at: " + resolution);
                    break;
                }
                continue;
            }
            logger.info("Found working resolution: " + resolution);
            break;
        }
        if (existent_resolution != null) {
            link.setProperty(PROPERTY_FORCED_RESOLUTION, existent_resolution);
            if (urls != null) {
                for (final Map<String, Object> url : urls) {
                    final String streamURL = (String) url.get("url");
                    if (StringUtils.isNotEmpty(streamURL) && !StringUtils.containsIgnoreCase(streamURL, "expiretime=")) {
                        return streamURL;
                    }
                }
            }
        }
        return null;
    }

    private static Object WAITLOCK = new Object();

    public static Request getRequest(Browser br, Plugin plugin, Request request) throws Exception {
        return getRequest(br, plugin, request, 45 * 1000);
    }

    private static AtomicBoolean thresholdInitialized = new AtomicBoolean(false);

    public static Request getRequest(Browser br, Plugin plugin, Request request, long waitMax) throws Exception {
        if (thresholdInitialized.compareAndSet(false, true)) {
            final Random random = new Random();
            // https://board.jdownloader.org/showthread.php?t=84120
            Browser.setRequestIntervalLimitGlobal(plugin.getHost(), 10000 - random.nextInt(2000));
        }
        synchronized (WAITLOCK) {
            getRequest(plugin, br, request);
            while (waitMax > 0) {
                if (request.getHttpConnection().getResponseCode() == 429 || (StringUtils.containsIgnoreCase(request.getHttpConnection().getContentType(), "json") && new Regex(request.getHtmlCode(), "\"status\"\\s*:\\s*429").matches())) {
                    Thread.sleep(15000);
                    waitMax -= 15000;
                    getRequest(plugin, br, request);
                } else {
                    break;
                }
            }
        }
        return request;
    }

    public static void getRequest(Plugin plugin, Browser br, Request request) throws Exception {
        if (plugin instanceof PluralsightCom) {
            ((PluralsightCom) plugin).sendRequest(br, request);
        } else if (plugin instanceof PluralsightComDecrypter) {
            ((PluralsightComDecrypter) plugin).sendRequest(br, request);
        }
    }

    @Override
    public void sendRequest(Browser ibr, Request request) throws Exception {
        super.sendRequest(ibr, request);
    }

    @Deprecated
    public static Request getClips(final Browser br, final Plugin plugin, final String courseSlug) throws Exception {
        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("query", "query  BootstrapPlayer  {  rpc  {   bootstrapPlayer  {  profile  {   firstName   lastName   email   username   userHandle   authed   isAuthed   plan  }  course(courseId:  \"" + courseSlug + "\")  {   name   title   courseHasCaptions   translationLanguages  {   code   name   }   supportsWideScreenVideoFormats   timestamp   modules  {   name   title   duration   formattedDuration   author   authorized   clips  {    authorized   clipId    duration   formattedDuration   id   index   moduleIndex   moduleTitle   name   title   watched   }   }  }   }  }}");
        params.put("variables", "{}");
        final PostRequest request = br.createPostRequest("https://app.pluralsight.com/player/api/graphql", JSonStorage.toString(params));
        request.setContentType("application/json;charset=UTF-8");
        request.getHeaders().put("Origin", "https://app.pluralsight.com");
        return getRequest(br, plugin, request);
    }

    private AvailableStatus fetchFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        final String storedDirecturl = link.getStringProperty(PROPERTY_DIRECTURL);
        if (storedDirecturl != null) {
            final Request checkStream = getRequest(br, this, br.createHeadRequest(storedDirecturl));
            final URLConnectionAdapter con = checkStream.getHttpConnection();
            try {
                if (looksLikeDownloadableContent(con)) {
                    logger.info("Availablecheck via stored directurl successful");
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    return AvailableStatus.TRUE;
                } else {
                    logger.info("Availablecheck via stored directurl failed");
                    resetDirecturl(link);
                }
            } finally {
                con.disconnect();
            }
        }
        if (isDownload) {
            return AvailableStatus.UNCHECKABLE;
        } else if (link.getKnownDownloadSize() == -1) {
            /* Only check for filesize if none has been set yet. */
            logger.info("Looking for filesize");
            final String directurl = this.getDirecturl(br, link);
            if (!StringUtils.isEmpty(directurl)) {
                final Request req = getRequest(br, this, br.createHeadRequest(directurl));
                final URLConnectionAdapter con = req.getHttpConnection();
                try {
                    if (looksLikeDownloadableContent(con)) {
                        if (con.getCompleteContentLength() > 0) {
                            link.setVerifiedFileSize(con.getCompleteContentLength());
                        }
                        link.setProperty(PROPERTY_DIRECTURL, directurl);
                        return AvailableStatus.TRUE;
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                } finally {
                    con.disconnect();
                }
            }
            return AvailableStatus.UNCHECKABLE;
        } else {
            return AvailableStatus.TRUE;
        }
    }

    private void resetDirecturl(final DownloadLink link) {
        if (!isSubtitle(link)) {
            link.removeProperty(PROPERTY_DIRECTURL);
        }
    }

    @Override
    protected boolean looksLikeDownloadableContent(final URLConnectionAdapter urlConnection) {
        if (super.looksLikeDownloadableContent(urlConnection) && urlConnection.getCompleteContentLength() > 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        downloadStream(link, null);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        downloadStream(link, account);
    }

    private void downloadStream(final DownloadLink link, final Account account) throws Exception {
        /** 2020-02-17: Wait between download-starts according to: https://board.jdownloader.org/showthread.php?t=82533 */
        final PluralsightComConfig cfg = PluginJsonConfig.get(PluralsightComConfig.class);
        final WaitMode mode = cfg.getWaitMode();
        long waitMillisBetweenDownloads;
        if (mode == WaitMode.CUSTOM_WAIT) {
            waitMillisBetweenDownloads = cfg.getWaittimeBetweenDownloadsSeconds() * 1000;
        } else if (mode == WaitMode.LENGTH_OF_PREVIOUSLY_DOWNLOADED_VIDEO) {
            waitMillisBetweenDownloads = durationSecondsOfLastDownloadedClip.get() * 1000;
        } else {
            waitMillisBetweenDownloads = durationSecondsOfLastDownloadedClip.get() * 1000 + cfg.getWaittimeBetweenDownloadsSeconds() * 1000;
        }
        if (cfg.isAddRandomDelaySecondsBetweenDownloads()) {
            waitMillisBetweenDownloads = new Random().nextInt(cfg.getAdditionalWaittimeBetweenDownloadsMaxSeconds()) * 1000;
        }
        final long passedTimeMillisSinceLastDownload = Time.systemIndependentCurrentJVMTimeMillis() - timestampLastDownloadStarted.get();
        if (passedTimeMillisSinceLastDownload < waitMillisBetweenDownloads) {
            this.sleep(waitMillisBetweenDownloads - passedTimeMillisSinceLastDownload, link);
        }
        final boolean resume = true;
        /*
         * More possible but let's limit it to 1 as this website doesn't like users establishing a lot of connections at the same/in a short
         * time.
         */
        final int maxchunks = 1;
        if (!attemptStoredDownloadurlDownload(link, resume, maxchunks)) {
            logger.info("Generating fresh directurl");
            if (isSubtitle(link)) {
                /* Subtitle URLs are static -> Subtitle must have been deleted. */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (account != null) {
                login(account, false);
            }
            fetchFileInformation(link, account, true);
            String directurl = null;
            if (StringUtils.isEmpty(directurl)) {
                directurl = getStreamURL(br, link, null);
                if (StringUtils.isEmpty(directurl)) {
                    handleErrors(account);
                    if (account == null || !AccountType.PREMIUM.equals(account.getType())) {
                        throw new AccountRequiredException();
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
            }
            dl = BrowserAdapter.openDownload(br, link, directurl, resume, maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection();
                } catch (IOException e) {
                    logger.log(e);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty(PROPERTY_DIRECTURL, directurl);
        } else {
            logger.info("Re-using existing directurl");
        }
        // TODO subtitle
        /*
         * if (PluginJsonConfig.get(PluralsightComConfig.class).isDownloadSubtitles()) { PostRequest postRequest =
         * getSubtitlesRequest(link); br.getPage(postRequest); if (postRequest.getHttpConnection().getResponseCode() != 200) { throw new
         * PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Cannot dowmload subtitles"); } String subtitles = getSubtitles(postRequest,
         * link); if (!subtitles.isEmpty()) { String path = new File(link.getFileOutput()).getParent(); // String fullPath = path + "\\" +
         * link.getFinalFileName().replaceFirst("[.][^.]+$", "") + ".srt"; String finalNameNoEx =
         * Files.getFileNameWithoutExtension(link.getName()); String fullPath = path + "\\" + finalNameNoEx + ".srt";
         * java.nio.file.Files.write(Paths.get(fullPath), subtitles.getBytes()); } }
         */
        synchronized (timestampLastDownloadStarted) {
            timestampLastDownloadStarted.set(Time.systemIndependentCurrentJVMTimeMillis());
        }
        synchronized (durationSecondsOfLastDownloadedClip) {
            durationSecondsOfLastDownloadedClip.set(link.getIntegerProperty(PROPERTY_DURATION_SECONDS, 0));
        }
        dl.startDownload();
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final boolean resume, final int maxchunks) throws Exception {
        final String url = link.getStringProperty(PROPERTY_DIRECTURL);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, resume, maxchunks);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Exception e) {
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            dl = null;
            /* Do not retry with same invalid directURL. */
            link.removeProperty(PROPERTY_DIRECTURL);
            return false;
        }
    }

    private void handleErrors(final Account account) throws PluginException {
        /*
         * 2020-04-27: E.g.
         * {"success":false,"error":{"message":"user not authorized"},"meta":{"status":403,"libraries":[]},"trace":[{"service":
         * "videoservices_clip","version":"1.0.450","latency":44,"fn":"viewClipV3"}]}
         */
        if (br.getHttpConnection().getResponseCode() == 403) {
            if (account != null) {
                throw new AccountUnavailableException("Session expired or premium required to download content?", 5 * 60 * 1000l);
            } else {
                throw new AccountRequiredException();
            }
        }
    }

    // private String getSubtitles(PostRequest postRequest, DownloadLink link) throws IOException, PluginException {
    // String content = postRequest.getResponseText();
    // ObjectMapper mapper = new ObjectMapper();
    // JsonNode root = mapper.readTree(content);
    // if (root.isMissingNode() || root.isNull()) {
    // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "subtitles does not exist");
    // }
    // StringBuilder sb = new StringBuilder();
    // int i = 0;
    // for (JsonNode jsonNode : root) {
    // ++i;
    // String msStart = jsonNode.get("displayTimeOffset").asText();
    // String fullTime = getTime(msStart);
    // sb.append(String.valueOf(i)).append(System.lineSeparator()).append(fullTime).append(" --> ");
    // String fullTime2;
    // if (i < root.size()) {
    // fullTime2 = getTime(root.get(i).get("displayTimeOffset").asText());
    // } else {
    // String duration = link.getProperty("duration").toString();
    // fullTime2 = getTime(duration);
    // }
    // sb.append(fullTime2).append(System.lineSeparator());
    // sb.append(jsonNode.get("text").asText().replace("\"", "")).append(System.lineSeparator());
    // if (i != root.size()) {
    // sb.append(System.lineSeparator());
    // }
    // }
    // return sb.toString();
    // }
    // private PostRequest getSubtitlesRequest(DownloadLink link) throws IOException {
    // final UrlQuery urlParams = UrlQuery.parse(link.getOriginUrl());
    // final Map<String, Object> params = new HashMap<>();
    // params.put("cn", Integer.valueOf(urlParams.get("clip")));
    // params.put("lc", "en");
    // params.put("a", urlParams.get("author"));
    // params.put("m", urlParams.get("name"));
    // PostRequest postRequest = new PostRequest("https://app.pluralsight.com/player/retrieve-captions");
    // // postRequest.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:56.0) Gecko/20100101 Firefox/56.0");
    // postRequest.getHeaders().put("Content-Type", "application/json;charset=UTF-8");
    // String paramsPost = JSonStorage.toString(params);
    // postRequest.setPostDataString(paramsPost);
    // return postRequest;
    // }
    //
    // public static String getTime(String ms) {
    // String[] millisAndSeconds = ms.replaceAll("[A-Za-z]+", "").split("\\.");
    // millisAndSeconds[1] = String.format("%-3s", Integer.parseInt(millisAndSeconds[1])).replace(' ', '0');
    // String totalSecondsTemps = millisAndSeconds[1].substring(millisAndSeconds[1].lastIndexOf(".") + 1, 3);
    // long millis = Long.valueOf(totalSecondsTemps);
    // long totalSeconds = Long.valueOf(millisAndSeconds[0]);
    // long hours = TimeUnit.SECONDS.toHours(totalSeconds);
    // long minutes = TimeUnit.SECONDS.toMinutes(totalSeconds);
    // long seconds = TimeUnit.SECONDS.toSeconds(totalSeconds);
    // for (int j = 0; j < minutes; j++) {
    // seconds -= 60;
    // }
    // for (int j = 0; j < hours - 1; j++) {
    // minutes -= 1;
    // }
    // for (int j = 0; j < hours; j++) {
    // minutes -= 60;
    // }
    // return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, millis);
    // }
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public String getDescription() {
        return "Download course videos from Pluralsight.com";
    }

    @Override
    public Class<? extends PluralsightComConfig> getConfigInterface() {
        return PluralsightComConfig.class;
    }
}