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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "nicovideo.jp" }, urls = { "https?://(?:www\\.)?nicovideo\\.jp/watch/(?:sm|so|nm)?(\\d+)" })
public class NicoVideoJp extends PluginForHost {
    private static final String           MAINPAGE                    = "https://www.nicovideo.jp/";
    private static final String           ONLYREGISTEREDUSERTEXT      = "Only downloadable for registered users";
    private static final String           CUSTOM_DATE                 = "CUSTOM_DATE";
    private static final String           CUSTOM_FILENAME             = "CUSTOM_FILENAME";
    private static final String           TYPE_NM                     = "https?://(www\\.)?nicovideo\\.jp/watch/nm\\d+";
    private static final String           TYPE_SM                     = "https?://(www\\.)?nicovideo\\.jp/watch/sm\\d+";
    private static final String           TYPE_SO                     = "https?://(www\\.)?nicovideo\\.jp/watch/so\\d+";
    /* Other types may redirect to this type. This is the only type which is also downloadable without account (sometimes?). */
    private static final String           TYPE_WATCH                  = "https?://(www\\.)?nicovideo\\.jp/watch/\\d+";
    private static final String           default_extension           = "mp4";
    private static final String           NOCHUNKS                    = "NOCHUNKS";
    private static final String           AVOID_ECONOMY_MODE          = "AVOID_ECONOMY_MODE";
    private static final boolean          FREE_RESUME                 = true;
    private static final int              FREE_MAXCHUNKS              = 0;
    private static final boolean          ACCOUNT_FREE_RESUME         = true;
    private static final int              ACCOUNT_FREE_MAXCHUNKS      = 0;
    private static final int              economy_active_wait_minutes = 30;
    private static final String           html_account_needed         = "account\\.nicovideo\\.jp/register\\?from=watch\\&mode=landing\\&sec=not_login_watch";
    public static final long              trust_cookie_age            = 300000l;
    /* note: CAN NOT be negative or zero! (ie. -1 or 0) Otherwise math sections fail. .:. use [1-20] */
    private static AtomicInteger          totalMaxSimultanFree        = new AtomicInteger(2);
    private static AtomicInteger          totalMaxSimultanFreeAccount = new AtomicInteger(2);
    /* don't touch the following! */
    private static AtomicInteger          maxPremium                  = new AtomicInteger(1);
    private static AtomicInteger          maxFree                     = new AtomicInteger(1);
    private static Object                 LOCK                        = new Object();
    private LinkedHashMap<String, Object> entries                     = null;

