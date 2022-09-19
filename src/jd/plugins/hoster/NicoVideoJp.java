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

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.requests.PostRequest;
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

import org.appwork.storage.JSonStorage;
import org.appwork.utils.StringUtils;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "nicovideo.jp" }, urls = { "https?://(?:www\\.)?nicovideo\\.jp/watch/(?:sm|so|nm)?(\\d+)" })
public class NicoVideoJp extends PluginForHost {
    private static final String  CUSTOM_DATE               = "CUSTOM_DATE";
    private static final String  CUSTOM_FILENAME           = "CUSTOM_FILENAME";
    private static final String  TYPE_NM                   = "https?://[^/]+/watch/nm\\d+";
    private static final String  TYPE_SM                   = "https?://[^/]+/watch/sm\\d+";
    private static final String  TYPE_SO                   = "https?://[^/]+/watch/so\\d+";
    /* Other types may redirect to this type. This is the only type which is also downloadable without account (sometimes?). */
    private static final String  TYPE_WATCH                = "https?://[^/]+/watch/\\d+";
    private static final String  default_extension         = "mp4";
    private static final boolean RESUME                    = true;
    private static final int     MAXCHUNKS                 = 0;
    private static final int     MAXDLS                    = -1;
    private Map<String, Object>  entries                   = null;
    private final String         PROPERTY_TITLE            = "title";
    private final String         PROPERTY_DATE_ORIGINAL    = "originaldate";
    private final String         PROPERTY_ACCOUNT_REQUIRED = "account_required";

