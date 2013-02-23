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
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sloozie.com" }, urls = { "http://(www\\.)?sloozie\\.com/(galleries/[a-z0-9]+|videos/[a-z0-9]+/.{1})" }, flags = { 0 })
public class SloozieCom extends PluginForHost {

    public SloozieCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String DLLINK = null;

    @Override
    public String getAGBLink() {
        return "http://www.sloozie.com/tos.php";
    }

    private static final String PICLINK = "http://(www\\.)?sloozie\\.com/galleries/[a-z0-9]+";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getURL().equals("http://www.sloozie.com/")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = null;
        final Browser br2 = br.cloneBrowser();
        if (downloadLink.getDownloadURL().matches(PICLINK)) {
            filename = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
            if (filename == null) filename = br.getRegex("").getMatch(0);
            // Try to get better quality first
            DLLINK = br.getRegex("id=\"imgMGZoom\" style=\"visibility: visible\"><a rel=\"shadowbox;options=\\{displayNav:true\\}\" href=\"(http://[^<>\"]*?)\"").getMatch(0);
            // Nothing found? Grab normal quality
            if (DLLINK == null) DLLINK = br.getRegex("title=\"Show full size\"></div><img src=\"(http://[^<>\"]*?)\"").getMatch(0);
            if (filename == null || DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            DLLINK = Encoding.htmlDecode(DLLINK);
            filename = filename.trim();
            String ext = DLLINK.substring(DLLINK.lastIndexOf("."));
            if (ext == null || ext.length() > 5) ext = ".jpg";
            filename = Encoding.htmlDecode(filename) + ext;
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openGetConnection(DLLINK);
                if (!con.getContentType().contains("html")) downloadLink.setDownloadSize(con.getLongContentLength());
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        } else {
            filename = br.getRegex("id=\"txtVDCaption\">([^<>\"]*?)\\-\\-</div>").getMatch(0);
            if (filename == null) filename = br.getRegex("<title>Sloozie\\&#039;s homemade videos \\-([^<>\"]*?)</title>").getMatch(0);
            if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            filename = Encoding.htmlDecode(filename.trim()) + ".mp4";
            String flashParameter[] = br.getRegex("V4CORE\\.put\\(\'d\',\\{\"m\":\"\\d+\",\"p\":\"\\d+\",\"v\":\"([^\"]+)\",\"q\":\"([a-z]+)\",\"fs\"").getRow(0);
            String jsCache = br.getRegex("src=\"(/js/cache/[0-9a-f]+\\.js)\">").getMatch(0);
            if (flashParameter == null || jsCache == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            DLLINK = "rtmp://video.sloozie.com/vod@mp4:sloozie/" + flashParameter[1] + "/" + flashParameter[0].replace("\\", "");
            // br2.getPage(jsCache);
        }
        downloadLink.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        download(downloadLink);
    }

    private void setupRTMPConnection(String[] stream, DownloadInterface dl) {
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        rtmp.setPlayPath(stream[1]);
        rtmp.setUrl(stream[0]);
        rtmp.setResume(true);
        rtmp.setSwfVfy("http://www.sloozie.com/swf/flowplayer.rtmp-3.2.3.swf");
        // rtmp.setRealTime();
    }

    private void download(final DownloadLink downloadLink) throws Exception {
        String stream[] = DLLINK.split("@");
        if (DLLINK.startsWith("rtmp")) {
            // downloadLink.setProperty("FLVFIXER", true);
            dl = new RTMPDownload(this, downloadLink, stream[0]);
            setupRTMPConnection(stream, dl);
            ((RTMPDownload) dl).startDownload();
        } else {
            // Don't allow chunks for picture download
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 1);
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
