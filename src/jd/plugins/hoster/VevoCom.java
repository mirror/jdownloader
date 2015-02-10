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

import java.util.ArrayList;
import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.config.Property;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vevo.com" }, urls = { "http://www\\.vevo\\.com/watch/([A-Za-z0-9\\-_]+/[^/]+/[A-Z0-9]+|[A-Z0-9]+)|http://vevo\\.ly/[A-Za-z0-9]+|http://videoplayer\\.vevo\\.com/embed/embedded\\?videoId=[A-Za-z0-9]+" }, flags = { 32 })
public class VevoCom extends PluginForHost {

    public VevoCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /** URL that contains all kinds of API/URL/site information: http://cache.vevo.com/a/swf/assets/xml/base_config_v3.xml?cb=20130110 */
    /**
     * API url (token needed, got no source for that at the moment): https://apiv2.vevo.com/video/<VIDEOID>/streams/hls?token=<APITOKEN> or
     * https://apiv2.vevo.com/video/<VIDEOID>?token=<APITOKEN>
     */
    /** Additional hint: Vevo also has Apps for a lot of platforms: http://www.vevo.com/c/DE/DE/apps */
    /** Additional thanks goes to: https://github.com/rg3/youtube-dl/blob/master/youtube_dl/extractor/vevo.py */

    /* Also possible: http://cache.vevo.com/a/swf/versions/3/player.swf?eurl=www.somewebsite.com&cb=<12-digit-number> */
    private static final String  player            = "http://cache.vevo.com/a/swf/versions/3/player.swf";

    /* Extension which will be used if no correct extension is found */
    private static final String  default_Extension = ".mp4";
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;

    private static final String  type_invalid      = "http://(www\\.)?vevo\\.com/watch/playlist";
    private static final String  type_short        = "http://vevo\\.ly/[A-Za-z0-9]+";
    private static final String  type_video        = "http://(www\\.)?vevo\\.com/watch/[A-Za-z0-9\\-_]+/.+";
    private static final String  type_video_short  = "http://(www\\.)?vevo\\.com/watch/[A-Za-z0-9]+";
    private static final String  type_embed        = "http://videoplayer\\.vevo\\.com/embed/embedded\\?videoId=[A-Za-z0-9]+";

    @Override
    public String getAGBLink() {
        return "http://www.vevo.com/c/DE/DE/legal/terms-conditions";
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        if (link.getDownloadURL().matches(type_embed)) {
            link.setUrlDownload("http://www.vevo.com/watch/" + link.getDownloadURL().substring(link.getDownloadURL().lastIndexOf("=") + 1));
        }
    }