    public NicoVideoJp(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://site.nicovideo.jp/premium_contents/");
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
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        final String fid = getFID(link);
        if (!link.isNameSet()) {
            link.setName(fid + ".mp4");
        }
        link.setProperty("extension", default_extension);
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(400);
        if (account != null) {
            this.login(account, false);
        }
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 400) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getURL().contains("account.nicovideo") || br.getHttpConnection().getResponseCode() == 403) {
            /* 2020-06-04: Redirect to login page = account required, response 403 = private video */
            /* Account required */
            if (account != null) {
                /* WTF, we should be logged-in! */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Expired session or account is lacking permissions to access this video", 5 * 60 * 1000l);
            } else {
                // return AvailableStatus.TRUE;
                throw new AccountRequiredException();
            }
        }
        if (br.containsHTML("(?i)this video inappropriate\\.<")) {
            final String watch = br.getRegex("(?i)harmful_link\" href=\"([^<>\"]*?)\">Watch this video</a>").getMatch(0);
            if (watch == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(watch);
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* This title is e.g. useful when a video is GEO-blocked. Example: https://www.nicovideo.jp/watch/so39439590 */
        String fallbackTitle = br.getRegex("<title>([^<]+)Niconico Video</title>").getMatch(0);
        if (fallbackTitle != null) {
            fallbackTitle = Encoding.htmlDecode(fallbackTitle).trim();
            link.setName(fallbackTitle + ".mp4");
        }
        String jsonapi = br.getRegex("data-api-data=\"([^\"]+)").getMatch(0);
        if (jsonapi != null) {
            jsonapi = jsonapi.replace("&quot;", "\"");
            entries = JavaScriptEngineFactory.jsonToJavaMap(jsonapi);
            final Map<String, Object> video = (Map<String, Object>) entries.get("video");
            final String description = (String) video.get("description");
            link.setProperty(PROPERTY_TITLE, Encoding.htmlDecode(video.get("title").toString()));
            final String registeredAt = (String) video.get("registeredAt");
            if (!StringUtils.isEmpty(registeredAt)) {
                link.setProperty(PROPERTY_DATE_ORIGINAL, registeredAt);
            }
            link.setFinalFileName(getFormattedFilename(link));
            // link.setDownloadSize((173222l + 64000l) / 8 * 241);
            if (!StringUtils.isEmpty(description) && link.getComment() == null) {
                link.setComment(Encoding.htmlDecode(description));
            }
            link.setProperty(PROPERTY_ACCOUNT_REQUIRED, video.get("isAuthenticationRequired"));
            if ((Boolean) video.get("isDeleted")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        return login(account, true);
    }

    @Override
    public String getAGBLink() {
        return "https://info.nicovideo.jp/base/rule.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return MAXDLS;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return MAXDLS;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link, null);
        handleDownload(link, null);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        if (link.getBooleanProperty(PROPERTY_ACCOUNT_REQUIRED, false) && account == null) {
            throw new AccountRequiredException();
        } else if (br.containsHTML("(?)>\\s*Sorry, this video can only be viewed in the same region where it was uploaded")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "GEO-blocked");
        }
        final Map<String, Object> delivery = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "media/delivery");
        final Map<String, Object> movie = (Map<String, Object>) delivery.get("movie");
        final List<Map<String, Object>> audios = (List<Map<String, Object>>) movie.get("audios");
        final List<Map<String, Object>> videos = (List<Map<String, Object>>) movie.get("videos");
        /* Find best audio- and video quality: First available should be best */
        final String audioID = getHighestQualityStr(audios);
        final String videoID = getHighestQualityStr(videos);
        final Map<String, Object> session = (Map<String, Object>) movie.get("session");
        final Map<String, Object> apiInfo = (Map<String, Object>) JavaScriptEngineFactory.walkJson(session, "urls/{0}");
        final String apiURL = apiInfo.get("url").toString();
        final List<String> sessionAudios = (List<String>) session.get("audios");
        final List<String> sessionVideos = (List<String>) session.get("videos");
        final String sessionAudiosStr = sessionAudios.toString().replaceAll("(\\w+)(,|)", "\"$1\"$2").replaceAll("\\s+", "");
        final String sessionVideosStr = sessionVideos.toString().replaceAll("(\\w+)(,|)", "\"$1\"$2").replaceAll("\\s+", "");
        final String signature = session.get("signature").toString();
        // final long created_time = ((Number) entries.get("created_time")).longValue();
        // final long expire_time = ((Number) entries.get("expire_time")).longValue();
        final String token = session.get("token").toString();
        final String service_user_id = session.get("serviceUserId").toString();
        final Map<String, Object> tokenMap = JavaScriptEngineFactory.jsonToJavaMap(token);
        final String recipe_id = tokenMap.get("recipe_id").toString();
        final String player_id = tokenMap.get("player_id").toString();
        // final Map<String, Object> auth_types = (Map<String, Object>) session.get("auth_types");
        final String postData = "{\"session\":{\"recipe_id\":\"" + recipe_id + "\",\"content_id\":\"out1\",\"content_type\":\"movie\",\"content_src_id_sets\":[{\"content_src_ids\":[{\"src_id_to_mux\":{\"video_src_ids\":" + sessionVideosStr + ",\"audio_src_ids\":" + sessionAudiosStr + "}},{\"src_id_to_mux\":{\"video_src_ids\":[\"" + videoID + "\"],\"audio_src_ids\":[\"" + audioID + "\"]}}]}],\"timing_constraint\":\"unlimited\",\"keep_method\":{\"heartbeat\":{\"lifetime\":120000}},\"protocol\":{\"name\":\"http\",\"parameters\":{\"http_parameters\":{\"parameters\":{\"hls_parameters\":{\"use_well_known_port\":\"" + booleanToYesNo((Boolean) apiInfo.get("isWellKnownPort")) + "\",\"use_ssl\":\"" + booleanToYesNo((Boolean) apiInfo.get("isSsl"))
                + "\",\"transfer_preset\":\"\",\"segment_duration\":6000}}}}},\"content_uri\":\"\",\"session_operation_auth\":{\"session_operation_auth_by_signature\":{\"token\":\"" + token.replaceAll("\"", "\\\\\"") + "\",\"signature\":\"" + signature + "\"}},\"content_auth\":{\"auth_type\":\"ht2\",\"content_key_timeout\":600000,\"service_id\":\"nicovideo\",\"service_user_id\":\"" + service_user_id + "\"},\"client_info\":{\"player_id\":\"" + player_id + "\"},\"priority\":" + session.get("priority") + "}}";
        br.getHeaders().put("Accept", "application/json");
        br.getHeaders().put("Content-Type", "application/json");
        br.getHeaders().put("Origin", "https://www." + this.getHost());
        br.postPageRaw(apiURL + "?_format=json", postData);
        final Map<String, Object> response = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final Map<String, Object> data = (Map<String, Object>) response.get("data");
        final Map<String, Object> responseSession = (Map<String, Object>) data.get("session");
        final String streamURL = responseSession.get("content_uri").toString();
        if (StringUtils.isEmpty(streamURL)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        logger.info("Chosen media quality IDs: Audio: " + audioID + " | Video: " + videoID);
        /* Now acquire permission to watch/download this video --> Seems like this is not needed? */
        // br.getPage("https://nvapi.nicovideo.jp/v1/2ab0cbaa/watch?t=" +
        // URLEncode.decodeURIComponent(delivery.get("trackingId").toString()));
        /* Without this "heartbeat", our HLS stream would be invalid after ~120 seconds. */
        final HeartbeatThread heartbeat = new HeartbeatThread(br.cloneBrowser(), apiURL, session, response);
        heartbeat.start();
        try {
            final boolean isHLS = true;
            if (isHLS) {
                br.getPage(streamURL);
                final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
                checkFFmpeg(link, "Download a HLS Stream");
                dl = new HLSDownloader(link, br, hlsbest.getDownloadurl());
                dl.startDownload();
            } else {
                dl = new jd.plugins.BrowserAdapter().openDownload(br, link, streamURL, RESUME, MAXCHUNKS);
                if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                    logger.warning("The final dllink seems not to be a file!");
                    try {
                        br.followConnection(true);
                    } catch (final IOException e) {
                        logger.log(e);
                    }
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String final_filename = getFormattedFilename(link);
                link.setFinalFileName(final_filename);
                dl.startDownload();
            }
        } finally {
            heartbeat.interrupt();
        }
    }

    class HeartbeatThread extends Thread {
        final Browser             br;
        final String              apiURL;
        final Map<String, Object> session;
        final Map<String, Object> response;
        volatile boolean          runFlag = true;

        HeartbeatThread(final Browser br, final String apiURL, final Map<String, Object> session, final Map<String, Object> response) {
            this.br = br;
            this.apiURL = apiURL;
            this.session = session;
            this.response = response;
            setDaemon(true);
        }

        public void run() {
            final long heartbeatLifetime = ((Number) session.get("heartbeatLifetime")).longValue(); // typically 120000
            final long heartbeatIntervalMillis = heartbeatLifetime / 3;
            final Map<String, Object> responseSession = (Map<String, Object>) JavaScriptEngineFactory.walkJson(response, "data/session");
            PostRequest heatbeat = null;
            try {
                heatbeat = new PostRequest(apiURL + "/" + responseSession.get("id") + "?_format=json&_method=PUT");
            } catch (final Throwable e) {
            }
            final String json = JSonStorage.serializeToJson(response.get("data"));
            heatbeat.setPostDataString(json);
            while (runFlag) {
                try {
                    /* Typically wait 40 seconds */
                    sleep(heartbeatIntervalMillis);
                    br.getPage(heatbeat);
                } catch (final Throwable e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        @Override
        public void interrupt() {
            runFlag = false;
            super.interrupt();
        }
    }

    private String getHighestQualityStr(final List<Map<String, Object>> medias) {
        return getHighestQualityMap(medias).get("id").toString();
    }

    private Map<String, Object> getHighestQualityMap(final List<Map<String, Object>> medias) {
        Map<String, Object> highestQualityMap = null;
        /* Lowest number = best */
        int highestQualityLevel = -10;
        for (final Map<String, Object> media : medias) {
            if ((Boolean) media.get("isAvailable")) {
                final Map<String, Object> metadata = (Map<String, Object>) media.get("metadata");
                final int levelIndex;
                if (media.get("id").toString().contains("low")) {
                    /*
                     * 2022-07-27: Not sure about this but seems like if low (video-)quality is available, we need to prefer that otherwise
                     * we'll get an error 500 later on.
                     */
                    levelIndex = -3;
                } else {
                    levelIndex = ((Number) metadata.get("levelIndex")).intValue();
                }
                if (levelIndex < highestQualityLevel || highestQualityMap == null) {
                    highestQualityMap = media;
                    highestQualityLevel = levelIndex;
                }
            }
        }
        return highestQualityMap;
    }

    private String booleanToYesNo(final boolean bool) {
        if (bool) {
            return "yes";
        } else {
            return "no";
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account);
        handleDownload(link, account);
    }

    /**
     * orce = check cookies and perform a full login if that fails. !force = Accept cookies without checking if they're not older than
     * trust_cookie_age.
     */
    private AccountInfo login(final Account account, final boolean validateCookies) throws Exception {
        final AccountInfo ai = new AccountInfo();
        synchronized (account) {
            this.setBrowserExclusive();
            final Cookies userCookies = account.loadUserCookies();
            if (userCookies == null || userCookies.isEmpty()) {
                showCookieLoginInfo();
                throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_required());
            }
            br.setCookies(userCookies);
            if (!validateCookies) {
                return null;
            }
            br.getPage("https://account.nicovideo.jp/my/account");
            if (!isLoggedIn(br)) {
                if (account.hasEverBeenValid()) {
                    throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                } else {
                    throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                }
            }
            /*
             * When using cookie login user can enter whatever he wants into username field but we prefer unique names so user cannot add
             * the same account for the same service twice.
             */
            final String username = br.getRegex("class=\"item profile-nickname\"[^>]*>([^<]+)<").getMatch(0);
            if (username != null) {
                account.setUser(Encoding.htmlDecode(username).trim());
            }
            if (br.containsHTML("(?i)<span class=\"membership--status\">(?:Yearly|Monthly|Weekly|Daily) plan</span>")) {
                account.setType(AccountType.PREMIUM);
            } else {
                account.setType(AccountType.FREE);
            }
            ai.setUnlimitedTraffic();
            return ai;
        }
    }

    private boolean isLoggedIn(final Browser br) {
        if (br.containsHTML("/logout")) {
            return true;
        } else {
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    private String getFormattedFilename(final DownloadLink link) throws ParseException {
        final String extension = link.getStringProperty("extension", default_extension);
        final String videoid = this.getFID(link);
        String title = link.getStringProperty(PROPERTY_TITLE, null);
        final SubConfiguration cfg = SubConfiguration.getConfig(this.getHost());
        String formattedFilename = cfg.getStringProperty(CUSTOM_FILENAME, defaultCustomFilename);
        if (StringUtils.isEmpty(formattedFilename)) {
            /* Fallback */
            formattedFilename = defaultCustomFilename;
        }
        String dateStr = link.getStringProperty(PROPERTY_DATE_ORIGINAL);
        final String channelName = link.getStringProperty("channel", "");
        String formattedDate = null;
        if (dateStr != null && formattedFilename.contains("*date*")) {
            dateStr = dateStr.replace("T", ":");
            final String userDefinedDateFormat = cfg.getStringProperty(CUSTOM_DATE, "dd.MM.yyyy_HH-mm-ss");
            final Date date = new SimpleDateFormat("yyyy-MM-dd:HH:mm+ssss").parse(dateStr);
            if (userDefinedDateFormat != null) {
                try {
                    final SimpleDateFormat formatter = new SimpleDateFormat(userDefinedDateFormat);
                    formattedDate = formatter.format(date);
                } catch (Exception e) {
                    // prevent user error killing plugin.
                    formattedDate = "";
                }
            }
            formattedFilename = formattedFilename.replace("*date*", formattedDate);
        }
        formattedFilename = formattedFilename.replace("*videoid*", videoid);
        formattedFilename = formattedFilename.replace("*channelname*", channelName);
        formattedFilename = formattedFilename.replace("*ext*", "." + extension);
        // Insert filename at the end to prevent errors with tags
        formattedFilename = formattedFilename.replace("*videoname*", title);
        return formattedFilename;
    }

    @Override
    public String getDescription() {
        return "JDownloader's nicovideo.jp plugin helps downloading videoclips. JDownloader provides settings for the filenames.";
    }

    private final static String defaultCustomFilename = "*videoname**ext*";

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the filename properties"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_DATE, "Define how the date should look.").setDefaultValue("dd.MM.yyyy_HH-mm-ss"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize the filename! Example: '*channelname*_*date*_*videoname**ext*'"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME, "Define how the filenames should look:").setDefaultValue(defaultCustomFilename));
        final StringBuilder sb = new StringBuilder();
        sb.append("Explanation of the available tags:\r\n");
        sb.append("*channelname* = name of the channel/uploader\r\n");
        sb.append("*date* = date when the video was posted - appears in the user-defined format above\r\n");
        sb.append("*videoname* = name of the video without extension\r\n");
        sb.append("*videoid* = ID of the video e.g. 'sm12345678'\r\n");
        sb.append(String.format("*ext* = the extension of the file, in this case usually '.%s'", default_extension));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sb.toString()));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}