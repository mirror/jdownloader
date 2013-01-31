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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xxxaporn.com" }, urls = { "http://(www\\.)?(xxxaporndecrypted\\.com/\\d+/[A-Za-z0-9\\-_]+\\.html|media\\.xxxaporn\\.com/video/\\d+)" }, flags = { 0 })
public class XXXAPornCom extends PluginForHost {

    private String              DLLINK    = null;
    private static final String EMBEDTYPE = "http://(www\\.)?media\\.xxxaporn\\.com/video/\\d+";

    public XXXAPornCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://media.xxxaporn.com/static/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("xxxaporndecrypted.com/", "xxxaporn.com/"));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String filename = null;
        // Limit reached => Skip it
        if (downloadLink.getDownloadURL().matches(EMBEDTYPE)) {
            final String fileID = new Regex(downloadLink.getDownloadURL(), "xxxaporn\\.com/(video/)?(\\d+)").getMatch(1);
            br.getPage("http://media.xxxaporn.com/media/player/config_embed.php?vkey=" + fileID);
            if (br.containsHTML("Invalid video key\\!")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            filename = new Regex(downloadLink.getDownloadURL(), "xxxaporn\\.com/\\d+/([A-Za-z0-9\\-_]+)").getMatch(0);
            if (filename == null) filename = br.getRegex("xxxaporn\\.com/video/\\d+/([a-z0-9\\-]+)</share>").getMatch(0);
            if (filename == null) filename = fileID;
            DLLINK = br.getRegex("<src>(http://[^<>\"]*?)</src>").getMatch(0);
        } else {
            br.getPage(downloadLink.getDownloadURL());
            if (br.containsHTML("No htmlCode read")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            filename = br.getRegex("<title>Viewing Media \\- (.*?):: Free Amateur Sex, Amateur Porn").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<div class=\"video\\-info\">[\t\n\r ]+<h1>(.*?)</h1>").getMatch(0);
            }
            DLLINK = br.getRegex("\\(\\'flashvars\\',\\'file=(http://[^<>\"]*?)\\'\\)").getMatch(0);
        }
        if (filename == null || DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLLINK = Encoding.htmlDecode(DLLINK);
        filename = filename.trim();
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ".flv");
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

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, -13);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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