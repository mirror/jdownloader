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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.appwork.storage.TypeRef;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.ffmpeg.json.StreamInfo;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.downloader.hls.M3U8Playlist;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.config.TwitterConfigInterface;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.decrypter.TwitterComCrawler;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class TwitterCom extends PluginForHost {
    public TwitterCom(PluginWrapper wrapper) {
        super(wrapper);
        /* 2020-01-20: Disabled login functionality as it is broken */
        this.enablePremium("https://twitter.com/signup");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        if (allowCookieLoginOnly) {
            return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.COOKIE_LOGIN_ONLY };
        } else {
            return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.COOKIE_LOGIN_OPTIONAL };
        }
    }

    public static List<String[]> getPluginDomains() {
        return TwitterComCrawler.getPluginDomains();
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            final StringBuilder regex = new StringBuilder();
            regex.append("https?://[a-z0-9]+\\.twimg\\.com/media/[^/]+");
            regex.append("|https?://amp\\.twimg\\.com/prod/[^<>\"]*?/vmap/[^<>\"]*?\\.vmap");
            regex.append("|https?://video\\.twimg\\.com/amplify_video/vmap/\\d+\\.vmap");
            regex.append("|https?://amp\\.twimg\\.com/v/.+");
            regex.append("|https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:i/videos/tweet/\\d+|[^/]+/status/\\d+(/video/\\d+)?)");
            ret.add(regex.toString());
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://twitter.com/tos";
    }

    @Override
    public boolean allowHandle(final DownloadLink link, final PluginForHost plugin) {
        /* Do not allow multihoster usage! */
        return link.getHost().equalsIgnoreCase(plugin.getHost());
    }

    public static final Pattern           TYPE_DIRECT                                            = Pattern.compile("(?i)https?://[a-z0-9]+\\.twimg\\.com/.+");
    public static final String            TYPE_VIDEO_DIRECT                                      = "(?i)https?://amp\\.twimg\\.com/v/.+";
    public static final String            TYPE_VIDEO_VMAP                                        = "(?i)^https?://.*\\.vmap$";
    public static final Pattern           TYPE_VIDEO_EMBED                                       = Pattern.compile("(?i)https?://[^/]+/i/videos/tweet/(\\d+)");
    public static final String            TYPE_VIDEO_SPECIFIC                                    = "(?i)https://[^/]+/([^/]+)/status/(\\d+)/video/(\\d+)";
    private static final String           TYPE_TWEET_TEXT                                        = "(?i)https?://[^/]+/([^/]+)/status/(\\d+)";
    /* Connection stuff - don't allow chunks as we only download small pictures/videos */
    private final int                     MAXCHUNKS                                              = 1;
    private String                        dllink                                                 = null;
    public static final String            COOKIE_KEY_LOGINED_CSRFTOKEN                           = "ct0";
    public static final String            PROPERTY_DIRECTURL                                     = "directlink";
    public static final String            PROPERTY_DIRECTURL_hls_master                          = "directlink_hls_master";
    private static final String           PROPERTY_DOWNLOADLINK_TIMESTAMP_MP4_PROGRESSIVE_FAILED = "timestamp_mp4_progressive_failed";
    private static final String           PROPERTY_DOWNLOADLINK_TIMESTAMP_MP4_HLS_FAILED         = "timestamp_mp4_hls_failed";
    private static final String           PROPERTY_DOWNLOADLINK_TIMESTAMP_IMAGE_ORIG_FAILED      = "timestamp_image_orig_failed";
    private final AtomicReference<String> lastUsedVideoDirecturlproperty                         = new AtomicReference<String>();
    /* 2020-07-02: Only cookie login is supported! */
    private static final boolean          allowCookieLoginOnly                                   = true;
    private final String                  IMAGE_ORIG_SUFFIX                                      = ":orig";
    private static Object                 LOCK                                                   = new Object();
    /**
     * 2024-22-29: Disabled this as mostly progressive download will fail which would result in fallback to HLS download but Twitter HLS
     * streams do not have sound so we want to avoid downloading them for now.
     */
    private final boolean                 allowVideoStreamingTypeFallback                        = false;

    public static Browser prepBR(final Browser br) {
        br.setAllowedResponseCodes(new int[] { 429 });
        return br;
    }

    @Override
    public void init() {
        super.init();
        TwitterComCrawler.setRequestIntervallLimits();
    }

    @Override
    public String getPluginContentURL(final DownloadLink link) {
        /* Expose direct-URLs to user as they are static. */
        final String directurl = link.getStringProperty(PROPERTY_DIRECTURL);
        final String hls_master = link.getStringProperty(PROPERTY_DIRECTURL_hls_master);
        if (PluginJsonConfig.get(TwitterConfigInterface.class).isPreferHLSVideoDownload() && hls_master != null) {
            return hls_master;
        } else if (directurl != null) {
            return directurl;
        } else {
            return super.getPluginContentURL(link);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null, false);
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        if (link != null && PluginJsonConfig.get(TwitterConfigInterface.class).isPreferHLSVideoDownload() && link.hasProperty(PROPERTY_DIRECTURL_hls_master)) {
            return false;
        } else {
            return true;
        }
    }

    public static boolean isText(final DownloadLink link) {
        if (link.getPluginPatternMatcher().matches(TYPE_TWEET_TEXT)) {
            return true;
        } else if (StringUtils.equals(link.getStringProperty(TwitterComCrawler.PROPERTY_TYPE), TwitterComCrawler.TYPE_TEXT)) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isVideo(final DownloadLink link) {
        final String url = link.getPluginPatternMatcher();
        if (new Regex(url, TYPE_VIDEO_EMBED).patternFind() || url.matches(TYPE_VIDEO_SPECIFIC) || url.matches(TYPE_VIDEO_VMAP) || url.matches(TYPE_VIDEO_DIRECT)) {
            return true;
        } else if (StringUtils.equals(link.getStringProperty(TwitterComCrawler.PROPERTY_TYPE), TwitterComCrawler.TYPE_VIDEO)) {
            return true;
        } else {
            return false;
        }
    }

    private String getTweetID(final DownloadLink link) {
        final String tweetIDFromProperty = link.getStringProperty(TwitterComCrawler.PROPERTY_TWEET_ID);
        if (tweetIDFromProperty != null) {
            return tweetIDFromProperty;
        } else {
            /* Backward compatibility */
            return getTweetIDFromURL(link.getPluginPatternMatcher());
        }
    }

    private String getTweetIDFromURL(final String url) {
        if (new Regex(url, TYPE_VIDEO_EMBED).patternFind()) {
            return new Regex(url, TYPE_VIDEO_EMBED).getMatch(0);
        } else if (url.matches(TYPE_VIDEO_SPECIFIC)) {
            return new Regex(url, TYPE_VIDEO_SPECIFIC).getMatch(1);
        } else if (url.matches(TYPE_TWEET_TEXT)) {
            return new Regex(url, TYPE_TWEET_TEXT).getMatch(0);
        } else {
            return null;
        }
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        String filenameFromHeader = null;
        if (isText(link)) {
            if (StringUtils.isEmpty(getTweetText(link))) {
                /* This should never happen! */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } else {
            prepBR(this.br);
            /* Most items will come from crawler. */
            final String tweetID = getTweetID(link);
            String vmap_url = null;
            boolean possibly_geo_blocked = false;
            if (link.getPluginPatternMatcher().matches(TYPE_VIDEO_DIRECT) || link.getPluginPatternMatcher().matches(TYPE_VIDEO_VMAP)) {
                /* 2022-02-02: Old handling, rarely used e.g. for: https://video.twimg.com/amplify_video/vmap/<videoID>.vmap */
                br.getPage(link.getPluginPatternMatcher());
                if (br.getHttpConnection().getResponseCode() == 403) {
                    throw new AccountRequiredException();
                } else if (br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (link.getPluginPatternMatcher().matches(TYPE_VIDEO_VMAP)) {
                    /* Direct vmap url was added by user- or decrypter. */
                    vmap_url = link.getPluginPatternMatcher();
                } else {
                    /* Videolink was added by user or decrypter. */
                    vmap_url = br.getRegex("name=\"twitter:amplify:vmap\" content=\"(https?://[^<>\"]*?\\.vmap)\"").getMatch(0);
                }
                if (StringUtils.isEmpty(vmap_url)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                br.getPage(vmap_url);
                this.dllink = regexVideoVmapHighestQualityURL(br);
                if (StringUtils.isEmpty(dllink)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } else if (isVideo(link)) {
                this.dllink = getStoredVideoDirecturl(link);
                if (StringUtils.isEmpty(this.dllink)) {
                    logger.info("Trying to obtain fresh video directurl");
                    final boolean useCrawler;
                    /*
                     * 2022-02-02: Legacy handling: TODO: Hardcode set 'useCrawler' to false after 04-2022 to fix rare issue with single
                     * embedded video URLs. Don't do this earlier as it will kill filenames of existing videos!
                     */
                    if (link.hasProperty(TwitterComCrawler.PROPERTY_VIDEO_DIRECT_URLS_ARE_AVAILABLE_VIA_API_EXTENDED_ENTITY)) {
                        if (link.getBooleanProperty(TwitterComCrawler.PROPERTY_VIDEO_DIRECT_URLS_ARE_AVAILABLE_VIA_API_EXTENDED_ENTITY, false)) {
                            useCrawler = true;
                        } else {
                            useCrawler = false;
                        }
                    } else if (!link.hasProperty(TwitterComCrawler.PROPERTY_BITRATE)) {
                        /* Link was added to host plugin directly -> Don't use crawler handling. */
                        useCrawler = false;
                    } else {
                        useCrawler = true;
                    }
                    if (useCrawler) {
                        logger.info("Obtaining new directurl via crawler");
                        final PluginForDecrypt crawlerplugin = this.getNewPluginForDecryptInstance(this.getHost());
                        final String tweetVideoURL = TwitterComCrawler.createVideourl(tweetID);
                        final CryptedLink param = new CryptedLink(tweetVideoURL, link);
                        final ArrayList<DownloadLink> results = crawlerplugin.decryptIt(param, null);
                        if (results.isEmpty()) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Single tweet video item crawler failure");
                        }
                        DownloadLink result = null;
                        if (results.size() == 1) {
                            /* Simple handling: Assume what we want is the first item */
                            result = results.get(0);
                        } else {
                            /* Tweet contains multiple media items */
                            for (final DownloadLink tmp : results) {
                                if (StringUtils.equalsIgnoreCase(tmp.getStringProperty(TwitterComCrawler.PROPERTY_MEDIA_ID), link.getStringProperty(TwitterComCrawler.PROPERTY_MEDIA_ID))) {
                                    result = tmp;
                                    break;
                                }
                            }
                        }
                        if (result == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Failed to refresh directurl");
                        }
                        this.dllink = getStoredVideoDirecturl(result);
                        if (StringUtils.isEmpty(dllink)) {
                            /* This should never happen */
                            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Failed to refresh directurl");
                        }
                        /* Take over properties from crawler result */
                        link.setProperties(result.getProperties());
                    } else {
                        /* 2018-11-13: Using static token */
                        final boolean use_static_token = true;
                        final String authorization_token;
                        if (use_static_token) {
                            authorization_token = "AAAAAAAAAAAAAAAAAAAAAIK1zgAAAAAA2tUWuhGZ2JceoId5GwYWU5GspY4%3DUq7gzFoCZs1QfwGoVdvSac3IniczZEYXIcDyumCauIXpcAPorE";
                        } else {
                            br.getPage(link.getPluginPatternMatcher());
                            if (this.br.getHttpConnection().getResponseCode() == 403) {
                                throw new AccountRequiredException();
                            } else if (this.br.getHttpConnection().getResponseCode() == 404) {
                                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                            }
                            final String jsURL = br.getRegex("<script src=\"(https?://[^\"]+/TwitterVideoPlayerIframe[^\"]+\\.js)\">").getMatch(0);
                            if (jsURL == null) {
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            }
                            br.getPage(jsURL);
                            authorization_token = br.getRegex("Authorization:\"Bearer ([^\"]+)\"").getMatch(0);
                            if (authorization_token == null) {
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            }
                        }
                        br.getHeaders().put("Authorization", "Bearer " + authorization_token);
                        br.getHeaders().put("Accept", "*/*");
                        br.getHeaders().put("Origin", "https://twitter.com");
                        br.getHeaders().put("Referer", "https://" + this.getHost() + "/i/videos/tweet/" + tweetID);
                        final Browser brc = br.cloneBrowser();
                        brc.setAllowedResponseCodes(400);
                        synchronized (LOCK) {
                            TwitterComCrawler.prepAPIHeaders(brc);
                            /* Set guest_token header if needed. */
                            if (account == null) {
                                final String guest_token = TwitterComCrawler.getAndSetGuestToken(this, brc);
                                if (guest_token != null) {
                                    brc.getHeaders().put("x-guest-token", guest_token);
                                } else {
                                    logger.warning("Failed to get guesttoken");
                                }
                            }
                            /*
                             * Without guest_token in header we might often get blocked here with this response: HTTP/1.1 429 Too Many
                             * Requests --> {"errors":[{"message":"Rate limit exceeded","code":88}]}
                             */
                            brc.getPage("https://api.twitter.com/1.1/videos/tweet/config/" + tweetID + ".json");
                            if (brc.getHttpConnection().getResponseCode() == 400) {
                                /* Invalid videoID format --> Offline */
                                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                            } else if (brc.getHttpConnection().getResponseCode() == 404) {
                                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                            } else if (brc.getHttpConnection().getResponseCode() == 403) {
                                /* 403 is typically 'rights missing' but in this case it means that the content is offline. */
                                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                            }
                            checkGenericErrors(br);
                            /* Now we got some old errorhandling */
                            final String errorcode = PluginJSonUtils.getJson(brc, "error");
                            final String errormessage = PluginJSonUtils.getJson(brc, "message");
                            if (!StringUtils.isEmpty(errorcode)) {
                                logger.info("Failure, errorcode: " + errorcode);
                                if (!StringUtils.isEmpty(errormessage)) {
                                    logger.info("Errormessage: " + errormessage);
                                }
                                if (errorcode.equals("239")) {
                                    /*
                                     * 2019-08-20: {"errors":[{"code":239,"message":"Bad guest token."}]}
                                     */
                                    logger.info("Possible token failure 239, retrying");
                                    TwitterComCrawler.resetGuestToken();
                                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 353", 3 * 60 * 1000l);
                                } else if (errorcode.equals("353")) {
                                    logger.info("Possible token failure 353, retrying");
                                    TwitterComCrawler.resetGuestToken();
                                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 353", 2 * 60 * 1000l);
                                } else {
                                    logger.warning("Unknown error");
                                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown API error " + errorcode);
                                }
                            }
                        }
                        final Map<String, Object> root = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
                        final Map<String, Object> track = (Map<String, Object>) root.get("track");
                        if ((Boolean) track.get("isEventGeoblocked") == Boolean.TRUE) {
                            possibly_geo_blocked = true;
                        }
                        dllink = (String) track.get("playbackUrl");
                        if (StringUtils.isEmpty(dllink)) {
                            vmap_url = (String) track.get("vmapUrl");
                            if (StringUtils.isEmpty(vmap_url)) {
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            }
                            br.getPage(vmap_url);
                            this.dllink = regexVideoVmapHighestQualityURL(this.br);
                        }
                    }
                }
            } else {
                /* Image */
                // TYPE_DIRECT - jpg/png/mp4
                if (StringUtils.containsIgnoreCase(link.getPluginPatternMatcher(), "jpg") || StringUtils.containsIgnoreCase(link.getPluginPatternMatcher(), "png")) {
                    if (looksLikeOriginalImageIsUnavailable(link)) {
                        /* Make sure not to have the IMAGE_ORIG_SUFFIX in link to avoid http response 404 */
                        dllink = link.getPluginPatternMatcher().replace(IMAGE_ORIG_SUFFIX, "");
                    } else if (link.getPluginPatternMatcher().contains(":large")) {
                        dllink = link.getPluginPatternMatcher().replaceFirst(":large", IMAGE_ORIG_SUFFIX);
                    } else if (link.getPluginPatternMatcher().matches("(?i).+\\.(jpg|jpeg|png)$")) {
                        /* Append this to get the highest quality possible */
                        dllink = link.getPluginPatternMatcher() + IMAGE_ORIG_SUFFIX;
                    } else {
                        dllink = link.getPluginPatternMatcher();
                    }
                } else {
                    dllink = link.getPluginPatternMatcher();
                }
            }
            if (!StringUtils.isEmpty(dllink)) {
                if (dllink.contains(".m3u8")) {
                    checkFFProbe(link, "Download a HLS Stream");
                    br.setAllowedResponseCodes(new int[] { 403 });
                    br.getPage(dllink);
                    if (br.getHttpConnection().getResponseCode() == 403) {
                        /* 2017-06-01: Unsure because browser shows the thumbnail and video 'wants to play' but doesn't. */
                        // throw new PluginException(LinkStatus.ERROR_FATAL, "GEO-blocked or offline content");
                        if (possibly_geo_blocked) {
                            /* We already had the info before that this content is probably GEO-blocked - now we know it for sure! */
                            throw new PluginException(LinkStatus.ERROR_FATAL, "GEO-blocked");
                        } else {
                            /* Account with adequate permissions required to access this content. */
                            throw new AccountRequiredException();
                        }
                    } else if (br.getHttpConnection().getResponseCode() == 404) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    final HlsContainer hlsBest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
                    this.dllink = hlsBest.getDownloadurl();
                    if (!isDownload) {
                        /* Find estimated filesize */
                        final HLSDownloader downloader = new HLSDownloader(link, br, dllink);
                        final StreamInfo streamInfo = downloader.getProbe();
                        if (streamInfo == null) {
                            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "HLS failure");
                        }
                        final int hlsBandwidth = hlsBest.getBandwidth();
                        if (hlsBandwidth > 0) {
                            for (M3U8Playlist playList : downloader.getPlayLists()) {
                                playList.setAverageBandwidth(hlsBandwidth);
                            }
                        }
                        final long estimatedSize = downloader.getEstimatedSize();
                        link.setDownloadSize(estimatedSize);
                    }
                } else if (!isDownload || link.getFinalFileName() == null) {
                    URLConnectionAdapter con = null;
                    try {
                        final Browser brc = br.cloneBrowser();
                        con = brc.openHeadConnection(dllink);
                        handleConnectionErrors(link, brc, con);
                        if (con.getCompleteContentLength() > 0) {
                            if (con.isContentDecoded()) {
                                link.setDownloadSize(con.getCompleteContentLength());
                            } else {
                                link.setVerifiedFileSize(con.getCompleteContentLength());
                            }
                        }
                        filenameFromHeader = getFileNameFromHeader(con);
                        if (filenameFromHeader != null) {
                            /* Do some corrections */
                            filenameFromHeader = Encoding.htmlDecode(filenameFromHeader).replace(":orig", "").trim();
                            if (tweetID != null && !filenameFromHeader.contains(tweetID)) {
                                filenameFromHeader = tweetID + "_" + filenameFromHeader;
                            }
                            final String ext = getExtensionFromMimeType(con.getContentType());
                            if (ext != null) {
                                filenameFromHeader = applyFilenameExtension(filenameFromHeader, "." + ext);
                            }
                        }
                    } finally {
                        if (con != null) {
                            con.disconnect();
                        }
                    }
                }
            }
        }
        final String tweetID = link.getStringProperty(TwitterComCrawler.PROPERTY_TWEET_ID);
        if (tweetID != null) {
            /* Item from crawler which shall contain all information needed to set custom filenames. */
            TwitterComCrawler.setFormattedFilename(link);
        } else if (filenameFromHeader != null) {
            link.setFinalFileName(filenameFromHeader);
        }
        return AvailableStatus.TRUE;
    }

    private boolean looksLikeOriginalImageIsUnavailable(final DownloadLink link) {
        if (System.currentTimeMillis() - link.getLongProperty(PROPERTY_DOWNLOADLINK_TIMESTAMP_IMAGE_ORIG_FAILED, 0) < 5 * 60 * 1000) {
            return true;
        } else {
            return false;
        }
    }

    private void checkGenericErrors(final Browser br) throws PluginException {
        if (br.getHttpConnection().getResponseCode() == 429) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Rate-limit reached", 5 * 60 * 1000l);
        }
    }

    private boolean looksLikeAllVideoStreamTypesAreBrokenOrOffline(final DownloadLink link) {
        if (looksLikeBrokenVideoStream(link, PROPERTY_DOWNLOADLINK_TIMESTAMP_MP4_PROGRESSIVE_FAILED) && looksLikeBrokenVideoStream(link, PROPERTY_DOWNLOADLINK_TIMESTAMP_MP4_HLS_FAILED)) {
            return true;
        } else {
            return false;
        }
    }

    private String getStoredVideoDirecturl(final DownloadLink link) {
        final String directurlproperty = getStoredVideoDirecturlProperty(link);
        lastUsedVideoDirecturlproperty.set(directurlproperty);
        return link.getStringProperty(directurlproperty);
    }

    private String getStoredVideoDirecturlProperty(final DownloadLink link) {
        final boolean allStreamsBrokenOrOffline = looksLikeAllVideoStreamTypesAreBrokenOrOffline(link);
        final boolean hasHlsLink = link.hasProperty(PROPERTY_DIRECTURL_hls_master);
        if (!allStreamsBrokenOrOffline && looksLikeBrokenVideoStream(link, PROPERTY_DOWNLOADLINK_TIMESTAMP_MP4_PROGRESSIVE_FAILED) && hasHlsLink) {
            /* Fallback to hls because progressive stream looks to be broken. */
            return PROPERTY_DIRECTURL_hls_master;
        } else if (!allStreamsBrokenOrOffline && looksLikeBrokenVideoStream(link, PROPERTY_DOWNLOADLINK_TIMESTAMP_MP4_HLS_FAILED)) {
            /* Fallback to progressive stream because HLS stream looks to be broken. */
            return PROPERTY_DIRECTURL;
        } else if (PluginJsonConfig.get(TwitterConfigInterface.class).isPreferHLSVideoDownload() && hasHlsLink) {
            /* Prefer HLS stream because user wants it. */
            return PROPERTY_DIRECTURL_hls_master;
        } else {
            /* Last resort/default: Prefer progressive stream */
            return PROPERTY_DIRECTURL;
        }
    }

    /** Returns true if last download attempt lead to empty file. */
    private boolean looksLikeBrokenVideoStream(final DownloadLink link, final String videoStreamProperty) {
        if (System.currentTimeMillis() - link.getLongProperty(videoStreamProperty, 0) < 5 * 60 * 1000) {
            return true;
        } else {
            return false;
        }
    }

    /** Returns text of this tweet. Can be null as not all tweets have a post-text! */
    private String getTweetText(final DownloadLink link) {
        final String tweetText = link.getStringProperty(TwitterComCrawler.PROPERTY_TWEET_TEXT);
        return TwitterComCrawler.sanitizeTweetText(tweetText);
    }

    private static String regexVideoVmapHighestQualityURL(final Browser br) {
        /* Highest is first in the list */
        String videourl = br.getRegex("<MediaFile>\\s*<\\!\\[CDATA\\[\\s*(http[^<>\"]*?\\.mp4)").getMatch(0);
        if (videourl == null) {
            /* HLS */
            videourl = br.getRegex("<MediaFile type=\"application/x-mpegURL\">\\s*?<\\!\\[CDATA\\[(http[^<>\"]*?)\\]\\]>\\s*?</MediaFile>").getMatch(0);
        }
        return videourl;
    }

    public static String regexTwitterVideo(final String source) {
        String finallink = PluginJSonUtils.getJson(source, "video_url");
        // String finallink = new Regex(source, "video_url\\&quot;:\\&quot;(https:[^<>\"]*?\\.mp4)\\&").getMatch(0);
        // if (finallink != null) {
        // finallink = finallink.replace("\\", "");
        // }
        return finallink;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, null);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        requestFileInformation(link, null, true);
        if (link.getPluginPatternMatcher().matches(TYPE_TWEET_TEXT)) {
            /* Write text to file */
            final File dest = new File(link.getFileOutput());
            IO.writeToFile(dest, getTweetText(link).getBytes("UTF-8"), IO.SYNC.META_AND_DATA);
            /* Set filesize so user can see it in UI. */
            link.setVerifiedFileSize(dest.length());
            /* Set progress to finished - the "download" is complete. */
            link.getLinkStatus().setStatus(LinkStatus.FINISHED);
        } else {
            /* Download file */
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (this.dllink.contains(".m3u8")) {
                dl = new HLSDownloader(link, br, this.dllink);
            } else {
                dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, this.isResumeable(link, account), MAXCHUNKS);
                handleConnectionErrors(link, br, dl.getConnection());
            }
            if (dl.startDownload()) {
                resetFailedFormatsTimestamps(link);
            }
        }
    }

    private void handleConnectionErrors(final DownloadLink link, final Browser br, final URLConnectionAdapter con) throws PluginException, IOException {
        final boolean isVideo = StringUtils.containsIgnoreCase(con.getURL().toExternalForm(), ".mp4");
        final String contentType = con.getContentType();
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            if (con.getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403: Profile which posted this media has been deleted?", 10 * 60 * 1000l);
            } else if (con.getResponseCode() == 404) {
                if (StringUtils.containsIgnoreCase(con.getURL().toExternalForm(), ".mp4")) {
                    this.handleBrokenOrOfflineVideo(link);
                } else if (con.getURL().toExternalForm().endsWith(IMAGE_ORIG_SUFFIX)) {
                    handleBrokenOrOfflineOriginalImage(link);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 10 * 60 * 1000l);
                }
            } else if (con.getResponseCode() == 429) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Rate-limit reached", 5 * 60 * 1000l);
            } else if (con.getResponseCode() == 503) {
                /* 2021-06-24: Possible rate-limit */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 503", 5 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
            }
        } else if (isVideo && con.getCompleteContentLength() == 0 && (contentType == null || !contentType.contains("video"))) {
            /* Avoid download of empty video files. */
            logger.info("Looks like broken http video with filesize == 0");
            this.handleBrokenOrOfflineVideo(link);
        }
    }

    private void handleBrokenOrOfflineVideo(final DownloadLink link) throws PluginException {
        final String errorText = "Broken video?";
        if (looksLikeAllVideoStreamTypesAreBrokenOrOffline(link) || lastUsedVideoDirecturlproperty.get() == null) {
            /* This should never happen! */
            throw new PluginException(LinkStatus.ERROR_FATAL, errorText);
        } else {
            /* Retry with other streaming type */
            final String retryStreamTypeText;
            final String failproperty;
            if (lastUsedVideoDirecturlproperty.get().equals(PROPERTY_DIRECTURL_hls_master)) {
                /* We've tried HLS -> Fallback to progressive */
                failproperty = PROPERTY_DOWNLOADLINK_TIMESTAMP_MP4_HLS_FAILED;
                retryStreamTypeText = "Progressive";
            } else {
                /* We've tried progressive -> Fallback to HLS */
                failproperty = PROPERTY_DOWNLOADLINK_TIMESTAMP_MP4_PROGRESSIVE_FAILED;
                retryStreamTypeText = "HLS";
                if (!link.hasProperty(PROPERTY_DIRECTURL_hls_master)) {
                    /* No HLS fallback possible -> Display permanent error message to user */
                    throw new PluginException(LinkStatus.ERROR_FATAL, errorText);
                }
            }
            /* Save timestamp of failed streaming type which enables us to dynamically retry via other streaming method. */
            link.setProperty(failproperty, System.currentTimeMillis());
            if (allowVideoStreamingTypeFallback == false) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errorText, 5 * 60 * 1000);
            } else {
                throw new PluginException(LinkStatus.ERROR_RETRY, errorText + " | Try again with streaming type: " + retryStreamTypeText);
            }
        }
    }

    /** Call this when image URL ending with IMAGE_ORIG_SUFFIX leads to http response 404. */
    private void handleBrokenOrOfflineOriginalImage(final DownloadLink link) throws PluginException {
        final String errorText = "Broken image?";
        if (looksLikeOriginalImageIsUnavailable(link)) {
            throw new PluginException(LinkStatus.ERROR_FATAL, errorText);
        } else {
            link.setProperty(PROPERTY_DOWNLOADLINK_TIMESTAMP_IMAGE_ORIG_FAILED, System.currentTimeMillis());
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Looks like original image is unavailable or broken -> Retrying normal image download later");
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    public void login(final Account account, final boolean validateCookies) throws Exception {
        synchronized (account) {
            br.setCookiesExclusive(true);
            br.setFollowRedirects(true);
            final Cookies userCookies = account.loadUserCookies();
            if ((userCookies == null || userCookies.isEmpty()) && allowCookieLoginOnly) {
                showCookieLoginInfo();
                throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_required());
            }
            if (userCookies != null && !userCookies.isEmpty()) {
                /* 2020-02-13: Experimental - accepts cookies exported via browser addon "EditThisCookie" */
                br.setCookies(userCookies);
                if (!validateCookies) {
                    /* Do not check if cookies are valid. */
                    return;
                }
                if (checkLogin(br)) {
                    logger.info("User Cookie login successful");
                    return;
                } else if (account.hasEverBeenValid()) {
                    throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                } else {
                    throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                }
            }
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                /*
                 * Re-use cookies whenever possible as frequent logins will cause accounts to get blocked and owners will get warnings via
                 * E-Mail
                 */
                br.setCookies(cookies);
                if (!validateCookies) {
                    /* Do not check if cookies are valid. */
                    return;
                }
                if (this.checkLogin(br)) {
                    /* Set new cookie timestamp */
                    logger.info("Cookie login successful");
                    account.saveCookies(br.getCookies(br.getHost()), "");
                    return;
                } else {
                    logger.info("Cookie login failed");
                    br.clearCookies(null);
                    /* Delete invalid cookies so we won't try to re-use them again next time. */
                    account.clearCookies("");
                }
            }
            br.getPage("https://" + account.getHoster() + "/login");
            String authenticytoken = br.getRegex("type=\"hidden\" value=\"([^<>\"]*?)\" name=\"authenticity_token\"").getMatch(0);
            if (authenticytoken == null) {
                authenticytoken = br.getCookie(br.getHost(), "_mb_tk", Cookies.NOTDELETEDPATTERN);
            }
            if (authenticytoken == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String postData = "session%5Busername_or_email%5D=" + Encoding.urlEncode(account.getUser()) + "&session%5Bpassword%5D=" + Encoding.urlEncode(account.getPass()) + "&return_to_ssl=true&authenticity_token=" + Encoding.urlEncode(authenticytoken) + "&scribe_log=&redirect_after_login=&authenticity_token=" + Encoding.urlEncode(authenticytoken) + "&remember_me=1&ui_metrics=" + Encoding.urlEncode("{\"rf\":{\"\":208,\"\":-17,\"\":-29,\"\":-18},\"s\":\"\"}");
            br.postPage("/sessions", postData);
            if (br.getCookie(br.getHost(), "auth_token", Cookies.NOTDELETEDPATTERN) == null) {
                throw new AccountInvalidException();
            }
            account.saveCookies(br.getCookies(br.getHost()), "");
        }
    }

    private boolean checkLogin(final Browser br) throws IOException, PluginException {
        TwitterComCrawler.prepAPIHeaders(br);
        br.getPage("https://api.twitter.com/2/badge_count/badge_count.json?supports_ntab_urt=1");
        /* E.g. check for error "rate-limit reached" */
        this.checkGenericErrors(br);
        if (br.getRequest().getHttpConnection().getResponseCode() == 200) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        br.getPage("https://" + this.getHost() + "/i/api/1.1/account/settings.json?include_mention_filter=true&include_nsfw_user_flag=true&include_nsfw_admin_flag=true&include_ranked_timeline=true&include_alt_text_compose=true&ext=ssoConnections&include_country_code=true&include_ext_dm_nsfw_media_filter=true&include_ext_sharing_audiospaces_listening_data_with_followers=true");
        final Map<String, Object> user = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final String username = (String) user.get("screen_name");
        final Cookies userCookies = account.loadUserCookies();
        /*
         * Users can enter anything into the "username" field when cookie login is used --> Correct that so we got an unique 'username'
         * value. Otherwise users could easily add one account multiple times -> Could cause issues.
         */
        if (!StringUtils.isEmpty(username) && userCookies != null) {
            account.setUser(username);
        }
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        account.setMaxSimultanDownloads(1);
        account.setConcurrentUsePossible(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        login(account, false);
        handleDownload(link, account);
    }

    private void resetFailedFormatsTimestamps(final DownloadLink link) {
        link.removeProperty(PROPERTY_DOWNLOADLINK_TIMESTAMP_MP4_HLS_FAILED);
        link.removeProperty(PROPERTY_DOWNLOADLINK_TIMESTAMP_MP4_PROGRESSIVE_FAILED);
        link.removeProperty(PROPERTY_DOWNLOADLINK_TIMESTAMP_IMAGE_ORIG_FAILED);
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return TwitterConfigInterface.class;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        resetFailedFormatsTimestamps(link);
    }
}