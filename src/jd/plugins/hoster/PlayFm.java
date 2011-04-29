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

import org.appwork.utils.Regex;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "play.fm" }, urls = { "http://(www\\.)?play\\.fm/(recording/\\w+|(recordings)?#play_\\d+)" }, flags = { 0 })
public class PlayFm extends PluginForHost {

    private String       DLLINK   = null;
    private final String MAINPAGE = "http://www.play.fm";

    public PlayFm(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.play.fm/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, false, 30);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        setBrowserExclusive();
        String id = new Regex(downloadLink.getDownloadURL(), "(\\d+)").getMatch(-1);
        if (id == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }

        br.getPage(MAINPAGE + "/flexRead/recording?rec%5Fid=" + id);
        if (br.containsHTML("Sorry, we are down for maintenance")) { throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Sorry, we are down for maintenance", 5 * 60 * 1000l); }
        if (br.containsHTML("<error>")) {
            br.getPage(downloadLink.getDownloadURL());
            id = br.getRegex("<a class=\"playlink btn btn_play btn_light\".*?href=\"#play_(\\d+)\">").getMatch(0);
            br.getPage(MAINPAGE + "/flexRead/recording?rec%5Fid=" + id);
        }

        if (br.containsHTML("var vid_title = \"\"")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        if (br.containsHTML("<error>")) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        final String filename = br.getRegex("<title><!\\[CDATA\\[(.*?)\\]\\]></title>").getMatch(0);
        final String highBitrate = br.getRegex("<file_id>(.*?)</file_id>").getMatch(0);
        final String fileId1 = br.getRegex("<file_id>(.*?)</file_id>").getMatch(0, 1);
        final String url = br.getRegex("<url>(.*?)</url>").getMatch(0);
        final String uuid = br.getRegex("<uuid>(.*?)</uuid>").getMatch(0);

        if (filename == null || highBitrate == null || fileId1 == null || url == null || uuid == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

        DLLINK = "http://" + url + "/public/" + highBitrate + "/offset/0/sh/" + uuid + "/rec/" + id + "/jingle/" + fileId1 + "/loc/";
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".wav");

        final Browser br2 = br.cloneBrowser();
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
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