    public NicoVideoJp(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://secure.nicovideo.jp/secure/register");
        setConfigElements();
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public void init() {
        super.init();
        Browser.setRequestIntervalLimitGlobal(getHost(), 500);
    }

    /**
     * IMPORTANT: The site has a "normal" and "economy" mode. Normal mode = Higher video quality - mp4 streams. Economy mode = lower quality
     * - flv streams. Premium users are ALWAYS in the normal mode.
     *
     * @throws Exception
     */
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        final String fid = getFID(link);
        link.setProperty("extension", default_extension);
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        boolean loggedin = false;
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            this.login(aa, false);
            loggedin = true;
        }
        br.getPage(link.getPluginPatternMatcher());
        String fallback_filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
        if (fallback_filename == null) {
            fallback_filename = fid;
        }
        if (br.containsHTML("this video inappropriate.<")) {
            final String watch = br.getRegex("harmful_link\" href=\"([^<>\"]*?)\">Watch this video</a>").getMatch(0);
            if (watch == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(watch);
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (isPaidContent()) {
            link.setName(fallback_filename);
            return AvailableStatus.TRUE;
        } else if (isGeoBlocked()) {
            link.setName(fallback_filename);
            return AvailableStatus.TRUE;
        }
        String jsonapi = br.getRegex("data-api-data=\"(.*?)\" hidden>").getMatch(0);
        jsonapi = Encoding.htmlDecode(jsonapi);
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(jsonapi);
        entries = (LinkedHashMap<String, Object>) entries.get("video");
        final String title = (String) entries.get("title");
        String filename = title;
        if (StringUtils.isEmpty(filename)) {
            filename = this.getFID(link);
        }
        filename = title;
        link.setProperty("plainfilename", filename);
        // if (date == null) {
        // date = br.getRegex("property=\"video:release_date\" content=\"(\\d{4}\\-\\d{2}\\-\\d{2}T\\d{2}:\\d{2}\\+\\d{4})\"").getMatch(0);
        // }
        // if (date != null) {
        // link.setProperty("originaldate", date);
        // }
        // if (channel != null) {
        // link.setProperty("channel", channel);
        // }
        filename = getFormattedFilename(link);
        link.setName(filename);
        if (br.containsHTML(html_account_needed)) {
            link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.nicovideojp.only4registered", ONLYREGISTEREDUSERTEXT));
        }
        return AvailableStatus.TRUE;
    }

    private boolean isGeoBlocked() {
        return br.containsHTML(">\\s*Sorry, this video can only be viewed in the same region where it was uploaded");
    }

    private boolean isPaidContent() {
        return br.containsHTML(">\\s*This is a paid video");
    }

    private String getHtmlJson() {
        String json = br.getRegex("id=\"watchAPIDataContainer\" style=\"display:none\">(.*?)</div>").getMatch(0);
        if (json == null) {
            json = br.getRegex("data\\-api\\-data=\"([^\"]+)\"").getMatch(0);
        }
        return json;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        if (!account.getUser().matches(".+@.+\\..+")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte gib deine E-Mail Adresse ins Benutzername Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail adress in the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        br = new Browser();
        return login(account, true);
    }

    @Override
    public String getAGBLink() {
        return "http://info.nicovideo.jp/base/rule.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxFree.get();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxPremium.get();
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        // checkWatchableGeneral();
        if (isPaidContent()) {
            throw new AccountRequiredException();
        } else if (isGeoBlocked()) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "GEO-blocked");
        }
        final Object smileO = entries.get("smileInfo");
        String dllink = null;
        if (smileO != null) {
            /* 2020-03-25: Easy way to download: E.g. old content / low quality */
            entries = (LinkedHashMap<String, Object>) smileO;
            dllink = (String) entries.get("url");
        } else {
            /* TODO: Fix- and enable API handling */
            if (true) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final LinkedHashMap<String, Object> dmcInfo = (LinkedHashMap<String, Object>) entries.get("dmcInfo");
            final LinkedHashMap<String, Object> session_api = (LinkedHashMap<String, Object>) dmcInfo.get("session_api");
            final String signature = (String) session_api.get("signature");
            final String recipe_id = (String) session_api.get("recipe_id");
            final String player_id = (String) session_api.get("player_id");
            final String service_user_id = (String) session_api.get("service_user_id");
            final long created_time = JavaScriptEngineFactory.toLong(entries.get("created_time"), 0);
            final long expire_time = JavaScriptEngineFactory.toLong(entries.get("expire_time"), 0);
            final Object tokenO = session_api.get("token");
            final LinkedHashMap<String, Object> token = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap((String) tokenO);
            final ArrayList<Object> videos = (ArrayList<Object>) token.get("videos");
            final ArrayList<Object> audios = (ArrayList<Object>) token.get("audios");
            final ArrayList<Object> protocols = (ArrayList<Object>) token.get("protocols");
            final LinkedHashMap<String, Object> auth_types = (LinkedHashMap<String, Object>) session_api.get("auth_types");
            final String postData = String.format(
                    "{\"session\":{\"recipe_id\":\"%s\",\"content_id\":\"out1\",\"content_type\":\"movie\",\"content_src_id_sets\":[{\"content_src_ids\":[{\"src_id_to_mux\":{\"video_src_ids\":[\"archive_h264_1080p\",\"archive_h264_720p\",\"archive_h264_480p\",\"archive_h264_360p\",\"archive_h264_360p_low\"],\"audio_src_ids\":[\"archive_aac_128kbps\"]}},{\"src_id_to_mux\":{\"video_src_ids\":[\"archive_h264_720p\",\"archive_h264_480p\",\"archive_h264_360p\",\"archive_h264_360p_low\"],\"audio_src_ids\":[\"archive_aac_128kbps\"]}},{\"src_id_to_mux\":{\"video_src_ids\":[\"archive_h264_480p\",\"archive_h264_360p\",\"archive_h264_360p_low\"],\"audio_src_ids\":[\"archive_aac_128kbps\"]}},{\"src_id_to_mux\":{\"video_src_ids\":[\"archive_h264_360p\",\"archive_h264_360p_low\"],\"audio_src_ids\":[\"archive_aac_128kbps\"]}},{\"src_id_to_mux\":{\"video_src_ids\":[\"archive_h264_360p_low\"],\"audio_src_ids\":[\"archive_aac_128kbps\"]}}]}],\"timing_constraint\":\"unlimited\",\"keep_method\":{\"heartbeat\":{\"lifetime\":120000}},\"protocol\":{\"name\":\"http\",\"parameters\":{\"http_parameters\":{\"parameters\":{\"hls_parameters\":{\"use_well_known_port\":\"yes\",\"use_ssl\":\"yes\",\"transfer_preset\":\"\",\"segment_duration\":6000}}}}},\"content_uri\":\"\",\"session_operation_auth\":{\"session_operation_auth_by_signature\":{\"token\":\"{\\\"service_id\\\":\\\"nicovideo\\\",\\\"player_id\\\":\\\"%s\\\",\\\"recipe_id\\\":\\\"%s\\\",\\\"service_user_id\\\":\\\"%s\\\",\\\"protocols\\\":[{\\\"name\\\":\\\"http\\\",\\\"auth_type\\\":\\\"ht2\\\"},{\\\"name\\\":\\\"hls\\\",\\\"auth_type\\\":\\\"ht2\\\"}],\\\"videos\\\":[\\\"archive_h264_1080p\\\",\\\"archive_h264_360p\\\",\\\"archive_h264_360p_low\\\",\\\"archive_h264_480p\\\",\\\"archive_h264_720p\\\"],\\\"audios\\\":[\\\"archive_aac_128kbps\\\",\\\"archive_aac_64kbps\\\"],\\\"movies\\\":[],\\\"created_time\\\":%d,\\\"expire_time\\\":%d,\\\"content_ids\\\":[\\\"out1\\\"],\\\"heartbeat_lifetime\\\":120000,\\\"content_key_timeout\\\":600000,\\\"priority\\\":0,\\\"transfer_presets\\\":[]}\",\"signature\":\"%s\"}},\"content_auth\":{\"auth_type\":\"ht2\",\"content_key_timeout\":600000,\"service_id\":\"nicovideo\",\"service_user_id\":\"%s\"},\"client_info\":{\"player_id\":\"%s\"},\"priority\":0}}",
                    recipe_id, player_id, recipe_id, service_user_id, created_time, expire_time, signature, service_user_id, player_id);
            br.getHeaders().put("Accept", "application/json");
            br.getHeaders().put("Content-Type", "application/json");
            br.getHeaders().put("Origin", "https://www.nicovideo.jp");
            br.postPageRaw("https://api.dmc.nico/api/sessions?_format=json", postData);
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "data/session");
            // https://api.dmc.nico/api/sessions
            /* Most of the times an account is needed to watch/download videos. */
            if (br.containsHTML(html_account_needed)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, ONLYREGISTEREDUSERTEXT, PluginException.VALUE_ID_PREMIUM_ONLY);
            }
            dllink = (String) entries.get("content_uri");
        }
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        int maxChunks = FREE_MAXCHUNKS;
        if (link.getBooleanProperty(NOCHUNKS, false)) {
            maxChunks = 1;
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, FREE_RESUME, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String final_filename = getFormattedFilename(link);
        link.setFinalFileName(final_filename);
        try {
            /* add a download slot */
            controlFree(+1);
            dl.startDownload();
        } finally {
            /* remove download slot */
            controlFree(-1);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        checkWatchableGeneral();
        /* Can happen if its not clear whether the video is private or offline */
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (Encoding.htmlDecode(br.toString()).contains("closed=1&done=true")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l);
        } else if (br.containsHTML(">This is a private video and not available")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Now downloadable: This is a private video");
        }
        String dllink = getDllinkAccount();
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        int maxChunks = ACCOUNT_FREE_MAXCHUNKS;
        if (link.getBooleanProperty(NOCHUNKS, false) && getPluginConfig().getBooleanProperty(NOCHUNKS, true)) {
            maxChunks = 1;
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, ACCOUNT_FREE_RESUME, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String final_filename = getFormattedFilename(link);
        link.setFinalFileName(final_filename);
        try {
            /* add a download slot */
            controlPremium(+1);
            dl.startDownload();
        } finally {
            /* remove download slot */
            controlPremium(-1);
        }
    }

    private String getDllinkAccount() throws Exception {
        String dllink = null;
        // newest html5 (works 20170811)
        if (JavaScriptEngineFactory.walkJson(entries, "video/dmcInfo") != null) {
            ajaxPost("http://api.dmc.nico:2805/api/sessions?_format=json", constructJSON());
            dllink = PluginJSonUtils.getJson(ajax, "content_uri");
        }
        if (dllink == null) {
            // seems to be some fail over html5 (works 20170811)
            dllink = (String) JavaScriptEngineFactory.walkJson(entries, "video/smileInfo/url");
            if (dllink == null || "".equals(dllink)) {
                // really old shit (from premium), (works 20170811)
                final String flashvars = getHtmlJson();
                if (flashvars != null) {
                    if (br.getURL().matches(TYPE_SO)) {
                        br.postPage("http://flapi.nicovideo.jp/api/getflv", "v=" + this.getFID(this.getDownloadLink()));
                    } else if (br.getURL().matches(TYPE_NM) || this.getDownloadLink().getPluginPatternMatcher().matches(TYPE_SM)) {
                        /* 2018-11-14: Works */
                        final String vid = new Regex(br.getURL(), "((sm|nm)\\d+)$").getMatch(0);
                        br.postPage("http://flapi.nicovideo.jp/api/getflv", "v=" + vid);
                    }
                    dllink = getDllink_account_old(flashvars != null ? flashvars : br.toString());
                }
            }
        }
        return dllink;
    }

    private String getDllink_account_old(final String flashvars) {
        final String singleDecode = Encoding.htmlDecode(flashvars);
        String dllink = PluginJSonUtils.getJsonValue(singleDecode, "flvInfo");
        dllink = dllink != null ? asdf(dllink) : asdf(singleDecode);
        return dllink;
    }

    private Browser ajax = null;

    private void ajaxPost(String string, String string2) throws Exception {
        ajax = br.cloneBrowser();
        ajax.getHeaders().put("Accept", "application/json");
        ajax.getHeaders().put("Content-type", "application/json");
        ajax.postPageRaw(string, string2);
    }

    private String constructJSON() throws PluginException {
        try {
            final LinkedHashMap<String, Object> sesApi = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "video/dmcInfo/session_api");
            final String token = (String) sesApi.get("token");
            final String videos = sesApi.get("videos").toString().replaceAll("(\\w+)(,|)", "\"$1\"$2").replaceAll("\\s+", "");
            final String audios = sesApi.get("audios").toString().replaceAll("(\\w+)(,|)", "\"$1\"$2").replaceAll("\\s+", "");
            // new shit is all within json!
            final String output = "{\"session\":{\"recipe_id\":\"" + (String) sesApi.get("recipe_id") + "\",\"content_id\":\"" + (String) sesApi.get("content_id") + "\",\"content_type\":\"movie\",\"content_src_id_sets\":[{\"content_src_ids\":[{\"src_id_to_mux\":{\"video_src_ids\":" + videos + ",\"audio_src_ids\":" + audios + "}}]}],\"timing_constraint\":\"unlimited\",\"keep_method\":{\"heartbeat\":{\"lifetime\":120000}},\"protocol\":{\"name\":\"http\",\"parameters\":{\"http_parameters\":{\"parameters\":{\"http_output_download_parameters\":{\"use_well_known_port\":\"no\",\"use_ssl\":\"no\"}}}}},\"content_uri\":\"\",\"session_operation_auth\":{\"session_operation_auth_by_signature\":{\"token\":\"" + token.replaceAll("\"", "\\\\\"") + "\",\"signature\":\"" + (String) sesApi.get("signature") + "\"}},\"content_auth\":{\"auth_type\":\"ht2\",\"content_key_timeout\":"
                    + JavaScriptEngineFactory.toLong(sesApi.get("content_key_timeout"), 60000) + ",\"service_id\":\"nicovideo\",\"service_user_id\":\"" + sesApi.get("service_user_id") + "\"},\"client_info\":{\"player_id\":\"" + (String) sesApi.get("player_id") + "\"},\"priority\":" + sesApi.get("priority").toString() + "}}";
            return output;
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new PluginException(LinkStatus.ERROR_FATAL, "Not viewable, please confirm with your favourite webbrowser!");
    }

    /* Checks if a video is watch-/downloadable, works for logged in- and unregistered users. */
    private void checkWatchableGeneral() throws PluginException {
        if (this.br.containsHTML(">Unable to play video|>You can view this video by join") || br.containsHTML("class=\"login\\-box\\-title\"")) {
            throw new AccountRequiredException();
        }
    }

    private String asdf(String input) {
        String dllink = new Regex(input, "\\&url=(https?.*?)\\&").getMatch(0);
        if (dllink == null) {
            dllink = new Regex(input, "(https?://smile\\-[a-z]+\\d+\\.nicovideo\\.jp/smile\\?(?:v|m)=[0-9\\.]+[a-z]*)").getMatch(0);
        }
        String decodedInput = input;
        while (dllink == null && new Regex(decodedInput, "%[a-fA-F0-9]{2}").matches()) {
            decodedInput = Encoding.urlDecode(decodedInput, false);
            dllink = new Regex(decodedInput, "\\&url=(http.*?)\\&").getMatch(0);
            if (dllink == null) {
                dllink = new Regex(decodedInput, "(https?://smile\\-[a-z]+\\d+\\.nicovideo\\.jp/smile\\?(?:v|m)=[0-9\\.]+[a-z]*)").getMatch(0);
            }
        }
        return Encoding.urlDecode(dllink, false);
    }

    /**
     * orce = check cookies and perform a full login if that fails. !force = Accept cookies without checking if they're not older than
     * trust_cookie_age.
     */
    private AccountInfo login(final Account account, final boolean force) throws Exception {
        final AccountInfo ai = new AccountInfo();
        synchronized (LOCK) {
            this.setBrowserExclusive();
            final Cookies cookies = account.loadCookies("");
            if (cookies != null && !force) {
                /* 2016-05-04: Avoid full login whenever possible! */
                br.setCookies(this.getHost(), cookies);
                if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= trust_cookie_age && !force) {
                    /* We trust these cookies --> Do not check them */
                    return null;
                }
                br.getPage("https://www.nicovideo.jp/");
                if (br.containsHTML("/logout\">Log out</a>")) {
                    /* Save new cookie timestamp */
                    br.setCookies(this.getHost(), cookies);
                    return null;
                }
            }
            /* Try multiple times - it sometimes just doesn't work :( */
            boolean success = false;
            for (int i = 0; i <= 2; i++) {
                br = new Browser();
                br.setFollowRedirects(true);
                br.getPage("https://www.nicovideo.jp/");
                br.getPage("/login");
                // dont want to follow redirect here, as it takes you to homepage..
                br.setFollowRedirects(false);
                // this will redirect with session info.
                br.getHeaders().put("Accept-Encoding", "gzip, deflate, br");
                br.getHeaders().put("Referer", "https://account.nicovideo.jp/login");
                br.postPage("https://account.nicovideo.jp/api/v1/login?show_button_twitter=1&site=niconico&show_button_facebook=1", "mail_tel=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                final String redirect = br.getRedirectLocation();
                if (redirect != null) {
                    if (redirect.contains("&message=cant_login")) {
                        // invalid user:password, no need to retry.
                        throw new AccountInvalidException();
                    } else if (redirect.contains("//account.nicovideo.jp/login?")) {
                        br.getPage(redirect);
                    } else {
                        // do nothing!
                    }
                }
                if (br.getCookie(MAINPAGE, "user_session") == null || "deleted".equals(br.getCookie(MAINPAGE, "user_session"))) {
                    continue;
                }
                success = true;
                break;
            }
            if (!success) {
                throw new AccountInvalidException();
            }
            // there are multiple account types (free and paid services)
            br.getPage("//account.nicovideo.jp/my/account");
            if (br.containsHTML("<span class=\"membership--status\">(?:Yearly|Monthly|Weekly|Daily) plan</span>")) {
                account.setType(AccountType.PREMIUM);
            } else {
                account.setType(AccountType.FREE);
            }
            account.saveCookies(br.getCookies(this.getHost()), "");
            ai.setUnlimitedTraffic();
            return ai;
        }
    }

    @SuppressWarnings("deprecation")
    private String getFormattedFilename(final DownloadLink downloadLink) throws ParseException {
        final String extension = downloadLink.getStringProperty("extension", default_extension);
        final String videoid = this.getFID(downloadLink);
        String videoName = downloadLink.getStringProperty("plainfilename", null);
        final SubConfiguration cfg = SubConfiguration.getConfig("nicovideo.jp");
        String formattedFilename = cfg.getStringProperty(CUSTOM_FILENAME, defaultCustomFilename);
        if (formattedFilename == null || formattedFilename.equals("")) {
            formattedFilename = defaultCustomFilename;
        }
        if (!formattedFilename.contains("*videoname") || !formattedFilename.contains("*ext*")) {
            formattedFilename = defaultCustomFilename;
        }
        String date = downloadLink.getStringProperty("originaldate", null);
        final String channelName = downloadLink.getStringProperty("channel", null);
        String formattedDate = null;
        if (date != null && formattedFilename.contains("*date*")) {
            date = date.replace("T", ":");
            final String userDefinedDateFormat = cfg.getStringProperty(CUSTOM_DATE, "dd.MM.yyyy_HH-mm-ss");
            // 2009-08-30T22:49+0900
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd:HH:mm+ssss");
            Date dateStr = formatter.parse(date);
            formattedDate = formatter.format(dateStr);
            Date theDate = formatter.parse(formattedDate);
            if (userDefinedDateFormat != null) {
                try {
                    formatter = new SimpleDateFormat(userDefinedDateFormat);
                    formattedDate = formatter.format(theDate);
                } catch (Exception e) {
                    // prevent user error killing plugin.
                    formattedDate = "";
                }
            }
            if (formattedDate != null) {
                formattedFilename = formattedFilename.replace("*date*", formattedDate);
            } else {
                formattedFilename = formattedFilename.replace("*date*", "");
            }
        }
        formattedFilename = formattedFilename.replace("*videoid*", videoid);
        if (formattedFilename.contains("*channelname*")) {
            if (channelName != null) {
                formattedFilename = formattedFilename.replace("*channelname*", channelName);
            } else {
                formattedFilename = formattedFilename.replace("*channelname*", "");
            }
        }
        formattedFilename = formattedFilename.replace("*ext*", "." + extension);
        // Insert filename at the end to prevent errors with tags
        formattedFilename = formattedFilename.replace("*videoname*", videoName);
        return formattedFilename;
    }

    /**
     * Prevents more than one free download from starting at a given time. One step prior to dl.startDownload(), it adds a slot to maxFree
     * which allows the next singleton download to start, or at least try.
     *
     * This is needed because xfileshare(website) only throws errors after a final dllink starts transferring or at a given step within pre
     * download sequence. But this template(XfileSharingProBasic) allows multiple slots(when available) to commence the download sequence,
     * this.setstartintival does not resolve this issue. Which results in x(20) captcha events all at once and only allows one download to
     * start. This prevents wasting peoples time and effort on captcha solving and|or wasting captcha trading credits. Users will experience
     * minimal harm to downloading as slots are freed up soon as current download begins.
     *
     * @param controlFree
     *            (+1|-1)
     */
    public synchronized void controlPremium(final int num) {
        logger.info("maxPremium was = " + maxPremium.get());
        maxPremium.set(Math.min(Math.max(1, maxPremium.addAndGet(num)), totalMaxSimultanFreeAccount.get()));
        logger.info("maxPremium now = " + maxPremium.get());
    }

    public synchronized void controlFree(final int num) {
        logger.info("maxFree was = " + maxFree.get());
        maxFree.set(Math.min(Math.max(1, maxFree.addAndGet(num)), totalMaxSimultanFree.get()));
        logger.info("maxFree now = " + maxFree.get());
    }

    @Override
    public String getDescription() {
        return "JDownloader's nicovideo.jp plugin helps downloading videoclips. JDownloader provides settings for the filenames.";
    }

    private final static String defaultCustomFilename = "*videoname**ext*";

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the filename properties"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_DATE, JDL.L("plugins.hoster.nicovideojp.customdate", "Define how the date should look.")).setDefaultValue("dd.MM.yyyy_HH-mm-ss"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the filename! Example: '*channelname*_*date*_*videoname**ext*'"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME, JDL.L("plugins.hoster.nicovideojp.customfilename", "Define how the filenames should look:")).setDefaultValue(defaultCustomFilename));
        final StringBuilder sb = new StringBuilder();
        sb.append("Explanation of the available tags:\r\n");
        sb.append("*channelname* = name of the channel/uploader\r\n");
        sb.append("*date* = date when the video was posted - appears in the user-defined format above\r\n");
        sb.append("*videoname* = name of the video without extension\r\n");
        sb.append("*videoid* = ID of the video e.g. 'sm12345678'\r\n");
        sb.append(String.format("*ext* = the extension of the file, in this case usually '.%s'", default_extension));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sb.toString()));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        /* 2020-03-25: This setting is broken atm. --> Hardcoded disabled it */
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), NicoVideoJp.AVOID_ECONOMY_MODE, "Avoid economy mode - only download higher quality .mp4 videos?\r\n<html><b>Important: The default extension of all filenames is " + default_extension + ". It will be corrected once the downloads start if either this setting is active or the nicovideo site is in normal (NOT economy) mode!\r\nIf this setting is active and nicovideo is in economy mode, JDownloader will wait " + economy_active_wait_minutes + " minutes and try again afterwards.</br>\r\n2020-03-25: This setting is currently broken and thus hardcoded disabled!!</b></html>").setDefaultValue(false).setEnabled(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), NicoVideoJp.NOCHUNKS, "Enable Chunk Workaround").setDefaultValue(true));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}