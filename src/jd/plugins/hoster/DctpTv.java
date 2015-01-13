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

import java.io.IOException;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "dctp.tv" }, urls = { "http://(www\\.)?dctp\\.tv/filme/[a-z0-9_\\-]+/" }, flags = { 0 })
public class DctpTv extends PluginForHost {

    public DctpTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.dctp.tv/";
    }

    private static final String app = "dctpvod/";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("class=\\'error\\'>Der gewünschte Film ist \\(zur Zeit\\) nicht")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("property=\"media:title\" content=\"([^<>\"]*?)\"></span>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<meta name=\\'DC\\.title\\' content=\\'([^<>\"]*?)\\'").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim()) + ".flv");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String rtmpurl = "rtmp://mf.dctpvod.c.nmdn.net/" + app;
        try {
            dl = new RTMPDownload(this, downloadLink, rtmpurl);
        } catch (final NoClassDefFoundError e) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "RTMPDownload class missing");
        }
        /* Setup rtmp connection */
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        setupRtmp(rtmp, rtmpurl);
        rtmp.setResume(true);
        ((RTMPDownload) dl).startDownload();
    }

    public void setupRtmp(jd.network.rtmp.url.RtmpUrlConnection rtmp, final String clipuri) throws PluginException {
        final String playpathpart = br.getRegex("vods3/_definst/mp4:dctp_completed_media/([a-z0-9]{32})_iphone\\.m4v/playlist\\.m3u8\"").getMatch(0);
        if (playpathpart == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String heigth = br.getRegex("height = (\\d+);").getMatch(0);
        String aspect;
        if (heigth != null && heigth.equals("180")) {
            aspect = "16x9";
        } else {
            aspect = "4x3";
        }
        final String playpath = "mp4:" + playpathpart + "_dctp_0500_" + aspect + ".m4v";
        rtmp.setPlayPath(playpath);
        rtmp.setTcUrl("rtmp://mf.dctpvod.c.nmdn.net/dctpvod/");
        rtmp.setPageUrl(br.getURL());
        rtmp.setApp(app);
        rtmp.setFlashVer("WIN 16,0,0,235");
        rtmp.setSwfVfy("http://prod-dctptv-static.dctp.tv/dctptv-relaunch2012-63.swf");
        rtmp.setUrl(clipuri);
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}