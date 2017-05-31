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

import jd.PluginWrapper;
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

import org.jdownloader.controlling.ffmpeg.json.Stream;
import org.jdownloader.controlling.ffmpeg.json.StreamInfo;
import org.jdownloader.downloader.hls.HLSDownloader;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "smashcast.tv" }, urls = { "https?://(?:www\\.)?(?:hitbox\\.tv/video/\\d+|smashcast\\.tv/[^/]+/videos/\\d+)" })
public class SmashcastTv extends PluginForHost {
    public SmashcastTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    private Browser ajax         = null;
    private String  url          = null;
    private boolean server_error = false;

    @Override
    public String getAGBLink() {
        return "https://www.smashcast.tv/legal/terms-of-use";
    }

    @Override
    public String rewriteHost(String host) {
        if ("hitbox.tv".equals(getHost())) {
            return "smashcast.tv";
        }
        return super.rewriteHost(host);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        server_error = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String vid = new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0);
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135 Safari/537.36 ");
        br.getPage(downloadLink.getDownloadURL());
        ajax = br.cloneBrowser();
        ajax.getHeaders().put("Accept", "application/json, text/plain, */*");
        ajax.getPage("https://www." + this.getHost() + "/api/media/video/" + vid + "?showHidden=true");
        // auth sig
        final String auth = PluginJSonUtils.getJsonValue(ajax, "media_file");
        final String userId = PluginJSonUtils.getJsonValue(ajax, "user_id");
        final String userName = PluginJSonUtils.getJsonValue(ajax, "user_name");
        final String vidName = PluginJSonUtils.getJsonValue(ajax, "media_title");
        // not sure if this is needed, its done trying to get channel info, and then auths
        // Request URL:https://www.hitbox.tv/api/auth/media/video/88e235d7ffc3f79cf1de079646326890f8bcee4f-543bd5693d819
        // Request Method:OPTIONS
        // Status Code:200 OK
        // ajax = br.cloneBrowser();
        // ajax.getHeaders().put("Accept", "*/*");
        // ajax.getHeaders().put("Access-Control-Request-Headers", "accept, content-type");
        // ajax.getHeaders().put("Access-Control-Request-Method", "POST");
        // Request r = new Request("https://www.hitbox.tv/api/auth/media/video/" + vid) {
        //
        // @Override
        // public void preRequest() throws IOException {
        // }
        //
        // @Override
        // public long postRequest() throws IOException {
        // return 0;
        // }
        //
        // {
        // this.httpConnection.setRequestMethod(RequestMethod.OPTIONS);
        // }
        //
        // };
        // ajax.openRequestConnection(r);
        // same with this, say this is needed though not sure
        // auth
        // Request URL:https://www.hitbox.tv/api/auth/media/video/88e235d7ffc3f79cf1de079646326890f8bcee4f-543bd5693d819
        // Request Method:OPTIONS
        // Status Code:200 OK
        ajax = br.cloneBrowser();
        ajax.getHeaders().put("Accept", "application/json, text/plain, */*");
        ajax.getHeaders().put("Content-Type", "application/json;charset=UTF-8");
        final String crap = "{\"media_name\":\"" + auth + "\",\"media_type\":\"video\",\"authToken\":null}";
        ajax.postPageRaw("https://www." + this.getHost() + "/api/auth/media/video/" + auth, Encoding.urlEncode(crap));
        // Request URL:http://www.hitbox.tv/api/player/config/video/286169?redis=true&embed=false&qos=false&redis=true&showHidden=true
        // Request Method:GET
        ajax = br.cloneBrowser();
        ajax.getHeaders().put("Accept", "application/json, text/plain, */*");
        ajax.getHeaders().put("Content-Type", "application/json;charset=UTF-8");
        ajax.getPage("http://www." + this.getHost() + "/api/player/config/video/" + vid + "?redis=true&embed=false&qos=false&redis=true&showHidden=true");
        url = PluginJSonUtils.getJsonValue(ajax, "url");
        if (url == null || url.equals("")) {
            /* 2017-01-21: New */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (url != null && !url.endsWith(".m3u8")) {
            // http dl
            // {"key":"#$54d46eaa112f0508979","play":null,"clip":{"autoPlay":true,"autoBuffering":true,"bufferLength":"2","eventCategory":"QueenBee\/video\/5900","baseUrl":"http:\/\/edge.vie.hitbox.tv\/static\/videos\/recordings","url":"queenbee-1394512729.flv.mp4","stopLiveOnPause":true,"live":false,"smoothing":true,"provider":"pseudo","scaling":"fit","bitrates":[{"url":"queenbee-1394512729.flv.mp4","bitrate":738,"label":"HD
            // 720p","provider":"rtmpHitbox","isDefault":true}],"controls":false,"type":"video","adsPlayed":false},"plugins":{"pseudo":{"queryString":"?start=${start}","url":"flowplayer.pseudostreaming-3.2.12.swf"},"controls":null,"info":{"display":"none","url":"flowplayer.content-3.2.8.swf","html":"<p
            // align=\"center\"><\/p>","width":"50%","height":30,"backgroundColor":"#1A1A1A","backgroundGradient":"none","opacity":"1","borderRadius":10,"borderColor":"#999999","border":0,"color":"#FFFFFF","bottom":60,"zIndex":"10","closeButton":true,"style":{"p":{"fontSize":16,"fontFamily":"verdana,arial,helvetica","fontWeight":"normal"}}},"gatracker":{"url":"flowplayer.analytics-3.2.9.1.swf","event":{"all":true},"debug":false,"accountId":"UA-42900118-2"},"ova":{"url":"flowplayer.liverail-3.2.7.4.swf","LR_PUBLISHER_ID":20341,"LR_SCHEMA":"vast2-vpaid","LR_ADUNIT":"in","LR_VIDEO_POSITION":0,"LR_AUTOPLAY":1,"LR_CONTENT":6,"LR_TITLE":"1d275b55e3d50ee88df280b1c2caa07704717dcb1_531f22770d2bf","LR_VIDEO_ID":"109880","LR_MUTED":0,"CACHEBUSTER":1422982983,"TIMESTAMP":1422982983,"LR_LAYOUT_SKIN_MESSAGE":"Advertisement:
            // Stream will resume in {COUNTDOWN}
            // seconds.","LR_LIVESTREAM":1,"LR_LAYOUT_SKIN_ID":2,"LR_LAYOUT_LINEAR_PAUSEONCLICKTHRU":0,"LR_BITRATE":"high","LR_VIDEO_URL":"http:\/\/www.hitbox.tv\/video\/109880","LR_DESCRIPTION":"1d275b55e3d50ee88df280b1c2caa07704717dcb1_531f22770d2bf","LR_IP":"203.161.76.166"}},"canvas":{"backgroundGradient":"none"},"log":{"level":"debug","filter":"org.osmf*"},"showErrors":false,"settings":{"media_id":"-1","max_buffer_count":"3","buffer_length":"2","max_roundtrips":"3","reset_timeout":"60000","play_timeout":"15000","start_timeout":"10000","ad_plugin":"liverail-off","default_br":null,"enabled":"1"},"playlist":[]}
            final String server = PluginJSonUtils.getJsonValue(ajax, "baseUrl");
            final String label = PluginJSonUtils.getJsonValue(ajax, "label");
            if (!"null".equalsIgnoreCase(server) && server != null) {
                url = server + "/" + url;
            } else {
                url = "http://edge.vie." + this.getHost() + "/static/videos/recordings/" + url;
            }
            String ext = getFileNameExtensionFromString(url, ".mp4");
            if (ext == null || ext.length() > 5) {
                ext = ".mp4";
            }
            downloadLink.setFinalFileName(userName + " - " + vid + " - " + vidName + " - " + (label != null ? label.replace("HD ", "") : "") + ext);
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openGetConnection(url);
                if (!con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                return AvailableStatus.TRUE;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        } else {
            // hls
            // {"key":"#$54d46eaa112f0508979","play":null,"clip":{"autoPlay":true,"autoBuffering":true,"bufferLength":"2","eventCategory":"KingofPol\/video\/819952","baseUrl":null,"url":"http:\/\/edge.hls.vods.hitbox.tv\/static\/videos\/vods\/kingofpol\/88e235d7ffc3f79cf1de079646326890f8bcee4f-543bd5693d819\/kingofpol\/index.m3u8","stopLiveOnPause":true,"live":false,"smoothing":true,"provider":"pseudo","scaling":"fit","bitrates":[{"url":"\/kingofpol\/88e235d7ffc3f79cf1de079646326890f8bcee4f-543bd5693d819\/kingofpol\/index.m3u8","bitrate":0,"label":"HD
            // 1080p","provider":"rtmpHitbox","isDefault":true}],"controls":false,"type":"video","adsPlayed":false},"plugins":{"pseudo":{"url":"flashlsFlowPlayer.swf","hls_debug":false,"hls_debug2":false,"hls_lowbufferlength":5,"hls_minbufferlength":10,"hls_maxbufferlength":60,"hls_startfromlowestlevel":false,"hls_seekfromlowestlevel":false,"hls_live_flushurlcache":false,"hls_seekmode":"SEGMENT"},"controls":null,"info":{"display":"none","url":"flowplayer.content-3.2.8.swf","html":"<p
            // align=\"center\"><\/p>","width":"50%","height":30,"backgroundColor":"#1A1A1A","backgroundGradient":"none","opacity":"1","borderRadius":10,"borderColor":"#999999","border":0,"color":"#FFFFFF","bottom":60,"zIndex":"10","closeButton":true,"style":{"p":{"fontSize":16,"fontFamily":"verdana,arial,helvetica","fontWeight":"normal"}}},"gatracker":{"url":"flowplayer.analytics-3.2.9.1.swf","event":{"all":true},"debug":false,"accountId":"UA-42900118-2"},"ova":{"url":"flowplayer.liverail-3.2.7.4.swf","LR_PUBLISHER_ID":20341,"LR_SCHEMA":"vast2-vpaid","LR_ADUNIT":"in","LR_VIDEO_POSITION":0,"LR_AUTOPLAY":1,"LR_CONTENT":6,"LR_TITLE":"88e235d7ffc3f79cf1de079646326890f8bcee4f-543bd5693d819","LR_VIDEO_ID":"286169","LR_MUTED":0,"CACHEBUSTER":1422013048,"TIMESTAMP":1422013048,"LR_LAYOUT_SKIN_MESSAGE":"Advertisement:
            // Stream will resume in {COUNTDOWN}
            // seconds.","LR_LIVESTREAM":1,"LR_LAYOUT_SKIN_ID":2,"LR_LAYOUT_LINEAR_PAUSEONCLICKTHRU":0,"LR_BITRATE":"high","LR_VIDEO_URL":"http:\/\/www.hitbox.tv\/video\/286169","LR_DESCRIPTION":"88e235d7ffc3f79cf1de079646326890f8bcee4f-543bd5693d819","LR_IP":"203.161.76.166"}},"canvas":{"backgroundGradient":"none"},"log":{"level":"debug","filter":"org.osmf*"},"showErrors":false,"settings":{"media_id":"-1","max_buffer_count":"3","buffer_length":"2","max_roundtrips":"3","reset_timeout":"60000","play_timeout":"15000","start_timeout":"10000","ad_plugin":"liverail-off","default_br":null,"enabled":"1"},"playlist":[]}
            checkFFProbe(downloadLink, "File Checking a HLS Stream");
            if (downloadLink.getBooleanProperty("encrypted")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Encrypted HLS is not supported");
            }
            br.getHeaders().put("Accept", "*/*");
            br.getHeaders().put("X-Requested-With", "ShockwaveFlash/17.0.0.169");
            br.getHeaders().put("Referer", downloadLink.getContentUrl());
            // without this it will fail.
            br.getPage(url);
            url = br.getRegex("https?://[^\r\n]+\\.m3u8").getMatch(-1);
            if (url == null) {
                /* Either no hls url or instead a non-working http url. Such videos won't play via browser either! */
                server_error = true;
                return AvailableStatus.TRUE;
            }
            final HLSDownloader downloader = new HLSDownloader(downloadLink, br, url);
            final StreamInfo streamInfo = downloader.getProbe();
            if (streamInfo == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String extension = ".m4a";
            for (Stream s : streamInfo.getStreams()) {
                if ("video".equalsIgnoreCase(s.getCodec_type())) {
                    extension = ".mp4";
                    if (s.getHeight() > 0) {
                        downloadLink.setProperty("videoQuality", s.getHeight() + "p");
                    }
                    if (s.getCodec_name() != null) {
                        downloadLink.setProperty("videoCodec", s.getCodec_name());
                    }
                } else if ("audio".equalsIgnoreCase(s.getCodec_type())) {
                    if (s.getBit_rate() != null) {
                        downloadLink.setProperty("audioBitrate", (Integer.parseInt(s.getBit_rate()) / 1024) + "kbits");
                    }
                    if (s.getCodec_name() != null) {
                        downloadLink.setProperty("audioCodec", s.getCodec_name());
                    }
                }
            }
            downloadLink.setProperty("extension", extension);
            final String filename = userName + " - " + vid + " - " + vidName + "-" + downloadLink.getStringProperty("videoQuality", "Unknown") + "_" + downloadLink.getStringProperty("videoCodec", "Unknown") + "-" + downloadLink.getStringProperty("audioBitrate", "Unknown") + "_" + downloadLink.getStringProperty("audioCodec", "Unknown");
            downloadLink.setName(filename + extension);
            return AvailableStatus.TRUE;
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (server_error) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        }
        if (url.contains(".m3u8")) {
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            if (downloadLink.getBooleanProperty("encrypted")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Encrypted HLS is not supported");
            }
            br.getHeaders().put("Accept", "*/*");
            br.getHeaders().put("X-Requested-With", "ShockwaveFlash/17.0.0.169");
            br.getHeaders().put("Referer", downloadLink.getContentUrl());
            dl = new HLSDownloader(downloadLink, br, url);
            dl.startDownload();
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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
