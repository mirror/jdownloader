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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "vpornvideos.com" }, urls = { "https?://(?:www\\.)?vpornvideos\\.com/video/\\d+(?:/[A-Za-z0-9\\-]+)" })
public class VpornvideosCom extends PluginForHost {

    public VpornvideosCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags: Porn Plugin
    // protocol: no https
    // other: Using RTMP

    /* Extension which will be used if no correct extension is found */
    private static final String default_Extension = ".mp4";
    /* Connection stuff */
    private final int           free_maxdownloads = -1;

    private String              rtmpurl           = null;
    private String              rtmpplaypath      = null;

    @Override
    public String getAGBLink() {
        return "http://www.vpornvideos.com/terms.html";
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
        final String fid = new Regex(link.getDownloadURL(), "/video/(\\d+)").getMatch(0);
        String url_filename = new Regex(link.getDownloadURL(), "([a-z0-9\\-]+)$").getMatch(0).replace("-", " ");
        if (url_filename == null) {
            url_filename = fid;
        }
        String filename = br.getRegex("<div class=\"bar2 rounded3 title_text m3\"[^>]*?>([^<>\"]+)</div>").getMatch(0);
        if (filename == null) {
            filename = url_filename;
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage("/players/default/config.php?id=" + fid);
        rtmpurl = this.br.getRegex("(rtmp://[^<>\"\\']+)").getMatch(0);
        rtmpplaypath = this.br.getRegex("\\'url\\'\\s*?:\\s*?\\'([^<>\"\\']+)\\'").getMatch(0);
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
        try {
            dl = new RTMPDownload(this, downloadLink, rtmpurl);
        } catch (final NoClassDefFoundError e) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "RTMPDownload class missing");
        }
        /* Setup rtmp connection */
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        rtmp.setPageUrl(downloadLink.getDownloadURL());
        rtmp.setUrl(rtmpurl);
        rtmp.setPlayPath(rtmpplaypath);
        rtmp.setApp("vpornvideos");
        rtmp.setFlashVer("WIN 23,0,0,207");
        rtmp.setSwfVfy("http://www.vpornvideos.com/players/default/player.swf");
        rtmp.setLive(true);
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
