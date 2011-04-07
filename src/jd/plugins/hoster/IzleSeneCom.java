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
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "izlesene.com" }, urls = { "http://(www\\.)?izlesene\\.com/video/[a-z0-9\\-]+/\\d+" }, flags = { 0 })
public class IzleSeneCom extends PluginForHost {

    public IzleSeneCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String DLLINK = null;

    @Override
    public String getAGBLink() {
        return "http://www.izlesene.com/yardim/kullanim";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage("http://www.izlesene.com/player_xml/izlesene/" + new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0));
        if (!br.containsHTML("videoname")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<videoname>(.*?)</videoname>").getMatch(0);
        filename = filename.trim();
        downloadLink.setName(Encoding.htmlDecode(filename) + ".mp4");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        DLLINK = br.getRegex("<videosec>(.*?)</videosec>").getMatch(0);
        if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLLINK = "http://dcdn.nokta.com/" + DLLINK + "_1_2_1.xml";
        br.getPage(DLLINK);
        DLLINK = br.getRegex("<server durl=\"(http://.*?)\"").getMatch(0);
        if (DLLINK == null) DLLINK = br.getRegex("\"(http://istr\\d+\\.izlesene\\.com/data/videos/\\d+/\\d+\\-\\d+.{1,5})\"").getMatch(0);
        if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String ext = ".mp4";
        if (br.containsHTML("flv")) ext = ".flv";
        downloadLink.setFinalFileName(downloadLink.getName().replace(downloadLink.getName().substring(downloadLink.getName().length() - 4, downloadLink.getName().length()), ext));
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("(<title>404 Not Found</title>|<h1>404 Not Found</h1>)")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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
