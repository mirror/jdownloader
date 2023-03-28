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
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.appwork.utils.StringUtils;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.config.UdemyComConfig;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.downloadcontroller.SingleDownloadController;
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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "udemy.com" }, urls = { "https?://(?:www\\.)?udemydecrypted\\.com/(.+\\?dtcode=[A-Za-z0-9]+|lecture_id/\\d+)" })
public class UdemyCom extends PluginForHost {
    public UdemyCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.udemy.com/courses/");
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:
    /* Extension which will be used if no correct extension is found */
    private static final String  default_Extension                   = ".mp4";
    /* Connection stuff */
    private static final boolean FREE_RESUME                         = true;
    private static final int     FREE_MAXCHUNKS                      = 0;
    private static final int     FREE_MAXDOWNLOADS                   = 20;
    private String               dllink                              = null;
    private boolean              textAssetType                       = false;
    public static final String   TYPE_SINGLE_PREMIUM_WEBSITE         = "(.+/lecture/\\d+).*";
    public static final String   TYPE_SINGLE_PREMIUM_DECRYPRED       = ".+/lecture_id/\\d+$";
    private static final String  PROPERTY_IS_OFFICIALLY_DOWNLOADABLE = "is_officially_downloadable";
    private static final String  PROPERTY_IS_DRM_PROTECTED           = "is_drm_protected";

