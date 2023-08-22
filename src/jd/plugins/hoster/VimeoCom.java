//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.JVMVersion;
import org.appwork.utils.StringUtils;
import org.appwork.utils.Time;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.URLHelper;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.downloader.hls.M3U8Playlist;
import org.jdownloader.plugins.components.containers.VimeoContainer;
import org.jdownloader.plugins.components.containers.VimeoContainer.Quality;
import org.jdownloader.plugins.components.containers.VimeoContainer.Source;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.linkcrawler.LinkCrawlerDeepInspector;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.http.requests.GetRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vimeo.com" }, urls = { "decryptedforVimeoHosterPlugin://.+" })
public class VimeoCom extends PluginForHost {
    /* Skip HLS because of unsupported split video/audio. */
    public static final boolean ALLOW_HLS                       = false;
    private static final String MAINPAGE                        = "https://vimeo.com";
    private String              finalURL;
    public static final String  Q_MOBILE                        = "Q_MOBILE";
    public static final String  Q_ORIGINAL                      = "Q_ORIGINAL";
    public static final String  Q_HD                            = "Q_HD";
    public static final String  Q_SD                            = "Q_SD";
    public static final String  Q_BEST                          = "Q_BEST";
    public static final String  SUBTITLE                        = "SUBTITLE";
    public static final String  CUSTOM_DATE                     = "CUSTOM_DATE_3";
    public static final String  CUSTOM_PACKAGENAME_SINGLE_VIDEO = "CUSTOM_PACKAGENAME_SINGLE_VIDEO";
    public static final String  CUSTOM_FILENAME                 = "CUSTOM_FILENAME_3";
    public static final String  ALWAYS_LOGIN                    = "ALWAYS_LOGIN";
    public static final String  VVC                             = "VVC_1";
    public static final String  P_240                           = "P_240";
    public static final String  P_360                           = "P_360";
    public static final String  P_480                           = "P_480";
    public static final String  P_540                           = "P_540";
    public static final String  P_720                           = "P_720";
    public static final String  P_1080                          = "P_1080";
    public static final String  P_1440                          = "P_1440";
    public static final String  P_2560                          = "P_2560";
    public static final String  ASK_REF                         = "ASK_REF";
    public static final String  PROPERTY_PASSWORD_COOKIE_KEY    = "password_cookie_key";
    public static final String  PROPERTY_PASSWORD_COOKIE_VALUE  = "password_cookie_value";

