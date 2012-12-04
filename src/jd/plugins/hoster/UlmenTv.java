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

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ulmen.tv" }, urls = { "http://(www\\.)?ulmen\\.tv/[\\w\\-]+/\\d+/[\\w\\-]+" }, flags = { 32 })
public class UlmenTv extends PluginForHost {

    private String DLLINK;

    public UlmenTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.ulmen.tv/agb";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        download(downloadLink);
    }

    private void setupRTMPConnection(String[] stream, DownloadInterface dl) {
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        rtmp.setPlayPath("mp4:" + stream[1]);
        rtmp.setUrl(stream[0]);
        rtmp.setSwfVfy(stream[2]);
        rtmp.setPageUrl(stream[3]);
        rtmp.setRealTime();// Important!
        rtmp.setResume(true);
    }

    private void download(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String[] stream = DLLINK.split("@");
        if (DLLINK.startsWith("rtmp")) {
            downloadLink.setProperty("FLVFIXER", true);
            dl = new RTMPDownload(this, downloadLink, stream[0] + stream[1]);
            setupRTMPConnection(stream, dl);
            ((RTMPDownload) dl).startDownload();
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        String dllink = downloadLink.getDownloadURL();
        br.getPage(dllink);
        if (br.containsHTML(">Diese Wurst existiert leider nicht\\.") || br.getHttpConnection().getResponseCode() == 404) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        String fNameA[] = br.getRegex("<\\!\\-\\- Folge \\-\\->[\\r\\n\\s]+<span class=\"grey\">([^<]+)</span>[\\r\\n\\s]+<\\!\\-\\- Headline \\-\\->[\\r\\n\\s]+<h1>([^<]+)</h1>").getRow(0);
        DLLINK = br.getRegex("data\\-url=\"([^\"]+)\"").getMatch(0);
        String ext = DLLINK.substring(DLLINK.lastIndexOf("."));
        String next = br.getRegex("<script src=\"(/assets/application\\-[0-9a-f]+\\.js)\"").getMatch(0);
        if (fNameA == null || DLLINK == null || ext == null || next == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        String fileName = fNameA[0].replaceAll("\\|", "").replaceAll("\\s+", "_") + "__" + fNameA[1].replaceAll("[\t\r\n]+", "") + ext;
        fileName = Encoding.htmlDecode(fileName.trim());
        br.getPage(next);
        String flashUrl = br.getRegex("flowplayer\\(\"player\",\"/?([^\"]+)\"").getMatch(0);
        String rtmpUrl = br.getRegex("netConnectionUrl:\"(rtmp[^\"]+)\"\\},").getMatch(0);
        if (flashUrl == null || rtmpUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        flashUrl = "http://www.ulmen.tv/" + flashUrl;
        DLLINK = rtmpUrl + "@" + DLLINK + "@" + flashUrl + "@" + dllink;

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