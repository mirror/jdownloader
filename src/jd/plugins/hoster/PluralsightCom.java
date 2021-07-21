package jd.plugins.hoster;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.components.config.PluralsightComConfig;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.downloadcontroller.SingleDownloadController;
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
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.PluralsightComDecrypter;

/**
 *
 * @author Neokyuubi
 *
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 1, names = { "pluralsight.com" }, urls = { "https://app\\.pluralsight\\.com/course-player\\?clipId=[a-f0-9\\-]+" })
public class PluralsightCom extends antiDDoSForHost {
    private static WeakHashMap<Account, List<Long>> map100PerHour                        = new WeakHashMap<Account, List<Long>>();
    private static WeakHashMap<Account, List<Long>> map200Per4Hours                      = new WeakHashMap<Account, List<Long>>();
    public static final String                      PROPERTY_DURATION                    = "duration";
    public static final String                      PROPERTY_CLIP_ID                     = "clipID";
    public static final String                      PROPERTY_CLIP_VERSION                = "version";
    public static final String                      PROPERTY_MODULE_ORDER_ID             = "module";
    public static final String                      PROPERTY_ORDERING                    = "ordering";
    public static final String                      PROPERTY_SUPPORTS_WIDESCREEN_FORMATS = "supportsWideScreenVideoFormats";
    public static final String                      PROPERTY_TYPE                        = "type";
    public static final String                      PROPERTY_FORCED_RESOLUTION           = "forced_resolution";
    private static final String                     PROPERTY_DIRECTURL                   = "directurl";

    public PluralsightCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.pluralsight.com/pricing");
        /* 2020-02-17: According to: https://board.jdownloader.org/showthread.php?t=82533 */
        this.setStartIntervall(60 * 1000l);
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return PluralsightComConfig.class;
    }

    @Override
    public String getAGBLink() {
        return "https://www.pluralsight.com/terms";
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        login(account, br, this, true);
        if (!StringUtils.equals(br.getURL(), "https://app.pluralsight.com/web-analytics/api/v1/users/current")) {
            getRequest(br, this, br.createGetRequest("https://app.pluralsight.com/web-analytics/api/v1/users/current"));
        }
        final Map<String, Object> map = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        List<Map<String, Object>> subscriptions = (List<Map<String, Object>>) map.get("userSubscriptions");
        if (subscriptions == null) {
            subscriptions = (List<Map<String, Object>>) map.get("subscriptions");
        }
        final AccountInfo ai = new AccountInfo();
        if (subscriptions == null) {
            account.setType(AccountType.UNKNOWN);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Something went wrong with account verification type.");
        } else if (subscriptions.size() == 0) {
            account.setType(AccountType.FREE);
        } else {
            boolean isPremium = false;
            for (Map<String, Object> subscription : subscriptions) {
                final String expiresAt = subscription.get("expiresAt") != null ? (String) subscription.get("expiresAt") : null;
                if (expiresAt != null) {
                    final long validUntil = TimeFormatter.getMilliSeconds(expiresAt.replace("Z", "+0000"), "yyyy-MM-dd'T'HH:mm:ss.SSSZ", null);
                    if (validUntil > System.currentTimeMillis()) {
                        isPremium = true;
                        account.setType(AccountType.PREMIUM);
                        ai.setStatus("Premium Account:" + subscription.get("name"));
                        ai.setValidUntil(validUntil, br);
                        break;
                    }
                }
            }
            if (!isPremium) {
                account.setType(AccountType.FREE);
                ai.setStatus("Free (Expired) Account");
            }
        }
        account.setMaxSimultanDownloads(1);
        account.setConcurrentUsePossible(true);
        return ai;
    }

    public static void login(final Account account, final Browser br, final Plugin plugin, final boolean revalidate) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(plugin.getHost(), cookies);
                    if (!revalidate) {
                        return;
                    }
                    getRequest(br, plugin, br.createGetRequest("https://app.pluralsight.com/web-analytics/api/v1/users/current"));
                    final Request request = br.getRequest();
                    if (request.getHttpConnection().getResponseCode() == 200 && br.getHostCookie("PsJwt-production", Cookies.NOTDELETEDPATTERN) != null) {
                        account.saveCookies(br.getCookies(plugin.getHost()), "");
                        return;
                    }
                    /* Full login required */
                }
                // Login
                // Captcha And Login
                getRequest(br, plugin, br.createGetRequest("https://app.pluralsight.com/id/"));
                final Form form = br.getFormbyKey("Username");
                if (form == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final boolean isCaptchaVisible = br.getRegex("<input\\s+id=\"ReCaptchaSiteKey\"\\s+[\\w\\s\\d=\"]+(type=\"hidden\")[\\w\\s\\d=\"]+\\/>").getMatch(0) == null;
                if (br.containsHTML("ReCaptchaSiteKey") && isCaptchaVisible) {
                    final String recaptchaV2Response;
                    if (plugin instanceof PluginForHost) {
                        final PluginForHost plg = (PluginForHost) plugin;
                        final DownloadLink dlinkbefore = plg.getDownloadLink();
                        if (dlinkbefore == null) {
                            plg.setDownloadLink(new DownloadLink(plg, "Account", plg.getHost(), "http://" + account.getHoster(), true));
                        }
                        recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(plg, br, "6LeVIgoTAAAAAIhx_TOwDWIXecbvzcWyjQDbXsaV").getToken();
                        if (dlinkbefore != null) {
                            plg.setDownloadLink(dlinkbefore);
                        }
                    } else if (plugin instanceof PluginForDecrypt) {
                        recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2((PluginForDecrypt) plugin, br, "6LeVIgoTAAAAAIhx_TOwDWIXecbvzcWyjQDbXsaV").getToken();
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    form.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                }
                form.put("Username", URLEncoder.encode(account.getUser(), "UTF-8"));
                form.put("Password", URLEncoder.encode(account.getPass(), "UTF-8"));
                getRequest(br, plugin, br.createFormRequest(form));
                if (br.containsHTML(">Invalid user name or password<")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Invalid user name or password", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                if (br.getHostCookie("PsJwt-production", Cookies.NOTDELETEDPATTERN) == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                getRequest(br, plugin, br.createGetRequest("https://app.pluralsight.com/web-analytics/api/v1/users/current"));
                final Request request = br.getRequest();
                if (request.getHttpConnection().getResponseCode() == 401) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (request.getHttpConnection().getResponseCode() == 429) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Unfortunately the site is currently unavilable. We expect everything back in order shortly. If you continue to experience problems, let us know.", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                } else if (request.getHttpConnection().getResponseCode() != 200) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                account.saveCookies(br.getCookies(plugin.getHost()), "");
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
        final Account account = AccountController.getInstance().getValidAccount(this);
        if (account != null) {
            if (antiAccountBlockProtection(account)) {
                throw new AccountUnavailableException("Account block protection, please wait!", 5 * 60 * 1000l);
            }
            login(account, br, this, false);
            return fetchFileInformation(link, account);
        } else {
            return fetchFileInformation(link, null);
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

    public static String getStreamURL(final Browser br, final Plugin plugin, final DownloadLink link, QUALITY quality) throws Exception {
        /* 2020-04-21: Try and error. Browser does the same lol */
        final String[] resolutions = new String[] { "1280x720", "1024x768" };
        List<Map<String, Object>> urls = null;
        /* Re-use previously working resolution in case there is one. */
        String existant_resolution = link.getStringProperty(PROPERTY_FORCED_RESOLUTION);
        for (String resolution : resolutions) {
            if (existant_resolution != null) {
                plugin.getLogger().info("Override quality-check of " + resolution + " with " + existant_resolution);
                resolution = existant_resolution;
            } else {
                plugin.getLogger().info("Checking resolution: " + resolution);
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
                params.put("clipId", clipID);
                params.put("mediaType", "mp4");
                params.put("quality", resolution);
                params.put("online", true);
                params.put("boundedContext", "course");
                if (version != null) {
                    params.put("versionId", version);
                } else {
                    params.put("versionId", "");
                }
                request = br.createPostRequest("https://app.pluralsight.com/video/clips/v3/viewclip", JSonStorage.toString(params));
                request.setContentType("application/json;charset=UTF-8");
                request.getHeaders().put("Origin", "https://app.pluralsight.com");
                getRequest(br, plugin, request);
                /*
                 * 2020-04-21: E.g.
                 * {"success":false,"error":{"message":"1280x720.mp4 encoding not found"},"meta":{"statusCode":404},"trace":[{"service":
                 * "videoservices_clip","version":"1.0.450","latency":29,"fn":"viewClipV3"}]}
                 */
                if (br.containsHTML(resolution + ".mp4 encoding not found")) {
                    resolutionExists = false;
                } else {
                    final Map<String, Object> response = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                    urls = (List<Map<String, Object>>) response.get("urls");
                    existant_resolution = resolution;
                    plugin.getLogger().info("Found working resolution: " + resolution);
                }
            } else {
                /* Old handling */
                UrlQuery urlParams = UrlQuery.parse(link.getPluginPatternMatcher());
                final String author = urlParams.get("author");
                final String course = urlParams.get("course");
                final String clip = urlParams.get("clip");
                if (StringUtils.isEmpty(author) || StringUtils.isEmpty(course) || StringUtils.isEmpty(clip)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                params.put("query", "query viewClip { viewClip(input: { author: \"" + author + "\", clipIndex: " + clip + ", courseName: \"" + course + "\", includeCaptions: false, locale: \"en\", mediaType: \"mp4\", moduleName: \"" + urlParams.get("name") + "\" , quality: \"" + quality + "\"}) { urls { url cdn rank source }, status } }");
                params.put("variables", "{}");
                request = br.createPostRequest("https://app.pluralsight.com/player/api/graphql", JSonStorage.toString(params));
                request.setContentType("application/json;charset=UTF-8");
                request.getHeaders().put("Origin", "https://app.pluralsight.com");
                getRequest(br, plugin, request);
                final Map<String, Object> response = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                urls = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(response, "data/viewClip/urls");
                existant_resolution = resolution;
                break;
            }
            if (!resolutionExists) {
                plugin.getLogger().info("Resolution does not exist: " + resolution);
                if (existant_resolution != null) {
                    plugin.getLogger().info("Forced resolution is given, stopping anyways at: " + resolution);
                    break;
                }
                continue;
            }
            plugin.getLogger().info("Found working resolution: " + resolution);
            break;
        }
        if (existant_resolution != null) {
            link.setProperty(PROPERTY_FORCED_RESOLUTION, existant_resolution);
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

    private AvailableStatus fetchFileInformation(final DownloadLink link, final Account account) throws Exception {
        final String storedDirecturl = link.getStringProperty(PROPERTY_DIRECTURL);
        if (storedDirecturl != null) {
            final Request checkStream = getRequest(br, this, br.createHeadRequest(storedDirecturl));
            final URLConnectionAdapter con = checkStream.getHttpConnection();
            try {
                if (looksLikeDownloadableContent(con)) {
                    logger.info("Availablecheck via stored directurl sccessful");
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    return AvailableStatus.TRUE;
                } else {
                    logger.info("Availablecheck via stored directurl failed");
                    link.removeProperty(PROPERTY_DIRECTURL);
                }
            } finally {
                con.disconnect();
            }
        }
        if (Thread.currentThread() instanceof SingleDownloadController) {
            return AvailableStatus.UNCHECKABLE;
        } else if (link.getKnownDownloadSize() == -1) {
            /* Only check for filesize if none has been set yet. */
            final String streamURL = getStreamURL(br, this, link, null);
            if (!StringUtils.isEmpty(streamURL)) {
                final Request checkStream = getRequest(br, this, br.createHeadRequest(streamURL));
                final URLConnectionAdapter con = checkStream.getHttpConnection();
                try {
                    if (looksLikeDownloadableContent(con)) {
                        if (con.getCompleteContentLength() > 0) {
                            link.setVerifiedFileSize(con.getCompleteContentLength());
                        }
                        link.setProperty(PROPERTY_DIRECTURL, streamURL);
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

    @Override
    protected boolean looksLikeDownloadableContent(final URLConnectionAdapter urlConnection) {
        return super.looksLikeDownloadableContent(urlConnection) && urlConnection.getCompleteContentLength() > 0;
    }

    public static String correctFileName(String fileName) {
        fileName = fileName.replaceAll("\n", "").replaceAll("\r", "").replaceAll("[\\\\/:*?\"<>|]", "");
        Matcher p = Pattern.compile(".*?(\\s?)_(\\s?).*?").matcher(fileName);
        while (p.find()) {
            int g1S = p.toMatchResult().start(1);
            int g1E = p.toMatchResult().end(1);
            int g2S = p.toMatchResult().start(2);
            if (p.group(1).equals(" ") && p.group(2).equals(" ")) {
                fileName = fileName.substring(0, g1S) + " " + fileName.substring(g2S + 1, fileName.length());
            } else if (p.group(1).equals(" ")) {
                fileName = fileName.substring(0, g1S + 1) + fileName.substring(g1E + 1, fileName.length());
            } else if (p.group(2).equals(" ")) {
                fileName = fileName.substring(0, g1S) + fileName.substring(g1E + 1, fileName.length());
            }
        }
        return fileName;
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
        final boolean resume = true;
        /*
         * More possible but let's limit it to 1 as this website doesn't like users establishing a lot of connections at the same/in a short
         * time.
         */
        final int maxchunks = 1;
        if (!attemptStoredDownloadurlDownload(link, resume, maxchunks)) {
            logger.info("Generating fresh directurl");
            if (account != null) {
                login(account, br, this, false);
            }
            fetchFileInformation(link, account);
            String streamURL = null;
            if (StringUtils.isEmpty(streamURL)) {
                streamURL = getStreamURL(br, this, link, null);
                if (StringUtils.isEmpty(streamURL)) {
                    handleErrors(account);
                    if (account == null || !AccountType.PREMIUM.equals(account.getType())) {
                        throw new AccountRequiredException();
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
            }
            dl = BrowserAdapter.openDownload(br, link, streamURL, resume, maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection();
                } catch (IOException e) {
                    logger.log(e);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty(PROPERTY_DIRECTURL, streamURL);
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
        } catch (final Throwable e) {
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
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
        return "Download videos course from Pluralsight.com";
    }
}