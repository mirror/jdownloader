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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rtl2.de" }, urls = { "http://(www\\.)?rtl2\\.de/[\\w\\-]+/video/[\\w\\-]+/([\\w\\-]+/)?" }, flags = { 32 })
public class RTL2De extends PluginForHost {

    String DLCONTENT = null;

    public RTL2De(final PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall((Math.round(Math.random() * 3 + Math.random() * 3) + 3) * 1000l);
    }

    private void download(final DownloadLink downloadLink) throws Exception {
        if (DLCONTENT.startsWith("rtmp")) {
            final String[] urlTmp = DLCONTENT.split("/", 5);
            if (urlTmp != null && urlTmp.length < 5) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            dl = new RTMPDownload(this, downloadLink, DLCONTENT);

            String protocol = urlTmp[0];
            String host = urlTmp[2];
            String app = urlTmp[3];
            String path = urlTmp[4];

            final String url = protocol + "//" + host + ":1935/" + app;
            /* ondemand */
            String playpath = "mp4:" + path;
            /* vod */
            if ("vod".equals(app) || (path.startsWith("flv_free/") || path.startsWith("flv_dach/")) && !path.endsWith(".f4v")) playpath = path.substring(0, path.lastIndexOf("."));

            DLCONTENT = url + "@" + playpath;
            setupRTMPConnection(dl);
            if (!((RTMPDownload) dl).startDownload()) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 60 * 1000l);

        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

    }

    @Override
    public String getAGBLink() {
        return "http://www.rtl2.de/3733.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 2;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        download(downloadLink);
    }

    private HashMap<String, String> jsonParser(final String param) throws Exception {
        final Browser json = br.cloneBrowser();
        json.getPage("http://www.rtl2.de/video/php/get_video.php?" + param);
        String streamUrl = null, name = null, title = null;
        try {
            final org.codehaus.jackson.map.ObjectMapper mapper = new org.codehaus.jackson.map.ObjectMapper();
            final org.codehaus.jackson.JsonNode rootNode = mapper.readTree(json.toString());
            streamUrl = rootNode.path("video").path("streamurl").getTextValue();
            name = rootNode.path("video").path("vifo_name").getTextValue();
            title = rootNode.path("video").path("titel").getTextValue();
        } catch (final Throwable e) {
            return null;
        }
        if (streamUrl == null || name == null || title == null) { return null; }
        final HashMap<String, String> ret = new HashMap<String, String>();
        ret.put("streamurl", streamUrl);
        ret.put("name", name);
        ret.put("title", title);
        return ret;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("<title>RTL2 \\- Seite nicht gefunden \\(404\\)</title>")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        final String param = br.getRegex("(vico_id=\\d+\\&vivi_id=\\d+)").getMatch(0);
        if (param == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        final HashMap<String, String> ret = new HashMap<String, String>(jsonParser(param));
        if (ret == null || ret.size() == 0) { return AvailableStatus.UNCHECKED; }
        downloadLink.setFinalFileName(Encoding.htmlDecode(ret.get("name") + "__" + ret.get("title")).trim() + ".flv");
        DLCONTENT = ret.get("streamurl");
        return AvailableStatus.TRUE;
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

    private void setupRTMPConnection(final DownloadInterface dl) {
        final jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();

        rtmp.setPlayPath(DLCONTENT.split("@")[1]);
        rtmp.setSwfVfy("http://www.rtl2.de/flashplayer/vipo_player.swf");
        rtmp.setFlashVer("WIN 10,1,102,64");
        rtmp.setUrl(DLCONTENT.split("@")[0]);
        rtmp.setResume(true);
        rtmp.setTimeOut(-5);
        rtmp.setBuffer(60 * 1000);
    }

}