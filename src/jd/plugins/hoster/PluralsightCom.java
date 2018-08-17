package jd.plugins.hoster;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.config.PluralsightComConfig;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.scripting.JavaScriptEngineFactory;

/**
 *
 * @author Neokyuubi
 *
 */
@HostPlugin(revision = "$Revision: 1 $", interfaceVersion = 1, names = { "pluralsight.com" }, urls = { "https?://app\\.pluralsight\\.com\\/player\\??.+" })
public class PluralsightCom extends PluginForHost {
    public PluralsightCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.pluralsight.com/pricing");
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
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        login(account, br, this, true);
        if (!StringUtils.equals(br.getURL(), "https://app.pluralsight.com/web-analytics/api/v1/users/current")) {
            getRequest(br, this, br.createGetRequest("https://app.pluralsight.com/web-analytics/api/v1/users/current"));
        }
        final Map<String, Object> map = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final List<Map<String, Object>> userSubscriptions = (List<Map<String, Object>>) map.get("userSubscriptions");
        final AccountInfo ai = new AccountInfo();
        if (userSubscriptions == null) {
            account.setType(AccountType.UNKNOWN);
            ai.setStatus("Unknown");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Something went wrong with account verification type.");
        } else if (userSubscriptions.size() == 0) {
            account.setType(AccountType.FREE);
            ai.setStatus("Free Account");
        } else {
            boolean isPremium = false;
            for (Map<String, Object> userSubscription : userSubscriptions) {
                final String expiresAt = userSubscription.get("expiresAt") != null ? (String) userSubscription.get("expiresAt") : null;
                if (expiresAt != null) {
                    final long validUntil = TimeFormatter.getMilliSeconds(expiresAt.replace("Z", "+0000"), "yyyy-MM-dd'T'HH:mm:ss.SSSZ", null);
                    if (validUntil > System.currentTimeMillis()) {
                        isPremium = true;
                        account.setType(AccountType.PREMIUM);
                        ai.setStatus("Premium Account");
                        ai.setValidUntil(validUntil);
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

    public static void login(final Account account, Browser br, Plugin plugin, boolean revalidate) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(plugin.getHost(), cookies);
                    if (!revalidate && System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 5 * 60 * 1000l) {
                        return;
                    }
                    getRequest(br, plugin, br.createGetRequest("https://app.pluralsight.com/web-analytics/api/v1/users/current"));
                    final Request request = br.getRequest();
                    if (request.getHttpConnection().getResponseCode() == 200 && br.getHostCookie("PsJwt-production", Cookies.NOTDELETEDPATTERN) != null) {
                        account.saveCookies(br.getCookies(plugin.getHost()), "");
                        return;
                    }
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

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this);
        if (account != null) {
            try {
                login(account, br, this, false);
                return fetchFileInformation(link, account);
            } catch (PluginException e) {
                final LogInterface logger = getLogger();
                if (logger instanceof LogSource) {
                    handleAccountException(account, (LogSource) logger, e);
                } else {
                    handleAccountException(account, null, e);
                }
            }
        }
        return fetchFileInformation(link, account);
    }

    public String getStreamURL(DownloadLink link) throws PluginException, IOException, InterruptedException {
        final UrlQuery urlParams = UrlQuery.parse(link.getPluginPatternMatcher());
        final String author = urlParams.get("author");
        if (StringUtils.isEmpty(author)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String course = urlParams.get("course");
        if (StringUtils.isEmpty(course)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String clip = urlParams.get("clip");
        if (StringUtils.isEmpty(clip)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Map<String, Object> params = new HashMap<>();
        params.put("query", "query viewClip { viewClip(input: { author: \"" + author + "\", clipIndex: " + clip + ", courseName: \"" + course + "\", includeCaptions: false, locale: \"en\", mediaType: \"mp4\", moduleName: \"" + urlParams.get("name") + "\", quality: \"1280x720\" }) { urls { url cdn rank source }, status } }");
        params.put("variables", "{}");
        final PostRequest request = br.createPostRequest("https://app.pluralsight.com/player/api/graphql", JSonStorage.toString(params));
        request.setContentType("application/json;charset=UTF-8");
        request.getHeaders().put("Origin", "https://app.pluralsight.com");
        final Map<String, Object> response = JSonStorage.restoreFromString(getRequest(br, this, request).getHtmlCode(), TypeRef.HASHMAP);
        final List<Map<String, Object>> urls = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(response, "data/viewClip/urls");
        if (urls != null) {
            for (final Map<String, Object> url : urls) {
                final String streamURL = (String) url.get("url");
                if (StringUtils.isNotEmpty(streamURL) && !StringUtils.containsIgnoreCase(streamURL, "expiretime=")) {
                    return streamURL;
                }
            }
        }
        return null;
    }

    private static Object WAITLOCK = new Object();

    public static Request getRequest(Browser br, Plugin plugin, Request request) throws InterruptedException, IOException {
        return getRequest(br, plugin, request, 30 * 1000);
    }

    public static Request getRequest(Browser br, Plugin plugin, Request request, long waitMax) throws InterruptedException, IOException {
        br.getPage(request);
        int loop = 0;
        synchronized (WAITLOCK) {
            while (waitMax > 0) {
                if (request.getHttpConnection().getResponseCode() == 429) {
                    final int wait = 1000 * (++loop);
                    WAITLOCK.wait(wait);
                    waitMax -= wait;
                    br.getPage(request);
                } else {
                    break;
                }
            }
        }
        return request;
    }

    private String streamURL = null;

    private AvailableStatus fetchFileInformation(DownloadLink link, final Account account) throws Exception {
        streamURL = null;
        final UrlQuery urlParams = UrlQuery.parse(link.getPluginPatternMatcher());
        AvailableStatus result = null;// firstCheck(link, urlParams);
        if (result == AvailableStatus.TRUE) {
            return result;
        }
        if (!link.getBooleanProperty("isNameSet", false)) {
            final String course = urlParams.get("course");
            if (course == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            PluralsightCom.getRequest(br, this, br.createGetRequest("https://app.pluralsight.com/learner/content/courses/" + course));
            if (br.containsHTML("Not Found")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> map = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            final ArrayList<DownloadLink> clips = PluralsightCom.getClips(this, br, map);
            DownloadLink foundClip = null;
            if (clips != null) {
                final String clipID = urlParams.get("clip");
                if (clipID == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String moduleID = new Regex(urlParams.get("name"), "-m(\\d+)$").getMatch(0);
                for (final DownloadLink clip : clips) {
                    if (StringUtils.equals(clipID, String.valueOf(clip.getProperty("ordering")))) {
                        if (moduleID == null || StringUtils.equals(moduleID, String.valueOf(clip.getProperty("module")))) {
                            foundClip = clip;
                            break;
                        }
                    }
                }
            }
            if (foundClip == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                link.setFinalFileName(foundClip.getName());
                link.setProperty("isNameSet", true);
            }
        }
        streamURL = getStreamURL(link);
        if (link.getKnownDownloadSize() == -1 && !StringUtils.isEmpty(streamURL)) {
            final Request checkStream = getRequest(br, this, br.createHeadRequest(streamURL));
            final URLConnectionAdapter con = checkStream.getHttpConnection();
            if (con.getResponseCode() == 200 && !StringUtils.containsIgnoreCase(con.getContentType(), "text") && con.getCompleteContentLength() > 0) {
                link.setVerifiedFileSize(con.getCompleteContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (StringUtils.isEmpty(streamURL)) {
            if (Thread.currentThread() instanceof SingleDownloadController) {
                if (account == null || !AccountType.PREMIUM.equals(account.getType())) {
                    throw new AccountRequiredException();
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            return AvailableStatus.UNCHECKABLE;
        } else {
            return AvailableStatus.TRUE;
        }
    }

    public static ArrayList<DownloadLink> getClips(Plugin plugin, Browser br, Map<String, Object> map) throws Exception {
        final List<Map<String, Object>> modules = (List<Map<String, Object>>) map.get("modules");
        if (modules == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (modules.size() == 0) {
            return null;
        }
        final ArrayList<DownloadLink> ret = new ArrayList<>();
        int moduleIndex = 0;
        for (final Map<String, Object> module : modules) {
            moduleIndex++;
            final List<Map<String, Object>> clips = (List<Map<String, Object>>) module.get("clips");
            if (clips != null) {
                for (final Map<String, Object> clip : clips) {
                    final String playerUrl = (String) clip.get("playerUrl");
                    if (StringUtils.isEmpty(playerUrl)) {
                        continue;
                    }
                    final String url = br.getURL(playerUrl).toString();
                    final DownloadLink link = new DownloadLink(null, null, plugin.getHost(), url, true);
                    final String duration = (String) clip.get("duration");
                    link.setProperty("duration", duration);
                    final String clipId = (String) clip.get("clipId");
                    link.setProperty("clipID", clipId);
                    link.setLinkID(plugin.getHost() + "://" + clipId);
                    final String title = (String) clip.get("title");
                    final String moduleTitle = (String) clip.get("moduleTitle");
                    final Object ordering = clip.get("ordering");
                    link.setProperty("ordering", ordering);
                    link.setProperty("module", moduleIndex);
                    if (StringUtils.isNotEmpty(title) && StringUtils.isNotEmpty(moduleTitle) && ordering != null) {
                        String fullName = String.format("%02d", moduleIndex) + "-" + String.format("%02d", Long.parseLong(ordering.toString()) + 1) + " - " + moduleTitle + " -- " + title;
                        fullName = PluralsightCom.correctFileName(fullName);
                        link.setFinalFileName(fullName + ".mp4");
                        link.setProperty("isNameSet", true);
                    }
                    link.setProperty("type", "mp4");
                    link.setAvailable(true);
                    ret.add(link);
                }
            }
        }
        return ret;
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
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        downloadStream(link, streamURL);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        downloadStream(link, streamURL);
    }

    private void downloadStream(final DownloadLink link, final String streamURL) throws Exception {
        if (StringUtils.isEmpty(streamURL)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = BrowserAdapter.openDownload(br, link, streamURL, true, 0);
        if (StringUtils.containsIgnoreCase(dl.getConnection().getContentType(), "text")) {
            try {
                br.followConnection();
            } catch (IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
        return "Download videos course from Pluralsight.Com (Subtitles coming soon)";
    }
}