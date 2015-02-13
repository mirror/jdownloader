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

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.jdownloader.downloader.hls.HLSDownloader;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vevo.com" }, urls = { "http://vevodecrypted/\\d+" }, flags = { 2 })
public class VevoCom extends PluginForHost {

    public VevoCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    /** URL that contains all kinds of API/URL/site information: http://cache.vevo.com/a/swf/assets/xml/base_config_v3.xml?cb=20130110 */
    /**
     * API url (token needed, got no source for that at the moment): https://apiv2.vevo.com/video/<VIDEOID>/streams/hls?token=<APITOKEN> or
     * https://apiv2.vevo.com/video/<VIDEOID>?token=<APITOKEN>
     */
    /**
     * Also possible:
     * http://videoplayer.vevo.com/VideoService/AuthenticateVideo?isrc=<videoid>&domain=<some_domain>&authToken=X-X-X-X-X&pkey=X-X-X-X-X
     * This way is usually used for embedded videos. Also possible:
     * http://api.vevo.com/VideoService/AuthenticateVideo?isrc=<videoid>&domain=cache.vevo.com&pkey=af330f2c-5617-4e57-81b5-4a6edbef07cc
     */
    /* Also possible: https://svideoplayer.vevo.com/VideoService/AuthenticateVideo?isrc= */
    /** Additional hint: Vevo also has Apps for a lot of platforms: http://www.vevo.com/c/DE/DE/apps */
    /** Additional thanks goes to: https://github.com/rg3/youtube-dl/blob/master/youtube_dl/extractor/vevo.py */
    /**
     * Possible API "statusCode" codes an their meaning: 0=all ok, 304=Video is temporarily unavailable, 501=GEO block, 909=Video offline
     *
     *
     * List of all possible qualities: 'High': 'High Definition MP4' ,'Med': 'Standard Definition MP4' ,'Low': 'Ultra Low Definition MP4'
     * ,'564000': 'Very Low Definition MP4' ,'864000': 'Low Definition MP4' ,'1328000':'Standard Definition MP4' ,'1728000':'Standard
     * Definition HBR MP4' ,'2528000':'High Definition MP4' ,'3328000':'High Definition HBR MP4' ,'4392000':'Full High Definition MP4'
     * ,'5392000':'Full High Definition HBR MP4'
     *
     *
     *
     * /* Also possible: http://cache.vevo.com/a/swf/versions/3/player.swf?eurl=www.somewebsite.com&cb=<12-digit-number>
     */
    private static final String  player            = "http://cache.vevo.com/a/swf/versions/3/player.swf";

    /* Extension which will be used if no correct extension is found */
    private static final String  default_Extension = ".mp4";
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;

    private static final String  type_short        = "http://vevo\\.ly/[A-Za-z0-9]+";
    private static final String  type_watch        = "http://(www\\.)?vevo\\.com/watch/[A-Za-z0-9\\-_]+/[^/]+/[A-Z0-9]+";
    private static final String  type_watch_short  = "http://(www\\.)?vevo\\.com/watch/[A-Za-z0-9]+";
    private static final String  type_embedded     = "http://videoplayer\\.vevo\\.com/embed/embedded\\?videoId=[A-Za-z0-9]+";

    private static final long    streamtype_rtmp   = 1;
    private static final long    streamtype_http   = 2;
    private static final long    streamtype_hls    = 4;

    private boolean              geoblock_1        = false;
    private boolean              geoblock_2        = false;
    private long                 streamtype        = -1;
    String                       dllink            = null;

    @Override
    public String getAGBLink() {
        return "http://www.vevo.com/c/DE/DE/legal/terms-conditions";
    }

    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        if (downloadLink.getDownloadURL().contains("/playlist")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.setBrowserExclusive();
        br.setAllowedResponseCodes(500);
        br.setFollowRedirects(true);
        br.getPage(getMainlink(downloadLink));
        if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 500) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dllink = downloadLink.getStringProperty("directlink", null);
        downloadLink.setFinalFileName(downloadLink.getStringProperty("plain_filename", null));

