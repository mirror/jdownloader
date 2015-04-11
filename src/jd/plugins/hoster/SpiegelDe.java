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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "spiegel.de", "spiegel.tv" }, urls = { "http://cdn\\d+\\.spiegel\\.de/images/image[^<>\"/]+|http://(www\\.)?spiegel\\.de/video/(embedurl/)?[a-z0-9\\-_]*?video\\-[a-z0-9\\-_]*?\\.html", "http://(www\\.)?spiegel\\.tv/(#/)?filme/[a-z0-9\\-]+/" }, flags = { 0, 0 })
public class SpiegelDe extends PluginForHost {

    private final Pattern       pattern_supported_image          = Pattern.compile("http://cdn\\d+\\.spiegel\\.de/images/image[^<>\"/]+");
    private final Pattern       pattern_supported_video          = Pattern.compile("http://(www\\.|m\\.)?spiegel\\.de/video/.+");
    private final Pattern       pattern_supported_video_mobile   = Pattern.compile("http://m\\.spiegel\\.de/video/media/video\\-\\d+\\.html");
    private final Pattern       pattern_supported_spiegeltvfilme = Pattern.compile("http://(www\\.)?spiegel\\.tv/(#/)?filme/[a-z0-9\\-]+/");

    private static final String spiegeltvfilme_rtmp_app          = "schnee_vod/flashmedia/";
    private static final String spiegeltvfilme_apihost           = "http://spiegeltv-ivms2-restapi.s3.amazonaws.com";

    private String              DLLINK                           = null;

    /*
     * Important for pattern_supported_video: Way to get mobile versions of videos: Use a mobile UA - Video site:
     * http://m.spiegel.de/video/video-1234567.html AND link that leads to the final mobile video URL:
     * http://m.spiegel.de/video/media/video-1234567.html
     */
    /*
     * SECOND, NON-mobile way to get the finallinks: http://spiegel.de/video/media/video-1234567.html
     */
    /*
     * THIRD, NON-mobile way to get finallinks: http://video.spiegel.de/flash/1234567.xml Old video types/formats: Type 1: h263 flv Type 2:
     * flv mid (VP6) Type 3: h263 low Type 4: flv low (VP6) Type 5: flv high (VP6) (680544) Type 6: h263 3gp Type 7: h263 3gp low Type 8:
     * iphone mp4 Type 9: podcast mp4 640480 Type 15 : H264
     */

    public SpiegelDe(final PluginWrapper wrapper) {
        super(wrapper);
    }

