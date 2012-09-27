//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xtube.com" }, urls = { "http://(www\\.)?xtube\\.com/(watch|play_re)\\.php\\?v=[A-Za-z0-9_\\-]+" }, flags = { 0 })
public class XTubeCom extends PluginForHost {

    private String              DLLINK   = null;

    private static final String MAINPAGE = "http://www.xtube.com";

    public XTubeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("play_re", "watch"));
    }

    @Override
    public String getAGBLink() {
        return "http://wiki2.xtube.com/index.php?title=Terms_of_Use&action=purge";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie(MAINPAGE, "cookie_warning", "deleted");
        br.setCookie(MAINPAGE, "cookie_warning", "S");
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getURL().contains("?msg=Invalid+Video+ID") || br.containsHTML(">This video has been removed from XTube")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<div class=\"font_b_12px\">(.*?)</div><div").getMatch(0);
        String fileID = new Regex(downloadLink.getDownloadURL(), "xtube\\.com/watch\\.php\\?v=(.+)").getMatch(0);
        String ownerName = br.getRegex("\\.addVariable\\(\"user_id\", \"(.*?)\"\\);").getMatch(0);
        if (fileID == null || ownerName == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.postPage("http://www.xtube.com/find_video.php", "user%5Fid=" + Encoding.urlEncode(ownerName) + "&clip%5Fid=&video%5Fid=" + Encoding.urlEncode(fileID));
        DLLINK = br.getRegex("\\&filename=(http.*?)($|\r|\n| )").getMatch(0);
        if (DLLINK == null) DLLINK = br.getRegex("\\&filename=(%2Fvideos.*?hash.+)").getMatch(0);
        if (filename == null || DLLINK == null || DLLINK.length() > 500) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLLINK = Encoding.htmlDecode(DLLINK.trim());
        if (DLLINK.contains("/notfound")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        filename = filename.trim();
        downloadLink.setFinalFileName(filename + ".flv");
        br.setDebug(true);
        Browser br2 = br.cloneBrowser();
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
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}