        /* Handling especially for e.g. users from Portugal */
        if (br.containsHTML("THIS PAGE IS CURRENTLY UNAVAILABLE IN YOUR REGION")) {
            geoblock_1 = true;
            /* Simply access the smil url to determine online/offline status */
            this.br = new Browser();
            accessSmi_url(this.br, downloadLink);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        }
        streamtype = downloadLink.getLongProperty("sourcetype", -1);
        if (streamtype == streamtype_http) {
            URLConnectionAdapter con = null;
            try {
                try {
                    /* @since JD2 */
                    con = br.openHeadConnection(dllink);
                } catch (final BrowserException e) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (!con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (streamtype == -1) {
            /* Should never happen */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (geoblock_1) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Not downloadable in your country");
        }
        /* Check whether we have to go the rtmp way or can use the http streaming */
        if (streamtype == streamtype_rtmp) {
            downloadRTMP(downloadLink);
        } else if (streamtype == streamtype_http) {
            downloadHTTP(downloadLink);
        } else if (streamtype == streamtype_hls) {
            downloadHLS(downloadLink);
        }

    }

    @SuppressWarnings("deprecation")
    private void downloadRTMP(final DownloadLink downloadLink) throws Exception {
        final String playpath = dllink;
        final String rtmp_base = downloadLink.getStringProperty("rtmpbase", null);
        final String pageurl = downloadLink.getDownloadURL();
        String app = new Regex(rtmp_base, "([a-z0-9]+)$").getMatch(0);
        if (app == null) {
            app = "vevood";
        }
        try {
            dl = new RTMPDownload(this, downloadLink, rtmp_base);
        } catch (final NoClassDefFoundError e) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "RTMPDownload class missing");
        }
        /* Setup rtmp connection */
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        rtmp.setPageUrl(pageurl);
        rtmp.setUrl(rtmp_base);
        rtmp.setPlayPath(playpath);
        rtmp.setApp(app);
        rtmp.setSwfVfy(player);
        rtmp.setResume(false);
        ((RTMPDownload) dl).startDownload();
    }

    /*
     * 2nd way to get http streams: http://smilstream.vevo.com/HDFlash/v1/smil/<videoid>/<videoid>.smil
     *
     * Examplecode: http://bluecop-xbmc-repo.googlecode.com/svn-history/r383/trunk/plugin.video.vevo/default.py
     */
    private void downloadHTTP(final DownloadLink downloadLink) throws Exception {
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

    private void downloadHLS(final DownloadLink downloadLink) throws Exception {
        checkFFmpeg(downloadLink, "Download a HLS Stream");
        dl = new HLSDownloader(downloadLink, br, dllink);
        dl.startDownload();
    }

    private void accessSmi_url(final Browser br, final DownloadLink dl) throws IOException, PluginException {
        accessSmi_url(br, getFID(dl));
    }

    public static void accessSmi_url(final Browser br, final String fid) throws IOException, PluginException {
        br.getPage("http://smil.lvl3.vevo.com/Video/V2/VFILE/" + fid + "/" + fid.toLowerCase() + "r.smil");
    }

    @SuppressWarnings("deprecation")
    private String getURLfilename(final DownloadLink dl) {
        return getURLfilename(getMainlink(dl));
    }

    @SuppressWarnings("deprecation")
    public static String getURLfilename(final String parameter) {
        String url_fname = null;
        if (parameter.matches(type_short) || parameter.matches(type_watch_short)) {
            url_fname = new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0);

        } else if (parameter.matches(type_watch)) {
            /* User probably added invalid url */
            final Regex url_artist_title = new Regex(parameter, "vevo\\.com/watch/([A-Za-z0-9\\-_]+)/([^/]+)/.+");
            final String artist = url_artist_title.getMatch(0);
            final String title = url_artist_title.getMatch(1);
            url_fname = artist + " - " + title;
        } else {
            /* Probably unsupported url format - this should never happen */
            url_fname = null;
        }
        return url_fname;
    }

