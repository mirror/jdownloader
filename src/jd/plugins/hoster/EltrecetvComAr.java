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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "eltrecetv.com.ar" }, urls = { "http://(www\\.)?eltrecetv\\.com\\.ar/[^<>\"/]+/[^<>\"/]+" }, flags = { 0 })
public class EltrecetvComAr extends PluginForHost {

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

    private static final String app = "vod/13tv/";

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRequest().getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.containsHTML("class=\"player\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<title>(.*?) \\|.*?</title>").getMatch(0);

        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename).trim();
        downloadLink.setFinalFileName(filename + ".mp4");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        download(downloadLink);
    }

    private void download(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        final String[] qualities = { "1080", "720", "480", "360", "240" };
        String qualitiesjson = br.getRegex("data\\-levels=\\'\\[(.*?)\\]").getMatch(0);
        final String streamer = br.getRegex("data\\-streamer=\"([^<>\"]*?)\"").getMatch(0);
        if (streamer == null || qualitiesjson == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        qualitiesjson = qualitiesjson.replace("\\", "");
        String playpath = null;
        for (final String quality : qualities) {
            playpath = new Regex(qualitiesjson, "\"file\":\"([^<>\"]*?\\-" + quality + ".mp4)\"").getMatch(0);
            if (playpath != null) {
                break;
            }
        }
        if (playpath == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        playpath = "mp4:13tv/" + playpath;
        final String rtmp_r = "rtmp://" + streamer + "/" + app;

        dl = new RTMPDownload(this, downloadLink, rtmp_r);
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        rtmp.setPlayPath(playpath);
        rtmp.setUrl(rtmp_r);
        rtmp.setApp(app);
        rtmp.setSwfVfy("http://eltrecetv.cdncmd.com/sites/all/libraries/jwplayer5/player-licensed.swf");
        rtmp.setPageUrl(br.getURL());
        rtmp.setFlashVer("WIN 16,0,0,257");
        rtmp.setResume(false);
        ((RTMPDownload) dl).startDownload();
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