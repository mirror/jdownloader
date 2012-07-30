//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "celebritycunt.net" }, urls = { "http://(www\\.)?celebritycunt\\.net/video/\\d+" }, flags = { 0 })
public class CelebrityCuntNet extends PluginForHost {

    public CelebrityCuntNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.celebritycunt.net/page/tos/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>404 Page Not Found<|Sorry, the page you have requested does not exist)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<div class=\"title\\-player\">([^<>\"]*?)</div>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>([^<>\"]*?) \\- Celebritycunt\\.Net</title>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".flv");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = getdllink();
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    // Same code as for StileProjectCom
    private String getdllink() throws PluginException, IOException {
        final Regex videoMETA = br.getRegex("(VideoFile|VideoMeta)_(\\d+)");
        final String type = videoMETA.getMatch(0);
        final String id = videoMETA.getMatch(1);
        final String cb = br.getRegex("\\?cb=(\\d+)\\'").getMatch(0);
        if (type == null || id == null || cb == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final String postData = "cacheBuster=" + System.currentTimeMillis() + "&jsonRequest=%7B%22path%22%3A%22" + type + "%5F" + id + "%22%2C%22cb%22%3A%22" + cb + "%22%2C%22loaderUrl%22%3A%22http%3A%2F%2Fcdn1%2Estatic%2Eatlasfiles%2Ecom%2Fplayer%2Fmemberplayer%2Eswf%3Fcb%3D" + cb + "%22%2C%22returnType%22%3A%22json%22%2C%22file%22%3A%22" + type + "%5F" + id + "%22%2C%22htmlHostDomain%22%3A%22www%2Estileproject%2Ecom%22%2C%22height%22%3A%22508%22%2C%22appdataurl%22%3A%22http%3A%2F%2Fwww%2Estileproject%2Ecom%2Fgetcdnurl%2F%22%2C%22playerOnly%22%3A%22true%22%2C%22request%22%3A%22getAllData%22%2C%22width%22%3A%22640%22%7D";
        br.postPage("http://www.celebritycunt.net/getcdnurl/", postData);
        return br.getRegex("\"file\": \"(http://[^<>\"]*?)\"").getMatch(0);
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