//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.net.URLHelper;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
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
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "soundcloud.com" }, urls = { "https://(?:www\\.)?soundclouddecrypted\\.com/[A-Za-z\\-_0-9]+/[A-Za-z\\-_0-9]+(/[A-Za-z\\-_0-9]+)?" })
public class SoundcloudCom extends PluginForHost {
    public SoundcloudCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
        this.setConfigElements();
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        if (cookieLoginOnly) {
            return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.AUDIO_STREAMING, LazyPlugin.FEATURE.COOKIE_LOGIN_ONLY };
        } else {
            return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.AUDIO_STREAMING, LazyPlugin.FEATURE.COOKIE_LOGIN_OPTIONAL };
        }
    }

    /*
     * Last clientids (old to new): 2t9loNQH90kzJcsFCODdigxfp325aq4z, b45b1aa10f1ac2941910a7f0d10f8e28 fDoItMDbsbZz8dY16ZzARCZmzgHBPotA
     * 3904229f42df3999df223f6ebf39a8fe
     */
    public final static String   CLIENTID_8TRACKS                                                      = "3904229f42df3999df223f6ebf39a8fe";
    /* Another way to get final links: http://api.soundcloud.com/tracks/11111xxx_test_track_ID1111111/streams?format=json&consumer_key= */
    // public final static String CONSUMER_KEY_MYCLOUDPLAYERS_COM = "PtMyqifCQMKLqwP0A6YQ";
    /* json: media/encodings */
    public static final String[] stream_qualities                                                      = { "stream_url", "http_mp3_128_url", "hls_mp3_128_url" };
    // public static final String[] streamtypes = { "download", "stream", "streams" };
    private final static String  AUDIO_QUALITY_SELECTION_MODE                                          = "ONLY_DOWNLOAD_OFFICIALLY_DOWNLOADABLE_FILES";
    private final String         ALLOW_PREVIEW_DOWNLOAD                                                = "ALLOW_PREVIEW_DOWNLOAD";
    private final static String  CUSTOM_DATE                                                           = "CUSTOM_DATE";
    private final static String  CUSTOM_FILENAME_2                                                     = "CUSTOM_FILENAME_2";
    private final String         GRAB_PURCHASE_URL                                                     = "GRAB_PURCHASE_URL";
    private final String         GRAB500THUMB                                                          = "GRAB500THUMB";
    private final String         GRABORIGINALTHUMB                                                     = "GRABORIGINALTHUMB";
    private final String         CUSTOM_PACKAGENAME                                                    = "CUSTOM_PACKAGENAME";
    private final static String  SETS_ADD_POSITION_TO_FILENAME                                         = "SETS_ADD_POSITION_TO_FILENAME";
    private final static String  ENFORCE_FILESIZE_CALCULATION_EVEN_FOR_OFFICIALLY_DOWNLOADABLE_CONTENT = "ENFORCE_FILESIZE_CALCULATION_EVEN_FOR_OFFICIALLY_DOWNLOADABLE_CONTENT";
    private String               dllink                                                                = null;
    /* DownloadLink Filename / Packagizer properties */
    public static final String   PROPERTY_setsposition                                                 = "setsposition";
    public static final String   PROPERTY_secret_token                                                 = "secret_token_v2";
    public static final String   PROPERTY_playlist_id                                                  = "playlist_id";
    public static final String   PROPERTY_track_id                                                     = "track_id";
    public static final String   PROPERTY_url_username                                                 = "url_username";
    public static final String   PROPERTY_channel                                                      = "channel";
    public static final String   PROPERTY_title                                                        = "plainfilename";
    public static final String   PROPERTY_originaldate                                                 = "originaldate";
    public static final String   PROPERTY_directurl                                                    = "directurl";
    public static final String   PROPERTY_filetype                                                     = "type";
    public static final String   PROPERTY_chosen_quality                                               = "chosen_quality";
    public static final String   PROPERTY_duration_seconds                                             = "duration_seconds";
    public static final String   PROPERTY_QUALITY_sq                                                   = "sq";
    public static final String   PROPERTY_QUALITY_hq                                                   = "hq";
    public static final String   PROPERTY_STATE                                                        = "state";
    /* Account properties */
    private final String         PROPERTY_ACCOUNT_oauthtoken                                           = "oauthtoken";
    public static final String   PROPERTY_ACCOUNT_userid                                               = "userid";
    public static final String   PROPERTY_ACCOUNT_created_at                                           = "created_at";
    public static final String   PROPERTY_ACCOUNT_username                                             = "username";
    public static final String   PROPERTY_ACCOUNT_permalink                                            = "permalink";
    /* 2024-06-10: Account is needed for official downloads. */
    public static final boolean  ACCOUNT_NEEDED_FOR_OFFICIAL_DOWNLOADS                                 = true;
    /* API base URLs */
    public static final String   API_BASEv2                                                            = "https://api-v2.soundcloud.com";
    /* 2020-12-15: Website/API login is broken thus only cookie login is possible */
    private static final boolean cookieLoginOnly                                                       = true;

    public void correctDownloadLink(final DownloadLink link) {
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replace("soundclouddecrypted", "soundcloud"));
    }

    @Override
    public String getAGBLink() {
        return "https://soundcloud.com/terms-of-use";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    private static AtomicReference<String> appVersion = new AtomicReference<String>(null);
    private static AtomicReference<String> clientId   = new AtomicReference<String>(null);

    public static final String getAppVersion(final Browser obr) throws Exception {
        synchronized (appVersion) {
            if (appVersion.get() == null) {
                initValues(obr);
            }
            return appVersion.get();
        }
    }

    public static final String getClientId(final Browser obr) throws Exception {
        synchronized (clientId) {
            if (clientId.get() == null) {
                initValues(obr);
            }
            return clientId.get();
        }
    }

    private static String getClientIdV2() {
        return "CoeTA81rlM4PNaXs33YeRXZZAixneGwv";
    }

    private static String getAppVersionV2() {
        return "1549538778";
    }

    private static String getAppLocaleV2() {
        return "en";
    }

    private static void initValues(final Browser obr) throws Exception {
        /*
         * Important: Whenever the current Browser contains a json response it is definitely necessary to access the mainpage to find the
         * appVersion value!
         */
        final boolean requiresNewBrowser = obr == null || obr.getURL() == null || obr.toString().startsWith("{") || !Browser.getHost(obr.getURL()).equals("soundcloud.com") ? true : false;
        Browser br = requiresNewBrowser ? new Browser() : obr.cloneBrowser();
        if (appVersion.get() == null) {
            if (requiresNewBrowser) {
                br.getPage("https://soundcloud.com/");
            }
            final String av = br.getRegex("window\\.__sc_version\\s*=\\s*\"(\\d+)\"").getMatch(0);
            if (StringUtils.isEmpty(av)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                appVersion.set(av);
            }
        }
        if (clientId.get() == null) {
            if (br == null || br._getURL() == null || !br._getURL().getPath().equals("/")) {
                br = obr.cloneBrowser();
                br.getPage("https://soundcloud.com/");
            }
            final String script = br.getRegex("<script(?:\\s+[^>]+|\\s+)src\\s*=\\s*(\"|')([^>]*/app-[^>]*\\.js)\\1").getMatch(1);
            if (script != null) {
                br.getHeaders().put("Accept", "*/*");
                br.getPage(script);
                final String ci = br.getRegex("[^a-zA-Z0-9_-]+client_id\\s*:\\s*(\"|')([a-zA-Z0-9]+)\\1").getMatch(1);
                if (StringUtils.isNotEmpty(ci)) {
                    clientId.set(ci);
                }
            }
            if (clientId.get() == null) {
                final String assets[] = br.getRegex("(https?://[a-z0-9\\-]+\\.sndcdn\\.com/assets/[^\"]*?\\.js)").getColumn(0);
                for (String asset : assets) {
                    final Browser brc = br.cloneBrowser();
                    brc.getPage(asset);
                    final String ci = brc.getRegex("client_id\\s*:\\s*\"([a-zA-Z0-9]+)\"").getMatch(0);
                    if (StringUtils.isNotEmpty(ci)) {
                        clientId.set(ci);
                        break;
                    }
                }
            }
            if (clientId.get() == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this);
        return requestFileInformation(link, account, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        dllink = null;
        prepBR(br);
        if (account != null) {
            login(br, account, false);
        }
        final String songid = link.getStringProperty(PROPERTY_track_id);
        if (songid == null) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* 2021-06-30: New */
        final UrlQuery query = new UrlQuery();
        final String secret_token = link.getStringProperty(PROPERTY_secret_token);
        if (secret_token != null) {
            query.add("secret_token", Encoding.urlEncode(secret_token));
        }
        query.add("client_id", getClientId(br));
        query.add("app_version", getAppVersion(br));
        query.add("app_locale", getAppLocaleV2());
        /* Old way: */
        // br.getPage(API_BASEv2 + "/tracks?urns=soundcloud%3Atracks%3A" + songid + "&client_id=" + getClientId(br) + "&app_version=" +
        // SoundcloudCom.getAppVersion(br));
        br.getPage(API_BASEv2 + "/tracks/soundcloud:tracks:" + songid + "?" + query.toString());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 401) {
            // keys are incorrect --> This should never happen?!
            logger.warning("Error 401 --> Incorrect keys??");
            resetThis();
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        final Map<String, Object> response = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final AvailableStatus status = checkStatusJson(this, link, account, response);
        if (status.equals(AvailableStatus.FALSE)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String state = link.getStringProperty(PROPERTY_STATE);
        if (!"finished".equals(state)) {
            /* E.g. items which are still being processed and thus aren't streamable nor downloadable. */
            throw new PluginException(LinkStatus.ERROR_FATAL, "Item is not ready yet | Internal status: " + state);
        }
        /* "ALLOW" or "MONETIZE" = all good */
        final String songPolicy = (String) response.get("policy");
        if (StringUtils.equalsIgnoreCase(songPolicy, "BLOCK")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "This content is GEO-blocked");
        }
        if (isDownload) {
            boolean isOnlyPreviewDownloadable = false;
            final boolean looksLikeOfficiallyDownloadable = looksLikeOfficiallyDownloadable(response);
            if (songPolicy != null && songPolicy.equalsIgnoreCase("SNIP")) {
                /**
                 * Typically previews will also have a duration value of only "30000" --> 30 seconds </br>
                 * When logged in with a Soundcloud premium account, songs for which before only previews were available may change to
                 * "POLICY":"MONETIZE" --> Can be fully streamed by the user.
                 */
                isOnlyPreviewDownloadable = true;
            }
            final AudioQualitySelectionMode mode = getAudioQualitySelectionMode();
            if (mode == AudioQualitySelectionMode.ONLY_OFFICIAL_DOWNLOADS && !looksLikeOfficiallyDownloadable) {
                /* User only wants to download only officially downloadable items but this one is not officially downloadable. */
                throw new PluginException(LinkStatus.ERROR_FATAL, getPhrase("ERROR_NOT_OFFICIALLY_DOWNLOADABLE"));
            } else if (mode == AudioQualitySelectionMode.ONLY_OFFICIAL_DOWNLOADS && ACCOUNT_NEEDED_FOR_OFFICIAL_DOWNLOADS && looksLikeOfficiallyDownloadable && account == null) {
                /*
                 * User only wants to download only officially downloadable items and this item is officially downloadable but an account is
                 * required to perform official downloads.
                 */
                throw new AccountRequiredException("Account needed to download officially downloadable items");
            }
            dllink = getDirectlink(this, link, account, this.br, response);
            if (!dllink.contains("/playlist.m3u8")) {
                /* Only check filesize of progressive download-URLs. */
                if (!checkDirectLink(link, this.dllink)) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server issue or broken audio file");
                }
            }
            if (isOnlyPreviewDownloadable && (this.getPluginConfig().getBooleanProperty(ALLOW_PREVIEW_DOWNLOAD, defaultALLOW_PREVIEW_DOWNLOAD) == false || dllink == null)) {
                /*
                 * Song is a pay-only song plus user does not want to download the (30 second) preview [and/or no final downloadlink is
                 * available].
                 */
                throw new PluginException(LinkStatus.ERROR_FATAL, getPhrase("ERROR_PREVIEW_DOWNLOAD_DISABLED"));
            }
            // if (!looksLikeOfficiallyDownloadable && mode == AudioQualitySelectionMode.ONLY_OFFICIAL_DOWNLOADS) {
            // throw new PluginException(LinkStatus.ERROR_FATAL, getPhrase("ERROR_NOT_OFFICIALLY_DOWNLOADABLE"));
            // }
        }
        return AvailableStatus.TRUE;
    }

    /** Resets/Removes current clientID and appVersion. */
    private void resetThis() {
        synchronized (clientId) {
            clientId.set(null);
        }
        synchronized (appVersion) {
            appVersion.set(null);
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        handleDownload(link, null);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account, true);
        if (dllink.contains("/playlist.m3u8")) {
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, dllink);
            dl.startDownload();
        } else {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, 1);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 416) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 416", 5 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } else if (dl.getConnection().getLongContentLength() == 0) {
                br.followConnection(true);
                // start of file extension correction.
                // required because original-format implies the uploaded format might not be what the end user downloads.
                throw new PluginException(LinkStatus.ERROR_FATAL, "Not downloadable (zero length file)");
            }
            /*
             * E.g. official download: All filenames are set with .mp3 extension in crawler but official downloads can also e.g. be .wav
             * files.
             */
            final String serverFilename = Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection()));
            if (link.getFinalFileName() != null && serverFilename != null) {
                final String newExtension = Plugin.getFileNameExtensionFromString(serverFilename);
                if (newExtension != null) {
                    link.setFinalFileName(this.correctOrApplyFileNameExtension(link.getFinalFileName(), newExtension));
                }
            }
            dl.startDownload();
        }
    }

    public static AvailableStatus checkStatusJson(final Plugin plugin, final DownloadLink link, final Account account, final Map<String, Object> track) throws Exception {
        if (track == null) {
            return AvailableStatus.FALSE;
        }
        final Map<String, Object> user = (Map<String, Object>) track.get("user");
        final String user_permalink = (String) user.get("permalink");
        final String username = (String) user.get("username");
        final String title = (String) track.get("title");
        if (StringUtils.isEmpty(title)) {
            return AvailableStatus.FALSE;
        }
        final String stream_url = (String) track.get("stream_url");
        String secret_token = stream_url != null ? new Regex(stream_url, "secret_token=([A-Za-z0-9\\-_]+)").getMatch(0) : null;
        if (secret_token == null) {
            secret_token = (String) track.get("secret_token");
        }
        final String id = track.get("id").toString();
        try {
            final String description = (String) track.get("description");
            if (!StringUtils.isEmpty(description)) {
                link.setComment(description);
            }
        } catch (final Throwable ignore) {
        }
        final String date = (String) track.get("created_at");
        final Number duration = (Number) track.get("duration");
        if (duration != null) {
            link.setProperty(PROPERTY_duration_seconds, duration.intValue());
        }
        final String url = (String) track.get("download_url");
        final boolean looksLikeOfficiallyDownloadable = looksLikeOfficiallyDownloadable(track);
        /* Do this so PROPERTY_chosen_quality will get set for correct filesize calculation. */
        getDirectlink(plugin, link, account, null, track);
        if (looksLikeOfficiallyDownloadable && userPrefersOfficialDownload()) {
            /* File is officially downloadable */
            /**
             * Only set calculated filesize if wanted by user. </br>
             * Officially downloadable files could come in any bitrate thus we do by default not calculate the filesize for such items based
             * on an assumed bitrate.
             */
            if (userEnforcesFilesizeEstimationEvenForNonStreamDownloads()) {
                link.setDownloadSize(calculateFilesize(link));
            }
        } else {
            /* Streams */
            link.setDownloadSize(calculateFilesize(link));
        }
        if (!StringUtils.isEmpty(url)) {
            link.setProperty(PROPERTY_directurl, url + "?client_id=" + getClientId(null));
        }
        if (!StringUtils.isEmpty(username)) {
            link.setProperty(PROPERTY_channel, username);
        }
        link.setProperty(PROPERTY_title, title);
        link.setProperty(PROPERTY_originaldate, date);
        link.setProperty(PROPERTY_track_id, id);
        if (!StringUtils.isEmpty(secret_token)) {
            link.setProperty(PROPERTY_secret_token, secret_token);
        }
        link.setProperty(PROPERTY_url_username, user_permalink);
        final String formattedfilename = getFormattedFilename(link);
        link.setFinalFileName(formattedfilename);
        link.setProperty(PROPERTY_STATE, track.get("state"));
        return AvailableStatus.TRUE;
    }

    /* Estimate filesize based on their average bitrate. */
    public static long calculateFilesize(final DownloadLink link) {
        final int durationSeconds = link.getIntegerProperty(PROPERTY_duration_seconds);
        if (durationSeconds <= 0) {
            return -1;
        } else {
            return ((long) getExpectedAudioBitrate(link)) / 8 * 1024 * durationSeconds / 1000;
        }
    }

    /** Returns expected audio bitrate in kbit/s. */
    public static int getExpectedAudioBitrate(final DownloadLink link) {
        final String chosenQuality = link.getStringProperty(PROPERTY_chosen_quality);
        if (chosenQuality == null || chosenQuality.equals(PROPERTY_QUALITY_sq)) {
            return 128;
        } else {
            return 256;
        }
    }

    @Override
    public boolean isSpeedLimited(final DownloadLink link, final Account account) {
        return false;
    }

    public static final String TYPE_API_ALL      = "(?i)https?://api\\.soundcloud\\.com/tracks/\\d+/(stream|download)(\\?secret_token=[A-Za-z0-9\\-_]+)?";
    public static final String TYPE_API_STREAM   = "(?i)https?://api\\.soundcloud\\.com/tracks/\\d+/stream";
    public static final String TYPE_API_DOWNLOAD = "(?i)https?://api\\.soundcloud\\.com/tracks/\\d+/download";
    public static final String TYPE_API_TOKEN    = "(?i)https?://api\\.soundcloud\\.com/tracks/\\d+/stream\\?secret_token=[A-Za-z0-9\\-_]+";

    public static boolean looksLikeOfficiallyDownloadable(final Map<String, Object> track) {
        final boolean downloadable = ((Boolean) track.get("downloadable")).booleanValue();
        final Object has_downloads_left_o = track.get("has_downloads_left");
        final boolean is_definitly_downloadable;
        if (has_downloads_left_o != null && has_downloads_left_o instanceof Boolean) {
            /* 2016-12-14: 'has_downloads_left' Object is not always given */
            final boolean has_downloads_left = ((Boolean) has_downloads_left_o).booleanValue();
            is_definitly_downloadable = downloadable && has_downloads_left;
        } else {
            is_definitly_downloadable = downloadable;
        }
        return is_definitly_downloadable;
    }

    public static String getDirectlink(final Plugin plugin, final DownloadLink link, final Account account, final Browser br, final Map<String, Object> json) throws InterruptedException {
        try {
            final boolean looksLikeOfficiallyDownloadable = looksLikeOfficiallyDownloadable(json);
            String track_id = link.getStringProperty(SoundcloudCom.PROPERTY_track_id);
            if (track_id == null) {
                /* Fallback: Get that information from our current json. */
                track_id = json.get("id").toString();
            }
            final String secret_token = (String) json.get("secret_token");
            UrlQuery basicQuery = new UrlQuery();
            basicQuery.append("client_id", getClientId(null), true);
            basicQuery.append("app_version", SoundcloudCom.getAppVersion(null), false);
            basicQuery.append("app_locale", getAppLocaleV2(), false);
            if (!StringUtils.isEmpty(secret_token)) {
                basicQuery.append("secret_token", secret_token, true);
            }
            if (looksLikeOfficiallyDownloadable && userPrefersOfficialDownload() && (account != null || ACCOUNT_NEEDED_FOR_OFFICIAL_DOWNLOADS == false)) {
                /* Official download via official download button */
                /* Track is officially downloadable (download version = highest quality) */
                if (br == null) {
                    /* No browser given --> We can't perform http requests -> We can't generate final downloadurls! */
                    return null;
                }
                /* Do not use this anymore --> It will return the same we're doing here but as a v1 request URL! */
                // finallink = toString(json.get("download_url"));
                br.getPage(SoundcloudCom.API_BASEv2 + "/tracks/" + track_id + "/download?" + basicQuery.toString());
                if (br.getHttpConnection().getResponseCode() == 401) {
                    /*
                     * This should never happen. It may happen if official download is attempted while we're not logged in [ahh that also
                     * should never happen!].
                     */
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Official download failed: Permission missing?");
                }
                final Map<String, Object> entries = plugin.restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                return entries.get("redirectUri").toString();
            } else {
                /* Stream download */
                final Map<String, Object> media = (Map<String, Object>) json.get("media");
                if (media == null) {
                    return null;
                }
                final List<Map<String, Object>> transcodings = (List<Map<String, Object>>) media.get("transcodings");
                if (transcodings == null) {
                    return null;
                }
                Map<String, Object> transcodingHQ = null;
                Map<String, Object> transcodingSQ = null;
                for (final Map<String, Object> transcoding : transcodings) {
                    /* Skip still we don't need or can't handle. */
                    final String preset = transcoding.get("preset").toString();
                    final String quality = transcoding.get("quality").toString();
                    final Map<String, Object> format = (Map<String, Object>) transcoding.get("format");
                    final String protocol = format.get("protocol").toString();
                    final String mime_type = format.get("mime_type").toString();
                    /* Skip opus */
                    if (preset.contains("opus") || mime_type.contains("audio/ogg")) {
                        /* Skip because: Is never really available/working?! */
                        continue;
                    } else if (quality.equals(PROPERTY_QUALITY_sq) && protocol.equals("progressive")) {
                        /* Skip because: Available but doesn't work */
                        continue;
                    }
                    if (quality.equals(PROPERTY_QUALITY_hq)) {
                        transcodingHQ = transcoding;
                    } else {
                        transcodingSQ = transcoding;
                    }
                }
                final Map<String, Object> chosenTranscoding;
                if (transcodingHQ != null) {
                    /* E.g. pro/premium users */
                    chosenTranscoding = transcodingHQ;
                    link.setProperty(PROPERTY_chosen_quality, PROPERTY_QUALITY_hq);
                } else {
                    /* Free account / no account */
                    chosenTranscoding = transcodingSQ;
                    link.setProperty(PROPERTY_chosen_quality, PROPERTY_QUALITY_sq);
                }
                String streamUrl = (String) chosenTranscoding.get("url");
                if (!StringUtils.isEmpty(streamUrl)) {
                    /* Extra HTTP request required to find final downloadurl. */
                    final Map<String, Object> format = (Map<String, Object>) chosenTranscoding.get("format");
                    final String mime_type = (String) format.get("mime_type");
                    final String extension = Plugin.getExtensionFromMimeTypeStatic(mime_type);
                    if (extension != null) {
                        link.setProperty(PROPERTY_filetype, extension);
                    }
                    if (br == null) {
                        /*
                         * E.g. during crawling we only want to find the quality we will download later but we do not yet need to generate
                         * the final downloadURL.
                         */
                        return null;
                    } else {
                        plugin.getLogger().info("Chosen audio preset: " + chosenTranscoding.get("preset"));
                        final Browser br2 = br.cloneBrowser();
                        final UrlQuery query = UrlQuery.parse(streamUrl);
                        query.add("client_id", getClientId(null));
                        streamUrl = URLHelper.parseLocation(new URL(streamUrl), "?" + query.toString()).toString();
                        br2.getPage(streamUrl);
                        final Map<String, Object> urlMap = JSonStorage.restoreFromString(br2.getRequest().getHtmlCode(), TypeRef.MAP);
                        final String finallink = (String) urlMap.get("url");
                        if (!StringUtils.isEmpty(finallink)) {
                            return finallink;
                        }
                    }
                }
            }
        } catch (final InterruptedException e) {
            throw e;
        } catch (final Throwable e) {
            plugin.getLogger().log(e);
        }
        return null;
    }

    private boolean checkDirectLink(final DownloadLink link, final String directurl) throws Exception {
        URLConnectionAdapter con = null;
        try {
            final Browser br2 = br.cloneBrowser();
            con = br2.openGetConnection(directurl);
            if (!this.looksLikeDownloadableContent(con)) {
                link.setProperty(PROPERTY_directurl, Property.NULL);
                return false;
            } else if (con.getResponseCode() == 401) {
                link.setProperty(PROPERTY_directurl, Property.NULL);
                return false;
            } else {
                if (!con.getURL().toString().contains(".m3u8") && con.getCompleteContentLength() > 0) {
                    link.setDownloadSize(con.getCompleteContentLength());
                }
                link.setProperty(PROPERTY_directurl, directurl);
                return true;
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    public static final String testUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36";

    private Browser prepBR(final Browser br) {
        /* E.g. accessing invalid urls, their servers will return 503. */
        br.setAllowedResponseCodes(new int[] { 503 });
        br.setFollowRedirects(true);
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            br.getHeaders().put("User-Agent", testUserAgent);
        }
        return br;
    }

    public void login(final Browser br, final Account account, final boolean force) throws Exception {
        synchronized (account) {
            prepBR(br);
            String oauthtoken = account.getStringProperty(PROPERTY_ACCOUNT_oauthtoken);
            br.setCookiesExclusive(true);
            try {
                final Cookies cookies = account.loadCookies("");
                final Cookies userCookies = account.loadUserCookies();
                if (cookieLoginOnly && userCookies == null) {
                    showCookieLoginInfo();
                    throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_required());
                }
                if (userCookies != null) {
                    logger.info("Attempting user cookie login");
                    if (userCookies.get("oauth_token") == null) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Cookie login failed: Oauth token missing", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    br.setCookies(this.getHost(), userCookies);
                    oauthtoken = userCookies.get("oauth_token").getValue(); // Exception will occur if this cookie is not given!
                    br.getHeaders().put("Authorization", "OAuth " + oauthtoken);
                    if (this.checkLogin(br)) {
                        account.setProperty(PROPERTY_ACCOUNT_oauthtoken, oauthtoken);
                        return;
                    } else {
                        if (account.hasEverBeenValid()) {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                        } else {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                        }
                    }
                }
                if (cookies != null && oauthtoken != null) {
                    br.setCookies(this.getHost(), cookies);
                    br.getHeaders().put("Authorization", "OAuth " + oauthtoken);
                    if (!force) {
                        logger.info("Trust cookies without checking");
                        return;
                    } else if (this.checkLogin(br)) {
                        return;
                    }
                }
                logger.info("Full login required");
                br.clearAuthentications();
                br.clearCookies(this.getHost());
                br.getPage("https://soundcloud.com/");
                br.getPage("https://secure.soundcloud.com/web-auth?client_id=" + getClientIdV2() + "&device_id=TODO");
                final String loginbase = "https://api-auth.soundcloud.com";
                /* TODO: Add cookie/oauth check for v2 to prevent full login attempts! */
                br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.1");
                br.getHeaders().put("Content-Type", "application/json");
                /* TODO */
                br.getHeaders().put("sec-ch-ua", "\"Google Chrome\";v=\"87\", \" Not;A Brand\";v=\"99\", \"Chromium\";v=\"87\"");
                /* This one is stored in a JD file - URL is available after "secure.soundcloud.com/web-auth/" ... */
                br.getHeaders().put("X-Csrf-Token", "TODO");
                br.getHeaders().put("sec-ch-ua-mobile", "?0");
                br.getHeaders().put("Origin", "https://secure.soundcloud.com");
                br.getPage(loginbase + "/web-auth/identifier?" + Encoding.urlEncode(account.getUser()) + "&client_id=" + getClientIdV2());
                /* First check if users' E-Mail address exists */
                // final String identifier = PluginJSonUtils.getJson(br, "identifier");
                final String status = PluginJSonUtils.getJson(br, "status");
                if (!"in_use".equalsIgnoreCase(status)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                final String requesturl = loginbase + "/web-auth/sign-in/password?client_id=" + getClientIdV2();
                final Map<String, Object> data_credentials = new HashMap<String, Object>();
                data_credentials.put("identifier", account.getUser());
                data_credentials.put("password", account.getPass());
                final Map<String, Object> data = new HashMap<String, Object>();
                data.put("device_id", "TODO");
                data.put("client_id", getClientIdV2());
                /* Date: 2020-12-15 */
                data.put("recaptcha_pubkey", "6Ld72JcUAAAAAItDloUGqg6H38KK5j08VuQlegV1");
                data.put("recaptcha_response", null);
                data.put("signature", "TODO");
                data.put("user_agent", testUserAgent);
                data.put("credentials", data_credentials);
                final PostRequest loginReq = br.createJSonPostRequest(requesturl, JSonStorage.serializeToJson(data));
                br.openRequestConnection(loginReq);
                br.followConnection();
                // br.loadConnection(null);
                final String error = PluginJSonUtils.getJson(br, "error");
                if (!StringUtils.isEmpty(error)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                oauthtoken = PluginJSonUtils.getJson(br, "access_token");
                if (StringUtils.isEmpty(oauthtoken)) {
                    logger.info("Could not find oauth token -> Wrong password?");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                br.getHeaders().put("Authorization", "OAuth " + oauthtoken);
                account.setProperty(PROPERTY_ACCOUNT_oauthtoken, oauthtoken);
                account.saveCookies(br.getCookies(br.getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                    account.removeProperty(PROPERTY_ACCOUNT_oauthtoken);
                }
                throw e;
            } finally {
                /* Store userID separately as we might need it later. */
                if (oauthtoken != null) {
                    final Regex oauthInfo = new Regex(oauthtoken, "(\\d+)-(\\d+)-(\\d+)-([A-Za-z0-9]+)");
                    if (oauthInfo.matches()) {
                        account.setProperty(PROPERTY_ACCOUNT_userid, oauthInfo.getMatch(2));
                    } else {
                        logger.warning("Unexpected oauthtoken format");
                    }
                }
            }
        }
    }

    /** Checks if we're logged in */
    private boolean checkLogin(final Browser br) throws IOException {
        br.getPage(API_BASEv2 + "/me?client_id=" + getClientIdV2() + "&app_version=" + getAppVersionV2() + "&app_locale=" + getAppLocaleV2());
        if (br.getHttpConnection().getResponseCode() == 200) {
            logger.info("Cookie login successful");
            return true;
        } else {
            logger.info("Cookie login failed");
            return false;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        login(this.br, account, true);
        final AccountInfo ai = new AccountInfo();
        ai.setUnlimitedTraffic();
        if (br.getURL() == null || !br.getURL().contains(API_BASEv2 + "/me")) {
            br.getPage(API_BASEv2 + "/me?client_id=" + getClientIdV2() + "&app_version=" + getAppVersionV2() + "&app_locale=" + getAppLocaleV2());
        }
        Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
        final String created_at = (String) entries.get("created_at");
        if (!StringUtils.isEmpty(created_at)) {
            account.setProperty(SoundcloudCom.PROPERTY_ACCOUNT_created_at, created_at);
        }
        final String permalink = (String) entries.get("permalink");
        if (!StringUtils.isEmpty(permalink)) {
            account.setProperty(SoundcloudCom.PROPERTY_ACCOUNT_permalink, permalink);
        }
        final String username = (String) entries.get("username");
        if (!StringUtils.isEmpty(username)) {
            account.setProperty(SoundcloudCom.PROPERTY_ACCOUNT_username, username);
        }
        /*
         * 2020-12-15: At this moment only cookie login is possible which means in theory, user can enter anything in the username field ->
         * Let's fix that
         */
        final String email = (String) entries.get("primary_email");
        if (!StringUtils.isEmpty(email)) {
            account.setUser(email);
        }
        final boolean checkViaProfilePage = false;
        if (checkViaProfilePage) {
            final String acctype = (String) JavaScriptEngineFactory.walkJson(entries, "consumer_subscription/product/id");
            if ("free".equalsIgnoreCase(acctype)) {
                /* 2020-12-16: E.g. "consumer-high-tier" */
                account.setType(AccountType.FREE);
            } else {
                account.setType(AccountType.PREMIUM);
            }
        } else {
            br.getPage(API_BASEv2 + "/payments/quotations/consumer-subscription?client_id=" + getClientIdV2() + "&app_version=" + getAppVersionV2() + "&app_locale=" + getAppLocaleV2());
            entries = restoreFromString(br.toString(), TypeRef.MAP);
            entries = (Map<String, Object>) entries.get("active_subscription");
            final String expires_at = (String) entries.get("expires_at");
            final String packageName = (String) JavaScriptEngineFactory.walkJson(entries, "package/name");
            if (!StringUtils.isEmpty(packageName)) {
                if (!StringUtils.isEmpty(expires_at)) {
                    ai.setValidUntil(TimeFormatter.getMilliSeconds(expires_at, "yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH), this.br);
                }
                ai.setStatus(packageName);
                account.setType(AccountType.PREMIUM);
            } else {
                account.setType(AccountType.FREE);
            }
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleDownload(link, account);
    }

    public static String getFormattedFilename(final DownloadLink link) throws ParseException {
        final String url_username = link.getStringProperty(PROPERTY_url_username);
        String songTitle = link.getStringProperty(PROPERTY_title);
        final SubConfiguration cfg = SubConfiguration.getConfig("soundcloud.com");
        String formattedFilename = cfg.getStringProperty(CUSTOM_FILENAME_2, defaultCustomFilename);
        if (formattedFilename == null || formattedFilename.equals("")) {
            formattedFilename = defaultCustomFilename;
        }
        if (!formattedFilename.contains("*songtitle*") || !formattedFilename.contains("*ext*")) {
            formattedFilename = defaultCustomFilename;
        }
        String ext = link.getStringProperty(PROPERTY_filetype);
        if (ext != null) {
            if (!ext.startsWith(".")) {
                ext = "." + ext;
            }
        } else {
            /* Fallback/default */
            ext = ".mp3";
        }
        String date = link.getStringProperty(PROPERTY_originaldate);
        final String channelName = link.getStringProperty(PROPERTY_channel);
        final String track_id = link.getStringProperty(PROPERTY_track_id);
        String formattedDate = null;
        if (date != null && formattedFilename.contains("*date*")) {
            // 2011-08-10T22:50:49Z
            try {
                final String userDefinedDateFormat = cfg.getStringProperty(CUSTOM_DATE, defaultCustomDate);
                final SimpleDateFormat formatter;
                if (date.contains("/")) {
                    formatter = new SimpleDateFormat("yyyy/MM/dd'T'HH:mm:ss'Z'");
                } else {
                    formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                }
                final Date dateStr = formatter.parse(date);
                formattedDate = formatter.format(dateStr);
                final String defaultformattedDate = formattedDate;
                if (userDefinedDateFormat != null) {
                    try {
                        final SimpleDateFormat customFormatter = new SimpleDateFormat(userDefinedDateFormat);
                        formattedDate = customFormatter.format(dateStr);
                    } catch (final Exception ignore) {
                        // prevent user error killing plugin.
                        formattedDate = defaultformattedDate;
                    }
                }
            } catch (final Exception ignore) {
                // prevent user error killing plugin.
                formattedDate = null;
            }
            if (formattedDate != null) {
                formattedFilename = formattedFilename.replace("*date*", formattedDate);
            } else {
                formattedFilename = formattedFilename.replace("*date*", "");
            }
        }
        if (formattedFilename.contains("*linkid*")) {
            formattedFilename = formattedFilename.replace("*linkid*", track_id != null ? track_id : "");
        }
        if (formattedFilename.contains("*channelname*")) {
            formattedFilename = formattedFilename.replace("*channelname*", channelName != null ? channelName : "unknown");
        }
        if (formattedFilename.contains("*url_username*")) {
            formattedFilename = formattedFilename.replace("*url_username*", url_username != null ? url_username : "unknown");
        }
        formattedFilename = formattedFilename.replace("*ext*", ext);
        /* Insert title at the end to prevent errors with tags */
        formattedFilename = formattedFilename.replace("*songtitle*", songTitle);
        final String setsposition = link.getStringProperty(PROPERTY_setsposition, null);
        if (cfg.getBooleanProperty(SETS_ADD_POSITION_TO_FILENAME, defaultSETS_ADD_POSITION_TO_FILENAME) && setsposition != null) {
            formattedFilename = setsposition + formattedFilename;
        }
        return formattedFilename;
    }

    @Override
    public boolean allowHandle(final DownloadLink link, final PluginForHost plugin) {
        /* Do not allow URLs of this host to be downloaded via multihoster. */
        return link.getHost().equalsIgnoreCase(plugin.getHost());
    }

    @Override
    public String getDescription() {
        return "JDownloader's soundcloud.com plugin helps downloading audiofiles. JDownloader provides settings for custom filenames.";
    }

    private static HashMap<String, String> phrasesEN = new HashMap<String, String>() {
                                                         {
                                                             put("SETTING_GRAB_PURCHASE_URL", "Grab purchase URL?\r\n<html><b>The purchase-URL sometimes lead to external downloadlinks e.g. mediafire.com.</b></html>");
                                                             put("SETTING_AUDIO_QUALITY_SELECTION_MODE", "Audio quality selection mode");
                                                             put("SETTING_AUDIO_QUALITY_SELECTION_MODE_BEST", "Best quality (prefer items with downloadbutton)");
                                                             put("SETTING_AUDIO_QUALITY_SELECTION_MODE_STREAM", "Stream");
                                                             put("SETTING_AUDIO_QUALITY_SELECTION_MODE_ONLY_OFFICIAL_DOWNLOADABLE", "Only official downloads (items with downloadbutton)");
                                                             put("SETTING_ALLOW_PREVIEW_DOWNLOAD", "Download 30 second preview if a track is pay-only?");
                                                             put("SETTING_GRAB500THUMB", "Grab 500x500 thumbnail (.jpg)?");
                                                             put("SETTING_GRABORIGINALTHUMB", "Grab original thumbnail (.jpg)?");
                                                             put("SETTING_CUSTOM_DATE", "Define custom date:");
                                                             put("SETTING_CUSTOM_FILENAME_2", "Define custom filename:");
                                                             put("SETTING_CUSTOM_PACKAGENAME", "Define custom packagename:");
                                                             put("SETTING_LABEL_crawler", "Crawler settings:");
                                                             put("SETTING_LABEL_hoster", "Host plugin settings:");
                                                             put("SETTING_SETS_ADD_POSITION_TO_FILENAME", "Sets: Add position to the beginning of the filename e.g. 1.trackname.mp3?");
                                                             put("SETTING_LABEL_fnames_top", "Customize filenames/packagenames:");
                                                             put("SETTING_LABEL_customizefnames", "Customize the filenames:");
                                                             put("SETTING_LABEL_customizefnames_2", "Customize the filename! Example: '*channelname*_*date*_*songtitle**ext*'");
                                                             put("SETTING_LABEL_customizepackagenames", "Customize the packagename for playlists and 'soundcloud.com/user' links! Example: '*channelname* - *playlistname*':");
                                                             put("SETTING_LABEL_tags_filename", "Explanation of the available tags:\r\n*url_username* = Username located in the soundcloud url which was added to jd\r\n*channelname* = name of the channel/uploader\r\n*date* = date when the link was posted - appears in the user-defined format above\r\n*songtitle* = name of the song without extension\r\n*linkid* = unique ID of the link - can be used to avoid duplicate filename for different links\r\n*ext* = the extension of the file, in this case usually '.mp3'");
                                                             put("SETTING_LABEL_tags_packagename", "Explanation of the available tags:\r\n*url_username* = Username located in the soundcloud url which was added to jd\r\n*channelname* = name of the channel/uploader\r\n*playlistname* = name of the playlist (= username for 'soundcloud.com/user' links)\r\n*date* = date when the linklist was created - appears in the user-defined format above\r\n");
                                                             put("SETTING_add_track_position_to_beginning_of_filename", "Sets: Add position to the beginning of the filename e.g. (1.trackname.mp3)?");
                                                             put("SETTING_LABEL_advanced_settings", "Advanced settings (only change them if you know what you're doing)");
                                                             put("SETTING_enforce_filesize_calculation_even_for_officially_downloadable_tracks", "Force stream-filesize-calculation even for tracks with downloadbutton? Warning: These values can very greatly from the real filesizes!");
                                                             put("ERROR_NOT_OFFICIALLY_DOWNLOADABLE", "You want to download only officially downloadable items! This link is not officially downloadable!");
                                                             put("ERROR_PREVIEW_DOWNLOAD_DISABLED", "This is paid content which only has a 30 second preview available and download of previews is disabled in plugin settings!");
                                                         }
                                                     };
    private static HashMap<String, String> phrasesDE = new HashMap<String, String>() {
                                                         {
                                                             put("SETTING_GRAB_PURCHASE_URL", "Kauflink einfügen?\r\n<html><b>Der Kauflink führt manchmal zu externen Downloadmöglichkeiten z.B. mediafire.com.</b></html>");
                                                             put("SETTING_AUDIO_QUALITY_SELECTION_MODE", "Audioquailität Downloadmodus");
                                                             put("SETTING_AUDIO_QUALITY_SELECTION_MODE_BEST", "Beste Qualität");
                                                             put("SETTING_AUDIO_QUALITY_SELECTION_MODE_STREAM", "Stream");
                                                             put("SETTING_AUDIO_QUALITY_SELECTION_MODE_ONLY_OFFICIAL_DOWNLOADABLE", "Nur offiziell herunterladbare Elemente");
                                                             put("SETTING_ALLOW_PREVIEW_DOWNLOAD", "Für Bezahltitel: Lade 30 Sekunden Ausschnitt herunter?");
                                                             put("SETTING_GRAB500THUMB", "500x500 Thumbnail einfügen (.jpg)?");
                                                             put("SETTING_GRABORIGINALTHUMB", "Thumbnail in Originalgröße einfügen (.jpg)?");
                                                             put("SETTING_CUSTOM_DATE", "Lege das Datumsformat fest:");
                                                             put("SETTING_CUSTOM_FILENAME_2", "Lege das Muster für deine eigenen Dateinamen fest:");
                                                             put("SETTING_CUSTOM_PACKAGENAME", "Lege das Muster für Paketnamen fest:");
                                                             put("SETTING_SETS_ADD_POSITION_TO_FILENAME", "Sets: Zeige Position am Anfang des Dateinames Beispiel z.B. 1.trackname.mp3?");
                                                             put("SETTING_LABEL_crawler", "Crawler Einstellungen:");
                                                             put("SETTING_LABEL_hoster", "Hoster Plugin Einstellungen:");
                                                             put("SETTING_LABEL_fnames_top", "Lege eigene Datei-/Paketnamen fest:");
                                                             put("SETTING_LABEL_customizefnames", "Lege eigene Dateinamen fest:");
                                                             put("SETTING_LABEL_customizefnames_2", "Passe die Dateinamen an! Beispiel: '*channelname*_*date*_*songtitle**ext*'");
                                                             put("SETTING_LABEL_customizepackagenames", "Lege das Muster für Paketnamen fest für Playlists und 'soundcloud.com/user' Links! Beispiel: '*channelname* - *playlistname*':");
                                                             put("SETTING_LABEL_tags_filename", "Erklärung verfügbarer Tags:\r\n*url_username* = Benutzername, der in der hinzugefügten URL steht\r\n*channelname* = Name des Channels/Uploaders\r\n*date* = Datum an dem die Datei hochgeladen wurde - erscheint im benutzerdefinierten Format\r\n*songtitle* = Name des Songs ohne Endung\r\n*linkid* = Soundcloud-ID des links - Kann benutzt werden um Duplikate zu vermeiden\r\n*ext* = Dateiendung - normalerweise '.mp3'");
                                                             put("SETTING_LABEL_tags_packagename", "Erklärung verfügbarer Tags:\r\n*url_username* = Benutzername, der in der hinzugefügten URL steht\r\n*channelname* = Name des Channels/Uploaders\r\n*playlistname* = Name der Playliste (= Benutzername bei 'soundcloud.com/user' Links)\r\n*date* = Datum an dem die Playliste hochgeladen wurde - erscheint im benutzerdefinierten Format\r\n");
                                                             put("SETTING_add_track_position_to_beginning_of_filename", "Sets: Track-Position an den Anfang der Dateinamen setzen z.B. (1.trackname.mp3)?");
                                                             put("SETTING_LABEL_advanced_settings", "Erweiterte Einstellungen (verändere diese nur, wenn du weißt was du tust)");
                                                             put("SETTING_enforce_filesize_calculation_even_for_officially_downloadable_tracks", "Erzwinge Stream-Dateigrößenberechnung auch für Tracks mit Downloadbutton? Warnung! Diese Dateigrößen können stark von den echten abweichen!");
                                                             put("ERROR_NOT_OFFICIALLY_DOWNLOADABLE", "Du hast eingestellt, dass nur Elemente mit Downloadbutton heruntergeladen werden sollen! Dieser link ist nicht offiziell herunterladbar!");
                                                             put("ERROR_PREVIEW_DOWNLOAD_DISABLED", "Dieses Element ist nur als Vorschau verfügbar bzw. kaufbar und der Download solcher ist in den Plugineinstellungen deaktiviert!");
                                                         }
                                                     };

    /**
     * Returns a German/English translation of a phrase. We don't use the JDownloader translation framework since we need only German and
     * English.
     *
     * @param key
     * @return
     */
    private static String getPhrase(String key) {
        if ("de".equals(System.getProperty("user.language")) && phrasesDE.containsKey(key)) {
            return phrasesDE.get(key);
        } else if (phrasesEN.containsKey(key)) {
            return phrasesEN.get(key);
        } else {
            return "Translation not found!";
        }
    }

    public static enum AudioQualitySelectionMode implements LabelInterface {
        BEST {
            @Override
            public String getLabel() {
                return getPhrase("SETTING_AUDIO_QUALITY_SELECTION_MODE_BEST");
            }
        },
        PREFER_STREAM {
            @Override
            public String getLabel() {
                return getPhrase("SETTING_AUDIO_QUALITY_SELECTION_MODE_STREAM");
            }
        },
        ONLY_OFFICIAL_DOWNLOADS {
            @Override
            public String getLabel() {
                return getPhrase("SETTING_AUDIO_QUALITY_SELECTION_MODE_ONLY_OFFICIAL_DOWNLOADABLE");
            }
        };
    }

    private String[] getAudioQualitySelectionModeStrings() {
        final AudioQualitySelectionMode[] qualitySelectionModes = AudioQualitySelectionMode.values();
        final String[] ret = new String[qualitySelectionModes.length];
        for (int i = 0; i < qualitySelectionModes.length; i++) {
            ret[i] = qualitySelectionModes[i].getLabel();
        }
        return ret;
    }

    public static AudioQualitySelectionMode getAudioQualitySelectionMode() {
        final int arrayPos = SubConfiguration.getConfig("soundcloud.com").getIntegerProperty(AUDIO_QUALITY_SELECTION_MODE, defaultArrayPosAUDIO_QUALITY_SELECTION_MODE);
        if (arrayPos < AudioQualitySelectionMode.values().length) {
            return AudioQualitySelectionMode.values()[arrayPos];
        } else {
            return AudioQualitySelectionMode.values()[defaultArrayPosAUDIO_QUALITY_SELECTION_MODE];
        }
    }

    public static boolean userPrefersOfficialDownload() {
        final AudioQualitySelectionMode mode = getAudioQualitySelectionMode();
        return mode == AudioQualitySelectionMode.BEST || mode == AudioQualitySelectionMode.ONLY_OFFICIAL_DOWNLOADS;
    }

    public static boolean userEnforcesFilesizeEstimationEvenForNonStreamDownloads() {
        return SubConfiguration.getConfig("soundcloud.com").getBooleanProperty(ENFORCE_FILESIZE_CALCULATION_EVEN_FOR_OFFICIALLY_DOWNLOADABLE_CONTENT, defaultENFORCE_FILESIZE_CALCULATION_EVEN_FOR_OFFICIALLY_DOWNLOADABLE_CONTENT);
    }

    private static final int     defaultArrayPosAUDIO_QUALITY_SELECTION_MODE                                  = 0;
    private static final boolean defaultALLOW_PREVIEW_DOWNLOAD                                                = false;
    public static final boolean  defaultGRAB_PURCHASE_URL                                                     = false;
    public static final boolean  defaultGRAB500THUMB                                                          = false;
    public static final boolean  defaultGRABORIGINALTHUMB                                                     = false;
    private final static String  defaultCustomDate                                                            = "yyyy-MM-dd";
    private final static String  defaultCustomFilename                                                        = "*songtitle*_*linkid* - *channelname**ext*";
    private final static String  defaultCustomPackagename                                                     = "*channelname* - *playlistname*";
    private static final boolean defaultSETS_ADD_POSITION_TO_FILENAME                                         = false;
    public static final boolean  defaultENFORCE_FILESIZE_CALCULATION_EVEN_FOR_OFFICIALLY_DOWNLOADABLE_CONTENT = false;

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, getPhrase("SETTING_LABEL_crawler")));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_PURCHASE_URL, getPhrase("SETTING_GRAB_PURCHASE_URL")).setDefaultValue(defaultGRAB_PURCHASE_URL));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB500THUMB, getPhrase("SETTING_GRAB500THUMB")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRABORIGINALTHUMB, getPhrase("SETTING_GRABORIGINALTHUMB")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, getPhrase("SETTING_LABEL_hoster")));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), AUDIO_QUALITY_SELECTION_MODE, getAudioQualitySelectionModeStrings(), getPhrase("SETTING_AUDIO_QUALITY_SELECTION_MODE")).setDefaultValue(defaultArrayPosAUDIO_QUALITY_SELECTION_MODE));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_PREVIEW_DOWNLOAD, getPhrase("SETTING_ALLOW_PREVIEW_DOWNLOAD")).setDefaultValue(defaultALLOW_PREVIEW_DOWNLOAD));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, getPhrase("SETTING_LABEL_fnames_top")));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_DATE, getPhrase("SETTING_CUSTOM_DATE")).setDefaultValue(defaultCustomDate));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, getPhrase("SETTING_LABEL_customizefnames")));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, getPhrase("SETTING_LABEL_customizefnames_2")));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME_2, getPhrase("SETTING_CUSTOM_FILENAME_2")).setDefaultValue(defaultCustomFilename));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, getPhrase("SETTING_LABEL_tags_filename")));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, getPhrase("SETTING_LABEL_customizepackagenames")));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_PACKAGENAME, getPhrase("SETTING_CUSTOM_PACKAGENAME")).setDefaultValue(defaultCustomPackagename));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, getPhrase("SETTING_LABEL_tags_packagename")));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SETS_ADD_POSITION_TO_FILENAME, getPhrase("SETTING_add_track_position_to_beginning_of_filename")).setDefaultValue(defaultSETS_ADD_POSITION_TO_FILENAME));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        /* 2020-04-01: No advanced settings available anymore */
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, getPhrase("SETTING_LABEL_advanced_settings")));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ENFORCE_FILESIZE_CALCULATION_EVEN_FOR_OFFICIALLY_DOWNLOADABLE_CONTENT, getPhrase("SETTING_enforce_filesize_calculation_even_for_officially_downloadable_tracks")).setDefaultValue(defaultENFORCE_FILESIZE_CALCULATION_EVEN_FOR_OFFICIALLY_DOWNLOADABLE_CONTENT));
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        return false;
    }
}