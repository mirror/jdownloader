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
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "yuvutu.com" }, urls = { "http://[\\w\\.]*?yuvutu\\.com/modules\\.php\\?name=Video\\&op=view\\&video_id=\\d+" }, flags = { 0 })
public class YuvutuCom extends PluginForHost {

    public YuvutuCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String dllink = null;

    @Override
    public String getAGBLink() {
        return "http://www.yuvutu.com/modules.php?name=Video&op=terms";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://www.yuvutu.com/", "lang", "english");
        br.setCookie("http://www.yuvutu.com/", "warningcookie", "viewed");
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML(">The video you requested does not exist<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("Amateur Videos, Amateur Porn, TV Sex Porno XXX -(.*?)</title>").getMatch(0);
        if (filename == null) filename = br.getRegex("<span class=\"authorName\">.*?<a href=\".*?\">.*?</a>.*?</span>(.*?)</td>.*?<td class=\"videoTitle\"").getMatch(0);
        dllink = br.getRegex("value=\"file=(http.*?)\\&width").getMatch(0);
        if (dllink == null) dllink = br.getRegex("(http://vs\\d+\\.yuvutu\\.com/dl/.*?/streams/.*?\\.flv)").getMatch(0);
        if (filename == null || dllink == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        filename = filename.trim();
        downloadLink.setFinalFileName(filename + ".flv");
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = br2.openGetConnection(dllink);
        if (!con.getContentType().contains("html"))
            downloadLink.setDownloadSize(con.getLongContentLength());
        else
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
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
