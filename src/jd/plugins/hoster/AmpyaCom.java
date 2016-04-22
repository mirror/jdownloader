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

import java.util.ArrayList;
import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "ampya.com" }, urls = { "http://ampyadecrypted\\.com/\\d+" }, flags = { 0 })
public class AmpyaCom extends PluginForHost {

    public AmpyaCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://ampya.com/terms-of-use";
    }

    public static final String  api_base_url = "http://ampya.com/webservice/v1";
    private static final String app          = "tvrl";

    private String              continue_url = null;
    private String              mainlink     = null;

    @SuppressWarnings({ "unchecked", "deprecation" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        continue_url = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        mainlink = link.getStringProperty("mainlink", null);
        final String videoid = link.getStringProperty("videoid", null);
        if (mainlink == null || videoid == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        initializeSession(this.br);
        br.getPage(jd.plugins.hoster.AmpyaCom.getApiUrl(mainlink));
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
        final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("clips");
        for (final Object clipo : ressourcelist) {
            entries = (LinkedHashMap<String, Object>) clipo;
            final String video_id_temp = Long.toString(DummyScriptEnginePlugin.toLong(entries.get("video_id"), 0));
            if ("0".equals(video_id_temp)) {
                continue;
            }
            if (video_id_temp.equals(videoid)) {
                continue_url = (String) entries.get("source");
                break;
            }
        }
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (continue_url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!continue_url.startsWith("http")) {
            continue_url = "http://ampya.com/webservice" + continue_url;
        }
        this.br.getPage(continue_url);

        /* Chose highest quality available */
        String rtmpurl = null;
        final String[] qualities = { "fullhd", "hd", "medium", "low", "preview" };
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
        entries = (LinkedHashMap<String, Object>) DummyScriptEnginePlugin.walkJson(entries, "streaming/qualities");
        for (final String quality : qualities) {
            rtmpurl = (String) entries.get(quality);
            if (rtmpurl != null) {
                break;
            }
        }
        if (rtmpurl == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        String playpath = new Regex(rtmpurl, "(mp4:.+)").getMatch(0);
        if (playpath == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String token = new Regex(rtmpurl, "token=([A-Za-z0-9\\-_]+)").getMatch(0);
        if (!playpath.contains("token=") && token != null) {
            playpath += "?token=" + token;
        }
        try {
            dl = new RTMPDownload(this, downloadLink, rtmpurl);
        } catch (final NoClassDefFoundError e) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "RTMPDownload class missing");
        }
        /* Setup rtmp connection */
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        rtmp.setPageUrl(mainlink);
        rtmp.setUrl(rtmpurl);
        rtmp.setPlayPath(playpath);
        rtmp.setApp(app);
        rtmp.setFlashVer("WIN 21,0,0,213");
        rtmp.setSwfUrl("http://files.ampya.com/tr_video_player/4/TRVideoPlayer.swf");
        rtmp.setResume(false);
        ((RTMPDownload) dl).startDownload();
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    public static void initializeSession(final Browser br) throws Exception {
        br.getHeaders().put("Accept", "application/json, text/javascript");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Content-Type", "application/json");
        br.postPageRaw("http://ampya.com/webservice/v1/putpat/init", "{\"app\":{\"token\":\"ampya_web\",\"version\":\"3.0.0\",\"partner_id\":1},\"viewer\":\"\"}");
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
        final String session_id = (String) DummyScriptEnginePlugin.walkJson(entries, "session/session_id");
        final String csrf_token = (String) DummyScriptEnginePlugin.walkJson(entries, "session/csrf_token");
        if (session_id == null || csrf_token == null) {
            return;
        }
        br.getHeaders().put("X-Putpat-Session-Id", session_id);
        br.getHeaders().put("X-CSRF-Token", csrf_token);
    }

    public static String getApiUrl(final String parameter) {
        final String apiurl;
        if (parameter.matches("https?://(?:www\\.)?ampya\\.com/shows/[A-Za-z0-9\\-_]+")) {
            apiurl = api_base_url + "/shows/" + new Regex(parameter, "/shows/(.+)").getMatch(0) + ".clips";
        } else if (parameter.matches("http://(?:www\\.)?ampya\\.com/artists/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+")) {
            // http://ampya.com/webservice/v1/a/phoebe-ryan/chronic-lyric-video.clips
            final Regex info = new Regex(parameter, "/artists/([A-Za-z0-9\\-_]+)/([A-Za-z0-9\\-_]+)");
            apiurl = api_base_url + "/a/" + info.getMatch(0) + "/" + info.getMatch(1) + ".clips";
        } else {
            apiurl = null;
        }

        return apiurl;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}