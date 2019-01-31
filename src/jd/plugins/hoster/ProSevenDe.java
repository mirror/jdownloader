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

import java.util.concurrent.atomic.AtomicReference;

import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "7tv.de" }, urls = { "http://7tvdecrypted\\.de/\\d+" })
public class ProSevenDe extends PluginForHost {
    /** Other domains: proxieben.at (redirects to .de) */
    /** Tags: prosiebensat1.de, */
    /** Interesting extern lib: https://github.com/bromix/repository.bromix.storage/tree/master/plugin.video.7tv */
    private static final String            URLTEXT_NO_FLASH         = "no_flash_de";
    private static final String            URLTEXT_COUNTRYBLOCKED_1 = "/not_available_";
    private static final String            URLTEXT_COUNTRYBLOCKED_2 = "wrong_cc_de_en_";
    private static AtomicReference<String> agent_hbbtv              = new AtomicReference<String>(null);
    private static AtomicReference<String> agent_normal             = new AtomicReference<String>(null);
    private String                         json                     = null;
    private static final String[][]        bitrate_info             = { { "tp12", "" }, { "tp11", "2628" }, { "tp10", "2328" }, { "tp09", "1896" }, { "tp08", "" }, { "tp07", "" }, { "tp06", "1296" }, { "tp05", "664" }, { "tp04", "" }, { "tp03", "" }, { "tp02", "" }, { "tp01", "" } };

