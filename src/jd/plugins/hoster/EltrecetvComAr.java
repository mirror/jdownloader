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

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "eltrecetv.com.ar" }, urls = { "http://(www\\.)?eltrecetv\\.com\\.ar/[^<>\"/]+/[^<>\"/]+/\\d+/.+" }, flags = { 0 })
public class EltrecetvComAr extends PluginForHost {

    private String DLLINK = null;

    public EltrecetvComAr(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.eltrecetv.com.ar/terminos";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        download(downloadLink);
    }

    private void setupRTMPConnection(String[] stream, DownloadInterface dl) {
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        rtmp.setPlayPath("mp4:" + stream[1]);
        rtmp.setUrl(stream[0]);
        rtmp.setApp(new Regex(DLLINK, "//.*?/(.*?)@").getMatch(0));
        rtmp.setSwfVfy(stream[2]);
        rtmp.setPageUrl(stream[3]);
        rtmp.setFlashVer("WIN 10,1,102,64");
        rtmp.setResume(false);
        rtmp.setTimeOut(10);
    }

    private void download(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String[] stream = DLLINK.split("@");
        if (DLLINK.startsWith("rtmp")) {
            dl = new RTMPDownload(this, downloadLink, stream[0] + stream[1]);
            setupRTMPConnection(stream, dl);
            ((RTMPDownload) dl).startDownload();
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        String dllink = downloadLink.getDownloadURL();
        br.getPage(dllink);
        if (br.getRequest().getHttpConnection().getResponseCode() == 404) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?) \\|.*?</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("\\s+<h1>(.*?)</h1>").getMatch(0);
        }
        String id = new Regex(dllink, "/(\\d+)/[^/]+$").getMatch(0);
        br.getPage("http://www.eltrecetv.com.ar/playlist/" + id);
        String streamer = br.getRegex("<jwplayer:streamer>(rtmp.*?)</jwplayer:streamer>").getMatch(0);
        String playpath = br.getRegex("<media:content bitrate=\"\\d+\" url=\"([^\"]+)").getMatch(0);

        if (filename == null || streamer == null || playpath == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        DLLINK = streamer + "@" + playpath + "@http://cdn.eltrecetv.com.ar/sites/all/libraries/jwplayer/player.swf@" + downloadLink.getDownloadURL();
        downloadLink.setFinalFileName(filename + ".mp4");
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