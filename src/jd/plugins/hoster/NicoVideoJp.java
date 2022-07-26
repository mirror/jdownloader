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

import org.appwork.utils.StringUtils;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
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
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "nicovideo.jp" }, urls = { "https?://(?:www\\.)?nicovideo\\.jp/watch/(?:sm|so|nm)?(\\d+)" })
public class NicoVideoJp extends PluginForHost {
    private static final String  MAINPAGE                    = "https://www.nicovideo.jp/";
    private static final String  ONLYREGISTEREDUSERTEXT      = "Only downloadable for registered users";
    private static final String  CUSTOM_DATE                 = "CUSTOM_DATE";
    private static final String  CUSTOM_FILENAME             = "CUSTOM_FILENAME";
    private static final String  TYPE_NM                     = "https?://(www\\.)?nicovideo\\.jp/watch/nm\\d+";
    private static final String  TYPE_SM                     = "https?://(www\\.)?nicovideo\\.jp/watch/sm\\d+";
    private static final String  TYPE_SO                     = "https?://(www\\.)?nicovideo\\.jp/watch/so\\d+";
    /* Other types may redirect to this type. This is the only type which is also downloadable without account (sometimes?). */
    private static final String  TYPE_WATCH                  = "https?://(www\\.)?nicovideo\\.jp/watch/\\d+";
    private static final String  default_extension           = "mp4";
    private static final boolean RESUME                      = true;
    private static final int     MAXCHUNKS                   = 0;
    private static final int     MAXDLS                      = 1;
    private static final int     economy_active_wait_minutes = 30;
    private static final String  html_account_needed         = "account\\.nicovideo\\.jp/register\\?from=watch\\&mode=landing\\&sec=not_login_watch";
    public static final long     trust_cookie_age            = 300000l;
    private Map<String, Object>  entries                     = null;
    private final String         PROPERTY_TITLE              = "title";
    private final String         PROPERTY_DATE_ORIGINAL      = "originaldate";
    private final String         PROPERTY_ACCOUNT_REQUIRED   = "account_required";

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
        } else if (br.containsHTML("class=\"channel-invitation-box-title-text\"")) {
            /* Channel membership required to watch this content */
            if (account != null) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Expired session or channel membership required?", 5 * 60 * 1000l);
            } else {
                throw new AccountRequiredException();
            }
        }
        if (br.containsHTML("this video inappropriate.<")) {
            final String watch = br.getRegex("(?i)harmful_link\" href=\"([^<>\"]*?)\">Watch this video</a>").getMatch(0);
            if (watch == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(watch);
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String jsonapi = br.getRegex("data-api-data=\"([^\"]+)").getMatch(0);
        jsonapi = Encoding.htmlDecode(jsonapi);
        entries = JavaScriptEngineFactory.jsonToJavaMap(jsonapi);
        final Map<String, Object> video = (Map<String, Object>) entries.get("video");
        final String description = (String) video.get("description");
        link.setProperty(PROPERTY_TITLE, video.get("title"));
        final String registeredAt = (String) video.get("registeredAt");
        if (!StringUtils.isEmpty(registeredAt)) {
            link.setProperty(PROPERTY_DATE_ORIGINAL, registeredAt);
        }
        link.setFinalFileName(getFormattedFilename(link));
        // link.setDownloadSize((173222l + 64000l) / 8 * 241);
        if (br.containsHTML(html_account_needed)) {
            link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.nicovideojp.only4registered", ONLYREGISTEREDUSERTEXT));
        }
        if (!StringUtils.isEmpty(description) && link.getComment() == null) {
            link.setComment(description);
        }
        link.setProperty(PROPERTY_ACCOUNT_REQUIRED, video.get("isAuthenticationRequired"));
        if ((Boolean) video.get("isDeleted")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        if (!account.getUser().matches(".+@.+\\..+")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte gib deine E-Mail Adresse ins Benutzername Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail address in the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
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
        // checkWatchableGeneral();
        if (link.getBooleanProperty(PROPERTY_ACCOUNT_REQUIRED) && account == null) {
            throw new AccountRequiredException();
        }
        // TODO: Re-add errorhandling for GEO-blocked items
        final Map<String, Object> movie = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "media/delivery/movie");
        final List<Map<String, Object>> audios = (List<Map<String, Object>>) movie.get("audios");
        final List<Map<String, Object>> videos = (List<Map<String, Object>>) movie.get("videos");
        /* Find best audio- and video quality: First available should be best */
        String audioID = null, videoID = null;
        for (final Map<String, Object> audio : audios) {
            if ((Boolean) audio.get("isAvailable")) {
                audioID = audio.get("id").toString();
                break;
            }
        }
        for (final Map<String, Object> video : videos) {
            if ((Boolean) video.get("isAvailable")) {
                videoID = video.get("id").toString();
                break;
            }
        }
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
        final Map<String, Object> tokenMap = JavaScriptEngineFactory.jsonToJavaMap(token);
        final String recipe_id = tokenMap.get("recipe_id").toString();
        final String player_id = tokenMap.get("player_id").toString();
        final String service_user_id = tokenMap.get("service_user_id").toString();
        // final Map<String, Object> auth_types = (Map<String, Object>) session.get("auth_types");
        final String postData = "{\"session\":{\"recipe_id\":\"" + recipe_id + "\",\"content_id\":\"out1\",\"content_type\":\"movie\",\"content_src_id_sets\":[{\"content_src_ids\":[{\"src_id_to_mux\":{\"video_src_ids\":" + sessionVideosStr + ",\"audio_src_ids\":" + sessionAudiosStr + "}},{\"src_id_to_mux\":{\"video_src_ids\":[\"archive_h264_360p_low\"],\"audio_src_ids\":[\"" + audioID + "\"]}}]}],\"timing_constraint\":\"unlimited\",\"keep_method\":{\"heartbeat\":{\"lifetime\":120000}},\"protocol\":{\"name\":\"http\",\"parameters\":{\"http_parameters\":{\"parameters\":{\"hls_parameters\":{\"use_well_known_port\":\"" + booleanToYesNo((Boolean) apiInfo.get("isWellKnownPort")) + "\",\"use_ssl\":\"" + booleanToYesNo((Boolean) apiInfo.get("isSsl"))
                + "\",\"transfer_preset\":\"\",\"segment_duration\":6000}}}}},\"content_uri\":\"\",\"session_operation_auth\":{\"session_operation_auth_by_signature\":{\"token\":\"" + token.replaceAll("\"", "\\\\\"") + "\",\"signature\":\"" + signature + "\"}},\"content_auth\":{\"auth_type\":\"ht2\",\"content_key_timeout\":600000,\"service_id\":\"nicovideo\",\"service_user_id\":\"" + service_user_id + "\"},\"client_info\":{\"player_id\":\"" + player_id + "\"},\"priority\":0}}";
        br.getHeaders().put("Accept", "application/json");
        br.getHeaders().put("Content-Type", "application/json");
        br.getHeaders().put("Origin", "https://www." + this.getHost());
        br.postPageRaw(apiURL + "?_format=json", postData);
        if (br.containsHTML(html_account_needed)) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, ONLYREGISTEREDUSERTEXT, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        final Map<String, Object> response = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final Map<String, Object> session2 = (Map<String, Object>) JavaScriptEngineFactory.walkJson(response, "data/session");
        final String dllink = session2.get("content_uri").toString();
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final boolean isHLS = true;
        if (isHLS) {
            br.getPage(dllink);
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, hlsbest.getDownloadurl());
            dl.startDownload();
        } else {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, RESUME, MAXCHUNKS);
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
    private AccountInfo login(final Account account, final boolean force) throws Exception {
        final AccountInfo ai = new AccountInfo();
        synchronized (account) {
            this.setBrowserExclusive();
            final Cookies cookies = account.loadCookies("");
            if (cookies != null && !force) {
                /* 2016-05-04: Avoid full login whenever possible! */
                br.setCookies(this.getHost(), cookies);
                logger.info("Attempting cookie login");
                if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= trust_cookie_age && !force) {
                    /* We trust these cookies --> Do not check them */
                    logger.info("Trust cookies without check");
                    return null;
                }
                br.getPage("https://www.nicovideo.jp/");
                if (br.containsHTML("(?i)/logout\">Log out</a>")) {
                    /* Save new cookie timestamp */
                    logger.info("Cookie login successful");
                    br.setCookies(this.getHost(), cookies);
                    return null;
                }
                logger.info("Cookie login failed");
            }
            logger.info("Performing full login");
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
                if (br.getCookie(MAINPAGE, "user_session", Cookies.NOTDELETEDPATTERN) == null) {
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
            if (br.containsHTML("(?i)<span class=\"membership--status\">(?:Yearly|Monthly|Weekly|Daily) plan</span>")) {
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
        String date = link.getStringProperty(PROPERTY_DATE_ORIGINAL);
        final String channelName = link.getStringProperty("channel", "");
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
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}