    private String getMainlink(final DownloadLink dl) {
        return dl.getStringProperty("mainlink", null);
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
    public String getDescription() {
        return "JDownloader's vevo Plugin helps downloading videoclips from vevo.tv. You can choose between different video qualities.";
    }

    /** Settings stuff */
    private static final String ALLOW_HTTP_56   = "version_4_type_2_56";
    private static final String ALLOW_HTTP_500  = "version_4_type_2_500";
    private static final String ALLOW_HTTP_2000 = "version_4_type_2_2000";
    private static final String ALLOW_RTMP_500  = "version_4_type_1_500";
    private static final String ALLOW_RTMP_800  = "version_4_type_1_800";
    private static final String ALLOW_RTMP_1200 = "version_4_type_1_1200";
    private static final String ALLOW_RTMP_1600 = "version_4_type_1_1600";

    private static final String ALLOW_HLS_64    = "version_4_type_4_64";
    private static final String ALLOW_HLS_200   = "version_4_type_4_200";
    private static final String ALLOW_HLS_400   = "version_4_type_4_400";
    private static final String ALLOW_HLS_500   = "version_4_type_4_500";
    private static final String ALLOW_HLS_800   = "version_4_type_4_800";
    private static final String ALLOW_HLS_1200  = "version_4_type_4_1200";
    private static final String ALLOW_HLS_2400  = "version_4_type_4_2400";
    private static final String ALLOW_HLS_3200  = "version_4_type_4_3200";
    private static final String ALLOW_HLS_4200  = "version_4_type_4_4200";
    private static final String ALLOW_HLS_5200  = "version_4_type_4_5200";

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "HTTP formats:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_HTTP_56, JDL.L("plugins.hoster.vevoCom.ALLOW_HTTP_56", "Load videocodec H264/x264 56kBit/s 176x144 with audio codec AAC/quicktime 24/128/192kBit/s")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_HTTP_500, JDL.L("plugins.hoster.vevoCom.ALLOW_HTTP_500", "Load videocodec H264/x264 500 480x360 with audio codec AAC/quicktime 24/128/192kBit/s")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_HTTP_2000, JDL.L("plugins.hoster.vevoCom.ALLOW_HTTP_2000", "Load videocodec H264/x264 2000 1280x720 with audio codec AAC/quicktime 24/128/192kBit/s")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "HLS formats:"));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_HLS_64,
        // JDL.L("plugins.hoster.vevoCom.ALLOW_HLS_64",
        // "Load videocodec h264 64kBit/s 416x342 with audio codec AAC 128kBit/s")).setDefaultValue(false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_HLS_200,
        // JDL.L("plugins.hoster.vevoCom.ALLOW_HLS_200",
        // "Load videocodec h264 200kBit/s 416x342 with audio codec AAC 128kBit/s")).setDefaultValue(false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_HLS_400,
        // JDL.L("plugins.hoster.vevoCom.ALLOW_HLS_400",
        // "Load videocodec h264 400kBit/s 480x270 with audio codec AAC 128kBit/s")).setDefaultValue(false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_HLS_500,
        // JDL.L("plugins.hoster.vevoCom.ALLOW_HLS_500",
        // "Load videocodec h264 500kBit/s 640x360 with audio codec AAC 128kBit/s")).setDefaultValue(false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_HLS_800,
        // JDL.L("plugins.hoster.vevoCom.ALLOW_HLS_800",
        // "Load videocodec h264 800kBit/s 640x360 with audio codec AAC 128kBit/s")).setDefaultValue(false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_HLS_1200,
        // JDL.L("plugins.hoster.vevoCom.ALLOW_HLS_1200",
        // "Load videocodec h264 1200kBit/s 960x540 with audio codec AAC 128kBit/s")).setDefaultValue(false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_HLS_2400,
        // JDL.L("plugins.hoster.vevoCom.ALLOW_HLS_2400",
        // "Load videocodec h264 2400kBit/s 960x540 with audio codec AAC 128kBit/s")).setDefaultValue(false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_HLS_3200,
        // JDL.L("plugins.hoster.vevoCom.ALLOW_HLS_3200",
        // "Load videocodec h264 3200kBit/s 1280x720 with audio codec AAC 128kBit/s")).setDefaultValue(false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_HLS_4200,
        // JDL.L("plugins.hoster.vevoCom.ALLOW_HLS_4200",
        // "Load videocodec h264 4200kBit/s 1920x1080 with audio codec AAC 128kBit/s")).setDefaultValue(false));
        // getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_HLS_5200,
        // JDL.L("plugins.hoster.vevoCom.ALLOW_HLS_5200",
        // "Load videocodec h264 5200kBit/s 1920x1080 with audio codec AAC 128kBit/s")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "RTMP formats:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_RTMP_500, JDL.L("plugins.hoster.vevoCom.ALLOW_RTMP_500", "Load videocodec x264 500kBit/s 212x288 with audio codec quicktime 64kBit/s")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_RTMP_800, JDL.L("plugins.hoster.vevoCom.ALLOW_RTMP_800", "Load videocodec x264 800kBit/s 212x288 with audio codec quicktime 64kBit/s")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_RTMP_1200, JDL.L("plugins.hoster.vevoCom.ALLOW_RTMP_1200", "Load videocodec x264 1200kBit/s 768x432 with audio codec quicktime 128kBit/s")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_RTMP_1600, JDL.L("plugins.hoster.vevoCom.ALLOW_RTMP_1600", "Load videocodec x264 1600kBit/s 768x432 with audio codec quicktime 128kBit/s")).setDefaultValue(false));
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
