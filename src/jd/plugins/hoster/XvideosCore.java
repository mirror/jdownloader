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
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.PublicSuffixList;
import org.appwork.utils.net.httpconnection.HTTPConnection;
import org.appwork.utils.net.httpconnection.SSLSocketStreamOptions;
import org.appwork.utils.net.httpconnection.SSLSocketStreamOptionsModifier;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.AbstractRecaptchaV2.TYPE;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.downloader.hls.M3U8Playlist;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.net.BCSSLSocketStreamFactory;
import org.jdownloader.plugins.components.config.XvideosComConfigCore;
import org.jdownloader.plugins.components.config.XvideosComConfigCore.PreferredHLSQuality;
import org.jdownloader.plugins.components.config.XvideosComConfigCore.PreferredHTTPQuality;
import org.jdownloader.plugins.components.config.XvideosComConfigCore.PreferredOfficialDownloadQuality;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public abstract class XvideosCore extends PluginForHost {
    public XvideosCore(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getPluginContentURL(final DownloadLink link) {
        final String directurl = link.getStringProperty(PROPERTY_LAST_USED_DIRECTURL);
        final Class<? extends XvideosComConfigCore> cfi = this.getConfigInterface();
        if (cfi != null && PluginJsonConfig.get(cfi).isPluginContentURLExposeDirecturls() && directurl != null) {
            return directurl;
        } else {
            return super.getPluginContentURL(link);
        }
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX, LazyPlugin.FEATURE.COOKIE_LOGIN_OPTIONAL };
    }

    public static void setSSLSocketStreamOptions(Browser br) {
        br.setSSLSocketStreamOptions(new SSLSocketStreamOptionsModifier() {
            @Override
            public SSLSocketStreamOptions modify(SSLSocketStreamOptions sslSocketStreamOptions, HTTPConnection httpConnection) {
                final SSLSocketStreamOptions ret = new SSLSocketStreamOptions(sslSocketStreamOptions) {
                    public org.appwork.utils.net.httpconnection.SSLSocketStreamFactory getSSLSocketStreamFactory() {
                        return new BCSSLSocketStreamFactory();
                    };
                };
                ret.getDisabledCipherSuites().clear();
                ret.getCustomFactorySettings().add("JSSE_TLS1.3_ENABLED");
                ret.getCustomFactorySettings().add("BC_TLS1.3_ENABLED");
                return ret;
            }
        });
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser ret = super.createNewBrowserInstance();
        setSSLSocketStreamOptions(ret);
        return ret;
    }

    /**
     * Put dead domains in here so that URLs leading to dead domains will still get added successfully if the content behind them is still
     * online.
     */
    public String[] getDeadDomains() {
        return new String[] {};
    };

    /** Returns domains which might be available but should be avoided */
    public String[] getAvoidDomains() {
        return new String[] {};
    };

    protected abstract String[] getAllDomains();

    /** Fallback for if premium domain cannot be determined automatically. */
    protected abstract String getFallbackPremiumDomain();

    private String getPremiumDomain(final Account account) {
        if (account == null) {
            return this.getFallbackPremiumDomain();
        } else {
            return account.getStringProperty(PROPERTY_ACCOUNT_PREMIUM_DOMAIN, this.getFallbackPremiumDomain());
        }
    }

    protected String getPremiumBaseURL(final Account account) {
        final String premiumDomain = this.getPremiumDomain(account);
        if (PublicSuffixList.getInstance().getSubDomain(premiumDomain) != null) {
            return "https://" + premiumDomain;
        } else {
            return "https://www." + premiumDomain;
        }
    }

    protected String getPremiumAccountOverviewURL(final Account account) {
        return getPremiumBaseURL(account) + "/account/premium";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String videoid = getVideoID(link);
        if (videoid != null) {
            return getHost() + "://" + videoid;
        } else {
            return super.getLinkID(link);
        }
    }

    protected String getVideoID(final DownloadLink link) {
        final String storedInternalVideoID = link.getStringProperty(PROPERTY_VIDEOID);
        if (storedInternalVideoID != null) {
            return storedInternalVideoID;
        } else {
            return getVideoidFromURL(link.getPluginPatternMatcher());
        }
    }

    protected String getVideoidFromURL(final String url) {
        if (url == null) {
            return null;
        } else if (url.matches(type_embed)) {
            return new Regex(url, type_embed).getMatch(0);
        } else if (url.matches(type_normal)) {
            return new Regex(url, type_normal).getMatch(0);
        } else if (url.matches(type_normal_dot)) {
            return new Regex(url, type_normal_dot).getMatch(0);
        } else if (url.matches(type_normal_dash)) {
            return new Regex(url, type_normal_dash).getMatch(0);
        } else if (url.matches(type_special1)) {
            return new Regex(url, type_special1).getMatch(0);
        } else if (url.matches(type_special2)) {
            return new Regex(url, type_special2).getMatch(2);
        } else {
            return null;
        }
    }

    private String getVideoID(final Browser br, final DownloadLink link) {
        final String videoIDByURL = this.getVideoID(link);
        if (videoIDByURL != null) {
            return videoIDByURL;
        } else {
            final String videoidFromHTML = br.getRegex("/embedframe/(\\d+)").getMatch(0);
            if (videoidFromHTML != null) {
                link.setProperty(PROPERTY_VIDEOID, videoidFromHTML);
                return videoidFromHTML;
            } else {
                return null;
            }
        }
    }

    protected String getURLTitle(final DownloadLink link) {
        final String url = link.getPluginPatternMatcher();
        if (url == null) {
            return null;
        } else if (url.matches(type_special1)) {
            return new Regex(url, type_special1).getMatch(1);
        } else if (url.matches(type_special2)) {
            return new Regex(url, type_special2).getMatch(1);
        } else {
            /* Not all URLs have titles */
            return null;
        }
    }

    /* xvideos.com */
    protected static final String type_normal                     = "(?i)https?://[^/]+/video(\\d+)(/(.+))?$";
    protected static final String type_normal_dot                 = "(?i)https?://[^/]+/video\\.([a-z0-9\\-]+)(.*?/[^/]+)?$";
    /* xnxx.gold */
    protected static final String type_normal_dash                = "(?i)https?://[^/]+/video-([a-z0-9\\-]+)(/[^/]+)?$";                          // xnxx.com&
    // xnxx.gold
    protected static final String type_embed                      = "(?i)https?://[^/]+/embedframe/(\\d+)";
    protected static final String type_special1                   = "(?i)https?://[^/]+/[^/]+/upload/[^/]+/(\\d+)/([^/]+)";
    protected static final String type_special2                   = "(?i)https?://[^/]+/[^/]+/(upload|pornstar|model)/([a-z0-9\\-\\_]+)/(\\d+).*";
    protected static final String NOCHUNKS                        = "NOCHUNKS";
    private String                streamURL                       = null;
    private HlsContainer          hlsContainer                    = null;
    public static final String    PROPERTY_USERNAME               = "username";
    private static final String   PROPERTY_TAGS                   = "tags";
    private static final String   PROPERTY_VIDEOID                = "videoid";
    private static final String   PROPERTY_LAST_USED_DIRECTURL    = "last_used_directurl";
    private final String          PROPERTY_ACCOUNT_PREMIUM_DOMAIN = "premium_domain";

    protected String getContentURL(final DownloadLink link) {
        String url = link.getPluginPatternMatcher();
        if (!url.matches(type_normal) && !url.matches(type_normal_dash) && !url.matches(type_normal_dot)) {
            final String normalContentURL = buildNormalContentURL(link);
            if (normalContentURL != null) {
                url = normalContentURL;
            }
        }
        /*
         * 2020-10-12: In general, we use the user-added domain but some are dead but the content might still be alive --> Use main plugin
         * domain for such cases
         */
        final String[] deadDomains = getDeadDomains();
        boolean replacedDeadDomain = false;
        if (deadDomains != null) {
            for (final String deadDomain : deadDomains) {
                if (url.contains(deadDomain)) {
                    url = url.replaceFirst("(?i)" + Pattern.quote(deadDomain) + "/", this.getHost() + "/");
                    replacedDeadDomain = true;
                    break;
                }
            }
        }
        final String[] avoidDomains = this.getAvoidDomains();
        if (avoidDomains != null && !replacedDeadDomain) {
            for (final String deadDomain : avoidDomains) {
                if (url.contains(deadDomain)) {
                    url = url.replaceFirst("(?i)" + Pattern.quote(deadDomain) + "/", this.getHost() + "/");
                    break;
                }
            }
        }
        return url;
    }

    abstract String buildNormalContentURL(final DownloadLink link);

    private boolean isValidVideoURL(final DownloadLink link, final String url, final boolean setFilesize) throws Exception {
        if (StringUtils.isEmpty(url)) {
            return false;
        } else {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(url);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0 && setFilesize) {
                        if (con.isContentDecoded()) {
                            link.setDownloadSize(con.getCompleteContentLength());
                        } else {
                            link.setVerifiedFileSize(con.getCompleteContentLength());
                        }
                    }
                    return true;
                } else {
                    throw new IOException();
                }
            } catch (final IOException e) {
                logger.log(e);
                return false;
            } finally {
                if (con != null) {
                    try {
                        con.disconnect();
                    } catch (Throwable e) {
                    }
                }
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account, false);
    }

    public static void disableAutoTranslation(final Plugin plugin, final String host, Browser br) throws IOException {
        br.getHeaders().put("Accept-Language", "en-gb");
        final Browser brc = br.cloneBrowser();
        brc.setFollowRedirects(true);
        if (brc.getRequest() == null) {
            brc.getPage("https://www." + host);
        }
        brc.getPage("/change-language/en");
        brc.postPage("/feature-disabled", "featureid=at");
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, Account account, final boolean isDownload) throws Exception {
        final String contentURL = this.getContentURL(link);
        final String urlTitle = getURLTitle(link);
        if (!link.isNameSet() && urlTitle != null) {
            link.setName(urlTitle + ".mp4");
        }
        br.setFollowRedirects(false);
        br.getHeaders().put("Accept-Encoding", "gzip");
        /* 2021-07-07: They seem to ignore this header and set language randomly or by IP or by user-choice! */
        br.getHeaders().put("Accept-Language", "en-gb");
        if (account != null) {
            this.login(account, false);
        }
        final boolean useLanguageSwitcherHandling = true;
        if (useLanguageSwitcherHandling) {
            /**
             * Use this to prefer English language. </br>
             * 2021-07-07: Not yet required - only in crawler plugin: Seems like they set the language for the main website/video overview
             * based on IP and for single videos, default is English(?)
             */
            disableAutoTranslation(this, Browser.getHost(contentURL), br);
        }
        br.getPage(contentURL);
        int counter = 0;
        String videoID = this.getVideoID(link);
        while (br.getRedirectLocation() != null) {
            final String redirect = br.getRedirectLocation();
            /*
             * 2019-09-30: Only set new URL if it is valid. E.g. when using xvideos2.com (= for india) in germany, it will only redirect us
             * to their mainpage!
             */
            if (this.canHandle(redirect) && videoID != null && redirect.contains(videoID)) {
                logger.info("Setting new PluginPatternMatcher: " + redirect);
                link.setPluginPatternMatcher(redirect);
            } else {
                logger.info("Progressing to redirect WITHOUT setting new PluginPatternMatcher: " + redirect);
            }
            if (counter >= 10) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Too many redirects");
            }
            br.getPage(redirect);
            counter += 1;
        }
        if (br.containsHTML("(?i)(This video has been deleted|Page not found|>Sorry, this video is not available\\.|>We received a request to have this video deleted|class=\"inlineError\")")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!this.canHandle(br.getURL())) {
            /* 2020-12-15: E.g. redirect to mainpage */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* Original title (independent from selected language) */
        String filename = br.getRegex("\"video_title_ori\"\\s*:\\s*\"(.*?)\"").getMatch(0);
        if (filename == null) {
            /* Can be translated titles */
            filename = br.getRegex("\"video_title\"\\s*:\\s*\"(.*?)\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("(?i)<title>([^<>\"]*?)\\- XVIDEOS\\.COM</title>").getMatch(0);
            }
        }
        {
            /* Set packagizer properties */
            final String uploadername = PluginJSonUtils.getJson(br, "uploader");
            if (StringUtils.isEmpty(link.getStringProperty(PROPERTY_USERNAME)) && !StringUtils.isEmpty(uploadername)) {
                link.setProperty(PROPERTY_USERNAME, uploadername);
            }
            final String[] tagsList = br.getRegex("<a[^>]*href=\"/tags/([^\"]+)\"[^>]*class=\"btn btn-default\"[^>]*>").getColumn(0);
            if (tagsList.length > 0) {
                final StringBuilder sb = new StringBuilder();
                for (String tag : tagsList) {
                    tag = Encoding.htmlDecode(tag).trim();
                    if (StringUtils.isNotEmpty(tag)) {
                        if (sb.length() > 0) {
                            sb.append(",");
                        }
                        sb.append(tag);
                    }
                }
                if (sb.length() > 0) {
                    link.setProperty(PROPERTY_TAGS, sb.toString());
                }
            }
        }
        videoID = getVideoID(this.br, link);
        if (videoID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (filename != null) {
            filename = Encoding.unicodeDecode(filename);
            filename = Encoding.htmlDecode(filename);
            filename = videoID + "_" + filename.trim();
            link.setFinalFileName(filename + ".mp4");
        } else {
            logger.warning("Failed to find nice final filename");
        }
        final XvideosComConfigCore config = getConfigInterface() != null ? PluginJsonConfig.get(getConfigInterface()) : null;
        if (isDownload || (config != null && !config.isEnableFastLinkcheckForHostPlugin())) {
            final String hlsMaster = br.getRegex("setVideoHLS\\('(.*?)'\\)").getMatch(0);
            /**
             * 2021-01-27: This website can "shadow ban" users who download "too much". They will then deliver all videos in 240p only. This
             * is an attempt to detect this.</br>
             * See also: https://board.jdownloader.org/showthread.php?t=86587 </br>
             * Do not check when premium account is given because it usually allows official downloads so downloads will work fine even if
             * HLS streaming is not available.
             */
            final boolean lowQualityBlockDetected = StringUtils.isEmpty(hlsMaster) && (account == null || account.getType() != AccountType.PREMIUM) && config != null && config.isTryToRecognizeLimit() && isDownload;
            if (config == null || config.isPreferHLSStreamDownload()) {
                logger.info("User prefers HLS download");
                if (StringUtils.isNotEmpty(hlsMaster)) {
                    logger.info("FoundHlsMaster --> Looking for preferred quality");
                    final Browser m3u8 = br.cloneBrowser();
                    m3u8.getPage(hlsMaster);
                    final int preferredHLSQuality = getPreferredHLSQuality();
                    HlsContainer userPreferredQuality = null;
                    final List<HlsContainer> hlsqualities = HlsContainer.getHlsQualities(m3u8);
                    for (final HlsContainer currentQuality : hlsqualities) {
                        final int width = currentQuality.getHeight();
                        if (width == preferredHLSQuality) {
                            /* We found the quality our user prefers. */
                            userPreferredQuality = currentQuality;
                            break;
                        }
                    }
                    final HlsContainer chosenQuality;
                    if (userPreferredQuality != null) {
                        chosenQuality = userPreferredQuality;
                        logger.info("Found user selected HLS quality: " + preferredHLSQuality + ">" + userPreferredQuality.getHeight());
                    } else {
                        chosenQuality = HlsContainer.findBestVideoByBandwidth(hlsqualities);
                        logger.info("Failed to find user-selected HLS quality --> Fallback to BEST: " + chosenQuality.getHeight());
                    }
                    link.setProperty(PROPERTY_LAST_USED_DIRECTURL, chosenQuality.getDownloadurl());
                    if (isDownload) {
                        this.hlsContainer = chosenQuality;
                    }
                    /* Set estimated filesize. */
                    final List<M3U8Playlist> playLists = M3U8Playlist.loadM3U8(chosenQuality.getDownloadurl(), m3u8);
                    long estimatedSize = -1;
                    for (M3U8Playlist playList : playLists) {
                        if (chosenQuality.getBandwidth() > 0) {
                            playList.setAverageBandwidth(chosenQuality.getBandwidth());
                            estimatedSize += playList.getEstimatedSize();
                        }
                    }
                    if (estimatedSize > 0) {
                        link.setDownloadSize(estimatedSize);
                    }
                    return AvailableStatus.TRUE;
                } else {
                    logger.info("Failed to find HLS qualities!");
                }
            }
            /**
             * 2022-09-08: Looks like HLS is available up to 1080p while official downloads are only available for up to 360p (?). </br>
             * Tested with a free xvideos.com account. </br>
             * If official download was >= HLS/stream download it would make sense to prefer this over stream download.
             */
            String videoURL = null;
            if (account != null) {
                /* When logged-in, official downloadlinks can be available */
                logger.info("Looking for official download ...");
                final Browser brc = br.cloneBrowser();
                brc.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                brc.getHeaders().put("x-Requested-With", "XMLHttpRequest");
                brc.getPage(this.br.getURL("/video-download/" + videoID + "/"));
                if (brc.getURL().contains(videoID)) {
                    final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(brc.toString());
                    String bestQualityDownloadurl = null;
                    String bestQualityString = null;
                    String preferredQualityDownloadurl = null;
                    final String preferredQualityStr = getPreferredOfficialDownloadQualityStr();
                    final String[] qualities = getOfficialDownloadQualitiesSorted();
                    for (final String qualityStrTmp : qualities) {
                        final String downloadURLTmp = (String) entries.get(qualityStrTmp);
                        if (!StringUtils.isEmpty(downloadURLTmp)) {
                            if (bestQualityDownloadurl == null) {
                                bestQualityDownloadurl = downloadURLTmp;
                                bestQualityString = qualityStrTmp;
                            }
                            if (StringUtils.equals(qualityStrTmp, preferredQualityStr)) {
                                preferredQualityDownloadurl = downloadURLTmp;
                                break;
                            }
                        }
                    }
                    if (preferredQualityDownloadurl != null) {
                        logger.info("Using user selected quality: " + preferredQualityStr);
                        videoURL = preferredQualityDownloadurl;
                    } else if (bestQualityDownloadurl != null) {
                        logger.info("Using best quality: " + bestQualityString);
                        videoURL = bestQualityDownloadurl;
                    } else {
                        logger.warning("Failed to find any official downloads -> json has changed?");
                    }
                } else {
                    /*
                     * Either user has a free account or something with the login went wrong. All premium users should be able to download
                     * via download button!
                     */
                    logger.info("No official video download possible");
                }
                if (StringUtils.isEmpty(videoURL)) {
                    logger.info("Failed to find any official downloadlink");
                }
            }
            if (StringUtils.isEmpty(videoURL)) {
                /* Download http streams */
                final PreferredHTTPQuality qualityhttp = getPreferredHTTPQuality();
                boolean foundValidURL = false;
                String httpVideoURL = null;
                switch (qualityhttp) {
                case HIGH:
                    httpVideoURL = getVideoHigh(br);
                    if (isValidVideoURL(link, httpVideoURL, true)) {
                        foundValidURL = true;
                        break;
                    }
                case LOW:
                    httpVideoURL = getVideoLow(br);
                    if (isValidVideoURL(link, httpVideoURL, true)) {
                        foundValidURL = true;
                        break;
                    }
                default:
                    httpVideoURL = getVideoFlv(br);
                    if (isValidVideoURL(link, httpVideoURL, true)) {
                        foundValidURL = true;
                        break;
                    }
                }
                if (foundValidURL && httpVideoURL != null) {
                    videoURL = Encoding.htmlOnlyDecode(httpVideoURL);
                }
            }
            if (videoURL != null) {
                link.setProperty(PROPERTY_LAST_USED_DIRECTURL, videoURL);
            }
            if (StringUtils.isEmpty(videoURL)) {
                throw new AccountRequiredException();
            } else if (lowQualityBlockDetected) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Low quality block active", 60 * 60 * 1000l);
            } else if (isDownload) {
                streamURL = videoURL;
            }
        }
        return AvailableStatus.TRUE;
    }

    private String getVideoFlv(final Browser br) {
        final String dllink = br.getRegex("flv_url=(.*?)\\&").getMatch(0);
        if (dllink == null) {
            return decode(br.getRegex("encoded=(.*?)\\&").getMatch(0));
        } else {
            return dllink;
        }
    }

    private String getVideoHigh(final Browser br) {
        return br.getRegex("(?i)html5player\\.setVideoUrlHigh\\('(.*?)'\\)").getMatch(0);
    }

    private String getVideoLow(final Browser br) {
        return br.getRegex("(?i)html5player\\.setVideoUrlLow\\('(.*?)'\\)").getMatch(0);
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, null, true);
        handleDownload(link, null);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception {
        final String contentURL = this.getContentURL(link);
        if (Browser.getHost(contentURL).equalsIgnoreCase(this.getPremiumDomain(account)) && (account == null || account.getType() != AccountType.PREMIUM) && this.streamURL == null && this.hlsContainer == null) {
            throw new AccountRequiredException();
        }
        if (streamURL != null) {
            int chunks = 0;
            if (link.getBooleanProperty(XvideosCore.NOCHUNKS, false)) {
                chunks = 1;
            }
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, streamURL, true, chunks);
            if (!looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(XvideosCore.NOCHUNKS, false) == false) {
                    link.setProperty(XvideosCore.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } else if (hlsContainer != null) {
            final String m3u8 = hlsContainer.getDownloadurl();
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, m3u8);
            dl.startDownload();
        } else {
            /* This should never happen! */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @SuppressWarnings("deprecation")
    private static String decode(String encoded) {
        if (encoded == null) {
            return null;
        }
        /* CHECK: we should always use new String (bytes,charset) to avoid issues with system charset and utf-8 */
        encoded = new String(jd.crypt.Base64.decode(encoded));
        String[] encodArr = encoded.split("-");
        String encodedUrl = "";
        for (String i : encodArr) {
            int charNum = Integer.parseInt(i);
            if (charNum != 0) {
                encodedUrl = encodedUrl + (char) charNum;
            }
        }
        return calculate(encodedUrl);
    }

    private static String calculate(String src) {
        if (src == null) {
            return null;
        }
        String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMabcdefghijklmnopqrstuvwxyzabcdefghijklm";
        String calculated = "";
        int i = 0;
        while (i < src.length()) {
            char character = src.charAt(i);
            int pos = CHARS.indexOf(character);
            if (pos > -1) {
                character = CHARS.charAt(13 + pos);
            }
            calculated = calculated + character;
            i++;
        }
        return calculated;
    }

    private PreferredHTTPQuality getPreferredHTTPQuality() {
        final Class<? extends XvideosComConfigCore> clazz = getConfigInterface();
        if (clazz == null) {
            return PreferredHTTPQuality.HIGH;
        } else {
            return PluginJsonConfig.get(clazz).getPreferredHTTPQuality();
        }
    }

    private int getPreferredHLSQuality() {
        final Class<? extends XvideosComConfigCore> clazz = getConfigInterface();
        if (clazz == null) {
            return 2160;
        } else {
            final PreferredHLSQuality preferredHLSQuality = PluginJsonConfig.get(clazz).getPreferredHLSQuality();
            switch (preferredHLSQuality) {
            case Q2160P:
                return 2160;
            case Q1080P:
                return 1080;
            case Q720P:
                return 720;
            case Q480P:
                return 480;
            case Q360P:
                return 360;
            default:
                /* This should never happen */
                return -1;
            }
        }
    }

    /** Get user preferred official download quality as String. */
    private String getPreferredOfficialDownloadQualityStr() {
        final Class<? extends XvideosComConfigCore> clazz = getConfigInterface();
        if (clazz == null) {
            return officialDownloadQualityToString(null);
        } else {
            final PreferredOfficialDownloadQuality preferredOfficialDownloadQuality = PluginJsonConfig.get(clazz).getPreferredOfficialDownloadQuality();
            return officialDownloadQualityToString(preferredOfficialDownloadQuality);
        }
    }

    /** Returns array containing all possible official download qualities sorted from best to worst. */
    private String[] getOfficialDownloadQualitiesSorted() {
        final PreferredOfficialDownloadQuality[] allPossibleOfficialDownloadQualities = PreferredOfficialDownloadQuality.values();
        int index = 0;
        final String[] allPossibleOfficialDownloadQualitiesStr = new String[allPossibleOfficialDownloadQualities.length];
        for (final PreferredOfficialDownloadQuality possibleOfficialDownloadQuality : allPossibleOfficialDownloadQualities) {
            allPossibleOfficialDownloadQualitiesStr[index] = officialDownloadQualityToString(possibleOfficialDownloadQuality);
            index += 1;
        }
        return allPossibleOfficialDownloadQualitiesStr;
    }

    private String officialDownloadQualityToString(PreferredOfficialDownloadQuality downloadQuality) {
        if (downloadQuality == null) {
            downloadQuality = PreferredOfficialDownloadQuality.Q2160P;
        }
        switch (downloadQuality) {
        case Q2160P:
            return "URL_MP4_4K";
        case Q1080P:
            return "URL_MP4HD";
        case Q360P:
            return "URL";
        case Q240P:
            return "URL_LOW";
        default:
            /* This should never happen */
            return null;
        }
    }

    public boolean login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                /* Prefer English language */
                br.getHeaders().put("Accept-Language", "en-gb");
                final Cookies cookiesUser = account.loadUserCookies();
                final Cookies cookiesFree = account.loadCookies("");
                final Cookies cookiesPremium = account.loadCookies("premium");
                if (cookiesUser != null) {
                    logger.info("Attempting user-cookie login");
                    setCookies(br, cookiesUser);
                    br.setCookies(this.getPremiumDomain(account), cookiesUser);
                    if (attemptCookieLogin(account)) {
                        logger.info("User-Cookie login successful");
                        return true;
                    } else {
                        logger.info("User-Cookie login failed");
                        if (account.hasEverBeenValid()) {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                        } else {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                        }
                    }
                }
                if (cookiesFree != null && cookiesPremium != null) {
                    logger.info("Attempting cookie login");
                    setCookies(br, cookiesFree);
                    br.setCookies(this.getPremiumDomain(account), cookiesPremium);
                    if (!force) {
                        logger.info("Trust cookies without check");
                        return false;
                    }
                    if (attemptCookieLogin(account)) {
                        logger.info("Cookie login successful");
                        return true;
                    } else {
                        logger.info("Cookie login failed");
                        br.clearAll();
                    }
                }
                logger.info("Performing full login");
                if (!StringUtils.contains(account.getUser(), "@")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new AccountInvalidException("Bitte gib deine E-Mail Adresse in das 'Benutzername' Feld ein!");
                    } else {
                        throw new AccountInvalidException("Please enter your e-mail address into the 'username' field!");
                    }
                }
                final boolean useAjaxLogin = true;
                if (useAjaxLogin) {
                    br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                    br.getPage("https://www." + this.getHost() + "/");
                    br.postPage("/account/signinform", "");
                    Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
                    final String formHTML1 = (String) entries.get("form");
                    if (!StringUtils.isEmpty(formHTML1)) {
                        br.getRequest().setHtmlCode(formHTML1);
                    }
                } else {
                    br.getPage("https://www." + this.getHost() + "/account/");
                }
                final Form loginform = br.getFormbyProperty("id", "signin-form");
                if (loginform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else if (!loginform.hasInputFieldByName("signin-form%5Bpassword%5D")) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put(Encoding.urlEncode("signin-form[login]"), Encoding.urlEncode(account.getUser()));
                loginform.put(Encoding.urlEncode("signin-form[password]"), Encoding.urlEncode(account.getPass()));
                loginform.put(Encoding.urlEncode("signin-form[rememberme]"), "on");
                if (CaptchaHelperHostPluginRecaptchaV2.containsRecaptchaV2Class(loginform.getHtmlCode()) || loginform.hasInputFieldByName(Encoding.urlEncode("signin-form[hidden_captcha]"))) {
                    final CaptchaHelperHostPluginRecaptchaV2 rc2 = new CaptchaHelperHostPluginRecaptchaV2(this, br);
                    final String recaptchaV2Response = rc2.getToken();
                    /* 2021-10-01: psp: Captchas can be easily triggered by using a VPN. Last time I've used a Japan VPN. */
                    if (rc2.getType() == TYPE.INVISIBLE) {
                        loginform.put(Encoding.urlEncode("signin-form[hidden_captcha]"), Encoding.urlEncode(recaptchaV2Response));
                    } else {
                        loginform.put(Encoding.urlEncode("g-recaptcha-response"), Encoding.urlEncode(recaptchaV2Response));
                    }
                }
                br.submitForm(loginform);
                Map<String, Object> ajaxLoginResponse = null;
                if (useAjaxLogin) {
                    ajaxLoginResponse = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
                    final String formHTML2 = (String) ajaxLoginResponse.get("form");
                    if (!StringUtils.isEmpty(formHTML2)) {
                        /* 2FA login */
                        br.getRequest().setHtmlCode(formHTML2);
                    }
                }
                /* 2FA login */
                final Form unknownBrowserForm = br.getFormbyProperty("id", "unknown-browser-form");
                final Form totpAuthForm = br.getFormbyProperty("id", "totp-auth");
                if (unknownBrowserForm != null) {
                    /* 2FA login via mail --> xvideos.com security feature, not selected by user! */
                    /* TODO: Test this or wait for user-feedback */
                    final DownloadLink dl_dummy;
                    if (this.getDownloadLink() != null) {
                        dl_dummy = this.getDownloadLink();
                    } else {
                        dl_dummy = new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true);
                    }
                    String twoFACode = getUserInput("Enter 2FA code that was sent to your email " + account.getUser(), dl_dummy);
                    if (twoFACode != null) {
                        twoFACode = twoFACode.trim();
                    }
                    if (twoFACode == null || !twoFACode.matches("\\d{6}")) {
                        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                            throw new AccountUnavailableException("\r\nUngültiges Format der 2-faktor-Authentifizierung!", 1 * 60 * 1000l);
                        } else {
                            throw new AccountUnavailableException("\r\nInvalid 2-factor-authentication code format!", 1 * 60 * 1000l);
                        }
                    }
                    logger.info("Submitting 2FA mail code");
                    unknownBrowserForm.put(Encoding.urlEncode("unknown-browser-form[code]"), twoFACode);
                    br.submitForm(unknownBrowserForm);
                    if (useAjaxLogin) {
                        ajaxLoginResponse = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
                    }
                } else if (totpAuthForm != null) {
                    /* TOTP login -> 2FA login which user has enabled via opt-in in his account. */
                    final DownloadLink dl_dummy;
                    if (this.getDownloadLink() != null) {
                        dl_dummy = this.getDownloadLink();
                    } else {
                        dl_dummy = new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true);
                    }
                    String twoFACode = getUserInput("Enter 2FA TOTP code or any of your backup codes", dl_dummy);
                    twoFACode = twoFACode.trim();
                    logger.info("Submitting 2FA TOTP code");
                    totpAuthForm.put(Encoding.urlEncode("totp-auth[code]"), Encoding.urlEncode(twoFACode));
                    /* Do not ask again for TOTP code for 30 days. */
                    totpAuthForm.put(Encoding.urlEncode("totp-auth[remember_me]"), "on");
                    // totpAuthForm.put(Encoding.urlEncode("totp-auth[post_referer]"), Encoding.urlEncode("https://de.xvideos.com/"));
                    br.submitForm(totpAuthForm);
                    if (useAjaxLogin) {
                        ajaxLoginResponse = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
                    }
                }
                if (useAjaxLogin) {
                    final String premium_redirect = (String) ajaxLoginResponse.get("premium_redirect");
                    final String redirect_domain = (String) ajaxLoginResponse.get("redirect_domain");
                    /*
                     * E.g. xnxx.gold response: {"form_valid":true,"form_displayed":"signin","user_main_cat":"straight","is_premium":true}
                     */
                    if (!StringUtils.isEmpty(redirect_domain)) {
                        /* FREE account */
                        br.getPage(redirect_domain);
                    } else if (!StringUtils.isEmpty(premium_redirect)) {
                        /* PREMIUM account */
                        br.getPage(getPremiumBaseURL(account) + "/?" + premium_redirect);
                    } else {
                        /* Get mainpage */
                        br.getPage("/");
                    }
                }
                if (!isLoggedin(br)) {
                    final String loginFailureText = new Regex(PluginJSonUtils.unescape(br.getRequest().getHtmlCode()), "class=\"help-block error-block\">([^<]+)</span>").getMatch(0);
                    if (loginFailureText != null && DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                        throw new AccountInvalidException(Encoding.htmlDecode(loginFailureText).trim());
                    } else if (unknownBrowserForm != null) {
                        throw new AccountInvalidException("2FA mail login failed");
                    } else if (totpAuthForm != null) {
                        throw new AccountInvalidException("2FA TOTP login failed");
                    } else {
                        throw new AccountInvalidException();
                    }
                }
                if (isPremium(this.br)) {
                    account.setType(AccountType.PREMIUM);
                } else {
                    account.setType(AccountType.FREE);
                }
                /*
                 * Always save both types of cookies otherwise cookie-login won't work and/or we'd never be able to notice whenever e.g. a
                 * free account changes to premium status.
                 */
                final Map<String, Object> map = getJsonMap(br);
                final Map<String, Object> domains = (Map<String, Object>) map.get("domains");
                final String premiumDomain = Browser.getHost(domains.get("premium").toString(), true);
                account.saveCookies(br.getCookies(this.getHost()), "");
                account.saveCookies(br.getCookies(premiumDomain), "premium");
                account.setProperty(PROPERTY_ACCOUNT_PREMIUM_DOMAIN, premiumDomain);
                return true;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                    account.clearCookies("premium");
                }
                throw e;
            }
        }
    }

    private boolean attemptCookieLogin(final Account account) throws Exception {
        /* Will redirect to premium domain if we're logged in as premium user */
        br.getPage("https://www." + this.getHost() + "/");
        if (isLoggedin(br)) {
            logger.info("Cookie login successful");
            /* Refresh cookie timestamp */
            account.saveCookies(br.getCookies(this.getHost()), "");
            /* Now check if we have a free- or a premium account */
            br.getPage(this.getPremiumAccountOverviewURL(account));
            if (isPremium(br)) {
                account.saveCookies(br.getCookies(this.getPremiumDomain(account)), "premium");
                account.setType(AccountType.PREMIUM);
            } else {
                account.setType(AccountType.FREE);
            }
            final Map<String, Object> map = getJsonMap(br);
            final Map<String, Object> domains = (Map<String, Object>) map.get("domains");
            final String premiumDomain = Browser.getHost(domains.get("premium").toString(), true);
            account.setProperty(PROPERTY_ACCOUNT_PREMIUM_DOMAIN, premiumDomain);
            return true;
        } else {
            logger.info("Cookie login failed");
            return false;
        }
    }

    private void setCookies(final Browser br, final Cookies cookies) {
        for (final String domain : this.getAllDomains()) {
            br.setCookies(domain, cookies);
        }
    }

    /**
     * Only use this when on this page: https://www.domain.tld/account/premium </br>
     * 2021-03-08: Free users cannot even view the account panel so checking for any elements in there is good enough as premium indicator!
     *
     * @throws Exception
     */
    private static boolean isPremium(final Browser br) throws Exception {
        /*
         * 2021-03-08: "Cancel subsrciption" button is not always there is they also got packages that are a one time pay thing and don't
         * have to be cancelled by the user!
         */
        final Map<String, Object> map = getJsonMap(br);
        final Map<String, Object> login_info = (Map<String, Object>) JavaScriptEngineFactory.walkJson(map, "dyn/login_info");
        return ((Boolean) login_info.get("is_premium"));
    }

    private static Map<String, Object> getJsonMap(final Browser br) throws Exception {
        final String json = br.getRegex("window\\.xv\\.conf=(\\{.*?\\});\\s*</script>").getMatch(0);
        if (json != null) {
            return JavaScriptEngineFactory.jsonToJavaMap(json);
        }
        return null;
    }

    /**
     * Works for free- and premium domain (for xvideos.red only if user has premium)!
     *
     * @throws Exception
     */
    private static boolean isLoggedin(final Browser br) throws Exception {
        try {
            final Map<String, Object> map = getJsonMap(br);
            final Map<String, Object> login_info = (Map<String, Object>) JavaScriptEngineFactory.walkJson(map, "dyn/login_info");
            return ((Boolean) login_info.get("is_logged"));
        } catch (final Throwable e) {
            return false;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        /* 2021-01-20: Doesn't work */
        // if (account.getType() == AccountType.PREMIUM) {
        // /* Try to find expire-date if we know that this is a premium account */
        // try {
        // br.getHeaders().put("Referer", URL_BASE_PREMIUM + "/account/premium");
        // /* Make sure it is set to English as the pattern below only works for the english version of their website. */
        // br.getPage(URL_BASE_PREMIUM + "/change-language/en");
        // br.getPage("/account/premium?language=en");
        // final String expireDate = br.getRegex("Activated until ([A-Za-z]+ \\d{1,2}, \\d{4})").getMatch(0);
        // if (expireDate != null) {
        // ai.setValidUntil(TimeFormatter.getMilliSeconds(expireDate, "MMM, dd, yyyy", Locale.ENGLISH), br);
        // }
        // } catch (final Throwable e) {
        // }
        // }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        // login(account, false);
        requestFileInformation(link, account, true);
        handleDownload(link, account);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        /* 2020-10-05: No captchas at all */
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public String getAGBLink() {
        return null;
    }

    @Override
    public Class<? extends XvideosComConfigCore> getConfigInterface() {
        return null;
    }
}