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

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
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

import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "soundcloud.com" }, urls = { "https://(www\\.)?soundclouddecrypted\\.com/[A-Za-z\\-_0-9]+/[A-Za-z\\-_0-9]+(/[A-Za-z\\-_0-9]+)?" })
public class SoundcloudCom extends PluginForHost {
    public SoundcloudCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
        this.setConfigElements();
    }

    /*
     * Last clientid: 2t9loNQH90kzJcsFCODdigxfp325aq4z, b45b1aa10f1ac2941910a7f0d10f8e28 fDoItMDbsbZz8dY16ZzARCZmzgHBPotA
     */
    public final static String   CLIENTID_8TRACKS                            = "3904229f42df3999df223f6ebf39a8fe";
    /* Another way to get final links: http://api.soundcloud.com/tracks/11111xxx_test_track_ID1111111/streams?format=json&consumer_key= */
    public final static String   CONSUMER_KEY_MYCLOUDPLAYERS_COM             = "PtMyqifCQMKLqwP0A6YQ";
    /* Before: 9194598 */
    public static final String[] stream_qualities                            = { "stream_url", "http_mp3_128_url", "hls_mp3_128_url" };
    /*
     * List of the old handling - keep this as information:
     * https://api.soundcloud.com/tracks/<TRACKID>/<STREAMTYPE>?format=json&client_id=%s&app_version=%s
     */
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
    public final static String   SETS_USE_APIv1                              = "SETS_USE_APIv1";
    private String               dllink                                      = null;
    private boolean              serverissue                                 = false;
    private boolean              is_officially_downloadable                  = false;
    private boolean              is_geo_blocked                              = false;
    private boolean              is_only_preview_downloadable                = false;

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("soundclouddecrypted", "soundcloud"));
    }

    @Override
    public String getAGBLink() {
        return "http://soundcloud.com/terms-of-use";
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
            final String av = br.getRegex("window\\.__sc_version = \"(\\d+)\"").getMatch(0);
            if (av == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            appVersion.set(av);
        }
        if (clientId.get() == null) {
            if (br == null || br._getURL() == null || !br._getURL().getPath().equals("/")) {
                br = obr.cloneBrowser();
                br.getPage("https://soundcloud.com/");
            }
            final String script = br.getRegex("<script(?:\\s+[^>]+|\\s+)src=(\"|')([^>]*/app-[^>]*\\.js)\\1").getMatch(1);
            if (script == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getHeaders().put("Accept", "*/*");
            br.getPage(script);
            final String ci = br.getRegex("[^a-zA-Z0-9_-]+client_id\\s*:\\s*(\"|')([a-zA-Z0-9]+)\\1").getMatch(1);
            clientId.set(ci);
        }
    }

    @SuppressWarnings({ "deprecation", "unused" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink parameter) throws Exception {
        dllink = null;
        serverissue = false;
        is_geo_blocked = false;
        String secrettoken = parameter.getStringProperty("secret_token", null);
        prepBR(br);
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            login(br, aa, false);
        }
        String songid = parameter.getStringProperty("track_id", null);
        Map<String, Object> response = null;
        if (songid == null) {
            br.getPage(parameter.getDownloadURL());
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
        if (secrettoken == null) {
            br.getPage("https://api-v2.soundcloud.com/tracks?urns=soundcloud%3Atracks%3A" + songid + "&client_id=" + getClientId(br) + "&app_version=" + SoundcloudCom.getAppVersion(br));
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (br.getHttpConnection().getResponseCode() == 401) {
                // keys are incorrect
                resetThis();
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            response = getStartJsonMap(br.toString());
            final AvailableStatus status = checkStatusJson(this, parameter, response, true);
            if (status.equals(AvailableStatus.FALSE)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /* !is_geo_blocked = policy equals "ALLOW" (the usual case). */
            is_geo_blocked = response.get("policy").equals("BLOCK");
        }
        if (!is_geo_blocked) {
            /*
             * Only do/try linkcheck if we know that the track is NOT geo-blocked. Attempting to get a downloadurl for GEO-blocked content
             * will result in response 400.
             */
            // Other handling for private links
            if (br.containsHTML("<sharing>private</sharing>") && ENABLE_TYPE_PRIVATE || secrettoken != null) {
                /* TODO: Find example links for this case, then use getDirectlink function here as well! */
                if (secrettoken == null) {
                    secrettoken = br.getRegex("\\?secret_token=([A-Za-z0-9\\-_]+)</uri>").getMatch(0);
                }
                if (secrettoken != null) {
                    br.getPage("https://api.soundcloud.com/i1/tracks/" + songid + "/streams?secret_token=" + secrettoken + "&client_id=" + getClientId(br) + "&app_version=" + SoundcloudCom.getAppVersion(br));
                } else {
                    br.getPage("https://api.soundcloud.com/i1/tracks/" + songid + "/streams?client_id=" + getClientId(br) + "&app_version=" + SoundcloudCom.getAppVersion(br));
                }
                if (br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                dllink = PluginJSonUtils.getJsonValue(br, "http_mp3_128_url");
                if (dllink == null || dllink.equals("")) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dllink = Encoding.unicodeDecode(dllink);
                dllink = Encoding.htmlDecode(dllink);
            } else {
                dllink = getDirectlink(br.toString(), songid);
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                is_officially_downloadable = isREALYDownloadable(response);
            }
            if (!dllink.contains("/playlist.m3u8")) {
                checkDirectLink(parameter);
            }
        }
        return AvailableStatus.TRUE;
    }

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
        requestFileInformation(link);
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
        if (dllink == null && link.getBooleanProperty("rtmp", false)) {
            /* TODO: Fix/remove/implement this */
            link.setProperty("directlink", Property.NULL);
            throw new PluginException(LinkStatus.ERROR_FATAL, "Not downloadable");
        } else if (dllink.contains("/playlist.m3u8")) {
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, dllink);
            dl.startDownload();
        } else {
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, 1);
            if (dl.getConnection().getContentType().contains("html")) {
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 416) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 416", 5 * 60 * 1000l);
                }
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // start of file extension correction.
            // required because original-format implies the uploaded format might not be what the end user downloads.
            if (dl.getConnection().getLongContentLength() == 0) {
                link.setProperty("rtmp", true);
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
        if (filename == null) {
            if (fromHostplugin) {
                parameter.getLinkStatus().setStatusText(JDL.L("plugins.hoster.SoundCloudCom.status.pluginBroken", "The host plugin is broken!"));
            }
            return AvailableStatus.FALSE;
        }
        filename = filename.trim();
        filename = plugin.encodeUnicode(filename);
        final String stream_url = toString(source.get("stream_url"));
        final String secret_token = stream_url != null ? new Regex(stream_url, "secret_token=([A-Za-z0-9\\-_]+)").getMatch(0) : null;
        final String id = toString(source.get("id"));
        /* Only availavle in APIV1, at least for playlist-requests */
        final String filesize = toString(source.get("original_content_size"));
        try {
            final String description = toString(source.get("description"));
            if (description != null) {
                parameter.setComment(description);
            }
        } catch (Throwable e) {
        }
        final String date = toString(source.get("created_at"));
        String username = (String) JavaScriptEngineFactory.walkJson(source, "user/username");
        String type = toString(source.get("original_format"));
        if (type == null || type.equals("raw")) {
            type = "mp3";
        }
        final String url = toString(source.get("download_url"));
        final boolean is_downloadable = isREALYDownloadable(source);
        if (url != null && is_downloadable) {
            /* we have original file downloadable */
            if (filesize != null) {
                parameter.setDownloadSize(Long.parseLong(filesize));
            }
            if (fromHostplugin) {
                parameter.getLinkStatus().setStatusText(JDL.L("plugins.hoster.SoundCloudCom.status.downloadavailable", "Original file is downloadable"));
            }
        } else {
            type = "mp3";
            if (fromHostplugin) {
                parameter.getLinkStatus().setStatusText(JDL.L("plugins.hoster.SoundCloudCom.status.previewavailable", "Preview (Stream) is downloadable"));
            }
        }
        if (url != null) {
            parameter.setProperty("directlink", url + "?client_id=" + getClientId(null));
        }
        if (username != null) {
            username = Encoding.htmlDecode(username.trim());
            parameter.setProperty("channel", username);
        }
        parameter.setProperty("plainfilename", filename);
        parameter.setProperty("originaldate", date);
        parameter.setProperty("track_id", id);
        parameter.setProperty("type", type);
        if (secret_token != null && !fromHostplugin) {
            parameter.setProperty("secret_token", secret_token);
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
    public boolean isSpeedLimited(DownloadLink link, Account account) {
        return false;
    }

    public static final String TYPE_API_ALL      = "https?://api\\.soundcloud\\.com/tracks/\\d+/(stream|download)(\\?secret_token=[A-Za-z0-9\\-_]+)?";
    public static final String TYPE_API_STREAM   = "https?://api\\.soundcloud\\.com/tracks/\\d+/stream";
    public static final String TYPE_API_DOWNLOAD = "https?://api\\.soundcloud\\.com/tracks/\\d+/download";
    public static final String TYPE_API_TOKEN    = "https?://api\\.soundcloud\\.com/tracks/\\d+/stream\\?secret_token=[A-Za-z0-9\\-_]+";

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getStartJsonMap(final String input) throws Exception {
        final Object responseo = JavaScriptEngineFactory.jsonToJavaObject(input);
        final Map<String, Object> response = (Map<String, Object>) JavaScriptEngineFactory.walkJson(responseo, "{0}");
        return response;
    }

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

    public static String getDirectlink(final String input, String track_id) {
        final Browser br2 = new Browser();
        String finallink = null;
        try {
            Map<String, Object> json = getStartJsonMap(input);
            final boolean is_downloadable = isREALYDownloadable(json);
            final String secret_token = new Regex(input, "secret_token=([A-Za-z0-9\\-_]+)").getMatch(0);
            if (track_id == null) {
                track_id = PluginJSonUtils.getJsonValue(input, "id");
            }
            if (is_downloadable) {
                /* Track is officially downloadable (download version = higher quality than stream version) */
                /* Downloadable version = highest quality! */
                finallink = toString(json.get("download_url"));
                if (finallink != null) {
                    /* We have it? Let's make it valid! */
                    if (!finallink.contains("?")) {
                        finallink += "?";
                    } else {
                        finallink += "&";
                    }
                    finallink += "client_id=" + getClientId(null) + "&app_version=" + SoundcloudCom.getAppVersion(null);
                }
            } else {
                /* Normal- or hls stream */
                if (secret_token != null) {
                    /* Special rare case */
                    br2.getPage("https://api.soundcloud.com/i1/tracks/" + track_id + "/streams?secret_token=" + secret_token + "&client_id=" + getClientId(null) + "&app_version=" + SoundcloudCom.getAppVersion(null));
                } else {
                    br2.getPage("https://api.soundcloud.com/tracks/" + track_id + "/streams" + "?format=json&client_id=" + getClientId(null) + "&app_version=" + SoundcloudCom.getAppVersion(null));
                }
                json = JavaScriptEngineFactory.jsonToJavaMap(br2.toString());
                for (final String quality : stream_qualities) {
                    finallink = (String) json.get(quality);
                    if (finallink != null) {
                        break;
                    }
                }
            }
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        return finallink;
    }

    private void checkDirectLink(final DownloadLink downloadLink) throws Exception {
        URLConnectionAdapter con = null;
        try {
            final Browser br2 = br.cloneBrowser();
            con = br2.openGetConnection(dllink);
            if (con.getResponseCode() == 401) {
                downloadLink.setProperty("directlink", Property.NULL);
                downloadLink.setProperty("rtmp", true);
                serverissue = true;
                return;
            }
            downloadLink.setProperty("rtmp", false);
            if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                downloadLink.setProperty("directlink", Property.NULL);
                serverissue = true;
                return;
            }
            downloadLink.setDownloadSize(con.getLongContentLength());
            downloadLink.setProperty("directlink", dllink);
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    private Browser prepBR(final Browser br) {
        /* E.g. accessing invalid urls, their servers will return 503. */
        br.setAllowedResponseCodes(503);
        br.setFollowRedirects(true);
        return br;
    }

    private static final String MAINPAGE = "http://soundcloud.com";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    public void login(final Browser br, final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                prepBR(br);
                String oauthtoken = null;
                // Load cookies
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?>) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(MAINPAGE, key, value);
                        }
                        oauthtoken = account.getStringProperty("oauthtoken", null);
                        if (oauthtoken != null) {
                            br.getHeaders().put("Authorization", "OAuth " + oauthtoken);
                        }
                    }
                }
                boolean fulllogin = true;
                if (!force) {
                    return;
                } else if (ret != null && force) {
                    // Prevent full login to prevent login captcha (when user is away)
                    boolean browserexception = false;
                    try {
                        br.getPage("https://api.soundcloud.com/me/messages/unread?limit=3&offset=0&linked_partitioning=1&client_id=" + getClientId(null) + "&app_version=" + SoundcloudCom.getAppVersion(null));
                    } catch (final BrowserException ebr) {
                        browserexception = true;
                    }
                    if (br.getRequest().getHttpConnection().getResponseCode() == 401) {
                        fulllogin = true;
                    } else if (browserexception) {
                        fulllogin = true;
                    } else {
                        fulllogin = false;
                    }
                }
                if (fulllogin) {
                    br.clearCookies(MAINPAGE);
                    try {
                        /* not available in old stable */
                        br.setAllowedResponseCodes(new int[] { 422 });
                    } catch (Throwable e) {
                    }
                    br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:21.0) Gecko/20100101 Firefox/21.0");
                    br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                    br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
                    br.getPage("https://soundcloud.com/connect?client_id=" + getClientId(null) + "&response_type=token&scope=non-expiring%20fast-connect%20purchase%20upload&display=next&redirect_uri=https%3A//soundcloud.com/soundcloud-callback.html");
                    br.setFollowRedirects(false);
                    URLConnectionAdapter con = null;
                    try {
                        con = br.openPostConnection("https://soundcloud.com/connect/login", "remember_me=on&redirect_uri=https%3A%2F%2Fsoundcloud.com%2Fsoundcloud-callback.html&response_type=token&scope=non-expiring+fast-connect+purchase+upload&display=next&client_id=" + getClientId(null) + "&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                        if (con.getResponseCode() == 422) {
                            br.followConnection();
                            final String rcID = br.getRegex("\\?k=([^<>\"]*?)\"").getMatch(0);
                            if (rcID == null) {
                                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Login function broken, please contact our support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                            }
                            final Recaptcha rc = new Recaptcha(br, this);
                            rc.setId(rcID);
                            rc.load();
                            final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                            final DownloadLink dummyLink = new DownloadLink(this, "Account", "soundcloud.com", "http://soundcloud.com", true);
                            final String c = getCaptchaCode("recaptcha", cf, dummyLink);
                            con = br.openPostConnection("https://soundcloud.com/connect/login", "remember_me=on&redirect_uri=https%3A%2F%2Fsoundcloud.com%2Fsoundcloud-callback.html&response_type=token&scope=non-expiring+fast-connect+purchase+upload&display=next&client_id=" + getClientId(null) + "&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c));
                            br.followConnection();
                        }
                    } finally {
                        try {
                            con.disconnect();
                        } catch (Throwable e) {
                        }
                    }
                    oauthtoken = br.getRegex("access_token=([^<>\"]*?)\\&").getMatch(0);
                    if (oauthtoken != null) {
                        br.getHeaders().put("Authorization", "OAuth " + oauthtoken);
                        account.setProperty("oauthtoken", oauthtoken);
                        logger.info("Found and set oauth token");
                    } else {
                        logger.info("Could not find oauth token");
                    }
                    String continueLogin = br.getRegex("\"(https://soundcloud\\.com/soundcloud\\-callback\\.html[^<>\"]*?)\"").getMatch(0);
                    if (continueLogin == null) {
                        continueLogin = br.getRedirectLocation();
                    }
                    if (continueLogin == null || !"free".equals(br.getCookie("https://soundcloud.com/", "c"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nUngültiger Benutzername oder ungültiges Passwort!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    br.getPage(continueLogin);
                    // Save cookies
                    final HashMap<String, String> cookies = new HashMap<String, String>();
                    final Cookies add = br.getCookies(MAINPAGE);
                    for (final Cookie c : add.getCookies()) {
                        cookies.put(c.getKey(), c.getValue());
                    }
                    account.setProperty("name", Encoding.urlEncode(account.getUser()));
                    account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                    account.setProperty("cookies", cookies);
                }
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(br, account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setUnlimitedTraffic();
        account.setValid(true);
        ai.setStatus("Registered (free) User");
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
        final String track_id = downloadLink.getStringProperty("track_id", null);
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
        final String setsposition = downloadLink.getStringProperty("setsposition", null);
        if (cfg.getBooleanProperty(SETS_ADD_POSITION_TO_FILENAME, false) && setsposition != null) {
            formattedFilename = setsposition + formattedFilename;
        }
        return formattedFilename;
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
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
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, getPhrase("SETTING_LABEL_advanced_settings")));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SETS_USE_APIv1, getPhrase("SETS_USE_APIv1")).setDefaultValue(defaultSETS_USE_APIv1));
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

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("nopremium"))) {
            /* free accounts also have captchas */
            return true;
        }
        if (acc.getStringProperty("session_type") != null && !"premium".equalsIgnoreCase(acc.getStringProperty("session_type"))) {
            return true;
        }
        return false;
    }
}