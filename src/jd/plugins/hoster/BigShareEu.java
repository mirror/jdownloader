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
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bigshare.eu" }, urls = { "http://[\\w\\.]*?bigshare\\.eu/[download\\.php\\?id=].*[0-9A-Z]" }, flags = { 2 })

public class BigShareEu extends PluginForHost {

    private static final String IP_BLOCKED_MSG1 = "You have got max allowed bandwidth size per hour";
    private static final String IP_BLOCKED_MSG2 = "You have got max allowed download sessions from the same IP";
    
    public BigShareEu(PluginWrapper wrapper) {
        super(wrapper);
    }
    
    // @Override
    public String getAGBLink() {
        return "http://bigshare.eu/rules.php";
    }
    
    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        if (!(br.containsHTML("Your requested file is not found"))) {
            String filename = br.getRegex("File name:.*?<td align=left width=150px>(.*?)</td>").getMatch(0);
            String filesize = br.getRegex("File size:.*?<td align=left>(.*?)</td>").getMatch(0);
            if (!(filename == null || filesize  == null)) {
                downloadLink.setName(filename);
                downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
                return AvailableStatus.TRUE;
            }
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }
    
    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (br.containsHTML(IP_BLOCKED_MSG1) || br.containsHTML(IP_BLOCKED_MSG2)) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        }
        String linkurl = br.getRegex("<input.*document.location=\"(.*?)\";").getMatch(0);
        if (linkurl == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        }
        br.setFollowRedirects(false);
        dl = br.openDownload(downloadLink, linkurl, false, 1);
        if (!(dl.getConnection().isContentDisposition())) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        URLConnectionAdapter con = dl.getConnection();
        if (con.getResponseCode() != 200 && con.getResponseCode() != 206) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 1000l);
        }
        dl.startDownload();
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    // @Override
    public void reset() {
    }

    // @Override
    public void resetPluginGlobals() {
    }
    
    // @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
