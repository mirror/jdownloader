//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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

import java.util.Calendar;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "u-tube.ru" }, urls = { "http://(www\\.)?u\\-tube\\.ru/pages/video/\\d+/" }, flags = { 32 })
public class UTubeRu extends PluginForHost {

    private String DLLINK;

    public UTubeRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.u-tube.ru/info/agreement/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        download(downloadLink);
    }

    private void setupRTMPConnection(String[] stream, DownloadInterface dl) {
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        rtmp.setUrl(stream[0]);
        rtmp.setSwfVfy(stream[2]);
        rtmp.setConn("O:1");
        rtmp.setConn("NO:capabilities:1");
        rtmp.setConn("O:0");
        rtmp.setConn("NS:sid:" + stream[4]);
        rtmp.setConn("NS:key:" + stream[3]);
        rtmp.setConn("O:0");
        rtmp.setPlayPath(stream[1]);
        rtmp.setResume(true);
    }

    private void download(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String[] stream = DLLINK.split("@");
        if (DLLINK.startsWith("rtmp")) {
            // downloadLink.setProperty("FLVFIXER", true);
            dl = new RTMPDownload(this, downloadLink, stream[0]);
            setupRTMPConnection(stream, dl);
            ((RTMPDownload) dl).startDownload();
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.setCustomCharset("utf-8");
        String dllink = downloadLink.getDownloadURL();
        br.getPage(dllink);
        if (br.getRedirectLocation() != null) {
            if (br.getRedirectLocation().endsWith("pages/video/")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            br.setFollowRedirects(true);
            br.getPage(dllink);
        }
        String next = br.getRegex("flashvars\\.file\\s+=\\s+\"(http://.*?)\\&key=").getMatch(0);
        String flashUrl = br.getRegex("swfobject\\.embedSWF\\(\"(http://[^\"]+)\",").getMatch(0);
        if (next == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        Calendar cal = Calendar.getInstance();
        br.getPage(next + "&ts=" + cal.get(Calendar.DAY_OF_MONTH));
        String fileName = br.getRegex("<title>\\!\\[CDATA\\[([^\\]]+)\\]\\]</title>").getMatch(0);
        String streamer = br.getRegex("<st>(rtmp[^<]+)</st>").getMatch(0);
        String playpath = br.getRegex("<location>([^<]+)</location>").getMatch(0);
        String key = br.getRegex("<key>([^<]+)</key>").getMatch(0);
        String sid = br.getRegex("sid=\"([^\"]+)\"").getMatch(0);
        if (fileName == null || streamer == null || playpath == null || key == null || sid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        playpath = playpath.substring(0, playpath.lastIndexOf("."));
        fileName = Encoding.htmlDecode(fileName.trim() + ".mp4");
        if (flashUrl == null) flashUrl = "http://u-tube.ru//upload/others/flvplayer.swf?20121215";

        DLLINK = streamer + "@" + playpath + "@" + flashUrl + "@" + key + "@" + sid;
        downloadLink.setName(fileName);
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

}