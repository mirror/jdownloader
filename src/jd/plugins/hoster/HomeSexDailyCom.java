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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "homesexdaily.com" }, urls = { "http://(www\\.)?homesexdaily\\.com/(video/.*?\\.html|flv_player/data/playerConfigEmbed/\\d+\\.xml)" }, flags = { 0 })
public class HomeSexDailyCom extends PluginForHost {

    private String DLLINK = null;

    public HomeSexDailyCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.homesexdaily.com/terms/";
    }

    private static final String EMBEDLINK = "http://(www\\.)?homesexdaily\\.com/flv_player/data/playerConfigEmbed/\\d+\\.xml";

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        String filename = null;
        if (downloadLink.getDownloadURL().matches(EMBEDLINK)) {
            // Set name so the extension won't be ".xml" so users can use the filetype filter in a better way, also for offline links
            downloadLink.setName(new Regex(downloadLink.getDownloadURL(), "(\\d+)\\.xml$").getMatch(0) + ".flv");
            if (br.containsHTML("<video SD=\"http://www\\.homesexdaily\\.com/files/\"")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            filename = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
            DLLINK = br.getRegex("<video SD=\"(http://[^<>\"]*?)\"").getMatch(0);
        } else {
            if (br.containsHTML("(This video does not exist\\!<|<title>Home Sex Daily</title>)") || br.getURL().equals("http://www.homesexdaily.com/")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            filename = br.getRegex("<div class=\"col\\-l\">[\t\n\r ]+<h1>(.*?)</h1>").getMatch(0);
            if (filename == null) filename = br.getRegex("<title>(.*?) \\- Home Sex Daily</title>").getMatch(0);
            DLLINK = br.getRegex("var playlist = \\[ \\{ url: \\'(http://.*?)\\' \\} \\]").getMatch(0);
            if (DLLINK == null) DLLINK = br.getRegex("\\'(http://(www\\.)?media\\.homesexdaily\\.com:\\d+/flv/.*?)\\'").getMatch(0);
        }
        if (filename == null || DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLLINK = Encoding.htmlDecode(DLLINK);
        filename = filename.trim();
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + DLLINK.subSequence(DLLINK.length() - 4, DLLINK.length()));
        Browser br2 = br.cloneBrowser();
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

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}