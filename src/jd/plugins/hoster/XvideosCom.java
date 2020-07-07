//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.util.ArrayList;
import java.util.List;

import jd.PluginWrapper;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.downloader.hls.M3U8Playlist;
import org.jdownloader.plugins.components.config.XvideosComConfig;
import org.jdownloader.plugins.components.config.XvideosComConfig.PreferredHLSQuality;
import org.jdownloader.plugins.components.config.XvideosComConfig.PreferredHTTPQuality;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.config.PluginJsonConfig;

//xvideos.com by pspzockerscene
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class XvideosCom extends PluginForHost {
    public XvideosCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "xvideos.com", "xvideos.es", "xvideos2.com", "xvideos2.es", "xvideos3.com", "xvideos3.es" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(video\\d+/.*|embedframe/\\d+|[a-z0-9\\-]+/(upload|pornstar|model)/[a-z0-9\\-_]+/\\d+/(\\d+)?)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getVideoID(link);
        if (linkid != null) {
            return getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getVideoID(final DownloadLink link) {
        final String url = link.getPluginPatternMatcher();
        if (url != null) {
            String linkid = new Regex(url, "/(?:video|embedframe/)(\\d+)").getMatch(0);
            if (linkid == null) {
                linkid = new Regex(url, "[^/]+/[a-z0-9\\-_]+/(\\d+)$").getMatch(0);
            }
            return linkid;
        } else {
            return null;
        }
    }

    @Override
    public String getAGBLink() {
        return "http://info.xvideos.com/legal/tos/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private static final String type_normal  = "https?://[^/]+/video[0-9]+/.*";
    private static final String type_embed   = "https?://[^/]+/embedframe/\\d+";
    private static final String type_special = "https?://[^/]+/([a-z0-9\\-\\_]+/upload/[a-z0-9\\-]+/\\d+|prof\\-video\\-click/(upload|pornstar)/[a-z0-9\\-\\_]+/\\d+)";
    private static final String NOCHUNKS     = "NOCHUNKS";
    private String              streamURL    = null;
    private HlsContainer        hlsContainer = null;

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        if (link.getPluginPatternMatcher().matches(type_embed) || link.getPluginPatternMatcher().matches(type_special)) {
            link.setUrlDownload(new Regex(link.getPluginPatternMatcher(), "https?://[^/]+").getMatch(-1) + "/video" + new Regex(link.getPluginPatternMatcher(), "(\\d+)$").getMatch(0) + "/");
            link.setContentUrl(link.getPluginPatternMatcher());
        }
        link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
    }

    private boolean isValidVideoURL(final DownloadLink downloadLink, final String url) throws Exception {
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        URLConnectionAdapter con = null;
        try {
            Thread.sleep(2000);
            final Browser br2 = br.cloneBrowser();
            br2.setFollowRedirects(true);
            con = br2.openHeadConnection(Encoding.htmlOnlyDecode(url));
            if (StringUtils.containsIgnoreCase(con.getContentType(), "video") && con.getResponseCode() == 200) {
                downloadLink.setDownloadSize(con.getLongContentLength());
                return true;
            } else {
                return false;
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

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        link.setName(new Regex(link.getPluginPatternMatcher(), "https?://[^/]+/(.+)").getMatch(0));
        br.setFollowRedirects(false);
        br.getHeaders().put("Accept-Encoding", "gzip");
        br.getHeaders().put("Accept-Language", "en-gb");
        br.getPage(link.getPluginPatternMatcher());
        while (br.getRedirectLocation() != null) {
            final String redirect = br.getRedirectLocation();
            logger.info("Setting new downloadlink: " + redirect);
            /*
             * 2019-09-30: Only set new URL if it is valid. E.g. when using xvideos2.com (= for india) in germany, it will only redirect us
             * to their mainpage!
             */
            if (new Regex(redirect, this.getSupportedLinks()).matches()) {
                link.setPluginPatternMatcher(br.getRedirectLocation());
            }
            br.getPage(redirect);
        }
        if (br.containsHTML("(This video has been deleted|Page not found|>Sorry, this video is not available\\.|>We received a request to have this video deleted|class=\"inlineError\")") || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("\"video_title_ori\"\\s*:\\s*\"(.*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("\"video_title\"\\s*:\\s*\"(.*?)\"").getMatch(0);
        }
        if (filename != null) {
            filename = Encoding.unicodeDecode(filename);
            filename = Encoding.htmlDecode(filename);
        }
        if (filename == null) {
            filename = br.getRegex("<h2>([^<>\"]*?)<span class").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>([^<>\"]*?)\\- XVIDEOS\\.COM</title>").getMatch(0);
            }
            filename = Encoding.htmlDecode(filename);
        }
        {
            /* Set packagizer property */
            final String uploadername = PluginJSonUtils.getJson(br, "uploader");
            if (StringUtils.isEmpty(link.getStringProperty("username", null)) && !StringUtils.isEmpty(uploadername)) {
                link.setProperty("username", uploadername);
            }
        }
        final String videoID = getVideoID(link);
        if (videoID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (filename == null) {
            /* Fallback */
            filename = videoID;
        } else {
            filename = videoID + "_" + filename;
        }
        if (PluginJsonConfig.get(XvideosComConfig.class).isPreferHLSDownload()) {
            final String hlsURL = getVideoHLS();
            if (StringUtils.isNotEmpty(hlsURL)) {
                final Browser m3u8 = br.cloneBrowser();
                m3u8.getPage(hlsURL);
                final int preferredHLSQuality = getPreferredHLSQuality();
                HlsContainer selectedQuality = null;
                final List<HlsContainer> hlsqualities = HlsContainer.getHlsQualities(m3u8);
                for (final HlsContainer currentQuality : hlsqualities) {
                    final int width = currentQuality.getHeight();
                    if (width == preferredHLSQuality) {
                        logger.info("Found user selected HLS quality: " + preferredHLSQuality);
                        selectedQuality = currentQuality;
                        break;
                    }
                }
                if (selectedQuality == null) {
                    logger.info("Failed to find user-selected HLS quality --> Fallback to BEST");
                    selectedQuality = HlsContainer.findBestVideoByBandwidth(hlsqualities);
                }
                if (selectedQuality != null) {
                    if (Thread.currentThread() instanceof SingleDownloadController) {
                        this.hlsContainer = selectedQuality;
                    }
                    final List<M3U8Playlist> playLists = M3U8Playlist.loadM3U8(selectedQuality.getDownloadurl(), m3u8);
                    long estimatedSize = -1;
                    for (M3U8Playlist playList : playLists) {
                        if (selectedQuality.getBandwidth() > 0) {
                            playList.setAverageBandwidth(selectedQuality.getBandwidth());
                            estimatedSize += playList.getEstimatedSize();
                        }
                    }
                    if (estimatedSize > 0) {
                        link.setDownloadSize(estimatedSize);
                    }
                    filename = filename.trim() + ".mp4";
                    link.setFinalFileName(filename);
                    return AvailableStatus.TRUE;
                }
            }
        }
        String videoURL = null;
        final PreferredHTTPQuality qualityhttp = getPreferredHTTPQuality();
        if (qualityhttp == PreferredHTTPQuality.HIGH) {
            videoURL = getVideoHigh();
        } else {
            videoURL = getVideoLow();
        }
        if (videoURL == null) {
            /* Fallback / try to find BEST */
            logger.info("Failed to find selected http quality");
            videoURL = getVideoHigh();
            if (!isValidVideoURL(link, videoURL)) {
                videoURL = getVideoLow();
                if (!isValidVideoURL(link, videoURL)) {
                    videoURL = getVideoFlv();
                    if (!isValidVideoURL(link, videoURL)) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                }
            }
        }
        videoURL = Encoding.htmlOnlyDecode(videoURL);
        if (Thread.currentThread() instanceof SingleDownloadController) {
            streamURL = videoURL;
        }
        filename = filename.trim() + getFileNameExtensionFromString(videoURL, ".mp4");
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    private String getVideoHLS() {
        return br.getRegex("html5player\\.setVideoHLS\\('(.*?)'\\)").getMatch(0);
    }

    private String getVideoFlv() {
        final String dllink = br.getRegex("flv_url=(.*?)\\&").getMatch(0);
        if (dllink == null) {
            return decode(br.getRegex("encoded=(.*?)\\&").getMatch(0));
        } else {
            return dllink;
        }
    }

    private String getVideoHigh() {
        return br.getRegex("html5player\\.setVideoUrlHigh\\('(.*?)'\\)").getMatch(0);
    }

    private String getVideoLow() {
        return br.getRegex("html5player\\.setVideoUrlLow\\('(.*?)'\\)").getMatch(0);
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (streamURL != null) {
            int chunks = 0;
            if (link.getBooleanProperty(XvideosCom.NOCHUNKS, false)) {
                chunks = 1;
            }
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, streamURL, true, chunks);
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(XvideosCom.NOCHUNKS, false) == false) {
                    link.setProperty(XvideosCom.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } else if (hlsContainer != null) {
            final String m3u8 = hlsContainer.getDownloadurl();
            checkFFmpeg(link, "Download a HLS Stream");
            sleep(2000, link);
            dl = new HLSDownloader(link, br, m3u8);
            dl.startDownload();
        } else {
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
        return PluginJsonConfig.get(XvideosComConfig.class).getPreferredHTTPQuality();
    }

    private int getPreferredHLSQuality() {
        PreferredHLSQuality preferredHLSQuality = PluginJsonConfig.get(XvideosComConfig.class).getPreferredHLSQuality();
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
            return -1;
        }
    }

    @Override
    public Class<XvideosComConfig> getConfigInterface() {
        return XvideosComConfig.class;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}