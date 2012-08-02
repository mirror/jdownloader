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
import jd.plugins.download.DownloadInterface;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rutube.ru" }, urls = { "http://(www\\.)?video\\.rutube\\.ru/[0-9a-f]{32}/?" }, flags = { 32 })
public class RuTubeRu extends PluginForHost {

    private String DLLINK;

    public RuTubeRu(final PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(5000l);
    }

    @Override
    public String getAGBLink() {
        return "http://rutube.ru/agreement.html";
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
        rtmp.setPlayPath(stream[2]);
        rtmp.setUrl(stream[0] + stream[1]);
        rtmp.setSwfVfy("http://rutube.ru/player.swf");
        rtmp.setApp(stream[1].replaceFirst("/", ""));
        rtmp.setResume(true);
    }

    private void download(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String[] stream = DLLINK.split("@");
        if (DLLINK.startsWith("rtmp")) {
            dl = new RTMPDownload(this, downloadLink, stream[0] + stream[1] + stream[2]);
            setupRTMPConnection(stream, dl);
            ((RTMPDownload) dl).startDownload();
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        setBrowserExclusive();
        String dllink = downloadLink.getDownloadURL();
        String regId = "http://video\\.rutube\\.ru/([0-9a-f]{32})";
        String nextId = new Regex(dllink, regId).getMatch(0);

        br.getPage("http://rutube.ru/trackinfo/" + nextId + ".xml?referer=" + Encoding.urlEncode(dllink));
        String filename = br.getRegex("<title><\\!\\[CDATA\\[(.*?)\\]\\]></title>").getMatch(0);
        String filesize = br.getRegex("<size>(\\d+)</size>").getMatch(0);
        String videoBalancer = br.getRegex("<video_balancer>(http://.*?)</video_balancer>").getMatch(0);
        if (videoBalancer == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        br.getPage(videoBalancer + "?referer=" + dllink);
        String baseUrl = br.getRegex("<baseURL>(rtmp.?://.*?)</baseURL>").getMatch(0);
        String mediaUrl = br.getRegex("<media url=\"(/[^\"]+)").getMatch(0);
        if (baseUrl != null && mediaUrl != null) {
            String app = baseUrl.substring(baseUrl.lastIndexOf("/"));
            app = app.endsWith("/") ? app : app + "/";
            mediaUrl = mediaUrl.startsWith("/") ? mediaUrl.substring(1) : mediaUrl;
            if (!mediaUrl.startsWith("mp4:") && mediaUrl.contains("mp4:")) {
                app = mediaUrl.split("mp4:")[0];
                mediaUrl = mediaUrl.substring(mediaUrl.indexOf("mp4:"));
            }
            DLLINK = baseUrl + "@" + app + "@" + Encoding.htmlDecode(mediaUrl);
        }

        if (filename == null || DLLINK == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        downloadLink.setName(Encoding.htmlDecode(filename.trim()) + ".mp4");
        if (filesize != null) downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
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