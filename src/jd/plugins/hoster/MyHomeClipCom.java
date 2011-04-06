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
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "myhomeclip.com" }, urls = { "http://(www\\.)?myhomeclip\\.com/media/\\d+/.*?\\.html" }, flags = { 0 })
public class MyHomeClipCom extends PluginForHost {

    public MyHomeClipCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String DLLINK = null;

    @Override
    public String getAGBLink() {
        return "http://www.myhomeclip.com/DMCA.html";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(class=\"current\">PICTURE GALLERY</li>|The page you are trying to reach is not available|This may be due to one of the following reasons :|The URL you are trying to reach is erroneous or invalid\\.<br>|>If you came from another site, please report this broken link to them<|<title>MyHomeClip Free Homemade Amateur Videos, Amateur Babes Galleries and Voyeur Movies</title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<li class=\"text\"><h1>(.*?)</h1></li>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("title:\\'(.*?)\\'").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("class=\"main-sectioncontent\"><p class=\"footer\">[\t\n\r ]+<b>(.*?)</b>").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
                }
            }
        }
        DLLINK = br.getRegex("\"file\":\"(http.*?)\"").getMatch(0);
        if (DLLINK == null) DLLINK = br.getRegex("\"(http://cdn\\.empflix\\.com/empdl/.*?/.*?key=[a-z0-9]+)\"").getMatch(0);
        if (filename == null || DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLLINK = Encoding.htmlDecode(DLLINK);
        filename = filename.trim();
        String ext = ".flv";
        if (DLLINK.contains(".mp4")) ext = ".mp4";
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ext);
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
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
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
