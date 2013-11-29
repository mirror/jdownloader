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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wdr.de" }, urls = { "http://(www\\.)?wdr\\.de/(mediathek/html/regional/\\d{4}/\\d{2}/\\d{2}/[a-z0-9\\-_]+\\.xml|tv/rockpalast/extra/videos/\\d+/\\d+/\\w+\\.jsp)" }, flags = { 0 })
public class WdrDeMediathek extends PluginForHost {

    public WdrDeMediathek(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String DLLINK = null;

    @Override
    public String getAGBLink() {
        return "http://www1.wdr.de/themen/global/impressum/impressum116.html";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String startLink = downloadLink.getDownloadURL();
        br.getPage(startLink);

        if (startLink.matches("http://(www\\.)?wdr\\.de/tv/rockpalast/extra/videos/\\d+/\\d+/\\w+\\.jsp")) return requestRockpalastFileInformation(downloadLink);

        if (br.getURL().contains("/fehler.xml")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String sendung = br.getRegex("class=\"moSubText\">([^<>\"]*?)<br").getMatch(0);
        String filename = br.getRegex("<title>([^<>\"]*?)\\-  WDR MEDIATHEK \\- WDR\\.de</title>").getMatch(0);
        String preferedExt = ".mp4";
        if (br.containsHTML("<div class=\"audioContainer\">")) {
            DLLINK = br.getRegex("dslSrc: \\'dslSrc=(http://[^<>\"]*?)\\&amp;mediaDuration=\\d+\\'").getMatch(0);
            preferedExt = ".mp3";
        } else {
            final String mid = br.getRegex("mid: \\'(\\d+)\\'").getMatch(0);
            if (mid == null || filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.getPage("http://www.wdr.de/mediathek/codebase/intern/player.jsp?mid=" + mid + "&size=big");
            DLLINK = br.getRegex("type=\"video/quicktime\"[\t\n\r ]+data=\"(http://[^<>\"]*?)\"").getMatch(0);
        }
        if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLLINK = Encoding.htmlDecode(DLLINK.trim());
        filename = filename.trim().replace(":", " - ").replace("?", "");
        String ext = DLLINK.substring(DLLINK.lastIndexOf("."));
        if (ext == null || ext.length() > 5) ext = preferedExt;
        filename = Encoding.htmlDecode(filename);
        if (sendung != null) {
            downloadLink.setFinalFileName(Encoding.htmlDecode(sendung) + " - " + filename + ext);
        } else {
            downloadLink.setFinalFileName(filename + ext);
        }
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html"))
                downloadLink.setDownloadSize(con.getLongContentLength());
            else
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    private AvailableStatus requestRockpalastFileInformation(final DownloadLink downloadlink) throws IOException, PluginException {
        String fileName = br.getRegex("<h1 class=\"wsSingleH1\">([^<]+)</h1>[\r\n]+<h2>([^<]+)<").getMatch(0);
        DLLINK = br.getRegex("dslSrc=(.*?)\\&amp").getMatch(0);
        if (fileName == null || DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        downloadlink.setFinalFileName(fileName + ".mp4");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (DLLINK.startsWith("rtmp")) {
            dl = new RTMPDownload(this, downloadLink, DLLINK);
            setupRTMPConnection(DLLINK, dl);
            ((RTMPDownload) dl).startDownload();
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    private void setupRTMPConnection(String dllink, DownloadInterface dl) {
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        rtmp.setUrl(dllink);
        rtmp.setResume(true);
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