    public String getAGBLink() {
        return "http://www.spiegel.de/agb";
    }

    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        if (new Regex(link.getDownloadURL(), pattern_supported_spiegeltvfilme).matches()) {
            link.setUrlDownload(link.getDownloadURL().replace("/#/", "/"));
        } else if (new Regex(link.getDownloadURL(), pattern_supported_video).matches() || new Regex(link.getDownloadURL(), pattern_supported_video_mobile).matches()) {
            final String videoid = new Regex(link.getDownloadURL(), "video\\-(\\d+)").getMatch(0);
            link.setUrlDownload("http://www.spiegel.de/video/video-" + videoid + ".html");
        }
    }

    @SuppressWarnings("deprecation")
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws PluginException, IOException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        String filename = null;
        if (new Regex(downloadLink.getDownloadURL(), pattern_supported_video).matches()) {
            /* pattern_supported_video links can redirect to pattern_supported_spiegeltvfilme */
            br.getPage(downloadLink.getDownloadURL());
            final String redirect = br.getRedirectLocation();
            if (redirect != null) {
                if (new Regex(redirect, pattern_supported_spiegeltvfilme).matches()) {
                    downloadLink.setUrlDownload(redirect);
                } else {
                    br.getPage(redirect);
                }
            }
        }
        if (new Regex(downloadLink.getDownloadURL(), pattern_supported_spiegeltvfilme).matches()) {
            /* More info e.g. here: http://spiegeltv-prod-static.s3.amazonaws.com/projectConfigs/projectConfig.json?cache=648123456s5 */
            br.getPage(downloadLink.getDownloadURL());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("<title>([^<>]*?)</title>").getMatch(0);
            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /*
             * Use the code below if you want to use json-only or need additional information about the video such as its' 'created' and
             * last 'updated' dates, description and so on.
             */
            // final String spiegel_url_name = new Regex(downloadLink.getDownloadURL(), "spiegel\\.tv/filme/([^<>\"]+)/").getMatch(0);
            // final String id = br.getRegex("<li id=\\'id\\'>(\\d+)</li>").getMatch(0);
            // if (id == null) {
            // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // }
            // br.getPage(spiegeltvfilme_apihost + "/version.json");
            // final String apiversion = getJson("version_name");
            // br.getPage(spiegeltvfilme_apihost + "/" + apiversion + "/restapi/media/" + id + ".json");
            // /* Get short information about the video */
            // br.getPage(spiegeltvfilme_apihost + "/" + apiversion + "/restapi/slugs/" + spiegel_url_name + ".json");

            filename += ".flv";
        } else {
            if (new Regex(downloadLink.getDownloadURL(), pattern_supported_image).matches()) {
                DLLINK = downloadLink.getDownloadURL();
                /* Prefer filenames set in decrypter in case user added a complete gallery. */
                filename = downloadLink.getStringProperty("decryptedfilename", null);
                if (filename == null) {
                    filename = new Regex(DLLINK, "/images/(.+)").getMatch(0);
                }
            } else {
                if (br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                final String videoserver = br.getRegex("var server[\t\n\r ]*?=[\t\n\r ]*?\"(https?://[^<>\"]*?)\"").getMatch(0);
                final String file = br.getRegex("spStartVideo\\d+\\([^<>\"\\)]+\\'([^<>\"]*?\\.(mp4|flv))\\'").getMatch(0);
                filename = br.getRegex("class=\"module\\-title\">([^<>]*?)</div>").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("property=\"og:title\"[\t\n\r ]*?content=\"([^<>\"]*?) \\- SPIEGEL ONLINE \\- Video\"").getMatch(0);
                }
                if (filename == null || videoserver == null || file == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (file.equals("")) {
                    /*
                     * We could now access this but it should return a 404 and the video is offline:
                     * http://video.spiegel.de/flash/1234567.xml
                     */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                br.getHeaders().put("Accept", "video/webm,video/ogg,video/*;q=0.9,application/ogg;q=0.7,audio/*;q=0.6,*/*;q=0.5");
                DLLINK = videoserver + file;
                if (DLLINK.contains(".flv")) {
                    filename += ".flv";
                } else {
                    filename += ".mp4";
                }
            }
            URLConnectionAdapter urlConnection = null;
            try {
                try {
                    /* @since JD2 */
                    urlConnection = br.openHeadConnection(DLLINK);
                } catch (final Throwable t) {
                    /* Not supported in old 0.9.581 Stable */
                    urlConnection = br.openGetConnection(DLLINK);
                }
                if (!urlConnection.isOK()) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                downloadLink.setDownloadSize(urlConnection.getLongContentLength());
            } catch (final IOException e) {
                logger.severe(e.getMessage());
                downloadLink.getLinkStatus().setStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } finally {
                try {
                    urlConnection.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        filename = Encoding.htmlDecode(filename).trim();
        filename = encodeUnicode(filename);
        downloadLink.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        this.requestFileInformation(downloadLink);
        if (new Regex(downloadLink.getDownloadURL(), pattern_supported_image).matches()) {
            this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, DLLINK, false, 1);
            this.dl.startDownload();
        } else if (new Regex(downloadLink.getDownloadURL(), pattern_supported_video).matches()) {
            this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, DLLINK, true, 0);
            if (this.dl.startDownload()) {
                if (downloadLink.getProperty("convertto") != null) {
                    JDUtilities.getPluginForDecrypt("youtube.com");
                    final jd.plugins.decrypter.TbCm.DestinationFormat convertTo = jd.plugins.decrypter.TbCm.DestinationFormat.valueOf(downloadLink.getProperty("convertto").toString());
                    jd.plugins.decrypter.TbCm.DestinationFormat inType;
                    if (convertTo == jd.plugins.decrypter.TbCm.DestinationFormat.VIDEOIPHONE || convertTo == jd.plugins.decrypter.TbCm.DestinationFormat.VIDEO_MP4 || convertTo == jd.plugins.decrypter.TbCm.DestinationFormat.VIDEO_3GP) {
                        inType = convertTo;
                    } else {
                        inType = jd.plugins.decrypter.TbCm.DestinationFormat.VIDEO_FLV;
                    }
                    /* to load the TbCm plugin */
                    // JDUtilities.getPluginForDecrypt("youtube.com");
                    if (!jd.plugins.decrypter.TbCm.ConvertFile(downloadLink, inType, convertTo)) {
                        logger.severe("Video-Convert failed!");
                    }

                }
            }
        } else {
            /* spiegel.tv pattern_supported_spiegeltvfilme */
            String uuid = br.getRegex("name=\\'DC\\.identifier\\' content=\\'([^<>\"]+)\\'").getMatch(0);
            if (uuid == null) {
                uuid = br.getRegex("id=\\'uuid\\'>([^<>\"]*?)<").getMatch(0);
            }
            if (uuid == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String scalefactor;
            if (br.containsHTML("id=\\'is_wide\\'>True<")) {
                scalefactor = "16x9";
            } else {
                scalefactor = "4x3";
            }
            final String rtmpurl = "rtmp://mf.schneevonmorgen.c.nmdn.net/" + spiegeltvfilme_rtmp_app;
            final String playpath = "mp4:" + uuid + "_spiegeltv_0500_" + scalefactor + ".m4v";

            try {
                dl = new RTMPDownload(this, downloadLink, rtmpurl);
            } catch (final NoClassDefFoundError e) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "RTMPDownload class missing");
            }
            /* Setup rtmp connection */
            jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
            rtmp.setPageUrl(this.br.getURL());
            rtmp.setUrl(rtmpurl);
            rtmp.setPlayPath(playpath);
            rtmp.setApp(spiegeltvfilme_rtmp_app);
            /* Make sure we're using the correct protocol! */
            rtmp.setProtocol(0);
            rtmp.setFlashVer("WIN 16,0,0,296");
            /* SWFvfy can also be used here - doesn't matter! */
            rtmp.setSwfUrl("http://prod-static.spiegel.tv/frontend-069.swf");
            rtmp.setResume(true);
            ((RTMPDownload) dl).startDownload();
        }
    }

    /** Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from String source.
     *
     * @author raztoki
     * */
    private String getJson(final String source, final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(source, key);
    }

    /**
     * Wrapper<br/>
     * Tries to return value of key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    private String getJson(final String key) {
        return jd.plugins.hoster.K2SApi.JSonUtils.getJson(br.toString(), key);
    }

    public void reset() {
    }

    public void resetDownloadlink(final DownloadLink link) {
    }

}