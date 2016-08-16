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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "luv2skeet.com" }, urls = { "http://(?:www\\.)?luv2skeet\\.com/\\d+(?:/[a-z0-9\\-]+)?" }, flags = { 0 })
public class Luv2skeetCom extends PluginForHost {

    public Luv2skeetCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags: Porn Plugin
    // protocol: no https
    // other: Using RTMP

    /* Extension which will be used if no correct extension is found */
    private static final String default_Extension = ".flv";
    /* Connection stuff */
    private final int           free_maxdownloads = -1;

    private String              rtmpurl           = null;
    private String              rtmpplaypath      = null;

    @Override
    public String getAGBLink() {
        return "http://www.luv2skeet.com/static/terms/";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String url_filename = new Regex(link.getDownloadURL(), "([a-z0-9\\-]+)$").getMatch(0).replace("-", " ");
        String filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) {
            filename = url_filename;
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String fid = new Regex(link.getDownloadURL(), "luv2skeet\\.com/(\\d+)").getMatch(0);
        br.getPage("http://www.luv2skeet.com/modules/video/player/jw/config.php?id=" + fid);
        rtmpurl = this.br.getRegex("<streamer>(rtmp[^<>\"]+)</streamer>").getMatch(0);
        rtmpplaypath = this.br.getRegex("<file>([^<>\"]+)</file>").getMatch(0);
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        String ext = default_Extension;
        if (rtmpplaypath != null && rtmpplaypath.contains("mp4")) {
            ext = ".mp4";
        }
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (this.rtmpurl == null || this.rtmpplaypath == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // final String app = "l2svod";
        try {
            dl = new RTMPDownload(this, downloadLink, rtmpurl);
        } catch (final NoClassDefFoundError e) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "RTMPDownload class missing");
        }
        /* Setup rtmp connection */
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        rtmp.setPageUrl(this.br.getURL());
        rtmp.setUrl(rtmpurl);
        rtmp.setPlayPath(rtmpplaypath);
        // rtmp.setApp(app);
        rtmp.setFlashVer("WIN 20,0,0,306");
        rtmp.setSwfUrl("http://www." + this.br.getHost() + "/misc/jwplayer/player.swf");
        rtmp.setResume(false);
        ((RTMPDownload) dl).startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
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