    public ProSevenDe(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.prosieben.de/service/nutzungsbedingungen/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private void prepareBrowser() {
        if (agent_hbbtv.get() == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            agent_hbbtv.set(jd.plugins.hoster.MediafireCom.hbbtvUserAgent());
        }
        if (agent_normal.get() == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            agent_normal.set(jd.plugins.hoster.MediafireCom.stringUserAgent());
        }
    }

    /* E.g. information about a single video: http://contentapi.sim-technik.de/mega-app/v2/pro7/phone/video/4041141 */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        prepareBrowser();
        br.setFollowRedirects(true);
        final String decrypter_filename = downloadLink.getStringProperty("decrypter_filename", null);
        final String mainlink = getMainlink(downloadLink);
        if (mainlink == null || decrypter_filename == null) {
            /* E.g. for old links! */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.getPage(mainlink);
        if (isOffline(this.br)) {
            /* Page offline */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        downloadLink.setFinalFileName(decrypter_filename);
        return AvailableStatus.TRUE;
    }

    private String getMainlink(final DownloadLink dl) {
        return dl.getStringProperty("mainlink", null);
    }

    public static boolean isOffline(final Browser br) {
        boolean offline = false;
        if (br.getHttpConnection().getResponseCode() == 404) {
            /* Page offline */
            offline = true;
        }
        if (br.containsHTML(">Das Video ist nicht mehr verfügbar|Leider ist das Video nicht mehr verfügbar")) {
            /* Video offline */
            offline = true;
        }
        return offline;
    }

    /*
     * Explanation of the parameter "method" when getting downloadlink: 1=http, 2=rtmp(e)[Depends on video, usually rtmp], 3=rtmpt, 4=HLS
     * (Crypted/uncrypted depends on video)
     */
    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        /* Let's find the downloadlink */
        final String clip_id = new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0);
        if (clip_id == null) {
            /* This should never happen! */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* TODO: Maybe implement the current version of this request though this old one still works great. */
        /*
         * Request V2: http://vas.sim-technik.de/vas/live/v2/videos/<clipID>/sources/url?access_token=sat1gold&client_location=<currentURL
         * >&client_name
         * =kolibri-1.11.3-hotfix1&client_id=<clientid>&server_id=<serverid>&source_ids=0%2C6%2C4&callback=_kolibri_jsonp_callbacks ._5236
         */
        /* HLS (does not work as a workaround for for rtmpe streams): http://vas.sim-technik.de/video/playlist.m3u8?ClipID=<ClipID> */
        // http://vas.sim-technik.de/video/video.json?clipid=clipID&app=megapp&method=4&drm=marlin2
        /*
         * Example of an hls url:
         * http://vodakpsdhls-vh.akamaihd.net/i/clips/09/09/4117599-1o6e6wx-tp,03,04,05,06,.mp4.csmil/master.m3u8?hdnea=st%3D1448240481%
         * 7Eexp%3D1448326881%7Eacl%3D%2Fi%2Fclips%2F09%2F09%2F4117599-1o6e6wx-tp%2C03%2C04%2C05%2C06%2C.mp4%2A%7Ehmac%
         * 3Dedb40ef2bc90a159a2fc660e108a8e4ed9f934ee5c2e5376222ecc54bfe47f3a&__a__=off
         */
        /*
         * First try to get a http stream --> Faster downloadspeed & more reliable/stable connection than rtmp and slightly better
         * videoquality
         */
        this.br = new Browser();
        /* User-Agent not necessarily needed */
        br.getHeaders().put("User-Agent", agent_hbbtv.get());
        /* method=6 needed so that the Highest quality-trick works see 'getDllink' method */
        br.getPage("http://vas.sim-technik.de/video/video.jsonp?clipid=" + clip_id + "&app=hbbtv&type=1&method=6&callback=video" + clip_id);
        getDllink();
        if (json == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (json.contains(URLTEXT_NO_FLASH) || json.contains(URLTEXT_COUNTRYBLOCKED_1) || json.contains(URLTEXT_COUNTRYBLOCKED_2)) {
            this.br = new Browser();
            /* User-Agent not necessarily needed */
            br.getHeaders().put("User-Agent", agent_normal.get());
            /* http stream not available[via the above method] --> Maybe here --> Or it's either rtmp or rtmpe */
            br.getPage("http://vas.sim-technik.de/video/video.jsonp?clipid=" + clip_id + "&app=moveplayer&method=6&callback=SIMVideoPlayer.FlashPlayer.jsonpCallback");
            getDllink();
            if (json == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        boolean failed = false;
        if (json.contains(URLTEXT_COUNTRYBLOCKED_1)) {
            failed = true;
            /* 2016-06-09: Try hls-fallback instead! */
            // throw new PluginException(LinkStatus.ERROR_FATAL, "This video is not available in your country #1");
        } else if (json.contains(URLTEXT_COUNTRYBLOCKED_2)) {
            failed = true;
            /* 2016-06-09: Try hls-fallback instead! */
            // throw new PluginException(LinkStatus.ERROR_FATAL, "This video is not available in your country #2");
        }
        if (json.startsWith("rtmp")) {
            /* rtmp */
            downloadRTMP(downloadLink);
        } else if (failed) {
            /* hls - try hls fallback! */
            /* 2016-06-16: TODO: Try APIV2 instead to find higher quality hds versions */
            /* THX: https://github.com/rg3/youtube-dl/blob/f9b1529af8aec98bffd42edb5be15e1ada791a20/youtube_dl/extractor/prosiebensat1.py */
            // final String access_token = "prosieben";
            // final String client_name = "kolibri-2.0.19-splec4";
            // final String client_location = Encoding.urlEncode(getMainlink(downloadLink));
            // final String videos_api_url = "http://vas.sim-technik.de/vas/live/v2/videos?" + "access_token=" + access_token +
            // "&client_name=" + client_name + "&client_location=" + client_location + "&ids=" + clip_id;
            // final String g = "01!8d8F_)r9]4s[qeuXfP%";
            // final String client_id = "" + JDHash.getSHA1(Encoding.UTF8Encode(clip_id + g + access_token + client_location + g +
            // client_name));
            // final String sources_api_url = "http://vas.sim-technik.de/vas/live/v2/videos/" + clip_id + "/sources?" + "access_token=" +
            // access_token + "&client_name=" + client_name + "&client_location=" + client_location;
            this.br.setFollowRedirects(true);
            br.getPage("http://vas.sim-technik.de/video/playlist.m3u8?ClipID=" + clip_id);
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
            if (hlsbest == null) {
                /* Fallback failed - no way to (legally) download this content! */
                logger.info("Seems like only encrypted HDS/RTMPE streams are available!");
                throw new PluginException(LinkStatus.ERROR_FATAL, "Protocol rtmpe:// not supported");
            }
            final String url_hls = hlsbest.getDownloadurl();
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            dl = new HLSDownloader(downloadLink, br, url_hls);
            dl.startDownload();
        } else {
            /* Happens if usually the clip is streamed via rtmpe --> No HbbTV version available either. */
            if (json.contains(URLTEXT_NO_FLASH)) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Protocol rtmpe:// not supported");
            }
            /*
             * TODO: Instead of just trying all qualities, consider to use the f4mgenerator XML file to find the existing qualities:
             * http://vas.sim-technik.de/f4mgenerator.f4m?cid=3868276&ttl=604800&access_token=kabeleins&cdn=akamai&token=
             * a3c706238cec19617b8e70b64480fa20aacc2a162a3bbd21294a8ddaf0209699&g=TGENNQIQUMYD&hdcore=3.7.0&plugin=aasp-3.7.0.39.44
             *
             * ... but it might happen that not all are listed so maybe trying all possible qualities makes more sense especially if one of
             * them is down e.g. because of server issues.
             */
            String dllink_temp = null;
            for (final String[] single_bitrate_info : bitrate_info) {
                /* Highest quality-trick */
                final String quality_current = new Regex(json, "(tp\\d{2}\\.mp4\\?token=)").getMatch(0);
                final String quality_highest = single_bitrate_info[0] + ".mp4?token=";
                if (quality_current != null) {
                    dllink_temp = json.replace(quality_current, quality_highest);
                    if (checkDirectLink(dllink_temp)) {
                        json = dllink_temp;
                        break;
                    }
                }
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, json, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                }
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    @SuppressWarnings("deprecation")
    private void downloadRTMP(final DownloadLink downloadLink) throws Exception {
        final String protocol = new Regex(this.json, "^(rtmp(?:e|t)?://)").getMatch(0);
        String app;
        if (protocol.equals("rtmpe://")) {
            app = "psdvodrtmpdrm";
            /*
             * We can still get rtmpe urls via the old API but they won't work anyways as they use handshake type 9 which (our) rtmpdump
             * does not support.
             */
            throw new PluginException(LinkStatus.ERROR_FATAL, "rtmpe:// not supported!");
        } else {
            app = "psdvodrtmp";
        }
        // app = "psdvodrtmpdrm";
        String url = protocol + app + ".fplive.net:1935/" + app;
        // url = "rtmpe://psdvodrtmpdrm.fplive.net:1935/psdvodrtmp";
        final String playpath = new Regex(this.json, "(mp4:.+)").getMatch(0);
        if (playpath == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // playpath = playpath.replace("?start_time=", "?country=DE&start_time=");
        dl = new RTMPDownload(this, downloadLink, json);
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        /* Setup connection */
        rtmp.setApp(app);
        rtmp.setUrl(url);
        rtmp.setPlayPath(playpath);
        rtmp.setFlashVer("WIN 17,0,0,169");
        rtmp.setSwfVfy("http://is.myvideo.de/player/GP/4.3.6/player.swf");
        rtmp.setPageUrl(downloadLink.getDownloadURL());
        rtmp.setResume(true);
        ((RTMPDownload) dl).startDownload();
    }

    private boolean checkDirectLink(final String directurl) {
        if (directurl != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openHeadConnection(directurl);
                if (!con.getContentType().contains("html") && con.getResponseCode() == 200) {
                    return true;
                }
            } catch (final Exception e) {
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return false;
    }

    /* Return string not needed but maybe useful for the future */
    private String getDllink() {
        String dllink = br.getRegex("\"VideoURL\"\\s?:\\s?\"((rtmp|http)[^\"]+)").getMatch(0);
        if (dllink != null) {
            dllink = dllink.replaceAll("\\\\", "");
        }
        json = dllink;
        return dllink;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}