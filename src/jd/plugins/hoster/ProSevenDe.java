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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "prosieben.de", "prosiebenmaxx.de", "the-voice-of-germany.de", "kabeleins.de", "sat1.de", "sat1gold.de", "sixx.de" }, urls = { "http://(www\\.)?prosieben\\.de/tv/[\\w\\-]+/videos?/[\\w\\-]+", "http://www\\.prosiebenmaxx\\.de/[^<>\"\\']*?videos?/[\\w\\-]+", "http://(www\\.)?the\\-voice\\-of\\-germany\\.de/video/[\\w\\-]+", "http://(www\\.)?kabeleins\\.de/tv/[\\w\\-]+/videos?/[\\w\\-]+", "http://(www\\.)?sat1\\.de/tv/[\\w\\-]+/videos?/[\\w\\-]+", "http://(www\\.)?sat1gold\\.de/tv/[\\w\\-]+/videos?/[\\w\\-]+", "http://(www\\.)?sixx\\.de/tv/[\\w\\-]+/videos?/[\\w\\-]+" }, flags = { 32, 32, 32, 32, 32, 32, 32 })
public class ProSevenDe extends PluginForHost {

    /** Other domains: proxieben.at (redirects to .de) */
    /** Tags: prosiebensat1.de, */

    private static AtomicReference<String> agent   = new AtomicReference<String>(null);
    private HashMap<String, String>        fileDesc;
    private String                         clipUrl = null;

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
        if (agent.get() == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            agent.set(jd.plugins.hoster.MediafireCom.hbbtvUserAgent());
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        /* Possible API (needs video-ID) http://hbbtv.sat1.de/video_center?action=action.get.clip&clip_id=<videoid>&category_id=123&order=1 */
        String jsonString = br.getRegex("\"json\",\\s+(?:\"|\\')(.*?)(?:\"|\\')\\);\n").getMatch(0);
        if (jsonString == null || !br.containsHTML("SIMVideoPlayer")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        jsonString = decodeUnicode(jsonString);
        /* Small corrections for the json parser. */
        jsonString = jsonString.replace("\\\"", "'");
        jsonString = jsonString.replace("\\", "");
        try {
            jsonParser(jsonString, "downloadFilename");
        } catch (final Throwable e) {
            return AvailableStatus.UNCHECKABLE;
        }
        if (fileDesc == null || fileDesc.size() < 5) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        for (Entry<String, String> next : fileDesc.entrySet()) {
            if (next.getValue() == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }

        clipUrl = fileDesc.get("downloadFilename");

        String ext = new Regex(clipUrl, "(\\.\\w{3})$").getMatch(0);
        ext = ext == null ? ".mp4" : ext;
        downloadLink.setFinalFileName(Encoding.htmlDecode((fileDesc.get("show_artist") + "_" + fileDesc.get("title")).trim()) + ext);
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    private void setupRTMPConnection(final DownloadInterface dl, final DownloadLink downloadLink, final String[] stream) {
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        rtmp.setApp(stream[1] + stream[2].substring(stream[2].indexOf("?")));
        rtmp.setUrl(stream[0] + stream[1]);
        rtmp.setPlayPath("mp4:" + stream[2]);
        rtmp.setSwfVfy("http://is.myvideo.de/player/GP/4.3.2/player.swf");
        rtmp.setPageUrl(downloadLink.getDownloadURL());
        rtmp.setResume(true);
    }

    private void downloadRTMP(final DownloadLink downloadLink, final String[] stream) throws Exception {
        dl = new RTMPDownload(this, downloadLink, clipUrl);
        setupRTMPConnection(dl, downloadLink, stream);
        ((RTMPDownload) dl).startDownload();
    }

    /*
     * Explanation of the parameter "method" when getting downloadlink: 1=http, 2=rtmp(e)[Depends on video, usually rtmp], 3=rtmpt, 4=HLS
     * (Crypted/uncrypted depends on video)
     */
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);

        /* Let's find the downloadlink */
        final String clipID = br.getRegex("\"clip_id\":[\t\n\r ]*?\"(\\d+)\"").getMatch(0);
        if (clipID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* First check if we already have an rtmp url - usually not. */
        if (!clipUrl.startsWith("rtmp")) {
            /* TODO: Maybe implement the current version of this request though this old one still works great. */
            /*
             * Request V2:
             * http://vas.sim-technik.de/vas/live/v2/videos/<clipID>/sources/url?access_token=sat1gold&client_location=<currentURL
             * >&client_name
             * =kolibri-1.11.3-hotfix1&client_id=<clientid>&server_id=<serverid>&source_ids=0%2C6%2C4&callback=_kolibri_jsonp_callbacks
             * ._5236
             */
            br.getPage("http://ws.vtc.sim-technik.de/video/video.jsonp?clipid=" + clipID + "&app=moveplayer&method=2&callback=SIMVideoPlayer.FlashPlayer.jsonpCallback");
            getDllink();
        }
        if (clipUrl == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Workaround to avoid rtmpe --> Does not work (yet) */
        // if (clipUrl.startsWith(Encoding.Base64Decode("cnRtcGU6Ly8="))) {
        // logger.info("Trying to avoid unsupported protocol");
        // this.br = new Browser();
        // br.getHeaders().put("User-Agent", agent.get());
        // br.getPage("http://ws.vtc.sim-technik.de/video/video.jsonp?clipid=" + clipID + "&app=hbbtv&type=1&method=1&callback=video" +
        // clipID);
        // getDllink();
        // }
        if (clipUrl == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        if (clipUrl.contains("/not_available_")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "This video is not available in your country #1");
        } else if (clipUrl.contains("wrong_cc_de_en_")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "This video is not available in your country #2");
        }
        if (clipUrl.startsWith("rtmp")) {
            clipUrl = clipUrl.replace("mp4:", "");
            String[] stream = new Regex(clipUrl, "(rtmp.?://[0-9a-z]+\\.fplive\\.net/)([0-9a-z]+/[\\w\\-]+/\\d+/)(.*?)$").getRow(0);
            if (stream != null && stream.length == 3) {
                downloadRTMP(downloadLink, stream);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else {
            /* Happens if usually the clip is streamed via rtmpe --> No HbbTV version available either. */
            if (clipUrl.contains("no_flash_de")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Protocol rtmpe:// not supported");
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, clipUrl, true, 0);
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

    /* Return string not needed but maybe useful for the future */
    private String getDllink() {
        String dllink = br.getRegex("\"VideoURL\"\\s?:\\s?\"((rtmp|http)[^\"]+)").getMatch(0);
        if (dllink != null) {
            dllink = dllink.replaceAll("\\\\", "");
        }
        clipUrl = dllink;
        return dllink;
    }

    private void jsonParser(final String json, final String path) throws Exception {
        final org.codehaus.jackson.map.ObjectMapper mapper = new org.codehaus.jackson.map.ObjectMapper();
        final org.codehaus.jackson.JsonNode rootNode = mapper.readTree(json);
        final Iterator<org.codehaus.jackson.JsonNode> catIter = rootNode.get("categoryList").iterator();
        while (catIter.hasNext()) {
            final Iterator<org.codehaus.jackson.JsonNode> clipIter = catIter.next().path("clipList").iterator();
            while (clipIter.hasNext()) {
                final org.codehaus.jackson.JsonNode ta = clipIter.next();
                final org.codehaus.jackson.JsonNode tb = ta.path("metadata");
                fileDesc = new HashMap<String, String>();
                if (ta.path("title") != null) {
                    fileDesc.put("title", ta.path("title").getTextValue());
                }
                if (ta.path("id") != null) {
                    fileDesc.put("id", ta.path("id").getTextValue());
                }
                if (tb.path(path) != null) {
                    fileDesc.put(path, tb.path(path).getTextValue());
                }
                if (tb.path("show_artist") != null) {
                    fileDesc.put("show_artist", tb.path("show_artist").getTextValue());
                }
                if (tb.path("geoblocking") != null) {
                    fileDesc.put("geoblocking", tb.path("geoblocking").getTextValue());
                }
            }
        }
    }

    public String decodeUnicode(final String s) {
        final Pattern p = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
        String res = s;
        final Matcher m = p.matcher(res);
        while (m.find()) {
            res = res.replaceAll("\\" + m.group(0), Character.toString((char) Integer.parseInt(m.group(1), 16)));
        }
        return res;
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