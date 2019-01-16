//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mtv.com" }, urls = { "http://viacommgid/mgid:.+" })
public class VivaTv extends PluginForHost {
    public VivaTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.viva.tv/agb";
    }

    /* Important data */
    public static final String   url_service_feed_api_mtvnn_v2                       = "http://api.mtvnn.com/v2/mrss.xml?uri=%s";
    public static final String   url_service_feed_mtvnservices                       = "http://media.mtvnservices.com/video/feed.jhtml?ref=None&type=error&uri=%s&geo=DE&orig=&franchise=&dist=";
    public static final String   url_service_feed_intl_mtvnservices                  = "http://intl.mtvnservices.com/mrss/%s";
    public static final String   url_service_feed_COMEDYCENTRAL                      = "http://www.cc.com/feeds/mrss?uri=%s";
    public static final String   url_service_feed_SOUTHPARKSTUDIOS                   = "http://southpark.cc.com/feeds/video-player/mrss/%s";
    public static final String   url_service_feed_NICK_COM                           = "http://www.nick.com/dynamo/video/data/mrssGen.jhtml?mgid=%s";
    public static final String   url_service_feed_ESPERANTO                          = "http://intl.esperanto.mtvi.com/www/xml/video.jhtml?uri=%s";
    public static final String   url_service_feed_ESPERANTO_AS3                      = "http://intl.esperanto.mtvi.com/www/xml/video.jhtml?uri=%s&version=as3";
    public static final String   url_service_mediagen_mtvnservices_device            = "http://intl.mtvnservices.com/mediagen/%s/?device={device}";
    public static final String   url_service_mediagen_intl_mtvnservices              = "http://intl.mtvnservices.com/mediagen/%s/";
    public static final String   url_service_mediagen_mediautils_mtvnservices_device = "http://media-utils.mtvnservices.com/services/MediaGenerator/%s?device={device}";
    public static final String   url_service_mediagen_ESPERANTO                      = "http://intl.esperanto.mtvi.com/www/xml/media/mediaGen.jhtml?uri=%s";
    /*
     * E.g. json-version:
     * http://media-utils.mtvnservices.com/services/MediaGenerator/mgid:arc:episode:comedycentral.com:0e9587e2-d682-4c1d-a20c
     * -7e14e868ed59?device=iPad&context=mgid:arc:episode:comedycentral.com:c99b887e-5162-4c75-a691-04d1fc1c916f&format=json
     */
    public static final String   url_service_mediagen_media_utils_api                = "http://media-utils-api.mtvnservices.com/services/MediaGenerator/%s";
    public static final String   url_service_mediagen_mtv_com                        = "http://www.mtv.com/meta/context/mediaGen?uri=%s";
    public static final String   url_service_feed_mtv_com                            = "http://www.mtv.com/player/embed/AS3/rss/?uri=%s&ref=None";
    /**
     * NOT using mtv networks for streaming: bet.com<br />
     * NOT important/contains no(important) content: epixhd.com, centrictv.com, unplugged.mtvla.com<br />
     * Sites that did not work serverside: nick.com, nickjr.com<br />
     * Implementation not (yet) possible because of geoblock (germany): mtvla.com, mtv.ca<br />
     * Description of possible parameters in feed-urls:<br />
     * Possible values for "version": "as3"<br />
     */
    /**
     * EVERY MTV project has mgid strings! Core of this is either a (6-7 digits?) long ID or a hash-like id e.g. xxx-yyy-ggg-hhh-ttt. Only
     * mgids with the short IDs can be embedded into other websites (as far as I've seen it).
     */
    /**
     * About feeds: Feeds are the comon way to get the video urls. Each MTV site has different feed-URLs. The easiest way to find them is
     * accessing the "players" page e.g.:
     * http://media.mtvnservices.com/pmt-arc/e1/players/mgid:arc:video:mtvmovies.com:/context16/context2/config
     * .xml?uri=<MGID_GOES_HERE>&type=network&ref=movies.mtv.de&geo=DE&group=intl&network=None&device=Othe. Feed url will be in the <feed>
     * XML tag.
     */
    /** Note: There might also be a way to get mobile (http) links, see YT-dl project. */
    /** Tags: Viacom International Media Networks Northern Europe, mrss */
    /** Additional thanks goes to: https://github.com/rg3/youtube-dl/blob/master/youtube_dl/extractor/mtv.py */
    /* Plugin related things */
    private static final String  player_url                                          = "http://player.mtvnn.com/g2/g2player_2.2.1.swf";
    /* Obey german law - very important! */
    private static final boolean rtmpe_supported                                     = false;
    public static final String   default_ext                                         = ".mp4";
    private String               mgid                                                = null;
    private String               feed_url                                            = null;
    private String               mediagen_url                                        = null;

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        String user_added_url = link.getDownloadURL();
        /* Correct decrypted links */
        user_added_url = user_added_url.replace("_jd_decrypted_jd_", "");
        /* Usually they prefer http anyways */
        user_added_url = user_added_url.replace("https://", "http://");
        link.setUrlDownload(user_added_url);
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        String filename = link.getStringProperty("decryptedfilename", null);
        String ext = null;
        String description = null;
        this.setBrowserExclusive();
        prepBR(this.br);
        // final String main_url = link.getStringProperty("mainlink", null);
        this.mgid = getMGIDOutOfURL(link.getDownloadURL());
        this.feed_url = mgidGetFeedurlForMgid(this.mgid);
        this.mediagen_url = mgidGetMediagenurlForMgid(this.mgid);
        if (this.feed_url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (filename == null) {
            if (this.mgid.contains("nick")) {
                /* Very strange but this is needed for all "nick" websites */
                this.br.setCookie("nick.com", "Visited", "Yes");
            }
            /* Maybe filename was set in decypter already --> No reason to access feed here! */
            br.getPage(this.feed_url);
            final int responsecode = this.br.getHttpConnection().getResponseCode();
            if (responsecode == 404 || responsecode == 500 || this.br.toString().length() < 300) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = feedGetFilename();
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        description = feedGetDescription();
        ext = default_ext;
        filename = doFilenameEncoding(this, filename);
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        link.setFinalFileName(filename);
        if (description != null && description.length() > 20) {
            description = doEncoding(description);
            try {
                link.setComment(description);
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        downloadLink.setProperty("page_url", br.getURL());
        /*
         * In case we got embedded links, we are already on the feed_url. Find- and access it if still needed.
         */
        if (feed_url == null && mediagen_url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // br.getHeaders().put("Referer", player_url);
            // br.getPage(feed_url);
        }
        if (mediagen_url == null) {
            /* Find- and access mediagen. */
            mediagen_url = feedGetMediagenURL();
            if (mediagen_url == null) {
                /* Check if maybe we just got a nearly empty response --> Video won't load in the browser either! */
                if (br.containsHTML("<rss xmlns:media=")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error - video offline?");
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /**
             * Sometimes there are spaces at the end of the link, undefined parameters or it's simply url encoded though we need it plain
             * --> This can lead to 404 errors! --> This function takes care of these problems.
             */
            mediagen_url = Encoding.htmlDecode(mediagen_url);
            mediagen_url = mediagen_url.trim();
        }
        br.getHeaders().put("Referer", player_url);
        br.getPage(mediagen_url);
        if (br.getHttpConnection().getResponseCode() == 404) {
            /* Video temporarily or forever offline */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 3 * 60 * 60 * 1000l);
        } else if (br.containsHTML("status=\"esiblocked\"") || br.containsHTML("/error_country_block") || br.containsHTML(">Sorry, content is not available for your country\\.") || br.containsHTML("copyright_error\\.flv") || br.containsHTML(">Copyrights restrict us from playing this video in your country") || br.containsHTML("errorslates/video_error")) {
            /* Geo block */
            throw new PluginException(LinkStatus.ERROR_FATAL, "This video is not available from your location");
        } else if (br.containsHTML(">Sorry, this video is not found or no longer available due to date or rights restrictions")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Video doesn't exist anymore (?)", 60 * 60 * 1000l);
        } else if (br.containsHTML("Sorry, we're unable to play this video")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'Sorry, we're unable to play this video'", 3 * 60 * 60 * 1000l);
        }
        final boolean isJson = br.toString().startsWith("{");
        String src_url = null;
        if (isJson) {
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "package/video/item/{0}/rendition/{0}");
            src_url = (String) entries.get("src");
        } else {
            /* Chose highest quality available */
            final String[] srcs = br.getRegex("([a-z]+://[^<>\"]*?)</src>").getColumn(0);
            if (srcs == null || srcs.length == 0) {
                /* Very very rare case! */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 1 * 60 * 60 * 1000l);
            }
            /* Now get the best quality (highest width) */
            int best_width = 0;
            int tempwidth = 0;
            for (final String tmpsrc : srcs) {
                final String width = new Regex(tmpsrc, "_(\\d+)x\\d+_").getMatch(0);
                if (width != null) {
                    tempwidth = Integer.parseInt(width);
                    if (tempwidth > best_width) {
                        best_width = tempwidth;
                        src_url = tmpsrc;
                    }
                }
            }
            /* No width given? Grab the last element of the array - if this is not the best resolution, improve the upper function. */
            if (src_url != null) {
                logger.info("Found BEST downloadlink");
            } else {
                logger.info("Failed to find BEST downloadlink");
                src_url = srcs[srcs.length - 1];
            }
        }
        String httpurl = null;
        String hlsurl = null;
        if (src_url.contains(".m3u8")) {
            br.getPage(src_url);
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
            hlsurl = hlsbest.getDownloadurl();
        } else if (src_url.startsWith("http")) {
            /* In very rare cases we already have http urls */
            httpurl = src_url;
        } else {
            /* Prefer http - try to convert the rtmp(e) urls to http urls --> Works in about 50% of all cases! */
            httpurl = convertRTMPtoHTTP(src_url);
        }
        if (httpurl != null) {
            downloadHTTP(downloadLink, httpurl);
        } else if (hlsurl != null) {
            downloadHLS(downloadLink, hlsurl);
        } else {
            downloadRTMP(downloadLink, src_url);
        }
    }

    private void downloadHTTP(final DownloadLink downloadLink, final String http_src) throws Exception {
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, http_src, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void downloadHLS(final DownloadLink downloadLink, final String hls_src) throws Exception {
        checkFFmpeg(downloadLink, "Download a HLS Stream");
        dl = new HLSDownloader(downloadLink, br, hls_src);
        dl.startDownload();
    }

    private void downloadRTMP(final DownloadLink downloadLink, String rtmp_src) throws Exception {
        if (!rtmpe_supported) {
            /* Works in most cases. */
            rtmp_src = rtmp_src.replace("rtmpe://", "rtmp://");
        }
        final String ext = rtmp_src.substring(rtmp_src.lastIndexOf(".") + 1);
        String app;
        String rtmphost;
        String playpath;
        String swfurl;
        // if (downloadLink.getDownloadURL().matches(type_mtviggy)) {
        // /* Usually the convertRTMPtoHTTP will work and this code is never executed. */
        // /* Original swfurl contains a lof more info but it's not needed! */
        // swfurl = "http://media.mtvnservices.com/player/prime/mediaplayerprime.2.5.3.swf?uri=" + mgid;
        // app = "viacommtvstrm";
        // rtmphost = "rtmpe://viacommtvstrmfs.fplive.net:1935/viacommtvstrm";
        // playpath = new Regex(rtmp_src, "/(gsp\\..+)").getMatch(0);
        // if (playpath == null) {
        // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // }
        // if (!rtmpe_supported) {
        // throw new PluginException(LinkStatus.ERROR_FATAL, "rtmpe:// not supported!");
        // }
        // rtmp_src = rtmp_src.replace("rtmp://", "rtmpe://");
        // }
        //
        swfurl = "http://player.mtvnn.com/g2/g2player_2.2.1.swf";
        app = new Regex(rtmp_src, "(ondemand/(?:(mtviestor|riptide)/)?)").getMatch(0);
        final Regex host_app = new Regex(rtmp_src, "(rtmp://[^/]*?/)ondemand/(.+)");
        rtmphost = host_app.getMatch(0);
        playpath = new Regex(rtmp_src, app + "(.+)").getMatch(0);
        if (rtmphost == null || playpath == null || app == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        rtmphost += app;
        playpath = ext + ":" + playpath;
        /* Small fix for serverside wrong rtmp urls */
        playpath = playpath.replace("_od_flv.flv", "_od_flv");
        try {
            dl = new RTMPDownload(this, downloadLink, rtmphost);
        } catch (final NoClassDefFoundError e) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "RTMPDownload class missing");
        }
        /* Setup rtmp connection */
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        rtmp.setPageUrl(downloadLink.getStringProperty("page_url", null));
        rtmp.setUrl(rtmp_src);
        rtmp.setApp(app);
        rtmp.setPlayPath(playpath);
        /* Make sure we're using the correct protocol! */
        if (!rtmpe_supported) {
            rtmp.setProtocol(0);
        }
        rtmp.setFlashVer("WIN 16,0,0,305");
        rtmp.setSwfVfy(swfurl);
        /* Our rtmp resuming isn't the best plus we got a lot of different servers so better disable resume to prevent errors. */
        rtmp.setResume(false);
        ((RTMPDownload) dl).startDownload();
    }

    public static Browser prepBR(final Browser br) {
        br.setAllowedResponseCodes(500);
        br.setFollowRedirects(true);
        return br;
    }

    private String feedGetTitle() {
        return feedGetTitle(this.br.toString());
    }

    public static String feedGetTitle(final String source) {
        String title = new Regex(source, "<media:title><\\!\\[CDATA\\[([^<>]*?)\\]\\]></media:title>").getMatch(0);
        if (title == null) {
            title = new Regex(source, "<media:title>([^<>]*?)</media:title>").getMatch(0);
        }
        if (title == null) {
            title = new Regex(source, "<title><\\!\\[CDATA\\[([^<>]*?)\\]\\]></title>").getMatch(0);
        }
        title = new Regex(source, "<title><\\!\\[CDATA\\[([^<>]*?)\\]\\]></title>").getMatch(0);
        if (title == null) {
            title = new Regex(source, "<media:title>([^<>]*?)</media:title>").getMatch(0);
        }
        if (title == null) {
            title = new Regex(source, "<title>([^<>]*?)</title>").getMatch(0);
        }
        return title;
    }

    private String feedGetFilename() {
        return feedGetFilename(this.br.toString());
    }

    private String feedGetFilename(final String source) {
        String filename = null;
        final String artist = new Regex(source, "<media:category scheme=\\'urn:mtvn:artist\\'>([^<>\"]*?)</media:category>").getMatch(0);
        final String album = new Regex(source, "<media:category scheme=\\'urn:mtvn:album\\'>([^<>\"]*?)</media:category>").getMatch(0);
        if (artist != null && album != null) {
            filename = artist + " - " + album;
        } else {
            filename = this.feedGetTitle();
        }
        if (filename != null) {
            filename = doFilenameEncoding(this, filename);
        }
        return filename;
    }

    private String feedGetDescription() {
        return feedGetDescription(this.br.toString());
    }

    public static String feedGetDescription(final String source) {
        String description = new Regex(source, "<media:description>[\t\n\r ]*?<\\!\\[CDATA\\[(.*?)\\]\\]>[\t\n\r ]*?</media:description>").getMatch(0);
        if (description == null) {
            description = new Regex(source, "<media:description>(.*?)</media:description>").getMatch(0);
        }
        return description;
    }

    private String feedGetMediagenURL() {
        String flvgen_url = br.getRegex("(https?://[^<>\"]*?/www/xml/flv/flvgen\\.jhtml\\?[^<>\"]*?hiLoPref=hi)").getMatch(0);
        if (flvgen_url == null) {
            flvgen_url = br.getRegex("(https?://[^<>\"]*?/www/xml/flv/flvgen\\.jhtml\\?[^<>\"]*?hiLoPref=lo)").getMatch(0);
        }
        if (flvgen_url != null) {
            flvgen_url = Encoding.htmlDecode(flvgen_url);
        }
        String mediagen_url = br.getRegex("(?:\\'|\")(https?://[^<>\"]*?mediaGen[^<>\"]*?)(?:\\'|\")").getMatch(0);
        if (mediagen_url == null) {
            mediagen_url = br.getRegex("(?:\\'|\")(https?://[^<>\"]*?/mediagen/[^<>\"/]*?)(?:\\'|\")").getMatch(0);
        }
        /* flvgen - special cases e.g. for uk.viva.tv and gameone.de */
        if (mediagen_url == null) {
            mediagen_url = flvgen_url;
        }
        return mediagen_url;
    }

    public static String getMGIDOutOfURL(final String url) {
        String mgid = null;
        if (url != null) {
            mgid = new Regex(url, "(mgid:[A-Za-z0-9_\\.\\-:]+)").getMatch(0);
        }
        return mgid;
    }

    /** Converts rtmp urls to http urls */
    private String convertRTMPtoHTTP(final String rtmpurl) {
        String httpurl = null;
        /* Small information from the gameone.de plugin */
        // startUrl = startUrl.replaceAll("media/mediaGen\\.jhtml\\?uri.*?\\.de:", "flv/flvgen.jhtml?vid=");
        /*
         * E.g.rtmp(e): rtmpe://viacomccstrmfs.fplive.net/viacomccstrm/gsp.comedystor/com/dailyshow/TDS/Season_21/21031/
         * ds_21_031_act1_55d2de0e05_512x288_750_m30.mp4
         */
        /*
         * E.g. hls:
         * https://cp112366-f.akamaihd.net/i/mtvnorigin/gsp.comedystor/com/dailyshow/TDS/Season_21/21031/ds_21_031_act1_55d2de0e05_
         * ,384x216_200_b30
         * ,384x216_400_m30,512x288_750_m30,640x360_1200_m30,768x432_1700_m30,960x540_2200_m31,1280x720_3500_h32,.mp4.csmil/master
         * .m3u8?hdnea=st%3D1449327840%
         * 7Eexp%3D1449342240%7Eacl%3D%2Fi%2Fmtvnorigin%2Fgsp.comedystor%2Fcom%2Fdailyshow%2FTDS%2FSeason_21%2F21031%
         * 2Fds_21_031_act1_55d2de0e05_%2C384x216_200_b30%2C384x216_400_m30%2C512x288_750_m30%2C640x360_1200_m30%2C768x432_1700_m30%
         * 2C960x540_2200_m31%2C1280x720_3500_h32%2C.mp4.csmil%2F
         * *%7Ehmac%3D2e08c5d2409aab4aa8db4b9a9954607e133885d070f8ab6b994c8caa8c7342d7&__a__=off&__b__=450&__viacc__=NONE
         */
        /*
         * E.g. http:
         * http://viacommtvstrmfs.fplive.net/gsp.comedystor/com/dailyshow/TDS/Season_21/21031/ds_21_031_act1_55d2de0e05_512x288_750_m30.mp4
         */
        final String important_part = new Regex(rtmpurl, "rtmpe?://[^<>\"]+/(gsp\\..+)").getMatch(0);
        if (important_part != null) {
            /* Most times used for mtviggy.com */
            /* Also possible: http://a[1-20].akadl.mtvnservices.com/ depending on server/link structure */
            httpurl = "http://viacommtvstrmfs.fplive.net/" + important_part;
        } else if (rtmpurl.matches("rtmpe?://cp\\d+\\.edgefcs\\.net/ondemand/riptide/r2/.+")) {
            /* Most times used for gameone.de */
            /* Using (these particular) http urls will sometimes lead to 403/404 server errors. */
            httpurl = rtmpurl.replaceAll("^.*?/r2/", "http://cdn.riptide-mtvn.com/r2/");
        }
        return httpurl;
    }

    public static String doEncoding(String data) {
        data = Encoding.htmlDecode(data).trim();
        data = HTMLEntities.unhtmlentities(data);
        data = HTMLEntities.unhtmlAmpersand(data);
        data = HTMLEntities.unhtmlAngleBrackets(data);
        data = HTMLEntities.unhtmlSingleQuotes(data);
        data = HTMLEntities.unhtmlDoubleQuotes(data);
        data = data.replaceAll("&apos;", "'");
        return data;
    }

    public static String doFilenameEncoding(final Plugin plugin, String filename) {
        filename = Encoding.htmlDecode(filename).trim();
        filename = plugin.encodeUnicode(filename);
        filename = HTMLEntities.unhtmlentities(filename);
        filename = HTMLEntities.unhtmlAmpersand(filename);
        filename = HTMLEntities.unhtmlAngleBrackets(filename);
        filename = HTMLEntities.unhtmlSingleQuotes(filename);
        filename = HTMLEntities.unhtmlDoubleQuotes(filename);
        filename = filename.replaceAll("&apos;", "'");
        return filename;
    }

    /** Checks if a feed is online. */
    private void checkFeedAvailibility() throws PluginException {
        /* Check for invalid XML --> Video must be offline then */
        if (!br.containsHTML("</channel>") || br.getHttpConnection().getResponseCode() == 404 || (feedGetTitle() == null && br.containsHTML("<media:title/>"))) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    /** Static list of FEED-urls. If one is missing they can be found by accessing the correct player-URL (see list below). */
    public static HashMap<String, String> feedURLs                    = new HashMap<String, String>(new HashMap<String, String>() {
                                                                          {
                                                                              put("ALL_OTHERS", url_service_feed_api_mtvnn_v2);
                                                                                                                                                                                                                                                                                                                                    /*
                                                                                                                                                                                                                                                                                                                                     * Seems
                                                                                                                                                                                                                                                                                                                                     * like
                                                                                                                                                                                                                                                                                                                                     * this
                                                                                                                                                                                                                                                                                                                                     * one
                                                                                                                                                                                                                                                                                                                                     * is
                                                                                                                                                                                                                                                                                                                                     * used
                                                                                                                                                                                                                                                                                                                                     * for
                                                                                                                                                                                                                                                                                                                                     * most
                                                                                                                                                                                                                                                                                                                                     * big
                                                                                                                                                                                                                                                                                                                                     * mtv
                                                                                                                                                                                                                                                                                                                                     * sites
                                                                                                                                                                                                                                                                                                                                     * as
                                                                                                                                                                                                                                                                                                                                     * well
                                                                                                                                                                                                                                                                                                                                     */
                                                                              put("nick.de", url_service_feed_ESPERANTO_AS3);
                                                                              put("uk.viva.tv", url_service_feed_ESPERANTO);
                                                                              put("mtvworldwide", "http://all.mtvworldverticals.com/feed-xml/?uri=%s");
                                                                              put("mtv.de", "http://movies.mtv.de/mrss/%s");
                                                                              put("mtvmovies.com", "http://movies.mtv.de/mrss/%s");
                                                                              put("mtv.com", url_service_feed_mtv_com);
                                                                              put("mtvu.com", url_service_feed_mtv_com);
                                                                              put("southpark.de", "http://www.southpark.de/feeds/video-player/mrss/%s");
                                                                              put("southpark.cc.com", url_service_feed_SOUTHPARKSTUDIOS);
                                                                              put("southparkstudios.com", url_service_feed_SOUTHPARKSTUDIOS);
                                                                              put("gameone.de", "http://www.gameone.de/api/mrss/%s");
                                                                              put("gameone.de_2", "https://gameone.de/api/mrss/%s");
                                                                              put("vh1.com", "http://www.vh1.com/player/embed/AS3/rss/?uri=%s");
                                                                              put("vh1.com_2", "http://www.vh1.com/player/embed/AS3/fullepisode/rss/?uri=%s&ref={ref}&instance=vh1shows");
                                                                              put("tvland.com", "http://www.tvland.com/feeds/mrss/?uri=%s&tvlandSyndicated=true");
                                                                              put("spike.com", "http://www.spike.com/feeds/mrss/?uri=%s");
                                                                              put("nick.com", url_service_feed_NICK_COM);
                                                                              put("nicktoons.com", url_service_feed_NICK_COM);
                                                                              put("teennick.com", url_service_feed_NICK_COM);
                                                                              put("nickatnite.com", url_service_feed_NICK_COM);
                                                                              put("nickmom.com", "http://www.nickmom.com/services/mrss/?mgid=%s");
                                                                              put("cmt.com", "http://www.cmt.com/sitewide/apps/player/embed/rss/?uri=%s");
                                                                              put("cc.com", url_service_feed_COMEDYCENTRAL);
                                                                              put("tosh.comedycentral.com", url_service_feed_COMEDYCENTRAL);
                                                                              put("comedycentral.com", url_service_feed_COMEDYCENTRAL);
                                                                              put("mtv.com.au", "http://www.mtv.com.au/mrss/");
                                                                              put("logotv.com", "http://www.logotv.com/player/includes/rss.jhtml?uri=%s");
                                                                              put("mtvnn.com", "http://api.mtvnn.com/v2/mrss.xml?uri=%s");
                                                                          }
                                                                      });
    /** Static list of mediagen URLs. These are usually sub-URLs of feed-urls and they'll return the final downloadlinks. */
    /**
     * Possible parameters of mediagen URLs:
     *
     * uri=<mgid> --> The only parameter we really need. It contains the information which video we want to download.
     *
     * suppressRegisterBeacon=true|false --> Meaning unclear
     *
     * lang=de|en --> Preferred language
     *
     * cdnOverride=akamai|level3
     *
     * level3 = rtmpe://viacomvh1strmfs.fplive.net/viacomvh1strm/ ||||akamai = akamai = rtmpe://cp534.edgefcs.net/ondemand/mtvnorigin/
     *
     * acceptMethods= see 'possibleAcceptMethodsValues' HashMap below --> Defines the streaming method we prefer
     *
     */
    public static HashMap<String, String> mediagenURLs                = new HashMap<String, String>(new HashMap<String, String>() {
                                                                          {
                                                                                                                                                                                                                                                                                                                                    /*
                                                                                                                                                                                                                                                                                                                                     * For
                                                                                                                                                                                                                                                                                                                                     * some
                                                                                                                                                                                                                                                                                                                                     * of
                                                                                                                                                                                                                                                                                                                                     * these,
                                                                                                                                                                                                                                                                                                                                     * we
                                                                                                                                                                                                                                                                                                                                     * have
                                                                                                                                                                                                                                                                                                                                     * to
                                                                                                                                                                                                                                                                                                                                     * access
                                                                                                                                                                                                                                                                                                                                     * the
                                                                                                                                                                                                                                                                                                                                     * feed-
                                                                                                                                                                                                                                                                                                                                     * or
                                                                                                                                                                                                                                                                                                                                     * player
                                                                                                                                                                                                                                                                                                                                     * before
                                                                                                                                                                                                                                                                                                                                     * to
                                                                                                                                                                                                                                                                                                                                     * get
                                                                                                                                                                                                                                                                                                                                     * the
                                                                                                                                                                                                                                                                                                                                     * mediagen
                                                                                                                                                                                                                                                                                                                                     * -
                                                                                                                                                                                                                                                                                                                                     * URL.
                                                                                                                                                                                                                                                                                                                                     * This
                                                                                                                                                                                                                                                                                                                                     * means
                                                                                                                                                                                                                                                                                                                                     * that
                                                                                                                                                                                                                                                                                                                                     * having
                                                                                                                                                                                                                                                                                                                                     * the
                                                                                                                                                                                                                                                                                                                                     * mgid
                                                                                                                                                                                                                                                                                                                                     * is
                                                                                                                                                                                                                                                                                                                                     * not
                                                                                                                                                                                                                                                                                                                                     * always
                                                                                                                                                                                                                                                                                                                                     * enough
                                                                                                                                                                                                                                                                                                                                     * to
                                                                                                                                                                                                                                                                                                                                     * get
                                                                                                                                                                                                                                                                                                                                     * the
                                                                                                                                                                                                                                                                                                                                     * final
                                                                                                                                                                                                                                                                                                                                     * URLs.
                                                                                                                                                                                                                                                                                                                                     */
                                                                              put("videos.mtv.com", "http://videos.mtvnn.com/mediagen/<some kinda hash (length = 32)>");
                                                                                                                                                                                                                                                                                                                                    /*
                                                                                                                                                                                                                                                                                                                                     * Seems
                                                                                                                                                                                                                                                                                                                                     * like
                                                                                                                                                                                                                                                                                                                                     * this
                                                                                                                                                                                                                                                                                                                                     * one
                                                                                                                                                                                                                                                                                                                                     * is
                                                                                                                                                                                                                                                                                                                                     * used
                                                                                                                                                                                                                                                                                                                                     * for
                                                                                                                                                                                                                                                                                                                                     * most
                                                                                                                                                                                                                                                                                                                                     * big
                                                                                                                                                                                                                                                                                                                                     * mtv
                                                                                                                                                                                                                                                                                                                                     * sites
                                                                                                                                                                                                                                                                                                                                     * as
                                                                                                                                                                                                                                                                                                                                     * well
                                                                                                                                                                                                                                                                                                                                     */
                                                                              put("nick.de", url_service_mediagen_ESPERANTO);
                                                                                                                                                                                                                                                                                                                                    /*
                                                                                                                                                                                                                                                                                                                                     * Do
                                                                                                                                                                                                                                                                                                                                     * not
                                                                                                                                                                                                                                                                                                                                     * use
                                                                                                                                                                                                                                                                                                                                     * pre
                                                                                                                                                                                                                                                                                                                                     * -
                                                                                                                                                                                                                                                                                                                                     * defined
                                                                                                                                                                                                                                                                                                                                     * mediagen
                                                                                                                                                                                                                                                                                                                                     * for
                                                                                                                                                                                                                                                                                                                                     * uk
                                                                                                                                                                                                                                                                                                                                     * .
                                                                                                                                                                                                                                                                                                                                     * viva
                                                                                                                                                                                                                                                                                                                                     * .
                                                                                                                                                                                                                                                                                                                                     * tv
                                                                                                                                                                                                                                                                                                                                     * to
                                                                                                                                                                                                                                                                                                                                     * increase
                                                                                                                                                                                                                                                                                                                                     * chances
                                                                                                                                                                                                                                                                                                                                     * of
                                                                                                                                                                                                                                                                                                                                     * avoiding
                                                                                                                                                                                                                                                                                                                                     * their
                                                                                                                                                                                                                                                                                                                                     * GEO
                                                                                                                                                                                                                                                                                                                                     * -
                                                                                                                                                                                                                                                                                                                                     * block!
                                                                                                                                                                                                                                                                                                                                     */
                                                                              // put("uk.viva.tv", url_service_mediagen_ESPERANTO);
                                                                              put("mtv.com", url_service_mediagen_mtv_com);
                                                                              put("mtvu.com", url_service_mediagen_mtv_com);
                                                                              put("southpark.de", "http://www.southpark.de/feeds/video-player/mediagen?uri=%s&suppressRegisterBeacon=true&lang=de&acceptMethods=%s");
                                                                              put("southpark.cc.com", url_service_mediagen_media_utils_api);
                                                                              put("southparkstudios.com", "http://media-utils.mtvnservices.com/services/MediaGenerator/%s?aspectRatio=16:9&lang=de&context=Array&format=json&acceptMethods=%s");
                                                                              put("vh1.com", "http://www.vh1.com/player/embed/AS3/includes/mediaGen.jhtml?uri=%s");
                                                                              put("vh1.com_episodes", "http://www.vh1.com/meta/context/mediaGen?uri=%s");
                                                                              put("tvland.com", "http://www.tvland.com/feeds/mediagen/?uri=%s&device=None");
                                                                              put("spike.com", "http://www.spike.com/feeds/mediagen/?uri=%s");
                                                                              put("nick.com", "http://www.nick.com/dynamo/video/data/mediaGen.jhtml?mgid=%s");
                                                                              put("nickmom.com", url_service_mediagen_media_utils_api);
                                                                              put("cc.com", url_service_mediagen_media_utils_api);
                                                                              put("tosh.comedycentral.com", url_service_mediagen_mediautils_mtvnservices_device);
                                                                              put("comedycentral.com", url_service_mediagen_media_utils_api);
                                                                              put("cmt.com", "http://www.cmt.com/sitewide/apps/player/embed/includes/mediaGen.jhtml?uri=%s");
                                                                              put("cc.com", "http://www.cc.com/feeds/mediagen/?uri=%s&device={device}");
                                                                              put("mtv.com.au", url_service_mediagen_intl_mtvnservices);
                                                                                                                                                                                                                                                                                                                                    /*
                                                                                                                                                                                                                                                                                                                                     * Prefer
                                                                                                                                                                                                                                                                                                                                     * mediautils
                                                                                                                                                                                                                                                                                                                                     * api
                                                                                                                                                                                                                                                                                                                                     * url
                                                                                                                                                                                                                                                                                                                                     * as
                                                                                                                                                                                                                                                                                                                                     * this
                                                                                                                                                                                                                                                                                                                                     * can
                                                                                                                                                                                                                                                                                                                                     * avoid
                                                                                                                                                                                                                                                                                                                                     * geo
                                                                                                                                                                                                                                                                                                                                     * -
                                                                                                                                                                                                                                                                                                                                     * blocks
                                                                                                                                                                                                                                                                                                                                     * more
                                                                                                                                                                                                                                                                                                                                     * often
                                                                                                                                                                                                                                                                                                                                     */
                                                                              put("nicktoons.com", url_service_mediagen_mediautils_mtvnservices_device);
                                                                                                                                                                                                                                                                                                                                    /*
                                                                                                                                                                                                                                                                                                                                     * Prefer
                                                                                                                                                                                                                                                                                                                                     * mediautils
                                                                                                                                                                                                                                                                                                                                     * api
                                                                                                                                                                                                                                                                                                                                     * url
                                                                                                                                                                                                                                                                                                                                     * as
                                                                                                                                                                                                                                                                                                                                     * this
                                                                                                                                                                                                                                                                                                                                     * can
                                                                                                                                                                                                                                                                                                                                     * avoid
                                                                                                                                                                                                                                                                                                                                     * geo
                                                                                                                                                                                                                                                                                                                                     * -
                                                                                                                                                                                                                                                                                                                                     * blocks
                                                                                                                                                                                                                                                                                                                                     * more
                                                                                                                                                                                                                                                                                                                                     * often
                                                                                                                                                                                                                                                                                                                                     */
                                                                              put("teennick.com", url_service_mediagen_mediautils_mtvnservices_device);
                                                                                                                                                                                                                                                                                                                                    /*
                                                                                                                                                                                                                                                                                                                                     * Prefer
                                                                                                                                                                                                                                                                                                                                     * mediautils
                                                                                                                                                                                                                                                                                                                                     * api
                                                                                                                                                                                                                                                                                                                                     * url
                                                                                                                                                                                                                                                                                                                                     * as
                                                                                                                                                                                                                                                                                                                                     * this
                                                                                                                                                                                                                                                                                                                                     * can
                                                                                                                                                                                                                                                                                                                                     * avoid
                                                                                                                                                                                                                                                                                                                                     * geo
                                                                                                                                                                                                                                                                                                                                     * -
                                                                                                                                                                                                                                                                                                                                     * blocks
                                                                                                                                                                                                                                                                                                                                     * more
                                                                                                                                                                                                                                                                                                                                     * often
                                                                                                                                                                                                                                                                                                                                     */
                                                                              put("nickatnite.com", url_service_mediagen_mediautils_mtvnservices_device);
                                                                                                                                                                                                                                                                                                                                    /*
                                                                                                                                                                                                                                                                                                                                     * Prefer
                                                                                                                                                                                                                                                                                                                                     * mediautils
                                                                                                                                                                                                                                                                                                                                     * api
                                                                                                                                                                                                                                                                                                                                     * url
                                                                                                                                                                                                                                                                                                                                     * as
                                                                                                                                                                                                                                                                                                                                     * this
                                                                                                                                                                                                                                                                                                                                     * can
                                                                                                                                                                                                                                                                                                                                     * avoid
                                                                                                                                                                                                                                                                                                                                     * geo
                                                                                                                                                                                                                                                                                                                                     * -
                                                                                                                                                                                                                                                                                                                                     * blocks
                                                                                                                                                                                                                                                                                                                                     * more
                                                                                                                                                                                                                                                                                                                                     * often
                                                                                                                                                                                                                                                                                                                                     */
                                                                              put("logotv.com", url_service_mediagen_mediautils_mtvnservices_device);
                                                                              // put("logotv.com",
                                                                              // "http://www.logotv.com/player/includes/mediaGen.jhtml?uri=%s");
                                                                          }
                                                                      });
    public static HashMap<String, String> embedURLs                   = new HashMap<String, String>(new HashMap<String, String>() {
                                                                          {
                                                                                                                                                                                                                                                                                                                                    /*
                                                                                                                                                                                                                                                                                                                                     * Only
                                                                                                                                                                                                                                                                                                                                     * a
                                                                                                                                                                                                                                                                                                                                     * small
                                                                                                                                                                                                                                                                                                                                     * amount
                                                                                                                                                                                                                                                                                                                                     * if
                                                                                                                                                                                                                                                                                                                                     * embeddable
                                                                                                                                                                                                                                                                                                                                     * -
                                                                                                                                                                                                                                                                                                                                     * usually
                                                                                                                                                                                                                                                                                                                                     * embedded
                                                                                                                                                                                                                                                                                                                                     * links
                                                                                                                                                                                                                                                                                                                                     * are
                                                                                                                                                                                                                                                                                                                                     * never
                                                                                                                                                                                                                                                                                                                                     * needed
                                                                                                                                                                                                                                                                                                                                     * but
                                                                                                                                                                                                                                                                                                                                     * via
                                                                                                                                                                                                                                                                                                                                     * them
                                                                                                                                                                                                                                                                                                                                     * we
                                                                                                                                                                                                                                                                                                                                     * gan
                                                                                                                                                                                                                                                                                                                                     * get
                                                                                                                                                                                                                                                                                                                                     * the
                                                                                                                                                                                                                                                                                                                                     * players
                                                                                                                                                                                                                                                                                                                                     * url
                                                                                                                                                                                                                                                                                                                                     * which
                                                                                                                                                                                                                                                                                                                                     * contains
                                                                                                                                                                                                                                                                                                                                     * the
                                                                                                                                                                                                                                                                                                                                     * feed
                                                                                                                                                                                                                                                                                                                                     * -
                                                                                                                                                                                                                                                                                                                                     * URL
                                                                                                                                                                                                                                                                                                                                     * so
                                                                                                                                                                                                                                                                                                                                     * this
                                                                                                                                                                                                                                                                                                                                     * list
                                                                                                                                                                                                                                                                                                                                     * might
                                                                                                                                                                                                                                                                                                                                     * be
                                                                                                                                                                                                                                                                                                                                     * useful
                                                                                                                                                                                                                                                                                                                                     * in
                                                                                                                                                                                                                                                                                                                                     * the
                                                                                                                                                                                                                                                                                                                                     * future.
                                                                                                                                                                                                                                                                                                                                     * Strong
                                                                                                                                                                                                                                                                                                                                     * format
                                                                                                                                                                                                                                                                                                                                     * -
                                                                                                                                                                                                                                                                                                                                     * -
                                                                                                                                                                                                                                                                                                                                     * >
                                                                                                                                                                                                                                                                                                                                     * Put
                                                                                                                                                                                                                                                                                                                                     * mgid
                                                                                                                                                                                                                                                                                                                                     * in.
                                                                                                                                                                                                                                                                                                                                     */
                                                                              put("ALL_OTHERS", "http://media.mtvnservices.com/%s");
                                                                              put("mtv.com", "http://media.mtvnservices.com/embed/%s/");
                                                                          }
                                                                      });
    /**
     * These are only accessed for embedded videos. They contain the feed-URLs. This list might be useful in the future. Strong format:
     * 0=mgid without the actual ID (so it ends with ':'), 1=FULL mgid Example: http://media.mtvnservices
     * .com/pmt-arc/e1/players/mgid:uma:video:mtv.com:/context49 /config.xml?uri=mgid
     * :uma:video:mtv.com:1234567&type=normal&ref=None&geo=DE& group=music&network= None&device=Other&bParam=videoTypeId=31
     * &ownerOrgId=2&keywords =Some+keywords +in+this+format&keywords =Some+more+keywords+in+this+format&keywords
     * =Even+more+keywords+in+this+format
     */
    public static HashMap<String, String> playerURLs                  = new HashMap<String, String>(new HashMap<String, String>() {
                                                                          {
                                                                              put("mtv.com", "http://media.mtvnservices.com/pmt-arc/e1/players/%s/context49/config.xml?uri=%s");
                                                                              put("southpark.de", "http://media.mtvnservices.com/pmt-arc/e1/players/%s/context5/config.xml?uri=%s");
                                                                              put("tvland.com", "http://media.mtvnservices.com/pmt-arc/e1/players/%s/context3/config.xml?uri=%s");
                                                                              put("spike.com", "http://media.mtvnservices.com/pmt-arc/e1/players/%s/context4/config.xml?uri=%s");
                                                                              put("vh1.com", "http://media.mtvnservices.com/pmt-arc/e1/players/%s/context13/config.xml?uri=%s");
                                                                              put("cmt.com", "http://media.mtvnservices.com/pmt-arc/e1/players/%s/context40/context6/config.xml?uri=%s");
                                                                              put("mtvla.com", "http://media.mtvnservices.com/pmt-arc/e1/players/%s/config.xml?uri=%s");
                                                                              put("mtv.com.au", "http://media.mtvnservices.com/pmt-arc/e1/players/%s/context4/config.xml?uri=%s&type=network&ref=www.mtv.com.au&geo=DE&group=intl&network=None&device=Other");
                                                                              put("cc.com", "http://media.mtvnservices.com/pmt/e1/access/index.html?uri=%s&configtype=edge");
                                                                          }
                                                                      });
    public static HashMap<String, String> possibleAcceptMethodsValues = new HashMap<String, String>(new HashMap<String, String>() {
                                                                          {
                                                                                                                                                                                                                                                                                                                                    /*
                                                                                                                                                                                                                                                                                                                                     * "acceptMethods"
                                                                                                                                                                                                                                                                                                                                     * is
                                                                                                                                                                                                                                                                                                                                     * a
                                                                                                                                                                                                                                                                                                                                     * parameter
                                                                                                                                                                                                                                                                                                                                     * of
                                                                                                                                                                                                                                                                                                                                     * mediagen
                                                                                                                                                                                                                                                                                                                                     * URLs.
                                                                                                                                                                                                                                                                                                                                     * It
                                                                                                                                                                                                                                                                                                                                     * '
                                                                                                                                                                                                                                                                                                                                     * s
                                                                                                                                                                                                                                                                                                                                     * optional
                                                                                                                                                                                                                                                                                                                                     * but
                                                                                                                                                                                                                                                                                                                                     * has
                                                                                                                                                                                                                                                                                                                                     * an
                                                                                                                                                                                                                                                                                                                                     * influence
                                                                                                                                                                                                                                                                                                                                     * on
                                                                                                                                                                                                                                                                                                                                     * the
                                                                                                                                                                                                                                                                                                                                     * final
                                                                                                                                                                                                                                                                                                                                     * URLs.
                                                                                                                                                                                                                                                                                                                                     */
                                                                                                                                                                                                                                                                                                                                    /*
                                                                                                                                                                                                                                                                                                                                     * Default
                                                                                                                                                                                                                                                                                                                                     * seting
                                                                                                                                                                                                                                                                                                                                     * (if
                                                                                                                                                                                                                                                                                                                                     * ever
                                                                                                                                                                                                                                                                                                                                     * used)
                                                                                                                                                                                                                                                                                                                                     */
                                                                              put("default", "fms,hdn1,hds");
                                                                                                                                                                                                                                                                                                                                    /*
                                                                                                                                                                                                                                                                                                                                     * Returns
                                                                                                                                                                                                                                                                                                                                     * http
                                                                                                                                                                                                                                                                                                                                     * links
                                                                                                                                                                                                                                                                                                                                     * but
                                                                                                                                                                                                                                                                                                                                     * less
                                                                                                                                                                                                                                                                                                                                     * available
                                                                                                                                                                                                                                                                                                                                     * qualities
                                                                                                                                                                                                                                                                                                                                     * and
                                                                                                                                                                                                                                                                                                                                     * usually
                                                                                                                                                                                                                                                                                                                                     * not
                                                                                                                                                                                                                                                                                                                                     * as
                                                                                                                                                                                                                                                                                                                                     * good
                                                                                                                                                                                                                                                                                                                                     * as
                                                                                                                                                                                                                                                                                                                                     * their
                                                                                                                                                                                                                                                                                                                                     * rtmp
                                                                                                                                                                                                                                                                                                                                     * (
                                                                                                                                                                                                                                                                                                                                     * e)
                                                                                                                                                                                                                                                                                                                                     * streams
                                                                                                                                                                                                                                                                                                                                     */
                                                                              put("http", "http");
                                                                              put("hls", "http");
                                                                              put("hds", "http");
                                                                          }
                                                                      });

    /** Returns a feed URL based on the domain. */
    private String getFEEDurl(final String domain) throws PluginException {
        if (mgid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String feedurl = feedURLs.get(domain);
        feedurl = String.format(feedurl, mgid);
        return feedurl;
    }

    /** Returns a mediagen-feed URL based on the domain. */
    private String getMEDIAGENurl(final String domain) throws PluginException {
        if (mgid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String mediagenurl = mediagenURLs.get(domain);
        mediagenurl = String.format(mediagenurl, mgid);
        return mediagenurl;
    }

    /** TODO: finish this! */
    /** Returns a mediagen-feed URL based on the domain. */
    public static String mgidGetFeedurlForMgid(final String mgid) {
        if (mgid == null) {
            return null;
        }
        final String host = mgidGetHost(mgid);
        final String mgid_type = mgidGetType(mgid);
        String feedurl;
        if (mgid_type == null) {
            feedurl = null;
        } else if (mgid_type.equalsIgnoreCase("sensei")) {
            feedurl = url_service_feed_api_mtvnn_v2;
        } else if (mgid_type.equalsIgnoreCase("uma")) {
            feedurl = url_service_feed_mtv_com;
        } else {
            feedurl = feedURLs.get(host);
        }
        if (feedurl != null) {
            feedurl = String.format(feedurl, mgid);
        }
        return feedurl;
    }

    /** TODO: finish this! */
    /** Returns a mediagen-feed URL based on the domain. */
    public static String mgidGetMediagenurlForMgid(final String mgid) throws PluginException {
        if (mgid == null) {
            return null;
        }
        boolean mediagenurl_formatted = false;
        final String host = mgidGetHost(mgid);
        final String mgid_type = mgidGetType(mgid);
        String mediagenurl;
        if (mgid_type == null) {
            mediagenurl = null;
        } else if (host.equals("southpark.de")) {
            if (!rtmpe_supported) {
                /*
                 * Special case: In germany they only have lower quality http urls so we have to enforce http because if we convert rtmpe to
                 * http we will get the english version!
                 */
                mediagenurl = String.format(mediagenURLs.get(host), mgid, possibleAcceptMethodsValues.get("http"));
            } else {
                mediagenurl = String.format(mediagenURLs.get(host), mgid, possibleAcceptMethodsValues.get("default"));
            }
            mediagenurl_formatted = true;
        } else if (host.equals("southparkstudios.com")) {
            // http://media-utils.mtvnservices.com/services/MediaGenerator/mgid:arc:video:southparkstudios.com:5ec92f29-7934-4061-8336-734918193cd8?aspectRatio=16:9&lang=de&context=Array&format=json&acceptMethods=hls
            if (!rtmpe_supported) {
                /*
                 * Special case: In germany they only have lower quality http urls so we have to enforce hls to get higher quality and avoid
                 * rtmpe!
                 */
                mediagenurl = String.format(mediagenURLs.get(host), mgid, "hls");
            } else {
                mediagenurl = String.format(mediagenURLs.get(host), mgid, possibleAcceptMethodsValues.get("default"));
            }
            mediagenurl_formatted = true;
        } else {
            mediagenurl = mediagenURLs.get(host);
        }
        if (mediagenurl != null && !mediagenurl_formatted) {
            mediagenurl = String.format(mediagenurl, mgid);
        }
        return mediagenurl;
    }

    public static String mgidGetHost(final String mgid) {
        final String[] mgid_info = mgidGetInformation(mgid);
        return mgid_info[mgid_info.length - 2];
    }

    public static String mgidGetType(final String mgid) {
        return mgidGetInformation(mgid)[2];
    }

    public static String[] mgidGetInformation(final String mgid) {
        final String[] mgid_info;
        if (mgid == null) {
            mgid_info = null;
        } else {
            mgid_info = mgid.split(":");
        }
        return mgid_info;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}