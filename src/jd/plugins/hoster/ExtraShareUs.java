//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "extrashare.us" }, urls = { "http://[\\w\\.]*?extrashare.us/(\\w\\w/)?file/.+/.+" }, flags = { 0 })
public class ExtraShareUs extends PluginForHost {

    public ExtraShareUs(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String getAGBLink() {
        return "http://www.extrashare.us/rules.php";
    }

    public String getCoder() {
        return "TnS";
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Your requested file is not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<b>File name:</b></td>.*?<td align=.*?width=.*?>(.*?)</td>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("\"Click this to report for(.*?)\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("content=\"(.*?), The best file hosting service").getMatch(0);
                }
            }
        }
        String filesize = br.getRegex("<span class=\"style1\">\\|(.*?)</span").getMatch(0);
        if (filename == null || filename.matches("")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setFinalFileName(filename.trim());
        if (filesize != null) downloadLink.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(true);
        requestFileInformation(downloadLink);
        String link = br.getRegex(Pattern.compile("document.location=\"(.*)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (link == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, link, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("(Connection to database server failed|Error:Lost connection to MySQL server)")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Servererror!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public int getTimegapBetweenConnections() {
        return 500;
    }

    public int getMaxConnections() {
        return 1;
    }

    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void reset() {
    }

    public void resetPluginGlobals() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }
}