    public VimeoCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://vimeo.com/join");
        setConfigElements();
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.VIDEO_STREAMING };
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        final String url = link.getPluginPatternMatcher().replaceFirst("decryptedforVimeoHosterPlugin://", "https://");
        link.setPluginPatternMatcher(url);
    }

    @Override
    public String getAGBLink() {
        return "https://www.vimeo.com/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    public static Browser prepBrGeneral(Plugin plugin, final DownloadLink dl, final Browser prepBr) {
        final String vimeo_forced_referer = dl != null ? getForcedReferer(dl) : null;
        if (vimeo_forced_referer != null) {
            prepBr.getHeaders().put("Referer", vimeo_forced_referer);
        }
        /* we do not want German headers! */
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        prepBr.setAllowedResponseCodes(new int[] { 418, 410, 451, 406 });
        prepBr.setCookiesExclusive(true);
        prepBr.clearCookies(plugin.getHost());
        prepBr.setCookie(plugin.getHost(), "language", "en");
        return prepBr;
    }

    /* API - might be useful for the future: https://github.com/bromix/plugin.video.vimeo/blob/master/resources/lib/vimeo/client.py */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        br = prepBrGeneral(this, link, br);
        setBrowserExclusive();
        if (link.getBooleanProperty("offline", false)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // remove ORIGINAL and deprecated ID
        final String downloadlinkId = link.getLinkID().replaceFirst("_ORIGINAL$", "").replaceFirst("(\\d+x\\d+)(_\\d+_)", "$1_");
        final String videoQuality = link.getStringProperty("videoQuality", null);
        final boolean isSubtitle = (StringUtils.endsWithCaseInsensitive(videoQuality, "SUBTITLE") || StringUtils.containsIgnoreCase(videoQuality, "_SUBTITLE_")) || (StringUtils.endsWithCaseInsensitive(downloadlinkId, "SUBTITLE") || StringUtils.containsIgnoreCase(downloadlinkId, "_SUBTITLE_"));
        final boolean isHLS = StringUtils.endsWithCaseInsensitive(videoQuality, "HLS") || StringUtils.endsWithCaseInsensitive(downloadlinkId, "HLS");
        final boolean isWEB = StringUtils.endsWithCaseInsensitive(videoQuality, "WEB") || StringUtils.endsWithCaseInsensitive(downloadlinkId, "WEB");
        final boolean isDASH = StringUtils.endsWithCaseInsensitive(videoQuality, "DASH") || StringUtils.endsWithCaseInsensitive(downloadlinkId, "DASH");
        final boolean isDownload = StringUtils.endsWithCaseInsensitive(videoQuality, "DOWNLOAD") || StringUtils.endsWithCaseInsensitive(downloadlinkId, "DOWNLOAD");
        br.setFollowRedirects(true);
        final VIMEO_URL_TYPE type = getVimeoUrlType(link);
        URLConnectionAdapter con = null;
        finalURL = link.getStringProperty("directURL", null);
        if (finalURL != null) {
            try {
                final Browser brc = br.cloneBrowser();
                brc.setFollowRedirects(true);
                brc.getHeaders().put("Accept-Encoding", "identity");
                /* Some videos are hosted on Amazon S3, don't use head requests for this reason */
                con = brc.openGetConnection(finalURL);
                final int responseCode = con.getResponseCode();
                final String contentType = con.getContentType();
                if (responseCode == 200 && LinkCrawlerDeepInspector.looksLikeMpegURL(con) && isHLS) {
                    link.setFinalFileName(getFormattedFilename(link));
                    return AvailableStatus.TRUE;
                } else if (responseCode == 200 && StringUtils.containsIgnoreCase(contentType, "vtt") && isSubtitle) {
                    if (con.getLongContentLength() > 0) {
                        link.setVerifiedFileSize(con.getLongContentLength());
                    }
                    link.setFinalFileName(getFormattedFilename(link));
                    return AvailableStatus.TRUE;
                } else if (looksLikeDownloadableContent(con)) {
                    if (con.getLongContentLength() > 0) {
                        link.setVerifiedFileSize(con.getLongContentLength());
                    }
                    if (!link.hasProperty("videoTitle")) {
                        String fileName = Plugin.getFileNameFromDispositionHeader(con);
                        if (fileName == null) {
                            fileName = UrlQuery.parse(con.getURL().getQuery()).getDecoded("filename");
                        }
                        if (StringUtils.isNotEmpty(fileName)) {
                            final String videoTitle = fileName.replaceFirst("(\\.(mp4|mov|wmv|avi|flv|m4v))", "");
                            final String extension = fileName.replaceFirst("(.+?)\\.([a-z0-9]{3})$", "$2");
                            if (StringUtils.isNotEmpty(videoTitle)) {
                                link.setProperty("videoTitle", videoTitle);
                            }
                            if (StringUtils.isNotEmpty(extension)) {
                                link.setProperty("videoExt", "." + extension);
                            }
                        }
                    }
                    link.setFinalFileName(getFormattedFilename(link));
                    return AvailableStatus.TRUE;
                } else {
                    brc.followConnection(true);
                    if (VIMEO_URL_TYPE.PLAY.equals(type) && responseCode == 410) {
                        // expired
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    } else if (StringUtils.containsIgnoreCase(contentType, "json") || StringUtils.containsIgnoreCase(finalURL, "cold_request=1") || StringUtils.contains(con.getURL().toString(), "cold_request=1")) {
                        // defrosting, fetching from cold storage
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Defrosting download, please wait", 30 * 60 * 1000l);
                    } else {
                        /* directURL no longer valid */
                        finalURL = null;
                        link.setProperty("directURL", Property.NULL);
                    }
                }
            } catch (final IOException e) {
                link.setProperty("directURL", Property.NULL);
                throw e;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        final String videoID = getVideoID(link);
        if (videoID == null) {
            /* This should never happen! */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        switch (type) {
        case PLAY:
        case EXTERNAL:
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        default:
            break;
        }
        br = prepBrGeneral(this, link, new Browser());
        br.setFollowRedirects(true);
        final String forced_referer = getForcedReferer(link);
        final AtomicReference<String> referer = new AtomicReference<String>(forced_referer);
        final boolean alwaysLogin = getPluginConfig().getBooleanProperty(VimeoCom.ALWAYS_LOGIN, false);
        final Account account = (alwaysLogin || (Thread.currentThread() instanceof SingleDownloadController)) ? AccountController.getInstance().getValidAccount(this) : null;
        Object lock = new Object();
        if (account != null) {
            try {
                login(this, br, account);
                lock = account;
            } catch (PluginException e) {
                logger.log(e);
                final LogInterface logger = getLogger();
                if (logger instanceof LogSource) {
                    handleAccountException(account, logger, e);
                } else {
                    handleAccountException(account, null, e);
                }
            }
        }
        final Map<String, Object> properties = new HashMap<String, Object>();
        synchronized (lock) {
            try {
                accessVimeoURL(this, this.br, link.getPluginPatternMatcher(), referer, type, properties);
            } catch (PluginException e) {
                // TODO
                handlePW(link, br);
                accessVimeoURL(this, this.br, link.getPluginPatternMatcher(), referer, type, properties);
            }
            /* Video titles can be changed afterwards by the puloader - make sure that we always got the currrent title! */
            String videoTitle = null;
            try {
                final String json = getJsonFromHTML(this, this.br);
                Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(json);
                entries = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "vimeo_esi/config/clipData");
                if (entries != null) {
                    videoTitle = (String) entries.get("title");
                }
            } catch (final Throwable e) {
                logger.log(e);
            }
            // now we nuke linkids for videos.. crazzy... only remove the last one, _ORIGINAL comes from variant system
            final List<VimeoContainer> qualities = find(this, type, br, videoID, getUnlistedHash(link), properties, isDownload, isWEB || isDASH, isHLS, isSubtitle);
            if (qualities.isEmpty()) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            VimeoContainer container = null;
            if (downloadlinkId != null) {
                for (VimeoContainer quality : qualities) {
                    final String linkdupeid = quality.createLinkID(videoID);
                    // match refreshed qualities to stored reference, to make sure we have the same format for resume!
                    if (StringUtils.equalsIgnoreCase(linkdupeid, downloadlinkId)) {
                        container = quality;
                        break;
                    }
                }
            }
            if (container == null && videoQuality != null) {
                for (VimeoContainer quality : qualities) {
                    // match refreshed qualities to stored reference, to make sure we have the same format for resume!
                    if (videoQuality.equalsIgnoreCase(quality.getQuality().toString())) {
                        container = quality;
                        break;
                    }
                }
            }
            if (container == null) {
                final VimeoContainer vvc = getVimeoVideoContainer(link, true);
                if (vvc != null) {
                    for (VimeoContainer quality : qualities) {
                        final boolean sameHeight = quality.getHeight() == vvc.getHeight();
                        final boolean sameWidth = quality.getWidth() == vvc.getWidth();
                        final boolean sameSource = quality.getSource() == vvc.getSource();
                        final boolean sameQuality = quality.getQuality() == vvc.getQuality();
                        // we split HD/720,1080 into HD/720 and FHD/1080
                        final boolean sameCompatibleQuality = VimeoContainer.Quality.FHD.equals(quality.getQuality()) && VimeoContainer.Quality.HD.equals(vvc.getQuality());
                        if (sameHeight && sameWidth && sameSource && (sameQuality || sameCompatibleQuality)) {
                            container = quality;
                            break;
                        }
                    }
                }
            }
            if (container == null || container.getDownloadurl() == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "not found:linkID=" + downloadlinkId + "|quality=" + videoQuality);
            } else {
                logger.info("found:linkID=" + downloadlinkId + "|quality=" + videoQuality + "|container=" + container + "|url=" + container.getDownloadurl());
            }
            finalURL = container.getDownloadurl();
            switch (container.getSource()) {
            case DOWNLOAD:
            case WEB:
            case SUBTITLE:
                try {
                    /* Some videos are hosted on Amazon S3, don't use head requests for this reason */
                    final Browser brc = br.cloneBrowser();
                    brc.getHeaders().put("Accept-Encoding", "identity");
                    brc.setFollowRedirects(true);
                    con = brc.openGetConnection(finalURL);
                    if (!StringUtils.containsIgnoreCase(con.getContentType(), "html") && con.getResponseCode() == 200) {
                        if (con.getLongContentLength() > 0) {
                            link.setVerifiedFileSize(con.getLongContentLength());
                        }
                        link.setProperty("directURL", finalURL);
                    } else {
                        brc.followConnection(true);
                        if (con.getResponseCode() == 500) {
                            /* 2020-07-06: E.g. "Original" version of video is officially available but download is broken serverside. */
                            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 500");
                        } else if (StringUtils.containsIgnoreCase(con.getContentType(), "json") || StringUtils.containsIgnoreCase(finalURL, "cold_request=1") || StringUtils.contains(con.getURL().toString(), "cold_request=1")) {
                            // defrosting, fetching from cold storage
                            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Defrosting download, please wait", 30 * 60 * 1000l);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                    }
                } finally {
                    if (con != null) {
                        con.disconnect();
                    }
                }
                break;
            case HLS:
                if (container.getEstimatedSize() != null) {
                    link.setDownloadSize(container.getEstimatedSize());
                }
                break;
            default:
                break;
            }
            if (!link.hasProperty("videoTitle") && !StringUtils.isEmpty(videoTitle)) {
                link.setProperty("videoTitle", videoTitle);
            }
            link.setFinalFileName(getFormattedFilename(link));
            return AvailableStatus.TRUE;
        }
    }

    public static String getVideoID(final DownloadLink dl) {
        return dl.getStringProperty("videoID", null);
    }

    private String getUnlistedHash(final DownloadLink link) {
        String unlistedHash = jd.plugins.decrypter.VimeoComDecrypter.getUnlistedHashFromURL(link.getPluginPatternMatcher());
        if (StringUtils.isEmpty(unlistedHash)) {
            unlistedHash = link.getStringProperty("specialVideoID", null);
        }
        return unlistedHash;
    }

    public static enum VIMEO_URL_TYPE {
        PLAY, // https://player.vimeo.com/play/, links do expire!
        EXTERNAL, // https://player.vimeo.com/external/
        SHOWCASE,
        SHOWCASE_VIDEO,
        RAW,
        PLAYER, // https://player.vimeo.com/....
        PLAYER_UNLISTED, // https://player.vimeo.com/....h=... or // https://player.vimeo.com/.../...
        CONFIG_TOKEN, // .../config...token=....
        UNLISTED,
        NORMAL
    }

    public static VIMEO_URL_TYPE getUrlType(final String url) {
        if (url != null) {
            final String configToken = jd.plugins.decrypter.VimeoComDecrypter.getPlayerConfigTokenFromURL(url);
            if (configToken != null) {
                return VIMEO_URL_TYPE.CONFIG_TOKEN;
            } else {
                final String unlistedHash = jd.plugins.decrypter.VimeoComDecrypter.getUnlistedHashFromURL(url);
                if (unlistedHash != null) {
                    if (url.matches("(?i).*(\\?|&)portfolio_id=\\d+.*")) {
                        return VIMEO_URL_TYPE.RAW;
                    } else if (url.matches("^https?://player\\.vimeo.com/video/.+")) {
                        return VIMEO_URL_TYPE.PLAYER_UNLISTED;
                    } else {
                        return VIMEO_URL_TYPE.UNLISTED;
                    }
                } else if (url.matches("(?i)^https?://player\\.vimeo.com/play/.+")) {
                    return VIMEO_URL_TYPE.PLAY;
                } else if (url.matches("(?i)^https?://player\\.vimeo.com/external/.+")) {
                    return VIMEO_URL_TYPE.EXTERNAL;
                } else if (url.matches("(?i)^https?://player\\.vimeo.com/.+")) {
                    if (url.matches("(?i).*(\\?|&)portfolio_id=\\d+.*")) {
                        return VIMEO_URL_TYPE.RAW;
                    } else {
                        return VIMEO_URL_TYPE.PLAYER;
                    }
                } else if (url.matches(jd.plugins.decrypter.VimeoComDecrypter.LINKTYPE_SHOWCASE_VIDEO)) {
                    return VIMEO_URL_TYPE.SHOWCASE_VIDEO;
                } else if (url.matches(jd.plugins.decrypter.VimeoComDecrypter.LINKTYPE_SHOWCASE)) {
                    return VIMEO_URL_TYPE.SHOWCASE;
                }
            }
        }
        return VIMEO_URL_TYPE.RAW;
    }

    public static class WrongRefererException extends Exception {
        private final String         referer;
        private final VIMEO_URL_TYPE urlType;

        public WrongRefererException(VIMEO_URL_TYPE urlType, final String referer) {
            this.urlType = urlType;
            this.referer = referer;
        }

        public String getReferer() {
            return referer;
        }

        public VIMEO_URL_TYPE getUrlType() {
            return urlType;
        }
    }

    private static AtomicReference<String[]> VIEWER        = new AtomicReference<String[]>(null);
    private static AtomicLong                VIEWER_EXPIRE = new AtomicLong(0);

    public static String[] getVIEWER(final Plugin plugin, final Browser br) throws Exception {
        synchronized (VIEWER_EXPIRE) {
            final long now = Time.systemIndependentCurrentJVMTimeMillis();
            String viewer[] = VIEWER.get();
            if (viewer != null && now < VIEWER_EXPIRE.get()) {
                // avoid too many usages of same JWT
                VIEWER_EXPIRE.addAndGet(-10 * 1000l);
                return viewer;
            }
            final Browser brc = br.cloneBrowser();
            brc.getPage("https://vimeo.com/_rv/viewer");
            final String jwtToken = PluginJSonUtils.getJson(brc, "jwt");
            final String vuid = PluginJSonUtils.getJson(brc, "vuid");
            final String token = PluginJSonUtils.getJson(brc, "xsrft");
            if (StringUtils.isEmpty(jwtToken)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else if (StringUtils.isEmpty(vuid)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else if (StringUtils.isEmpty(token)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                viewer = new String[] { jwtToken, vuid, token };
                VIEWER.set(viewer);
                VIEWER_EXPIRE.set(now + (2 * 60 * 1000l));
                return viewer;
            }
        }
    }

    public static Map<String, Object> accessVimeoAPI(final Plugin plugin, final Browser br, final Map<String, Object> properties, final String videoID, final String unlistedHash, String jwt) throws Exception {
        final GetRequest apiRequest;
        if (unlistedHash != null) {
            apiRequest = br.createGetRequest(String.format("https://api.vimeo.com/videos/%s:%s", videoID, unlistedHash));
        } else {
            apiRequest = br.createGetRequest(String.format("https://api.vimeo.com/videos/%s", videoID));
        }
        if (Boolean.FALSE.equals(properties.get(apiRequest.getUrl()))) {
            plugin.getLogger().info("Skip accessVimeoAPI:" + apiRequest.getUrl());
            return null;
        } else {
            if (jwt == null) {
                jwt = getVIEWER(plugin, br)[0];
            }
            apiRequest.getHeaders().put("Authorization", "jwt " + jwt);
            apiRequest.setCustomCharset("UTF-8");
            br.getPage(apiRequest);
            if (isPasswordProtectedAPIError(br)) {
                throw new DecrypterRetryException(RetryReason.PASSWORD);
            } else {
                final Map<String, Object> apiResponse = apiResponseValidator(plugin, br);
                if (apiResponse == null) {
                    // do not retry accessVimeoAPI again
                    properties.put(apiRequest.getUrl(), Boolean.FALSE);
                    return null;
                } else {
                    return apiResponse;
                }
            }
        }
    }

    /**
     * Core function to access a vimeo URL for the first time! Make sure to call password handling afterwards! <br />
     * Important: Execute password handling afterwards!!
     */
    public static VIMEO_URL_TYPE accessVimeoURL(final Plugin plugin, final Browser br, final String url_source, final AtomicReference<String> forced_referer, final VIMEO_URL_TYPE urlTypeRequested, final Map<String, Object> properties) throws Exception {
        final String videoID = jd.plugins.decrypter.VimeoComDecrypter.getVideoidFromURL(url_source);
        String unlistedHash = jd.plugins.decrypter.VimeoComDecrypter.getUnlistedHashFromURL(url_source);
        if (StringUtils.isEmpty(unlistedHash) && plugin instanceof VimeoCom) {
            final PluginForHost plg = (PluginForHost) plugin;
            unlistedHash = ((VimeoCom) plugin).getUnlistedHash(plg.getDownloadLink());
        }
        final String configToken = jd.plugins.decrypter.VimeoComDecrypter.getPlayerConfigTokenFromURL(url_source);
        final String reviewHash = jd.plugins.decrypter.VimeoComDecrypter.getReviewHashFromURL(url_source);
        final String referer = forced_referer != null ? forced_referer.get() : null;
        final boolean apiMode = true;
        try {
            if (referer != null) {
                plugin.getLogger().info("Referer:" + referer);
                final URL url = new URL(referer);
                br.getHeaders().put("Origin", url.getProtocol() + "://" + url.getHost());
                br.getHeaders().put("Referer", referer);
            }
            plugin.getLogger().info("urlTypeRequested:" + urlTypeRequested);
            VIMEO_URL_TYPE newUrlType = null;
            if (reviewHash != null) {
                newUrlType = getUrlType(url_source);
                br.getPage(url_source.replace("/review/", "/review/data/"));
                final String jwt = PluginJSonUtils.getJson(br, "jwtToken");
                if (false && jwt != null && apiMode) {
                    // doesn't contain any config_url/streams
                    if (unlistedHash == null) {
                        unlistedHash = PluginJSonUtils.getJson(br, "unlistedHash");
                    }
                    Browser brc = br.cloneBrowser();
                    if (accessVimeoAPI(plugin, brc, properties, videoID, unlistedHash, jwt) != null) {
                        br.setRequest(brc.getRequest());
                    }
                }
                if (isPasswordProtectedReview(br)) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Password needed!");
                }
            } else if (urlTypeRequested == VIMEO_URL_TYPE.RAW || (urlTypeRequested == null && url_source.matches("https?://.*?vimeo\\.com.*?/review/.+")) || videoID == null) {
                /*
                 * 2019-02-20: Special: We have to access 'review' URLs same way as via browser - if we don't, we will get response 403/404!
                 * Review-URLs may contain a reviewHash which is required! If then, inside their json, the unlistedHash is present,
                 */
                newUrlType = getUrlType(url_source);
                plugin.getLogger().info("getUrlType:" + url_source + "->" + newUrlType);
                if (apiMode && videoID != null && reviewHash == null) {
                    Browser brc = br.cloneBrowser();
                    try {
                        if (accessVimeoAPI(plugin, brc, properties, videoID, unlistedHash, null) != null) {
                            br.setRequest(brc.getRequest());
                        } else {
                            br.getPage(url_source);
                        }
                    } catch (DecrypterRetryException e) {
                        if (RetryReason.PASSWORD.equals(e.getReason())) {
                            // required for password handling
                            br.getPage(url_source);
                        }
                        throw e;
                    }
                } else {
                    br.getPage(url_source);
                }
            } else if (urlTypeRequested == VIMEO_URL_TYPE.SHOWCASE) {
                newUrlType = VIMEO_URL_TYPE.SHOWCASE;
                br.getPage(url_source);
                if (br.containsHTML("\"clips\"\\s*:\\s*\\[\\s*\\]")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Maybe wrong referer:" + referer);
                }
            } else if (configToken != null || urlTypeRequested == VIMEO_URL_TYPE.CONFIG_TOKEN) {
                newUrlType = VIMEO_URL_TYPE.CONFIG_TOKEN;
                br.getPage(url_source);
            } else if (unlistedHash == null && (urlTypeRequested == VIMEO_URL_TYPE.PLAYER || (urlTypeRequested == null && referer != null))) {
                newUrlType = VIMEO_URL_TYPE.PLAYER;
                final String nonAPIRequestURL = "https://player.vimeo.com/video/" + videoID;
                if (apiMode) {
                    Browser brc = br.cloneBrowser();
                    try {
                        if (accessVimeoAPI(plugin, brc, properties, videoID, unlistedHash, null) != null) {
                            br.setRequest(brc.getRequest());
                        } else {
                            // video might be unlisted, unlisted_hash required for api.vimeo.com
                            br.getPage(nonAPIRequestURL);
                        }
                    } catch (DecrypterRetryException e) {
                        if (RetryReason.PASSWORD.equals(e.getReason())) {
                            // required for password handling
                            br.getPage(nonAPIRequestURL);
                        }
                        throw e;
                    }
                } else {
                    br.getPage(nonAPIRequestURL);
                }
            } else if (unlistedHash != null && (urlTypeRequested == VIMEO_URL_TYPE.UNLISTED || urlTypeRequested == VIMEO_URL_TYPE.PLAYER || urlTypeRequested == VIMEO_URL_TYPE.PLAYER_UNLISTED || urlTypeRequested == null)) {
                Browser brcPlayer = null;
                if (urlTypeRequested == VIMEO_URL_TYPE.PLAYER_UNLISTED) {
                    brcPlayer = br.cloneBrowser();
                    brcPlayer.getPage("https://player.vimeo.com/video/" + videoID + "?h=" + unlistedHash);
                    if (!brcPlayer.containsHTML("\"privacy\"\\s*:\\s*\"unlisted\"")) {
                        newUrlType = urlTypeRequested;
                        br.setRequest(brcPlayer.getRequest());
                    }
                }
                if (newUrlType == null) {
                    newUrlType = VIMEO_URL_TYPE.UNLISTED;
                    try {
                        final Browser brc = br.cloneBrowser();
                        final Map<String, Object> apiResponse = accessVimeoAPI(plugin, brc, properties, videoID, unlistedHash, null);
                        if (apiResponse != null) {
                            br.setRequest(brc.getRequest());
                        } else if (brc.getHttpConnection().getResponseCode() == 404) {
                            if (urlTypeRequested == VIMEO_URL_TYPE.PLAYER_UNLISTED) {
                                newUrlType = VIMEO_URL_TYPE.PLAYER_UNLISTED;
                                if (brcPlayer == null) {
                                    br.getPage("https://player.vimeo.com/video/" + videoID + "?h=" + unlistedHash);
                                } else {
                                    br.setRequest(brcPlayer.getRequest());
                                }
                            } else {
                                /* {"error": "The requested video couldn't be found."} */
                                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                            }
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                    } catch (DecrypterRetryException e) {
                        if (RetryReason.PASSWORD.equals(e.getReason()) && urlTypeRequested == VIMEO_URL_TYPE.PLAYER_UNLISTED) {
                            // required for password handling
                            if (brcPlayer == null) {
                                br.getPage("https://player.vimeo.com/video/" + videoID + "?h=" + unlistedHash);
                            } else {
                                br.setRequest(brcPlayer.getRequest());
                            }
                        }
                        throw e;
                    }
                }
            } else if (urlTypeRequested == VIMEO_URL_TYPE.SHOWCASE_VIDEO) {
                newUrlType = VIMEO_URL_TYPE.SHOWCASE_VIDEO;
                /*
                 * This will grant access to single video items based on previous session when password was needed and user entered correct
                 * password e.g. to crawl a password protected 'showcase' video album.
                 */
                if (properties.containsKey(PROPERTY_PASSWORD_COOKIE_KEY)) {
                    br.setCookie(plugin.getHost(), properties.get(PROPERTY_PASSWORD_COOKIE_KEY).toString(), properties.get(PROPERTY_PASSWORD_COOKIE_VALUE).toString());
                }
                br.getPage(url_source);
            } else {
                if (unlistedHash != null) {
                    newUrlType = VIMEO_URL_TYPE.UNLISTED;
                    br.getPage(String.format("https://vimeo.com/%s/%s", videoID, unlistedHash));
                } else {
                    newUrlType = VIMEO_URL_TYPE.NORMAL;
                    br.getPage("https://vimeo.com/" + videoID);
                }
            }
            if (br.getHttpConnection().getResponseCode() == 410) {
                // view: 7
                // configToken expired?
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Link expired!");
            } else if (br.getHttpConnection().getResponseCode() == 403) {
                // referer or account might be required
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "403");
            } else if (br.getHttpConnection().getResponseCode() == 404 || "This video does not exist\\.".equals(PluginJSonUtils.getJsonValue(br, "message"))) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Video does not exist");
            } else if (br.getHttpConnection().getResponseCode() == 451) {
                // HTTP/1.1 451 Unavailable For Legal Reasons
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "451");
            } else if (br.containsHTML(">There was a problem loading this video")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Problem loading this video");
            } else if (br.containsHTML(">\\s*Private Video on Vimeo\\s*<")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Private video");
            } else {
                return newUrlType;
            }
        } finally {
            final String vuid = br.getRegex("document\\.cookie\\s*=\\s*'vuid='\\s*\\+\\s*encodeURIComponent\\('(\\d+\\.\\d+)'\\)").getMatch(0);
            if (vuid != null) {
                br.setCookie(br.getURL(), "vuid", vuid);
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    @Override
    public String getMirrorID(DownloadLink link) {
        if (link != null) {
            final String id = link.getLinkID().replaceFirst("_ORIGINAL$", "");
            return "vimeo://" + id;
        } else {
            return super.getMirrorID(link);
        }
    }

    public void doFree(final DownloadLink downloadLink) throws Exception {
        if (finalURL == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            final String forced_referer = getForcedReferer(downloadLink);
            if (forced_referer != null) {
                br.setCurrentURL(forced_referer);
            }
            final VIMEO_URL_TYPE type = getVimeoUrlType(downloadLink);
            switch (type) {
            case EXTERNAL:
            case PLAY:
                // nothing
                break;
            default:
                br.getPage(downloadLink.getDownloadURL());
                break;
            }
            if (!finalURL.contains(".m3u8")) {
                dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, finalURL, true, 0);
                if (!dl.getConnection().isOK() || StringUtils.containsIgnoreCase(dl.getConnection().getContentType(), "html") || StringUtils.containsIgnoreCase(dl.getConnection().getContentType(), "json")) {
                    logger.warning("The final dllink seems not to be a file!");
                    br.followConnection(true);
                    if (VIMEO_URL_TYPE.PLAY.equals(type) && br.getHttpConnection().getResponseCode() == 410) {
                        // expired
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    } else if (StringUtils.containsIgnoreCase(dl.getConnection().getContentType(), "json") || StringUtils.containsIgnoreCase(finalURL, "cold_request=1") || StringUtils.contains(br.getURL().toString(), "cold_request=1")) {
                        // defrosting, fetching from cold storage
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Defrosting download, please wait", 30 * 60 * 1000l);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
            } else {
                // hls
                dl = new HLSDownloader(downloadLink, br, finalURL);
            }
            dl.startDownload();
        }
    }

    public static final String VIMEOURLTYPE = "VIMEOURLTYPE";

    protected VIMEO_URL_TYPE getVimeoUrlType(final DownloadLink link) {
        final String urlType = link.getStringProperty(VIMEOURLTYPE, null);
        if (urlType != null) {
            try {
                return VIMEO_URL_TYPE.valueOf(urlType);
            } catch (Throwable ignore) {
                logger.log(ignore);
            }
        }
        return getUrlType(link.getPluginPatternMatcher());
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleFree(link);
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        synchronized (account) {
            final AccountInfo ai = new AccountInfo();
            if (!account.getUser().matches(".+@.+\\..+")) {
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte gib deine E-Mail Adresse ins Benutzername Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail address in the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            setBrowserExclusive();
            login(this, br, account);
            if (br.getRequest() == null || !StringUtils.containsIgnoreCase(br.getHost(), "vimeo.com")) {
                br.getPage(MAINPAGE);
            }
            br.getPage("/settings");
            String type = br.getRegex("acct_status\">.*?>(.*?)<").getMatch(0);
            if (type == null) {
                type = br.getRegex("user_type'\\s*,\\s*'(.*?)'").getMatch(0);
                if (type == null) {
                    type = br.getRegex("\"user_type\"\\s*:\\s*\"(.*?)\"").getMatch(0);
                }
            }
            if (type != null) {
                ai.setStatus(type);
            } else {
                ai.setStatus(null);
            }
            account.setValid(true);
            ai.setUnlimitedTraffic();
            return ai;
        }
    }

    public static boolean isLoggedIn(Browser br) {
        if (br.getCookie(MAINPAGE, "vuid", Cookies.NOTDELETEDPATTERN) == null) {
            return false;
        } else if (!"1".equals(br.getCookie(MAINPAGE, "is_logged_in", Cookies.NOTDELETEDPATTERN))) {
            return false;
        } else if (br.getCookie(MAINPAGE, "vimeo", Cookies.NOTDELETEDPATTERN) == null) {
            return false;
        } else {
            return true;
        }
    }

    public static void login(final Plugin plugin, Browser br, Account account) throws PluginException, IOException {
        synchronized (account) {
            try {
                br = prepBrGeneral(plugin, null, br);
                br.setFollowRedirects(true);
                Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(MAINPAGE, cookies);
                    if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 1 * 60 * 1000l) {
                        /* We trust these cookies --> Do not check them */
                        return;
                    }
                    br.getPage(MAINPAGE);
                    if (!isLoggedIn(br)) {
                        cookies = null;
                    }
                }
                if (cookies == null) {
                    br.getPage("https://www.vimeo.com/log_in");
                    final String xsrft = getXsrft(br);
                    // static post are bad idea, always use form.
                    final Form login = br.getFormbyProperty("id", "login_form");
                    if (login == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    login.put("token", Encoding.urlEncode(xsrft));
                    login.put("email", Encoding.urlEncode(account.getUser()));
                    login.put("password", Encoding.urlEncode(account.getPass()));
                    br.submitForm(login);
                    if (br.getHttpConnection().getResponseCode() == 406) {
                        throw new AccountUnavailableException("Account login temp. blocked", 15 * 60 * 1000l);
                    }
                    if (!isLoggedIn(br)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(MAINPAGE), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    public static final String getXsrft(Browser br) throws PluginException {
        String xsrft = br.getRegex("vimeo\\.xsrft\\s*=\\s*('|\"|)([a-z0-9\\.]{32,})\\1").getMatch(1);
        if (xsrft == null) {
            xsrft = br.getRegex("\"xsrft\"\\s*:\\s*\"([a-z0-9\\.]{32,})\"").getMatch(0);
            if (xsrft == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        return xsrft;
    }

    public static boolean isPasswordProtected(final Browser br) throws PluginException {
        // view variable: 4 scheint private mit passwort zu sein
        // view 2 scheint referer
        return br.containsHTML("\\d+/password") || isPasswordProtectedReview(br) || isPasswordProtectedAPIError(br);
    }

    public static boolean isPasswordProtectedAPIError(final Browser br) throws PluginException {
        return br.containsHTML("\"error_code\"\\s*:\\s*2223") && br.containsHTML("\"error\"\\s*:\\s*\"Whoops! Please enter a password");
    }

    public static boolean isPasswordProtectedReview(final Browser br) throws PluginException {
        final String isLocked = PluginJSonUtils.getJson(br, "isLocked");
        return StringUtils.equals(isLocked, "true");
    }

    @Override
    public void init() {
        Browser.setRequestIntervalLimitGlobal(this.getHost(), 250);
    }

    /** Handles password protected URLs - usually correct password will already be given via decrypter handling! */
    private void handlePW(final DownloadLink link, final Browser br) throws Exception {
        if (isPasswordProtectedReview(br)) {
            /* 2020-07-28: New and differs from the other password handling */
            final String initialURL = br.getURL();
            final String videoID = new Regex(initialURL, "/review/data/(\\d+)").getMatch(0);
            if (videoID == null) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* First get "xsrft" token ... */
            br.getPage("https://vimeo.com/_rv/viewer");
            final String vuid = PluginJSonUtils.getJson(br, "vuid");
            final Form pwform = new Form();
            pwform.setMethod(MethodType.POST);
            pwform.setAction("/" + videoID + "/password");
            final String xsrft = PluginJSonUtils.getJson(br, "xsrft");
            if (StringUtils.isEmpty(xsrft)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            pwform.put("token", Encoding.urlEncode(xsrft));
            pwform.put("is_review", "1");
            /* Get password */
            String passCode = link.getDownloadPassword();
            if (passCode == null) {
                passCode = getUserInput("Password?", link);
                if (passCode == null) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Password needed!");
                }
            }
            pwform.put("password", Encoding.urlEncode(passCode));
            /* Submit password */
            br.submitForm(pwform);
            if (isPasswordProtectedReview(br)) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Password needed!");
            }
            if (!StringUtils.isEmpty(vuid)) {
                br.setCookie(br.getURL(), "vuid", vuid);
            }
        } else {
            final String xsrft = getXsrft(br);
            final Form pwform = jd.plugins.decrypter.VimeoComDecrypter.getPasswordForm(br);
            if (pwform == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String passCode = link.getDownloadPassword();
            if (passCode == null) {
                passCode = getUserInput("Password?", link);
                if (passCode == null) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Password needed!");
                }
            }
            pwform.put("token", xsrft);
            pwform.put("password", Encoding.urlEncode(passCode));
            br.submitForm(pwform);
            if (isPasswordProtected(br)) {
                link.setDownloadPassword(null);
                throw new PluginException(LinkStatus.ERROR_FATAL, "Password needed!");
            }
            link.setDownloadPassword(passCode);
        }
    }

    public static String getJsonFromHTML(Plugin plugin, final Browser br) {
        String ret = br.getRegex("window\\.vimeo\\.clip_page_config\\s*=\\s*(\\{.*?\\});").getMatch(0);
        if (ret == null) {
            ret = br.getRegex("window\\.vimeo\\.vod_title_page_config\\s*=\\s*(\\{.*?\\});").getMatch(0);
            if (ret == null) {
                ret = br.getRegex("window\\s*=\\s*_extend\\(window, (\\{.*?\\})\\);").getMatch(0);
                if (ret == null) {
                    /* 'normal' player.vimeo.com */
                    ret = br.getRegex("var\\s*config\\s*=\\s*(\\{.*?\\});").getMatch(0);
                    if (ret == null) {
                        /* player.vimeo.com with /config */
                        ret = br.getRegex("^\\s*(\\{.*?\\})\\s*$").getMatch(0);
                    }
                }
            }
        }
        if (ret == null) {
            /* 2022-11-10: player.vimeo.com/video/... */
            ret = br.getRegex("window\\.playerConfig\\s*=\\s*(\\{.*?\\})\\s*var\\s*\\w+\\s*= ").getMatch(0);
            if (ret == null) {
                ret = br.getRegex("window\\.playerConfig\\s*=\\s*(\\{.*?\\}); ").getMatch(0);
            }
        }
        return ret;
    }

    public static Map<String, Object> apiResponseValidator(final Plugin plugin, final Browser br) {
        try {
            final String json = getJsonFromHTML(plugin, br);
            final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(json);
            final List<Map<String, Object>> files = (List<Map<String, Object>>) entries.get("files");
            final List<Map<String, Object>> downloads = (List<Map<String, Object>>) entries.get("download");
            if ((files != null && files.size() > 0) || (downloads != null && downloads.size() > 0)) {
                return entries;
            }
        } catch (final Throwable e) {
            plugin.getLogger().log(e);
        }
        return null;
    }

    @SuppressWarnings({ "unchecked", "unused" })
    public static List<VimeoContainer> find(final Plugin plugin, final VIMEO_URL_TYPE urlTypeUsed, Browser ibr, final String videoID, final String unlistedHash, final Map<String, Object> properties, final Boolean download, final Boolean stream, final Boolean hls, final Boolean subtitles) throws Exception {
        plugin.getLogger().info("videoID=" + videoID + "|urlTypeUsed=" + urlTypeUsed + "|unlistedHash=" + unlistedHash + "|download=" + download + "|stream=" + stream + "|hls=" + hls + "|subtitles=" + subtitles);
        /*
         * little pause needed so the next call does not return trash
         */
        Thread.sleep(1000);
        if (!ibr.getURL().contains("api.vimeo.com/") && !VIMEO_URL_TYPE.PLAYER_UNLISTED.equals(urlTypeUsed)) {
            final Browser brc = ibr.cloneBrowser();
            if (accessVimeoAPI(plugin, brc, properties, videoID, unlistedHash, null) != null) {
                ibr = brc;
            }
        }
        boolean debug = false;
        String configURL = ibr.getRegex("data-config-url=\"(https?://player\\.vimeo\\.com/(v2/)?video/\\d+/config.*?)\"").getMatch(0);
        if (StringUtils.isEmpty(configURL)) {
            /* can be within json on the given page now.. but this is easy to just request again raz20151215 */
            configURL = PluginJSonUtils.getJsonValue(ibr, "config_url");
            if (StringUtils.isEmpty(configURL)) {
                /* 2019-02-20 */
                configURL = PluginJSonUtils.getJsonValue(ibr, "configUrl");
            }
        }
        final ArrayList<VimeoContainer> results = new ArrayList<VimeoContainer>();
        /**
         * "download_config":[] --> Download possible, "download_config":null --> No download available. <br />
         * 2019-04-30: Problem: On player.vimeo.com, this property is not given so we either have to visit the main video page just to find
         * out about this information or simply try it (current attempt). <br />
         * No matter which attempt we chose: We need one request more!
         */
        boolean download_might_be_possible = false;
        if (ibr.getURL().contains("player.vimeo.com/")) {
            /*
             * 2019-04-30: We've already accessed the player-page which means we can only assume whether a download might be possible or not
             * based on the added linktype.
             */
            final boolean force_download_attempt = false;
            if (urlTypeUsed == null || urlTypeUsed == VIMEO_URL_TYPE.RAW || urlTypeUsed == VIMEO_URL_TYPE.NORMAL || force_download_attempt) {
                download_might_be_possible = true;
            } else {
                download_might_be_possible = false;
            }
        } else {
            /* As stated in the other case, if we access the main video page first, it should contain this information. */
            final String file_transfer_url = PluginJSonUtils.getJson(ibr, "file_transfer_url");
            download_might_be_possible = file_transfer_url != null;
            if (!download_might_be_possible) {
                try {
                    final String json = getJsonFromHTML(plugin, ibr);
                    final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(json);
                    /* Empty Array = download possible, null = download NOT possible! */
                    Object download_might_be_possibleO = entries != null ? entries.get("download_config") : null;
                    if (download_might_be_possibleO == null && entries != null) {
                        final List<Map<String, Object>> downloads = (List<Map<String, Object>>) entries.get("download");
                        download_might_be_possibleO = downloads != null && downloads.size() > 0;
                    }
                    plugin.getLogger().info("download_config:" + download_might_be_possibleO);
                    download_might_be_possible = download_might_be_possibleO != null;
                } catch (final Throwable e) {
                    plugin.getLogger().log(e);
                }
            } else {
                plugin.getLogger().info("file_transfer_url:" + file_transfer_url);
            }
        }
        plugin.getLogger().info("Download possible:" + download_might_be_possible);
        if (!Boolean.FALSE.equals(download) && download_might_be_possible) {
            plugin.getLogger().info("query downloads");
            final List<VimeoContainer> downloadsFound = handleDownloadConfig(plugin, ibr, videoID);
            plugin.getLogger().info("downloads found:" + downloadsFound.size() + "|" + downloadsFound);
            results.addAll(downloadsFound);
        }
        /** 2019-04-30: Only try to grab streams if we failed to find any downloads. */
        final boolean checkStreams = (results.size() < 2 && (!Boolean.FALSE.equals(stream) || !Boolean.FALSE.equals(hls))) || !Boolean.FALSE.equals(subtitles);
        /* player.vimeo.com links = Special case as the needed information is already in our current browser. */
        if ((checkStreams || Boolean.TRUE.equals(subtitles) || Boolean.TRUE.equals(stream) || Boolean.TRUE.equals(hls)) && (configURL != null || ibr.getURL().contains("player.vimeo.com/"))) {
            plugin.getLogger().info("try to find streams");
            String json = getJsonFromHTML(plugin, ibr);
            Map<String, Object> filesMap = null;
            final List<Object> official_downloads_all = new ArrayList<Object>();
            if (json != null) {
                final Map<String, Object> jsonMap = JavaScriptEngineFactory.jsonToJavaMap(json);
                final List<Map<String, Object>> filesList = (List<Map<String, Object>>) jsonMap.get("files");// api.vimeo.com
                final Map<String, Object> request = (Map<String, Object>) jsonMap.get("request");
                final Object filesObject = JavaScriptEngineFactory.walkJson(jsonMap, "request/files");
                if (filesList != null && filesList.size() > 0) {
                    filesMap = jsonMap;
                } else if (filesObject != null) {
                    filesMap = jsonMap;
                }
            }
            if (filesMap == null || !Boolean.FALSE.equals(subtitles)) {
                if (configURL != null) {
                    final Browser brc = ibr.cloneBrowser();
                    brc.getHeaders().put("Accept", "*/*");
                    configURL = configURL.replaceAll("&amp;", "&");
                    Thread.sleep(100);
                    json = brc.getPage(configURL);
                } else {
                    /* Fallback */
                    json = getJsonFromHTML(plugin, ibr);
                    if (json == null) {
                        json = ibr.getRegex("a\\s*=\\s*(\\s*\\{\\s*\"cdn_url\".*?);if\\(\\!?a\\.request\\)").getMatch(0);
                        if (json == null) {
                            json = ibr.getRegex("t\\s*=\\s*(\\s*\\{\\s*\"cdn_url\".*?);if\\(\\!?t\\.request\\)").getMatch(0);
                            if (json == null) {
                                json = ibr.getRegex("^(\\s*\\{\\s*\"cdn_url\".+)").getMatch(0);
                            }
                        }
                    }
                }
                if (json != null) {
                    filesMap = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
                }
            }
            if (filesMap != null) {
                if (!Boolean.FALSE.equals(stream)) {
                    plugin.getLogger().info("query progressive streams");
                    final List<VimeoContainer> progressiveStreams = handleProgessive(plugin, ibr, filesMap);
                    plugin.getLogger().info("progressive streams found:" + progressiveStreams.size() + "|" + progressiveStreams);
                    if (progressiveStreams.size() == 0) {
                        plugin.getLogger().info("Failed to find any progressive streams -> Video is most likely only available via DASH and/or split audio/video HLS");
                    } else {
                        results.addAll(progressiveStreams);
                    }
                }
                if (!Boolean.FALSE.equals(hls) && ALLOW_HLS) {
                    // skip HLS because of unsupported split video/audio
                    plugin.getLogger().info("query hls streams");
                    final List<VimeoContainer> hlsStreams = handleHLS(plugin, ibr.cloneBrowser(), filesMap);
                    plugin.getLogger().info("hls streams found:" + hlsStreams.size() + "|" + hlsStreams);
                    results.addAll(hlsStreams);
                }
                if (!Boolean.FALSE.equals(subtitles)) {
                    plugin.getLogger().info("query subtitles");
                    final List<VimeoContainer> subtitlesFound = handleSubtitles(plugin, ibr, filesMap);
                    plugin.getLogger().info("subtitles found:" + subtitlesFound.size() + "|" + subtitlesFound);
                    results.addAll(subtitlesFound);
                }
            } else {
                plugin.getLogger().info("no json found!?");
            }
        }
        return results;
    }

    private static List<VimeoContainer> handleSubtitles(Plugin plugin, Browser br, Map<String, Object> entries) {
        final ArrayList<VimeoContainer> ret = new ArrayList<VimeoContainer>();
        try {
            List<Map<String, Object>> text_tracks = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entries, "request/text_tracks");
            if (text_tracks != null) {
                for (final Map<String, Object> text_track : text_tracks) {
                    final VimeoContainer vvc = new VimeoContainer();
                    final String url = (String) text_track.get("url");
                    final String lang = (String) text_track.get("lang");
                    if (url == null || lang == null) {
                        continue;
                    }
                    vvc.setSource(Source.SUBTITLE);
                    vvc.setDownloadurl(URLHelper.parseLocation(new URL("https://vimeo.com"), url).toString());
                    vvc.setLang(lang);
                    vvc.setExtension(".srt");
                    ret.add(vvc);
                }
            }
        } catch (final Throwable t) {
            plugin.getLogger().log(t);
        }
        return ret;
    }

    private static Number getNumber(Map<String, Object> map, String key) {
        final Object value = map.get(key);
        if (value instanceof Number) {
            return (Number) value;
        } else if (value instanceof String && ((String) value).matches("^\\d+&")) {
            return Long.parseLong(value.toString());
        } else if (value instanceof String) {
            return SizeFormatter.getSize(value.toString());
        } else {
            return null;
        }
    }

    /** Crawls official downloadURLs if available */
    private static List<VimeoContainer> handleDownloadConfig(Plugin plugin, final Browser ibr, final String ID) throws InterruptedException {
        final List<VimeoContainer> ret = new ArrayList<VimeoContainer>();
        try {
            String json = getJsonFromHTML(plugin, ibr);
            Map<String, Object> entries = null;
            final List<Object> official_downloads_all = new ArrayList<Object>();
            if (json != null) {
                final Map<String, Object> jsonMap = JavaScriptEngineFactory.jsonToJavaMap(json);
                final List<Map<String, Object>> files = (List<Map<String, Object>>) jsonMap.get("files");
                final boolean hasDownload = jsonMap.containsKey("download");
                final List<Map<String, Object>> downloads = hasDownload ? (List<Map<String, Object>>) jsonMap.get("download") : null;
                if (downloads != null && downloads.size() > 0) {
                    entries = jsonMap;
                    official_downloads_all.addAll(downloads);
                } else if (files != null && files.size() > 0 || !hasDownload) {
                    // no downloads available
                    return ret;
                }
            }
            if (official_downloads_all.size() == 0) {
                Thread.sleep(500);
                final Browser brc = ibr.cloneBrowser();
                final GetRequest request = brc.createGetRequest("https://" + plugin.getHost() + "/" + ID + "?action=load_download_config");
                request.getHeaders().put("Accept", "*/*");
                request.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                request.getHeaders().put("Cache-Control", "no-cache");
                request.getHeaders().put("Pragma", "no-cache");
                request.getHeaders().put("Connection", "closed");
                json = brc.getPage(request);
                entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
                final List<Map<String, Object>> downloads = (List<Map<String, Object>>) entries.get("files");// old
                if (downloads != null && downloads.size() > 0) {
                    official_downloads_all.addAll(downloads);
                }
            }
            if (official_downloads_all.size() > 0) {
                final Object official_download_single_original = entries.get("source_file");
                if (official_download_single_original != null) {
                    official_downloads_all.add(official_download_single_original);
                }
                for (final Object file : official_downloads_all) {
                    final Map<String, Object> info = (Map<String, Object>) file;
                    final String quality = (String) info.get("quality");
                    if (StringUtils.equalsIgnoreCase("hls", quality)) {
                        // skip HLS because of unsupported split video/audio
                        plugin.getLogger().info("Ignore:" + JSonStorage.toString(file));
                        continue;
                    }
                    boolean is_source = info.containsKey("is_source") && ((Boolean) info.get("is_source")).booleanValue();// old
                    is_source |= info.containsKey("quality") && StringUtils.equals("source", quality);// api.vimeo.com
                    final VimeoContainer vvc = new VimeoContainer();
                    String downloadURL = (String) info.get("download_url");
                    if (downloadURL == null) {
                        downloadURL = (String) info.get("link");// api.vimeo.com
                    }
                    vvc.setDownloadurl(downloadURL);
                    String ext = (String) info.get("extension");
                    if (StringUtils.isNotEmpty(ext)) {
                        vvc.setExtension("." + ext);
                    } else {
                        vvc.updateExtensionFromUrl();
                        if (vvc.getExtension() == null) {
                            ext = plugin.getExtensionFromMimeType((String) info.get("type"));// api.vimeo.com
                            if (ext == null) {
                                ext = plugin.getExtensionFromMimeType((String) info.get("mime"));// config
                            }
                            if (ext != null) {
                                vvc.setExtension("." + ext);
                            }
                        }
                    }
                    vvc.setWidth(((Number) info.get("width")).intValue());
                    vvc.setHeight(((Number) info.get("height")).intValue());
                    final Number fileSize = getNumber(info, "size");
                    if (fileSize != null) {
                        vvc.setFilesize(fileSize.longValue());
                    } else {
                        final String size_short = (String) info.get("size_short");
                        if (size_short != null) {
                            vvc.setEstimatedSize(SizeFormatter.getSize(size_short));
                        }
                    }
                    final Number fps = getNumber(info, "fps");
                    if (fps != null) {
                        vvc.setFramerate(fps.intValue());
                    }
                    vvc.setSource(Source.DOWNLOAD);
                    if (is_source) {
                        vvc.setQuality(Quality.ORIGINAL);
                    } else {
                        final String public_name = (String) info.get("public_name");
                        if ("sd".equals(public_name)) {
                            vvc.setQuality(Quality.SD);
                        } else if ("hd".equals(public_name)) {
                            vvc.setQuality(Quality.HD);
                        } else {
                            // not provided... determine by x and y
                            vvc.updateQualityByHeight();
                        }
                    }
                    ret.add(vvc);
                }
            }
        } catch (final InterruptedException e) {
            plugin.getLogger().log(e);
            throw e;
        } catch (final Throwable t) {
            plugin.getLogger().log(t);
        }
        return ret;
    }

    @Override
    public boolean isSpeedLimited(DownloadLink link, Account account) {
        return false;
    }

    /** Handles http streams (stream download!) */
    private static List<VimeoContainer> handleProgessive(final Plugin plugin, final Browser br, final Map<String, Object> entries) {
        final ArrayList<VimeoContainer> ret = new ArrayList<VimeoContainer>();
        try {
            List<Map<String, Object>> items = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entries, "request/files/progressive");
            if (items == null || items.size() == 0) {
                items = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entries, "files");
            }
            if (items != null) {
                // atm they only have one object in array [] and then wrapped in {}
                for (final Map<String, Object> item : items) {
                    if (StringUtils.equalsIgnoreCase("hls", (String) item.get("quality"))) {
                        // api.vimeo.com, progressive only
                        plugin.getLogger().info("Ignore:" + JSonStorage.toString(item));
                        continue;
                    }
                    final Object profile = item.get("profile");
                    if (profile != null && (!String.valueOf(profile).matches("^\\d+$")) && !String.valueOf(profile).matches("^live-\\d+p$")) {
                        // can be String and Integer and live-XXXp
                        // vimeo-transcode-storage-prod-> may fail in >cdn@vimeo.com does not have storage.objects.get access to the Google
                        // Cloud Storage object.
                        plugin.getLogger().info("Ignore:" + JSonStorage.toString(item));
                        continue;
                    }
                    final VimeoContainer vvc = new VimeoContainer();
                    String downloadURL = (String) item.get("url");
                    if (downloadURL == null) {
                        downloadURL = (String) item.get("link");// api.vimeo.com
                    }
                    vvc.setDownloadurl(downloadURL);
                    vvc.updateExtensionFromUrl();
                    if (vvc.getExtension() == null) {
                        String ext = plugin.getExtensionFromMimeType((String) item.get("type"));// api.vimeo.com
                        if (ext == null) {
                            ext = plugin.getExtensionFromMimeType((String) item.get("mime"));// config
                        }
                        if (ext != null) {
                            vvc.setExtension("." + ext);
                        }
                    }
                    vvc.setHeight(((Number) item.get("height")).intValue());
                    vvc.setWidth(((Number) item.get("width")).intValue());
                    final Object o_bitrate = item.get("bitrate");
                    if (o_bitrate != null) {
                        /* Bitrate is 'null' for vp6 codec */
                        vvc.setBitrate(((Number) o_bitrate).intValue());
                    }
                    final Number fileSize = getNumber(item, "size");
                    if (fileSize != null) {
                        vvc.setFilesize(fileSize.longValue());
                    } else {
                        final String size_short = (String) item.get("size_short");
                        if (size_short != null) {
                            vvc.setEstimatedSize(SizeFormatter.getSize(size_short));
                        }
                    }
                    final Number fps = getNumber(item, "fps");
                    if (fps != null) {
                        vvc.setFramerate(fps.intValue());
                    }
                    String rawQuality = (String) item.get("public_name");// api.vimeo.com
                    if (rawQuality == null) {
                        rawQuality = (String) item.get("quality");// config
                    }
                    vvc.setRawQuality(rawQuality);
                    if (StringUtils.contains(rawQuality, "1080")) {
                        vvc.setQuality(Quality.FHD);
                    } else if (StringUtils.contains(rawQuality, "720")) {
                        vvc.setQuality(Quality.HD);
                    } else if (StringUtils.contains(rawQuality, "1440")) {
                        vvc.setQuality(Quality.UHD);
                    } else if (StringUtils.contains(rawQuality, "2560")) {
                        vvc.setQuality(Quality.UHD_4K);
                    } else {
                        vvc.updateQualityByHeight();
                    }
                    vvc.setCodec(".mp4".equalsIgnoreCase(vvc.getExtension()) ? "h264" : "vp5");
                    vvc.setSource(Source.WEB);
                    ret.add(vvc);
                }
            }
        } catch (final Throwable t) {
            plugin.getLogger().log(t);
        }
        return ret;
    }

    private static List<VimeoContainer> handleHLS(Plugin plugin, final Browser br, final Map<String, Object> entries) {
        final ArrayList<VimeoContainer> ret = new ArrayList<VimeoContainer>();
        try {
            Map<String, Object> base = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "request/files/hls");
            if (base == null) {
                // TODO: add support for api.vimeo.com
            }
            if (base != null) {
                final String defaultCDN = (String) base.get("default_cdn");
                final String m3u8 = (String) JavaScriptEngineFactory.walkJson(base, defaultCDN != null ? "cdns/" + defaultCDN + "/url" : "cdns/{0}/url");
                final List<HlsContainer> qualities = HlsContainer.getHlsQualities(br, m3u8);
                long duration = -1;
                for (final HlsContainer quality : qualities) {
                    if (duration == -1) {
                        duration = 0;
                        final List<M3U8Playlist> m3u8s = quality.getM3U8(br.cloneBrowser());
                        duration = M3U8Playlist.getEstimatedDuration(m3u8s);
                    }
                    final VimeoContainer container = VimeoContainer.createVimeoVideoContainer(quality);
                    final int bandwidth;
                    if (quality.getAverageBandwidth() > 0) {
                        bandwidth = quality.getAverageBandwidth();
                    } else {
                        bandwidth = quality.getBandwidth();
                    }
                    if (duration > 0 && bandwidth > 0) {
                        final long estimatedSize = bandwidth / 8 * (duration / 1000);
                        container.setEstimatedSize(estimatedSize);
                    }
                    ret.add(container);
                }
            }
        } catch (final Throwable t) {
            plugin.getLogger().log(t);
        }
        return ret;
    }

    public static VimeoContainer getVimeoVideoContainer(final DownloadLink downloadLink, final boolean allowNull) throws Exception {
        synchronized (downloadLink) {
            final Object value = downloadLink.getProperty(VVC, null);
            if (value instanceof VimeoContainer) {
                return (VimeoContainer) value;
            } else if (value instanceof String) {
                final VimeoContainer ret = JSonStorage.restoreFromString(value.toString(), VimeoContainer.TYPE_REF);
                downloadLink.setProperty(VVC, ret);
                return ret;
            } else if (value instanceof Map) {
                final VimeoContainer ret = JSonStorage.restoreFromString(JSonStorage.toString(value), VimeoContainer.TYPE_REF);
                downloadLink.setProperty(VVC, ret);
                return ret;
            } else {
                if (allowNull) {
                    return null;
                } else {
                    if (value != null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, null, -1, new Exception(value.getClass().getSimpleName()));
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    public static String getFormattedFilename(final DownloadLink link) throws Exception {
        final VimeoContainer vvc = getVimeoVideoContainer(link, true);
        String videoTitle = link.getStringProperty("videoTitle", null);
        final SubConfiguration cfg = SubConfiguration.getConfig("vimeo.com");
        String formattedFilename = cfg.getStringProperty(CUSTOM_FILENAME, defaultCustomFilename);
        if (formattedFilename == null || formattedFilename.equals("")) {
            formattedFilename = defaultCustomFilename;
        }
        final String date = link.getStringProperty("originalDate", null);
        final String channelName = link.getStringProperty("channel", null);
        final String videoID = link.getStringProperty("videoID", null);
        final String videoQuality;
        final String videoFrameSize;
        final String videoBitrate;
        final String videoType;
        final String videoExt;
        if (vvc != null) {
            if (VimeoContainer.Source.SUBTITLE.equals(vvc.getSource())) {
                videoQuality = null;
                videoFrameSize = "";
                videoBitrate = "";
                videoType = vvc.getLang();
            } else {
                videoQuality = vvc.getQuality().toString();
                videoFrameSize = vvc.getWidth() + "x" + vvc.getHeight();
                videoBitrate = vvc.getBitrate() == -1 ? "" : String.valueOf(vvc.getBitrate());
                videoType = String.valueOf(vvc.getSource());
            }
            videoExt = vvc.getExtension();
        } else {
            videoQuality = link.getStringProperty("videoQuality", null);
            videoFrameSize = link.getStringProperty("videoFrameSize", "");
            videoBitrate = link.getStringProperty("videoBitrate", "");
            videoType = link.getStringProperty("videoType", null);
            videoExt = link.getStringProperty("videoExt", null);
        }
        String formattedDate = null;
        if (!StringUtils.isEmpty(date)) {
            final String userDefinedDateFormat = cfg.getStringProperty(CUSTOM_DATE, defaultCustomDate);
            if (userDefinedDateFormat != null) {
                try {
                    SimpleDateFormat formatter = getFormatterForDate(date);
                    final Date dateStr;
                    dateStr = formatter.parse(date);
                    formattedDate = formatter.format(dateStr);
                    Date theDate = formatter.parse(formattedDate);
                    formatter = new SimpleDateFormat(userDefinedDateFormat);
                    formattedDate = formatter.format(theDate);
                } catch (Exception e) {
                    // prevent user error killing plugin, use input-data as fallback.
                    formattedDate = date;
                }
            }
        }
        if (formattedDate != null) {
            formattedFilename = formattedFilename.replace("*date*", formattedDate);
        } else {
            formattedFilename = formattedFilename.replace("*date*", "");
        }
        if (formattedFilename.contains("*videoid*")) {
            formattedFilename = formattedFilename.replace("*videoid*", videoID);
        }
        if (formattedFilename.contains("*channelname*")) {
            if (channelName != null) {
                formattedFilename = formattedFilename.replace("*channelname*", channelName);
            } else {
                formattedFilename = formattedFilename.replace("*channelname*", "");
            }
        }
        // quality
        if (videoType != null) {
            formattedFilename = formattedFilename.replace("*type*", videoType);
        } else {
            formattedFilename = formattedFilename.replace("*type*", "");
        }
        // quality
        if (videoQuality != null) {
            formattedFilename = formattedFilename.replace("*quality*", videoQuality);
        } else {
            formattedFilename = formattedFilename.replace("*quality*", "");
        }
        // file extension
        if (videoExt != null) {
            formattedFilename = formattedFilename.replace("*ext*", videoExt);
        } else {
            formattedFilename = formattedFilename.replace("*ext*", ".mp4");
        }
        // Insert filename at the end to prevent errors with tags
        if (videoTitle != null) {
            formattedFilename = formattedFilename.replace("*videoname*", videoTitle);
        }
        // size
        formattedFilename = formattedFilename.replace("*videoFrameSize*", videoFrameSize);
        // bitrate
        formattedFilename = formattedFilename.replace("*videoBitrate*", videoBitrate);
        return formattedFilename;
    }

    public static SimpleDateFormat getFormatterForDate(final String dateSrc) {
        final SimpleDateFormat formatter;
        if (dateSrc.matches("\\d{4}\\-\\d{2}\\-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\+|-)\\d{2}:\\d{2}") && JVMVersion.isMinimum(JVMVersion.JAVA_1_7)) {
            // Java 1.6 does not support X ISO 8601 time zone
            formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        } else if (dateSrc.matches("\\d{4}\\-\\d{2}\\-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*")) {
            formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        } else if (dateSrc.matches("\\d{4}\\-\\d{2}\\-\\d{2}:\\d{2}:\\d{2}:\\d{2}")) {
            formatter = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss");
        } else {
            formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }
        return formatter;
    }

    public static String getForcedReferer(final DownloadLink dl) {
        return dl.getStringProperty("vimeo_forced_referer", null);
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        if (link != null) {
            link.removeProperty("directURL");
        }
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public String getDescription() {
        return "JDownloader's Vimeo Plugin helps downloading videoclips from vimeo.com. Vimeo provides different video qualities.";
    }

    public static final String  defaultCustomPackagenameSingleVideo = "*channelname* - *date* - *videoname*";
    private final static String defaultCustomFilename               = "*videoname*_*quality*_*type**ext*";
    public static final String  defaultCustomDate                   = "dd.MM.yyyy";

    private void setConfigElements() {
        final ConfigEntry loadbest = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_BEST, JDL.L("plugins.hoster.vimeo.best", "Returns a single <b>best</b> result per video url based on selection below.")).setDefaultValue(false);
        getConfig().addEntry(loadbest);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_ORIGINAL, "Load Original Version").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_HD, JDL.L("plugins.hoster.vimeo.loadhd", "Load HD Version")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_SD, JDL.L("plugins.hoster.vimeo.loadsd", "Load SD Version")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_MOBILE, JDL.L("plugins.hoster.vimeo.loadmobile", "Load Mobile Version")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SUBTITLE, JDL.L("plugins.hoster.vimeo.subtitle", "Load Subtitle")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), P_240, JDL.L("plugins.hoster.vimeo.240p", "240p")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), P_360, JDL.L("plugins.hoster.vimeo.360p", "360p")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), P_480, JDL.L("plugins.hoster.vimeo.480p", "480p")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), P_540, JDL.L("plugins.hoster.vimeo.540p", "540p")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), P_720, JDL.L("plugins.hoster.vimeo.720p", "720p")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), P_1080, JDL.L("plugins.hoster.vimeo.108p", "1080p")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), P_1440, JDL.L("plugins.hoster.vimeo.1440p", "1440p")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), P_2560, JDL.L("plugins.hoster.vimeo.2560p", "2560p")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ASK_REF, JDL.L("plugins.hoster.vimeo.askref", "Ask for referer")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_DATE, "Define date for custom filenames and package names:").setDefaultValue(defaultCustomDate));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize package name for single videos:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_PACKAGENAME_SINGLE_VIDEO, "Define package name:").setDefaultValue(defaultCustomPackagenameSingleVideo));
        final StringBuilder sb = new StringBuilder();
        sb.append("Explanation of the available tags:\r\n");
        sb.append("*channelname* = name of the channel/uploader\r\n");
        sb.append("*date* = date when the video was posted - appears in the user-defined format above\r\n");
        sb.append("*videoname* = name of the video without extension\r\n");
        sb.append("*videoid* = id of the video\r\n");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sb.toString()));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Customize filename:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getPluginConfig(), CUSTOM_FILENAME, "Define filename:").setDefaultValue(defaultCustomFilename));
        sb.append("*videoFrameSize* = size of video eg. 640x480 (not always available)\r\n");
        sb.append("*videoBitrate* = bitrate of video eg. xxxkbits (not always available)\r\n");
        sb.append("*type* = STREAM or DOWNLOAD\r\n");
        sb.append("*ext* = the extension of the file, in this case usually '.mp4'");
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, sb.toString()));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALWAYS_LOGIN, "Always login with account?").setDefaultValue(false));
    }
}