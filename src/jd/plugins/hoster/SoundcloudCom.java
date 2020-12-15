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
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.URLHelper;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.parser.UrlQuery;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.downloader.hls.HLSDownloader;
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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "soundcloud.com" }, urls = { "https://(www\\.)?soundclouddecrypted\\.com/[A-Za-z\\-_0-9]+/[A-Za-z\\-_0-9]+(/[A-Za-z\\-_0-9]+)?" })
public class SoundcloudCom extends PluginForHost {
    public SoundcloudCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
        this.setConfigElements();
    }

    /*
     * Last clientids (old to new): 2t9loNQH90kzJcsFCODdigxfp325aq4z, b45b1aa10f1ac2941910a7f0d10f8e28 fDoItMDbsbZz8dY16ZzARCZmzgHBPotA
     * 3904229f42df3999df223f6ebf39a8fe
     */
    public final static String   CLIENTID_8TRACKS                            = "3904229f42df3999df223f6ebf39a8fe";
    /* Another way to get final links: http://api.soundcloud.com/tracks/11111xxx_test_track_ID1111111/streams?format=json&consumer_key= */
    public final static String   CONSUMER_KEY_MYCLOUDPLAYERS_COM             = "PtMyqifCQMKLqwP0A6YQ";
    /* json: media/encodings */
    public static final String[] stream_qualities                            = { "stream_url", "http_mp3_128_url", "hls_mp3_128_url" };
    // public static final String[] streamtypes = { "download", "stream", "streams" };
    private final boolean        ENABLE_TYPE_PRIVATE                         = false;
    private static final String  ONLY_DOWNLOAD_OFFICIALLY_DOWNLOADABLE_FILES = "ONLY_DOWNLOAD_OFFICIALLY_DOWNLOADABLE_FILES";
    private static final String  ALLOW_PREVIEW_DOWNLOAD                      = "ALLOW_PREVIEW_DOWNLOAD";
    private final static String  CUSTOM_DATE                                 = "CUSTOM_DATE";
    private final static String  CUSTOM_FILENAME_2                           = "CUSTOM_FILENAME_2";
    private static final String  GRAB_PURCHASE_URL                           = "GRAB_PURCHASE_URL";
    private final String         GRAB500THUMB                                = "GRAB500THUMB";
    private final String         GRABORIGINALTHUMB                           = "GRABORIGINALTHUMB";
    private final String         CUSTOM_PACKAGENAME                          = "CUSTOM_PACKAGENAME";
    private final static String  SETS_ADD_POSITION_TO_FILENAME               = "SETS_ADD_POSITION_TO_FILENAME";
    private String               dllink                                      = null;
    private boolean              serverissue                                 = false;
    private boolean              is_officially_downloadable                  = false;
    private boolean              is_geo_blocked                              = false;
    private boolean              is_only_preview_downloadable                = false;
    /* DownloadLink Filename / Packagizer properties */
    public static final String   PROPERTY_setsposition                       = "setsposition";
    public static final String   PROPERTY_secret_token                       = "secret_token";
    public static final String   PROPERTY_playlist_id                        = "playlist_id";
    public static final String   PROPERTY_track_id                           = "track_id";
    public static final String   PROPERTY_directurl                          = "";
    /* Account properties */
    private static final String  PROPERTY_ACCOUNT_oauthtoken                 = "oauthtoken";
    /* API base URLs */
    public static final String   API_BASEv2                                  = "https://api-v2.soundcloud.com";

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("soundclouddecrypted", "soundcloud"));
    }

    @Override
    public String getAGBLink() {
        return "https://soundcloud.com/terms-of-use";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    /**
     * JD2 CODE. DO NOT USE OVERRIDE FOR JD=) COMPATIBILITY REASONS!
     */
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
        return requestFileInformation(link, false);
    }

    @SuppressWarnings({ "deprecation", "unused" })
    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        dllink = null;
        serverissue = false;
        is_geo_blocked = false;
        String secret_token = link.getStringProperty(PROPERTY_secret_token, null);
        prepBR(br);
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            login(br, aa, false);
        }
        String songid = link.getStringProperty(PROPERTY_track_id, null);
        Map<String, Object> response = null;
        if (songid == null) {
            /* This should never happen! */
            br.getPage(link.getPluginPatternMatcher());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            // this is poor way to determine the track id.
            songid = br.getRegex("soundcloud://sounds:(\\d+)").getMatch(0);
            if (songid == null) {
                /* 99,99% chance that the current url is not a song --> Offline */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        if (secret_token == null) {
            br.getPage(API_BASEv2 + "/tracks?urns=soundcloud%3Atracks%3A" + songid + "&client_id=" + getClientId(br) + "&app_version=" + SoundcloudCom.getAppVersion(br));
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.getHttpConnection().getResponseCode() == 401) {
                // keys are incorrect
                resetThis();
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            final List<Object> ressourcelist = JSonStorage.restoreFromString(br.toString(), TypeRef.LIST);
            response = (Map<String, Object>) ressourcelist.get(0);
            final AvailableStatus status = checkStatusJson(this, link, response, true);
            if (status.equals(AvailableStatus.FALSE)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /* !is_geo_blocked = policy equals "ALLOW" (the usual case). */
            is_geo_blocked = response.get("policy").equals("BLOCK");
        }
        if (!is_geo_blocked && isDownload) {
            /*
             * Only do/try linkcheck if we know that the track is NOT geo-blocked. Attempting to get a downloadurl for GEO-blocked content
             * will result in response 400.
             */
            /* TODO: Remove all APIv1 requests */
            // Other handling for private links
            if (br.containsHTML("<sharing>private</sharing>") && ENABLE_TYPE_PRIVATE || secret_token != null) {
                /* TODO: Find example links for this case, then use getDirectlink function here as well! */
                if (secret_token == null) {
                    secret_token = br.getRegex("\\?secret_token=([A-Za-z0-9\\-_]+)</uri>").getMatch(0);
                }
                if (secret_token != null) {
                    final String playlist_id = link.getStringProperty(SoundcloudCom.PROPERTY_playlist_id, null);
                    if (playlist_id != null) {
                        /* 2020-03-11: New: Song is part of playlist which needs secret_token */
                        final UrlQuery querytracks = new UrlQuery();
                        querytracks.add("playlistId", playlist_id);
                        querytracks.add("ids", songid);
                        querytracks.add("client_id", SoundcloudCom.getClientId(br));
                        querytracks.add("app_version", SoundcloudCom.getAppVersion(br));
                        querytracks.add("format", "json");
                        if (secret_token != null) {
                            querytracks.add("playlistSecretToken", secret_token);
                        }
                        br.getPage(API_BASEv2 + "/tracks?" + querytracks.toString());
                        final List<Object> ressourcelist = JSonStorage.restoreFromString(br.toString(), TypeRef.LIST);
                        dllink = getDirectlink(this, link, this.br, (Map<String, Object>) ressourcelist.get(0));
                    } else {
                        /* 2020-11-27: New: Song needs secret_token */
                        br.getPage(SoundcloudCom.API_BASEv2 + "/resolve?url=" + Encoding.urlEncode(link.getPluginPatternMatcher()) + "&_status_code_map%5B302%5D=10&_status_format=json&client_id=" + SoundcloudCom.getClientId(br));
                        /* 2020-03-17: TODO: Check if we still need this */
                        // br.getPage("https://api.soundcloud.com/i1/tracks/" + songid + "/streams?secret_token=" + secret_token +
                        // "&client_id=" + getClientId(br) + "&app_version=" + SoundcloudCom.getAppVersion(br));
                        dllink = getDirectlink(this, link, this.br, JSonStorage.restoreFromString(this.br.toString(), TypeRef.HASHMAP));
                    }
                } else {
                    /* 2020-03-17: TODO: Check if we still need this */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    // br.getPage("https://api.soundcloud.com/i1/tracks/" + songid + "/streams?client_id=" + getClientId(br) +
                    // "&app_version=" + SoundcloudCom.getAppVersion(br));
                    // final List<Object> ressourcelist = JSonStorage.restoreFromString(br.toString(), TypeRef.LIST);
                    // dllink = getDirectlink(this, link, this.br, (Map<String, Object>) ressourcelist.get(0));
                }
                if (br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } else {
                final List<Object> ressourcelist = JSonStorage.restoreFromString(br.toString(), TypeRef.LIST);
                dllink = getDirectlink(this, link, this.br, (Map<String, Object>) ressourcelist.get(0));
                if (StringUtils.isEmpty(dllink)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                is_officially_downloadable = isREALYDownloadable(response);
            }
            if (!dllink.contains("/playlist.m3u8")) {
                /* Only check filesize of http URLs */
                checkDirectLink(link);
            }
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
        doFree(link);
    }

    private void doFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        if (is_geo_blocked) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "This content is GEO-blocked");
        } else if ((is_only_preview_downloadable && !this.getPluginConfig().getBooleanProperty(ALLOW_PREVIEW_DOWNLOAD, defaultALLOW_PREVIEW_DOWNLOAD)) || (is_only_preview_downloadable && dllink == null)) {
            /* Song is a pay-only song plus user does not want to download the (30 second) preview [and/or no downloadlink is available]. */
            throw new PluginException(LinkStatus.ERROR_FATAL, "This is paid content which only has a 30 second preview available!");
        } else if (serverissue) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server issue", 10 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!is_officially_downloadable && this.getPluginConfig().getBooleanProperty(ONLY_DOWNLOAD_OFFICIALLY_DOWNLOADABLE_FILES, defaultONLY_DOWNLOAD_OFFICIALLY_DOWNLOADABLE_FILES)) {
            throw new PluginException(LinkStatus.ERROR_FATAL, getPhrase("ERROR_NOT_DOWNLOADABLE"));
        }
        if (dllink.contains("/playlist.m3u8")) {
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, dllink);
            dl.startDownload();
        } else {
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, 1);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 416) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 416", 5 * 60 * 1000l);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else if (dl.getConnection().getLongContentLength() == 0) {
                // start of file extension correction.
                // required because original-format implies the uploaded format might not be what the end user downloads.
                throw new PluginException(LinkStatus.ERROR_FATAL, "Not downloadable");
            }
            String oldName = link.getFinalFileName();
            if (oldName == null) {
                oldName = link.getName();
            }
            String serverFilename = Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection()));
            String newExtension = null;
            if (serverFilename == null) {
                logger.info("Server filename is null, keeping filename: " + oldName);
            } else {
                if (serverFilename.contains(".")) {
                    newExtension = serverFilename.substring(serverFilename.lastIndexOf("."));
                } else {
                    logger.info("HTTP headers don't contain filename.extension information");
                }
            }
            if (newExtension != null && !oldName.endsWith(newExtension)) {
                String oldExtension = null;
                if (oldName.contains(".")) {
                    oldExtension = oldName.substring(oldName.lastIndexOf("."));
                }
                if (oldExtension != null && oldExtension.length() <= 5) {
                    link.setFinalFileName(oldName.replace(oldExtension, newExtension));
                } else {
                    link.setFinalFileName(oldName + newExtension);
                }
            }
            dl.startDownload();
        }
    }

    public static AvailableStatus checkStatusJson(final Plugin plugin, DownloadLink parameter, Map<String, Object> source, boolean fromHostplugin) throws Exception {
        if (source == null) {
            return AvailableStatus.FALSE;
        }
        String filename = toString(source.get("title"));
        if (StringUtils.isEmpty(filename)) {
            if (fromHostplugin) {
                parameter.getLinkStatus().setStatusText("The host plugin is broken!");
            }
            return AvailableStatus.FALSE;
        }
        filename = filename.trim();
        filename = plugin.encodeUnicode(filename);
        final String stream_url = toString(source.get("stream_url"));
        String secret_token = stream_url != null ? new Regex(stream_url, "secret_token=([A-Za-z0-9\\-_]+)").getMatch(0) : null;
        /* 2020-11-27: Do not yet do this in stable!! */
        if (secret_token == null) {
            secret_token = (String) source.get("secret_token");
        }
        final String id = toString(source.get("id"));
        /* Only availavle in APIV1, at least for playlist-requests */
        final String filesize = toString(source.get("original_content_size"));
        try {
            final String description = toString(source.get("description"));
            if (!StringUtils.isEmpty(description)) {
                parameter.setComment(description);
            }
        } catch (Throwable e) {
        }
        final String date = toString(source.get("created_at"));
        String username = (String) JavaScriptEngineFactory.walkJson(source, "user/username");
        String type = toString(source.get("original_format"));
        if (StringUtils.isEmpty(type) || type.equals("raw")) {
            type = "mp3";
        }
        final String url = toString(source.get("download_url"));
        final boolean is_downloadable = isREALYDownloadable(source);
        if (!StringUtils.isEmpty(url) && is_downloadable) {
            /* we have original file downloadable */
            if (!StringUtils.isEmpty(filesize)) {
                parameter.setDownloadSize(Long.parseLong(filesize));
            }
            if (fromHostplugin) {
                parameter.getLinkStatus().setStatusText("Original file is downloadable");
            }
        } else {
            type = "mp3";
            if (fromHostplugin) {
                parameter.getLinkStatus().setStatusText("Preview (Stream) is downloadable");
            }
        }
        if (!StringUtils.isEmpty(url)) {
            parameter.setProperty(PROPERTY_directurl, url + "?client_id=" + getClientId(null));
        }
        if (!StringUtils.isEmpty(username)) {
            username = Encoding.htmlDecode(username.trim());
            parameter.setProperty("channel", username);
        }
        parameter.setProperty("plainfilename", filename);
        parameter.setProperty("originaldate", date);
        parameter.setProperty(PROPERTY_track_id, id);
        parameter.setProperty("type", type);
        if (!StringUtils.isEmpty(secret_token) && !fromHostplugin) {
            parameter.setProperty(PROPERTY_secret_token, secret_token);
        }
        final String formattedfilename = getFormattedFilename(parameter);
        parameter.setFinalFileName(formattedfilename);
        return AvailableStatus.TRUE;
    }

    private static String toString(Object object) {
        if (object == null) {
            return null;
        }
        return object.toString();
    }

    @Override
    public boolean isSpeedLimited(final DownloadLink link, final Account account) {
        return false;
    }

    public static final String TYPE_API_ALL      = "https?://api\\.soundcloud\\.com/tracks/\\d+/(stream|download)(\\?secret_token=[A-Za-z0-9\\-_]+)?";
    public static final String TYPE_API_STREAM   = "https?://api\\.soundcloud\\.com/tracks/\\d+/stream";
    public static final String TYPE_API_DOWNLOAD = "https?://api\\.soundcloud\\.com/tracks/\\d+/download";
    public static final String TYPE_API_TOKEN    = "https?://api\\.soundcloud\\.com/tracks/\\d+/stream\\?secret_token=[A-Za-z0-9\\-_]+";

    public static boolean isREALYDownloadable(final Map<String, Object> json) {
        final boolean downloadable = ((Boolean) json.get("downloadable")).booleanValue();
        final Object has_downloads_left_o = json.get("has_downloads_left");
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

    public static String getDirectlink(final Plugin plugin, final DownloadLink link, final Browser browser, final Map<String, Object> json) throws InterruptedException {
        String finallink = null;
        try {
            // try {
            // json = getStartJsonMap(browser.toString());
            // } catch (final Throwable e) {
            // plugin.getLogger().log(e);
            // }
            // if (json == null) {
            // json = JSonStorage.restoreFromString(browser.toString(), TypeRef.HASHMAP);
            // }
            final boolean is_downloadable = isREALYDownloadable(json);
            String track_id = link.getStringProperty(SoundcloudCom.PROPERTY_track_id);
            if (track_id == null) {
                track_id = (String) json.get("id");
            }
            /* TODO: Add secret_token to this if available */
            String secret_token = (String) json.get("secret_token");
            UrlQuery basicQuery = new UrlQuery();
            basicQuery.append("client_id", getClientId(null), true);
            basicQuery.append("app_version", SoundcloudCom.getAppVersion(null), false);
            basicQuery.append("app_locale", "de", false);
            if (!StringUtils.isEmpty(secret_token)) {
                /* Untested for video downloads */
                basicQuery.append("secret_token", secret_token, true);
            }
            if (is_downloadable) {
                /* Track is officially downloadable (download version = highest quality) */
                /* TODO: Use UrlQuery */
                /* Do not use this anymore --> It will return the same we're doing here but as a v1 request URL! */
                // finallink = toString(json.get("download_url"));
                finallink = SoundcloudCom.API_BASEv2 + "/tracks/" + track_id + "/download?" + basicQuery.toString();
                browser.getPage(finallink);
                finallink = PluginJSonUtils.getJson(browser, "redirectUri");
                return finallink;
            } else {
                final Map<String, Object> media = (Map<String, Object>) json.get("media");
                if (media != null && media.containsKey("transcodings")) {
                    String streamUrl = null;
                    final List<Map<String, Object>> transcodings = (List<Map<String, Object>>) media.get("transcodings");
                    for (Map<String, Object> transcoding : transcodings) {
                        if (!StringUtils.containsIgnoreCase(String.valueOf(transcoding.get("preset")), "mp3")) {
                            // eg opus
                            continue;
                        } else {
                            streamUrl = (String) transcoding.get("url");
                            if (transcoding.toString().contains("protocol=progressive")) {
                                break;
                            }
                        }
                    }
                    if (streamUrl != null) {
                        /* Extra HTTP request required to find final downloadurl. */
                        final Browser br2 = browser.cloneBrowser();
                        final UrlQuery query = new UrlQuery().parse(streamUrl);
                        query.add("client_id", getClientId(null));
                        streamUrl = URLHelper.parseLocation(new URL(streamUrl), "?" + query.toString()).toString();
                        br2.getPage(streamUrl);
                        final Map<String, Object> urlMap = JSonStorage.restoreFromString(br2.toString(), TypeRef.HASHMAP);
                        finallink = (String) urlMap.get("url");
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

    private void checkDirectLink(final DownloadLink link) throws Exception {
        URLConnectionAdapter con = null;
        try {
            final Browser br2 = br.cloneBrowser();
            con = br2.openGetConnection(dllink);
            if (con.getResponseCode() == 401) {
                link.setProperty(PROPERTY_directurl, Property.NULL);
                serverissue = true;
                return;
            }
            if (!this.looksLikeDownloadableContent(con)) {
                link.setProperty(PROPERTY_directurl, Property.NULL);
                serverissue = true;
                return;
            }
            link.setDownloadSize(con.getCompleteContentLength());
            link.setProperty(PROPERTY_directurl, dllink);
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    public static final String useragent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36";

    private Browser prepBR(final Browser br) {
        /* E.g. accessing invalid urls, their servers will return 503. */
        br.setAllowedResponseCodes(new int[] { 503 });
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", useragent);
        return br;
    }

    public void login(final Browser br, final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                prepBR(br);
                String oauthtoken = account.getStringProperty(PROPERTY_ACCOUNT_oauthtoken, null);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                final Cookies userCookies = Cookies.parseCookiesFromJsonString(account.getPass());
                /* 2020-12-15: Website/API login is broken thus only cookie login is possible */
                final boolean allowFullLogin = false;
                if (cookies != null && oauthtoken != null) {
                    br.setCookies(this.getHost(), cookies);
                    br.getHeaders().put("Authorization", "OAuth " + oauthtoken);
                    if (!force) {
                        logger.info("Trust cookies without checking");
                        return;
                    }
                    if (this.cookieCheck(br)) {
                        return;
                    }
                }
                if (userCookies != null) {
                    logger.info("Attempting user cookie login");
                    if (userCookies.get("oauth_token") == null) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Cookie login failed: Oauth token missing", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    br.setCookies(this.getHost(), userCookies);
                    oauthtoken = userCookies.get("oauth_token").getValue();
                    br.getHeaders().put("Authorization", "OAuth " + oauthtoken);
                    if (this.cookieCheck(br)) {
                        account.saveCookies(br.getCookies(this.getHost()), "");
                        account.setProperty(PROPERTY_ACCOUNT_oauthtoken, oauthtoken);
                        return;
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Cookie login failed", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                logger.info("Full login required");
                if (!allowFullLogin) {
                    showCookieLoginInformation();
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Cookie login required", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
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
                data.put("user_agent", useragent);
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
            }
        }
    }

    private Thread showCookieLoginInformation() {
        final Thread thread = new Thread() {
            public void run() {
                try {
                    final String help_article_url = "https://support.jdownloader.org/Knowledgebase/Article/View/account-cookie-login-instructions";
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = "Soundcloud - Login";
                        message += "Hallo liebe(r) Soundcloud NutzerIn\r\n";
                        message += "Um deinen Soundcloud Account in JDownloader verwenden zu können, musst du folgende Schritte beachten:\r\n";
                        message += "Folge der Anleitung im Hilfe-Artikel:\r\n";
                        message += help_article_url;
                    } else {
                        title = "Soundcloud - Login";
                        message += "Hello dear Soundcloud user\r\n";
                        message += "In order to use an account of this service in JDownloader, you need to follow these instructions:\r\n";
                        message += help_article_url;
                    }
                    final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                    dialog.setTimeout(3 * 60 * 1000);
                    if (CrossSystem.isOpenBrowserSupported() && !Application.isHeadless()) {
                        CrossSystem.openURL(help_article_url);
                    }
                    final ConfirmDialogInterface ret = UIOManager.I().show(ConfirmDialogInterface.class, dialog);
                    ret.throwCloseExceptions();
                } catch (final Throwable e) {
                    getLogger().log(e);
                }
            };
        };
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private boolean cookieCheck(final Browser br) throws IOException {
        br.getPage(API_BASEv2 + "/me?client_id=" + getClientIdV2() + "&app_version=" + getAppVersionV2() + "&app_locale=de");
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
        AccountInfo ai = new AccountInfo();
        login(this.br, account, true);
        ai.setUnlimitedTraffic();
        String acctype = null;
        if (br.getURL() == null || !br.getURL().contains(API_BASEv2 + "/me")) {
            br.getPage(API_BASEv2 + "/me?client_id=" + getClientIdV2() + "&app_version=" + getAppVersionV2() + "&app_locale=de");
        }
        final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        /*
         * 2020-12-15: At this moment only cookie login is possible which means in theory, user can enter anything in the username field ->
         * Let's fix that
         */
        final String email = (String) entries.get("primary_email");
        acctype = (String) JavaScriptEngineFactory.walkJson(entries, "consumer_subscription/product/id");
        if ("free".equalsIgnoreCase(acctype)) {
            ai.setStatus("Registered (free) account");
            account.setType(AccountType.FREE);
        } else {
            ai.setStatus("Premium account");
            account.setType(AccountType.PREMIUM);
        }
        if (!StringUtils.isEmpty(email)) {
            account.setUser(email);
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        login(br, account, false);
        doFree(link);
    }

    public static String getFormattedFilename(final DownloadLink downloadLink) throws ParseException {
        final String url_username = downloadLink.getStringProperty("plain_url_username", null);
        String songTitle = downloadLink.getStringProperty("plainfilename", null);
        final SubConfiguration cfg = SubConfiguration.getConfig("soundcloud.com");
        String formattedFilename = cfg.getStringProperty(CUSTOM_FILENAME_2, defaultCustomFilename);
        if (formattedFilename == null || formattedFilename.equals("")) {
            formattedFilename = defaultCustomFilename;
        }
        if (!formattedFilename.contains("*songtitle*") || !formattedFilename.contains("*ext*")) {
            formattedFilename = defaultCustomFilename;
        }
        String ext = downloadLink.getStringProperty("type", null);
        if (ext != null) {
            ext = "." + ext;
        } else {
            ext = ".mp3";
        }
        String date = downloadLink.getStringProperty("originaldate", null);
        final String channelName = downloadLink.getStringProperty("channel", null);
        final String track_id = downloadLink.getStringProperty(PROPERTY_track_id, null);
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
                    } catch (Exception e) {
                        // prevent user error killing plugin.
                        formattedDate = defaultformattedDate;
                    }
                }
            } catch (Exception e) {
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
        // Insert filename at the end to prevent errors with tags
        formattedFilename = formattedFilename.replace("*songtitle*", songTitle);
        final String setsposition = downloadLink.getStringProperty(PROPERTY_setsposition, null);
        if (cfg.getBooleanProperty(SETS_ADD_POSITION_TO_FILENAME, false) && setsposition != null) {
            formattedFilename = setsposition + formattedFilename;
        }
        return formattedFilename;
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        /* Do not allow URLs of this host to be downloaded via multihoster. */
        return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
    }

    @Override
    public String getDescription() {
        return "JDownloader's soundcloud.com plugin helps downloading audiofiles. JDownloader provides settings for custom filenames.";
    }

    private HashMap<String, String> phrasesEN = new HashMap<String, String>() {
                                                  {
                                                      put("SETTING_GRAB_PURCHASE_URL", "Grab purchase URL?\r\n<html><b>The purchase-URL sometimes lead to external downloadlinks e.g. mediafire.com.</b></html>");
                                                      put("SETTING_ONLY_DOWNLOAD_OFFICIALLY_DOWNLOADABLE_FILES", "Only download files which have a download button/are officially downloadable?\r\n<html><p style=\"color:#F62817\"><b>Warning: If you enable this, all soundcloud downloads without an official download possibility will get a red error state and will NOT be downloaded!</b></p></html>");
                                                      put("SETTING_ALLOW_PREVIEW_DOWNLOAD", "Download 30 second preview if a track is pay-only? [Not recommended]");
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
                                                      put("SETTING_LABEL_advanced_settings", "Advanced settings (only change them if you know what you're doing)");
                                                      put("ERROR_NOT_DOWNLOADABLE", "You disabled stream-downloads! This link is not officially downloadable!");
                                                      put("SETS_USE_APIv1", "Sets: Prefer usage of APIv1?\r\nWarning: GEO-blocked items will not appear in the linkgrabber anymore!");
                                                  }
                                              };
    private HashMap<String, String> phrasesDE = new HashMap<String, String>() {
                                                  {
                                                      put("SETTING_GRAB_PURCHASE_URL", "Kauflink einfügen?\r\n<html><b>Der Kauflink führt manchmal zu externen Downloadmöglichkeiten z.B. mediafire.com.</b></html>");
                                                      put("SETTING_ONLY_DOWNLOAD_OFFICIALLY_DOWNLOADABLE_FILES", "Lade nur Links mit offizieller downloadmöglichkeit/Downloadbutton herunter??\r\n<html><p style=\"color:#F62817\"><b>Warnung: Falls du das aktivierst werden alle Soundcloud Links ohne offizielle Downloadmöglichkeit einen roten Fehlerstatus bekommen und NICHT heruntergeladen!</b></p></html>");
                                                      put("SETTING_ALLOW_PREVIEW_DOWNLOAD", "Für Bezahltitel: Lade 30 Sekunden Ausschnitt herunter ?[Nicht ampfohlen!]");
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
                                                      put("SETTING_LABEL_advanced_settings", "Erweiterte Einstellungen (verändere diese nur, wenn du weißt was du tust)");
                                                      put("ERROR_NOT_DOWNLOADABLE", "Du hast stream-downloads deaktiviert! Dieser link ist nicht offiziell herunterladbar!");
                                                      put("SETS_USE_APIv1", "Sets: Verwende APIv1?\r\nBedenke, GEO-gesperrte Einträge fehlen dann im Linksammler!");
                                                  }
                                              };

    /**
     * Returns a German/English translation of a phrase. We don't use the JDownloader translation framework since we need only German and
     * English.
     *
     * @param key
     * @return
     */
    private String getPhrase(String key) {
        if ("de".equals(System.getProperty("user.language")) && phrasesDE.containsKey(key)) {
            return phrasesDE.get(key);
        } else if (phrasesEN.containsKey(key)) {
            return phrasesEN.get(key);
        }
        return "Translation not found!";
    }

    private static final boolean defaultONLY_DOWNLOAD_OFFICIALLY_DOWNLOADABLE_FILES = false;
    private static final boolean defaultALLOW_PREVIEW_DOWNLOAD                      = false;
    public static final boolean  defaultGRAB_PURCHASE_URL                           = false;
    public static final boolean  defaultGRAB500THUMB                                = false;
    public static final boolean  defaultGRABORIGINALTHUMB                           = false;
    public static final boolean  defaultSETS_USE_APIv1                              = false;
    private final static String  defaultCustomDate                                  = "dd.MM.yyyy";
    private final static String  defaultCustomFilename                              = "*songtitle*_*linkid* - *channelname**ext*";
    private final static String  defaultCustomPackagename                           = "*channelname* - *playlistname*";

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, getPhrase("SETTING_LABEL_crawler")));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB_PURCHASE_URL, getPhrase("SETTING_GRAB_PURCHASE_URL")).setDefaultValue(defaultGRAB_PURCHASE_URL));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRAB500THUMB, getPhrase("SETTING_GRAB500THUMB")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), GRABORIGINALTHUMB, getPhrase("SETTING_GRABORIGINALTHUMB")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, getPhrase("SETTING_LABEL_hoster")));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ONLY_DOWNLOAD_OFFICIALLY_DOWNLOADABLE_FILES, getPhrase("SETTING_ONLY_DOWNLOAD_OFFICIALLY_DOWNLOADABLE_FILES")).setDefaultValue(defaultONLY_DOWNLOAD_OFFICIALLY_DOWNLOADABLE_FILES));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_PREVIEW_DOWNLOAD, getPhrase("SETTING_ALLOW_PREVIEW_DOWNLOAD")).setDefaultValue(defaultONLY_DOWNLOAD_OFFICIALLY_DOWNLOADABLE_FILES));
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
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SETS_ADD_POSITION_TO_FILENAME, JDL.L("plugins.hoster.soundcloud.sets_add_position", "Sets: Add position to the beginning of the filename e.g. (1.trackname.mp3)?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        /* 2020-04-01: No advanced settings available anymore */
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, getPhrase("SETTING_LABEL_advanced_settings")));
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        /* 2020-04-01: Captchas are never required */
        return false;
    }
}