    /**
     * Possible API "statusCode" codes an their meaning: 0=all ok, 304=Video is temporarily unavailable, 501=GEO block, 909=Video offline
     *
     * @throws Exception
     */
    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        if (downloadLink.getDownloadURL().matches(type_invalid)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.setBrowserExclusive();
        br.setAllowedResponseCodes(500);
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 500) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (downloadLink.getDownloadURL().matches(type_short) && !br.getURL().matches(type_video)) {
            logger.info("Short url either redirected to unknown url format or is offline");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (downloadLink.getDownloadURL().matches(type_short)) {
            /* Set- and re-use new (correct) URL */
            downloadLink.setUrlDownload(br.getURL());
        }
        final String apijson = br.getRegex("apiResults:[\t\n\r ]*?(\\{.*?\\});").getMatch(0);
        if (apijson == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(apijson);
        final ArrayList<Object> videos = (ArrayList) entries.get("videos");
        if (videos == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final LinkedHashMap<String, Object> videoinfo = (LinkedHashMap<String, Object>) videos.get(0);
        if (videoinfo == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String year = Integer.toString(((Number) videoinfo.get("year")).intValue());
        final String artistsInfo = (String) videoinfo.get("artistsInfo");
        final String title = (String) videoinfo.get("title");
        if (title == null || artistsInfo == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String filename = year + "_" + artistsInfo + " - " + title;
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        downloadLink.setFinalFileName(filename + default_Extension);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        final String videoid = br.getRegex("\"isrc\":\"([^<>\"]*?)\"").getMatch(0);
        if (videoid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("Referer", player);
        /*
         * Also possible:
         * http://videoplayer.vevo.com/VideoService/AuthenticateVideo?isrc=<videoid>&domain=<some_domain>&authToken=X-X-X-X-X&pkey=X-X-X-X-X
         * This way is usually used for embedded videos.
         */
        br.getPage("http://videoplayer.vevo.com/VideoService/AuthenticateVideo?isrc=" + videoid);
        final String statusCode = getJson("statusCode");
        if (statusCode.equals("304")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Video is temporarily unavailable", 60 * 60 * 1000l);
        } else if (statusCode.equals("909")) {
            /* Should never happen */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* Check whether we have to go the rtmp way or can use the http streaming */
        if (statusCode.equals("501")) {
            downloadRTMP(downloadLink);
        } else {
            downloadHTTP(downloadLink);
        }

    }

    @SuppressWarnings("deprecation")
    private void downloadRTMP(final DownloadLink downloadLink) throws Exception {
        /* Remove old stuff */
        this.br = new Browser();
        final String fid = getFID(downloadLink);
        br.getPage("http://smil.lvl3.vevo.com/Video/V2/VFILE/" + fid + "/" + fid.toLowerCase() + "r.smil");
        final String[] playpaths = br.getRegex("<video src=\"(mp4:[^<>\"]*?.mp4)\"").getColumn(0);
        final String rtmpurl = br.getRegex("<meta base=\"(rtmp[^<>\"]*?)\"").getMatch(0);
        final String pageurl = downloadLink.getDownloadURL();
        if (playpaths == null || playpaths.length == 0 || rtmpurl == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String app = new Regex(rtmpurl, "([a-z0-9]+)$").getMatch(0);
        if (app == null) {
            app = "vevood";
        }
        /* Chose highest quality available */
        final String playpath = playpaths[playpaths.length - 1];
        try {
            dl = new RTMPDownload(this, downloadLink, rtmpurl);
        } catch (final NoClassDefFoundError e) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "RTMPDownload class missing");
        }
        /* Setup rtmp connection */
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        rtmp.setPageUrl(pageurl);
        rtmp.setUrl(rtmpurl);
        rtmp.setPlayPath(playpath);
        rtmp.setApp(app);
        rtmp.setSwfVfy(player);
        rtmp.setResume(true);
        ((RTMPDownload) dl).startDownload();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void downloadHTTP(final DownloadLink downloadLink) throws Exception {
        final String[] possibleQualities = { "High", "Med", "Low" };
        String dllink = checkDirectLink(downloadLink, "directlink");
        if (dllink == null) {
            String html_videosource = null;
            final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
            final LinkedHashMap<String, Object> video = (LinkedHashMap<String, Object>) entries.get("video");
            final ArrayList<Object> ressourcelist = (ArrayList) video.get("videoVersions");
            int count = ressourcelist.size();
            /* Explanation of sourceType: 0=undefined, 1=?Probably HDS?, 2=HTTP, 3=HLS iOS,4=HLS, 10=SmoothStreaming */
            /*
             * Explanation of version: Seems to be different vevo data servers as it has no influence on the videoquality: 0==, 1=?,
             * 2=aka.vevo.com, 3=lvl3.vevo.com, 4=aws.vevo.com --> version 2 never worked for me
             */
            /* Last checked: 08.01.2015 */
            LinkedHashMap<String, Object> tempmap = null;
            for (int counter = count - 1; counter >= 0; counter--) {
                tempmap = (LinkedHashMap<String, Object>) ressourcelist.get(counter);
                final int sourceType = ((Number) tempmap.get("sourceType")).intValue();
                /* We prefer http */
                if (sourceType == 2) {
                    html_videosource = (String) tempmap.get("data");
                    break;
                }
            }
            /* Clean that */
            html_videosource = html_videosource.replace("\\", "");
            for (final String possibleQuality : possibleQualities) {
                dllink = new Regex(html_videosource, "name=\"" + possibleQuality + "\" url=\"(http[^<>\"]*?)\"").getMatch(0);
                if (dllink != null) {
                    logger.info("Found video quality: " + possibleQuality);
                    break;
                }
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, free_resume, free_maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink", dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }

    /* Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "/");
        output = output.replace("\\", "");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    @SuppressWarnings("deprecation")
    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
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

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
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