    @Override
    public String getAGBLink() {
        return "https://www.udemy.com/terms/";
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("udemydecrypted.com/", "udemy.com/"));
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        if (link.getPluginPatternMatcher().matches(TYPE_SINGLE_PREMIUM_WEBSITE)) {
            /* Some errorhandling for old urls. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dllink = null;
        textAssetType = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String filename = link.getStringProperty("filename_decrypter", null);
        String url_embed = null;
        boolean loggedin = false;
        String description = null;
        final String asset_id = new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0);
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            try {
                login(aa, false);
                loggedin = true;
            } catch (final Exception e) {
                /* Throw Exception if we're in download mode. */
                if (Thread.currentThread() instanceof SingleDownloadController) {
                    throw e;
                } else {
                    logger.log(e);
                }
            }
        }
        String ext = null;
        String asset_type = link.getStringProperty("asset_type", "Video");
        final String lecture_id = link.getStringProperty("lecture_id", null);
        if (!loggedin && isAccountRequired(link)) {
            link.setName(asset_id);
            link.getLinkStatus().setStatusText("Cannot check this url without account");
            return AvailableStatus.TRUE;
        } else if (link.getPluginPatternMatcher().matches(TYPE_SINGLE_PREMIUM_DECRYPRED)) {
            if (!link.isNameSet()) {
                link.setName(asset_id);
            }
            final String courseid = link.getStringProperty("course_id", null);
            if (courseid == null) {
                /* This should never happen! */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /* Prepare the API-Headers to get the videourl */
            prepBRAPI(this.br);
            if (asset_type.equalsIgnoreCase("File")) {
                /* Download File (usually .jpg pictures). */
                this.br.getPage("https://www." + this.getHost() + "/api-2.0/users/me/subscribed-courses/" + courseid + "/lectures/" + lecture_id + "/supplementary-assets/" + asset_id + "?fields%5Basset%5D=download_urls,stream_urls");
                dllink = PluginJSonUtils.getJsonValue(this.br, "file");
            } else if (asset_type.equalsIgnoreCase("Article")) {
                this.br.getPage("https://www." + this.getHost() + "/api-2.0/assets/" + asset_id + "/?fields[asset]=@min,status,delayed_asset_message,processing_errors,body");
                textAssetType = true;
                ext = ".html";
            } else {
                /* Download Video/Article/PDF. */
                /*
                 * 2016-04-08: Changed parameters - parameters before:
                 * ?video_only=&auto_play=&fields%5Blecture%5D=asset%2Cembed_url&fields%5Basset
                 * %5D=asset_type%2Cdownload_urls%2Ctitle&instructorPreviewMode=False
                 */
                /*
                 * 2020-08-07: Changed parameters - request before: "https://www.udemy.com/api-2.0/users/me/subscribed-courses/" + courseid
                 * + "/lectures/" + lecture_id +
                 * "?fields%5Basset%5D=@min,download_urls,stream_urls,external_url,slide_urls&fields%5Bcourse%5D=id,is_paid,url&fields%5Blecture%5D=@default,view_html,course&page_config=ct_v4&tracking_tag=ctp_lecture"
                 */
                this.br.getPage("https://www." + this.getHost() + "/api-2.0/users/me/subscribed-courses/" + courseid + "/lectures/" + lecture_id + "?fields%5Basset%5D=@min,download_urls,stream_urls,external_url,slide_urls&fields%5Bcourse%5D=id,is_paid,url&fields%5Blecture%5D=asset,description,download_url,is_free,last_watched_second&fields[asset]=asset_type,length,media_license_token,course_is_drmed,media_sources,captions,thumbnail_sprite,slides,slide_urls,download_urls");
                if (this.br.getHttpConnection().getResponseCode() == 403) {
                    /* E.g. {"detail": "You do not have permission to perform this action."} */
                    /* User tries to download content which he did not buy/subscribe to. */
                    logger.info("You need to have an account with permission (e.g. you need to buy this content) to download this content");
                    throw new AccountRequiredException();
                } else if (br.getHttpConnection().getResponseCode() == 404) {
                    // this.br.getPage("https://www.udemy.com/api-2.0/lectures/" + fid_accountneeded +
                    // "/content?videoOnly=0&instructorPreviewMode=False");
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                final Map<String, Object> entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(this.br.toString());
                final String title_cleaned = (String) entries.get("title_cleaned");
                description = (String) entries.get("description");
                final Map<String, Object> asset = (Map<String, Object>) entries.get("asset");
                final boolean drm;
                if ((Boolean) asset.get("course_is_drmed") == Boolean.TRUE) {
                    /**
                     * Indicates that most of all courses' items are DRM protected but not necessary all of them! </br>
                     * Also course owners can enable official download functionality for their courses which would probably not change this
                     * boolean! </br>
                     * How to find non-DRM protected items: </br>
                     * 1. Open any course in your browser when logged in into any udemy.com account. </br>
                     * 2. Click on ""Preview this course" in the top right corner. </br>
                     * --> All items you can preview there (seems like random clips throughout the course) are streamable without DRM!
                     */
                    drm = true;
                    link.setProperty(PROPERTY_IS_DRM_PROTECTED, true);
                } else {
                    drm = false;
                    link.removeProperty(PROPERTY_IS_DRM_PROTECTED);
                }
                if (asset_type.equalsIgnoreCase("Article")) {
                    ext = ".txt";
                    textAssetType = true;
                } else {
                    if (!asset_type.equals("Video")) {
                        /* We assume it is a PDF. */
                        ext = ".pdf";
                    }
                }
                if (StringUtils.isEmpty(filename)) {
                    filename = (String) asset.get("title");
                }
                final Object download_urlsO = asset.get("download_urls");
                if (download_urlsO != null && download_urlsO instanceof Map) {
                    // DownloadURL
                    final List<Map<String, Object>> downloadList = (List<Map<String, Object>>) ((Map<String, Object>) download_urlsO).get(asset_type);
                    long maxQuality = -1;
                    final int preferredQualityWidth = getPreferredQualityWidth();
                    String bestQualityDownloadlink = null;
                    String userSelectedQualityDownloadurl = null;
                    for (final Map<String, Object> downloadEntry : downloadList) {
                        final long qualityTmp = JavaScriptEngineFactory.toLong(downloadEntry.get("label"), 0);
                        final String dllinkTmp = (String) downloadEntry.get("file");
                        if (StringUtils.isEmpty(dllinkTmp)) {
                            /* Skip invalid objects */
                            continue;
                        }
                        if (qualityTmp == preferredQualityWidth) {
                            userSelectedQualityDownloadurl = dllinkTmp;
                        }
                        if (qualityTmp > maxQuality) {
                            bestQualityDownloadlink = dllinkTmp;
                            maxQuality = qualityTmp;
                        }
                    }
                    if (userSelectedQualityDownloadurl != null) {
                        logger.info("Found user selected quality: " + preferredQualityWidth);
                        this.dllink = userSelectedQualityDownloadurl;
                    } else if (bestQualityDownloadlink != null) {
                        logger.info("Using BEST quality: " + maxQuality);
                        this.dllink = bestQualityDownloadlink;
                    }
                    if (!StringUtils.isEmpty(this.dllink)) {
                        link.setProperty(PROPERTY_IS_OFFICIALLY_DOWNLOADABLE, true);
                        logger.info("Found official Download:" + maxQuality + " | " + dllink);
                        final String filename_url = this.br.getRegex("response\\-content\\-disposition=attachment%3Bfilename=([^<>\"/\\\\]*)(mp4)?\\.mp4").getMatch(0);
                        if (StringUtils.isNotEmpty(filename_url)) {
                            filename = filename_url;
                        }
                    } else {
                        link.removeProperty(PROPERTY_IS_OFFICIALLY_DOWNLOADABLE);
                    }
                } else {
                    link.removeProperty(PROPERTY_IS_OFFICIALLY_DOWNLOADABLE);
                }
                final Object media_sourcesO = asset.get("media_sources");
                if (StringUtils.isEmpty(dllink) && media_sourcesO != null && media_sourcesO instanceof List) {
                    final List<Map<String, Object>> mediaList = (List<Map<String, Object>>) media_sourcesO;
                    /* First look for uncrypted HTTP URLs. */
                    long maxQuality = -1;
                    final int preferredQualityWidth = getPreferredQualityWidth();
                    String bestQualityDownloadlink = null;
                    String userSelectedQualityDownloadurl = null;
                    for (final Map<String, Object> mediaEntry : mediaList) {
                        final String src = (String) mediaEntry.get("src");
                        if ("video/mp4".equals(mediaEntry.get("type")) && !StringUtils.isEmpty(src)) {
                            final long qualityTmp = JavaScriptEngineFactory.toLong(mediaEntry.get("label"), 0);
                            if (qualityTmp == preferredQualityWidth) {
                                userSelectedQualityDownloadurl = src;
                            }
                            if (qualityTmp > maxQuality) {
                                bestQualityDownloadlink = src;
                                maxQuality = qualityTmp;
                            }
                        }
                    }
                    if (userSelectedQualityDownloadurl != null) {
                        logger.info("Found user selected quality: " + preferredQualityWidth);
                        this.dllink = userSelectedQualityDownloadurl;
                    } else if (bestQualityDownloadlink != null) {
                        logger.info("Using BEST quality: " + maxQuality);
                        this.dllink = bestQualityDownloadlink;
                    }
                    /*
                     * Check for valid HLS URLs. Most times those will be DRM protected so only get them if we know that the course is not
                     * DRM protected.
                     */
                    if (StringUtils.isEmpty(this.dllink)) {
                        for (final Map<String, Object> mediaEntry : mediaList) {
                            final String src = (String) mediaEntry.get("src");
                            if (!drm && "application/x-mpegURL".equals(mediaEntry.get("type")) && !StringUtils.isEmpty(src)) {
                                this.dllink = src;
                                break;
                            }
                        }
                    }
                    if (dllink != null) {
                        logger.info("Found usable URL in 'media_sources':" + dllink);
                    }
                }
                final Object stream_urlsO = asset.get("stream_urls");
                if (StringUtils.isEmpty(dllink) && stream_urlsO != null) {
                    /* Rare case: Uncrypted "streaming URLs" are available. */
                    final Map<String, Object> stream_urls = ((Map<String, Object>) stream_urlsO);
                    try {
                        final List<Map<String, Object>> videos = (List<Map<String, Object>>) stream_urls.get("Video");
                        if (videos != null && videos.size() > 0) {
                            long quality_best = -1;
                            for (final Map<String, Object> video : videos) {
                                final String file = (String) video.get("file");
                                final Object labelO = video.get("label");
                                final long quality;
                                if (labelO != null && labelO instanceof Number) {
                                    quality = ((Number) labelO).longValue();
                                } else if (labelO != null && labelO instanceof String) {
                                    /* E.g. '360p' or '360' */
                                    final String labelNumberStr = new Regex(labelO.toString(), "(\\d+)p?").getMatch(0);
                                    if (labelNumberStr == null) {
                                        /* E.g. "auto" */
                                        continue;
                                    }
                                    quality = Long.parseLong(labelNumberStr);
                                } else {
                                    quality = -1;
                                }
                                if (StringUtils.isEmpty(file) || quality == -1) {
                                    continue;
                                } else if (file.contains(".m3u8")) {
                                    /* Skip hls */
                                    continue;
                                }
                                if (quality > quality_best) {
                                    quality_best = quality;
                                    dllink = file;
                                }
                            }
                            if (quality_best > 0) {
                                logger.info("Found Stream:" + quality_best + "|" + dllink);
                            }
                        }
                    } catch (final Throwable e) {
                        logger.log(e);
                    }
                    /* json handling did not work or officially there are no downloadlinks --> Grab links from html inside json */
                    String json_view_html = (String) entries.get("view_html");
                    if (StringUtils.isEmpty(dllink) && !StringUtils.isEmpty(json_view_html)) {
                        /* 2022-01-12: Very old handling! Possibly not needed anymore! */
                        json_view_html = Encoding.unicodeDecode(json_view_html);
                        json_view_html = Encoding.htmlDecode(json_view_html);
                        final String jssource = new Regex(json_view_html, "sources\"\\s*?:\\s*?(\\[.*?\\])").getMatch(0);
                        if (jssource != null) {
                            /* 2017-04-24: New: json inside json - */
                            try {
                                long quality_best = -1;
                                final List<Map<String, Object>> videos = (List<Map<String, Object>>) JavaScriptEngineFactory.jsonToJavaObject(jssource);
                                for (final Map<String, Object> video : videos) {
                                    final String src = (String) video.get("src");
                                    final Object label = video.get("label");
                                    final long quality;
                                    if (label != null && label instanceof Number) {
                                        quality = ((Number) label).longValue();
                                    } else if (label != null && label instanceof String) {
                                        /* E.g. '360p' or '360' */
                                        quality = Long.parseLong(new Regex(label.toString(), "(\\d+)p?").getMatch(0));
                                    } else {
                                        quality = -1;
                                    }
                                    if (StringUtils.isEmpty(src) || quality == -1) {
                                        continue;
                                    } else if (src.contains(".m3u8")) {
                                        /* Skip hls */
                                        continue;
                                    }
                                    if (quality > quality_best) {
                                        quality_best = quality;
                                        dllink = src;
                                    }
                                }
                                if (quality_best > 0) {
                                    logger.info("Found JsonStream:" + quality_best + "|" + dllink);
                                }
                            } catch (final Throwable e) {
                                logger.log(e);
                            }
                        }
                    }
                    if (StringUtils.isEmpty(dllink)) {
                        /* Final fallback */
                        final String[] possibleQualities = { "HD", "SD", "1080", "720", "480", "360", "240", "144" };
                        for (final String possibleQuality : possibleQualities) {
                            dllink = new Regex(json_view_html, "<source src=\"(https?[^<>\"]+)\"[^>]+data\\-res=\"" + possibleQuality + "\" />").getMatch(0);
                            if (dllink != null) {
                                break;
                            }
                        }
                        if (dllink == null) {
                            /* Last chance -see if we can find ANY video-url */
                            dllink = new Regex(json_view_html, "\"(https?://udemy\\-assets\\-on\\-demand\\.udemy\\.com/[^<>\"]+\\.mp4[^<>\"]+)\"").getMatch(0);
                        }
                        if (dllink != null) {
                            /* Important! */
                            dllink = Encoding.htmlDecode(dllink);
                        }
                    }
                }
                if (filename == null) {
                    if (title_cleaned != null) {
                        filename = asset_id + "_" + title_cleaned;
                    } else {
                        filename = asset_id;
                    }
                }
            }
        } else {
            /* Old handling for free content */
            br.getPage(link.getPluginPatternMatcher());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.getURL().contains("/search/")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /* Normal (FREE) url */
            filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
            if (filename == null) {
                filename = new Regex(link.getDownloadURL(), "udemy\\.com/(.+)\\?dtcode=").getMatch(0);
            }
            url_embed = this.br.getRegex("(https?://(?:www\\.)?udemy\\.com/embed/video/[^<>\"]*?)\"").getMatch(0);
            if (url_embed == null || filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            this.br.getPage(url_embed);
            dllink = br.getRegex("\"file\":\"(https?[^<>\"]*?)\",\"label\":\"720p").getMatch(0);
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        if (ext == null && dllink != null && dllink.contains(".m3u8")) {
            ext = default_Extension;
        }
        if (ext == null) {
            ext = getFileNameExtensionFromString(dllink, default_Extension);
        }
        if (ext == null) {
            ext = default_Extension;
        }
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        link.setFinalFileName(filename);
        if (description != null && link.getComment() == null) {
            link.setComment(description);
        }
        if (StringUtils.startsWithCaseInsensitive(dllink, "http") && !dllink.contains(".m3u8")) {
            final Browser br2 = new Browser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown download error");
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    private boolean isAccountRequired(final DownloadLink link) {
        if (link.getPluginPatternMatcher().matches(TYPE_SINGLE_PREMIUM_DECRYPRED)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (this.isAccountRequired(link)) {
            throw new AccountRequiredException();
        } else {
            handleDownload(link);
        }
    }

    public void handleDownload(final DownloadLink link) throws Exception {
        if (textAssetType) {
            /* Download text/"Article" */
            /* Old json/field name */
            String html = PluginJSonUtils.getJsonValue(this.br, "view_html");
            if (StringUtils.isEmpty(html)) {
                /* 2020-08-07: New */
                html = PluginJSonUtils.getJsonValue(this.br, "body");
            }
            if (StringUtils.isEmpty(html)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* TODO: Maybe download nothing at all but write the found html directly into the file --> done. */
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, this.br.getURL(), FREE_RESUME, FREE_MAXCHUNKS);
            if (this.dl.startDownload()) {
                final File file_dest = new File(link.getFileOutput());
                final FileOutputStream fos = new FileOutputStream(file_dest);
                try {
                    fos.write(html.getBytes(Charset.forName("UTF-8")));
                } finally {
                    fos.close();
                }
            }
        } else {
            if (dllink == null) {
                drmCheck(link);
                if (!isOfficiallyDownloadable(link)) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Content might not be officially downloadable. Contact our support if you think this error message is wrong.");
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            /*
             * Remove old cookies and headers from Browser as they are not needed for their downloadurls in fact using them get you server
             * response 400.
             */
            this.br = new Browser();
            if (dllink.contains(".m3u8")) {
                drmCheck(link);
                /* 2016-08-23: HLS is preferred over http by their system */
                this.br.getPage(this.dllink);
                final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
                if (hlsbest == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String url_hls = hlsbest.getDownloadurl();
                checkFFmpeg(link, "Download a HLS Stream");
                dl = new HLSDownloader(link, br, url_hls);
                dl.startDownload();
            } else {
                if (this.isDrmProtected(link) && false) {
                    /* Do not check for DRM protected flag here as HTTP streams are never DRM protected. */
                    throw new PluginException(LinkStatus.ERROR_FATAL, "DRM protected");
                }
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, FREE_RESUME, FREE_MAXCHUNKS);
                if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                    br.followConnection(true);
                    if (dl.getConnection().getResponseCode() == 400) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                    } else if (dl.getConnection().getResponseCode() == 403) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                    } else if (dl.getConnection().getResponseCode() == 404) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Final downloadurl did not lead to downloadable content");
                    }
                }
                dl.startDownload();
            }
        }
    }

    /**
     * Throws Exception if content is DRM protected.
     *
     * @throws PluginException
     */
    private void drmCheck(final DownloadLink link) throws PluginException {
        if (this.isDrmProtected(link)) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "DRM protected");
        }
    }

    private boolean isOfficiallyDownloadable(final DownloadLink link) {
        if (link.hasProperty(PROPERTY_IS_OFFICIALLY_DOWNLOADABLE)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isDrmProtected(final DownloadLink link) {
        if (link.hasProperty(PROPERTY_IS_DRM_PROTECTED)) {
            return true;
        } else {
            return false;
        }
    }

    public void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                final Cookies userCookies = account.loadUserCookies();
                if (userCookies != null) {
                    /* 2021-02-03: Experimental */
                    logger.info("Checking user cookies");
                    br.setCookies(this.getHost(), userCookies);
                    if (!force) {
                        /* Do not check cookies */
                        return;
                    }
                    if (verifyCookies(account, userCookies)) {
                        logger.info("Successfully loggedin via user cookies");
                        /*
                         * User can enter anything in "username" field when logging in via cookies --> Try to get real username/email and
                         * set it so user cannot easily add the same cookies for the same account multiple times.
                         */
                        final String email = PluginJSonUtils.getJson(br, "email");
                        if (!StringUtils.isEmpty(email)) {
                            account.setUser(email);
                        }
                        return;
                    } else {
                        logger.info("Failed to login via user cookies");
                        if (account.hasEverBeenValid()) {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                        } else {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                        }
                    }
                }
                if (cookies != null) {
                    br.setCookies(this.getHost(), cookies);
                    if (!force) {
                        /* Do not check cookies */
                        return;
                    }
                    if (verifyCookies(account, cookies)) {
                        logger.info("Successfully loggedin via cookies");
                        account.saveCookies(br.getCookies(this.getHost()), "");
                        return;
                    } else {
                        logger.info("Failed to login via cookies");
                        br.clearAll();
                    }
                }
                logger.info("Performing full login");
                br.setFollowRedirects(true);
                br.getPage("https://www." + this.getHost() + "/join/login-popup/?displayType=ajax&display_type=popup&showSkipButton=1&returnUrlAfterLogin=https%3A%2F%2Fwww.udemy.com%2F&next=https%3A%2F%2Fwww.udemy.com%2F&locale=de_DE");
                final String csrftoken = br.getCookie(this.getHost(), "csrftoken");
                if (StringUtils.isEmpty(csrftoken)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String postData = "csrfmiddlewaretoken=" + csrftoken + "&locale=de_DE&email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&displayType=ajax";
                br.postPage("/join/login-popup/?displayType=ajax&display_type=popup&showSkipButton=1&returnUrlAfterLogin=https%3A%2F%2Fwww.udemy.com%2F&next=https%3A%2F%2Fwww.udemy.com%2F&locale=de_DE", postData);
                if (br.containsHTML("data-purpose=\"do-login\"")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    /** Sets given cookies and checks if we can login with them. */
    protected boolean verifyCookies(final Account account, final Cookies cookies) throws Exception {
        br.setCookies(this.getHost(), cookies);
        this.prepBRAPI(br);
        br.getPage("https://www." + this.getHost() + "/api-2.0/contexts/me/?header=True&footer=True");
        final String email = PluginJSonUtils.getJson(br, "email");
        if (!StringUtils.isEmpty(email)) {
            logger.info("Successfully logged in via cookies");
            return true;
        } else {
            logger.info("Cookie login failed");
            br.clearCookies(br.getHost());
            return false;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        account.setConcurrentUsePossible(true);
        account.setType(AccountType.PREMIUM);
        /* There is no separate free/premium - users can buy videos which will be available for their accounts afterwards. */
        ai.setStatus("Valid account");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        /* No need to log in - we're already logged in! */
        handleDownload(link);
    }

    public void prepBRAPI(final Browser br) {
        final String clientid = br.getCookie(this.getHost(), "client_id", Cookies.NOTDELETEDPATTERN);
        final String bearertoken = br.getCookie(this.getHost(), "access_token", Cookies.NOTDELETEDPATTERN);
        final String newrelicid = "XAcEWV5ADAEDUlhaDw==";
        if (clientid == null || bearertoken == null || newrelicid == null) {
            return;
        }
        br.getHeaders().put("X-NewRelic-ID", newrelicid);
        // this.br.getHeaders().put("X-Udemy-Client-Id", clientid);
        br.getHeaders().put("Authorization", "Bearer " + bearertoken);
        br.getHeaders().put("X-Udemy-Authorization", "Bearer " + bearertoken);
    }

    public static String getCourseIDFromHtml(final Browser br) {
        String courseid = br.getRegex("data\\-course\\-id=\"(\\d+)\"").getMatch(0);
        if (courseid == null) {
            courseid = br.getRegex("&quot;id&quot;:\\s*(\\d+)").getMatch(0);
        }
        return courseid;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private int getPreferredQualityWidth() {
        switch (PluginJsonConfig.get(UdemyComConfig.class).getPreferredDownloadQuality()) {
        case Q144P:
            return 144;
        case Q360P:
            return 360;
        case Q480P:
            return 480;
        case Q720P:
            return 720;
        case Q1080P:
            return 1080;
        case Q2160P:
            return 2160;
        default:
            return 2160;
        }
    }

    @Override
    public Class<? extends UdemyComConfig> getConfigInterface() {
        return UdemyComConfig.class;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
