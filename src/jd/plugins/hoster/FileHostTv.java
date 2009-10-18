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

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filehost.tv" }, urls = { "http://[\\w\\.]*?filehost\\.tv/f/[0-9]+/.*?\\.html" }, flags = { 0 })
public class FileHostTv extends PluginForHost {

    public FileHostTv(PluginWrapper wrapper) {
        super(wrapper);
        br.setFollowRedirects(true);
    }

    public String getAGBLink() {
        return "http://filehost.tv/rules";
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://filehost.tv", "filehosttv_lang", "en");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("class=\"failure\"")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex(">Filename:</td>.*?<td.*?<h2><strong><font.*?>(.*?)</font>").getMatch(0);
        String filesize = br.getRegex(">Size:</td>.*?<td valign=.*?<span style=.*?>(.*?)</span>").getMatch(0);
        if (filesize == null || filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.getPage("http://filehost.tv/get");
        // IP:BLOCKED errorhandling
        if (br.containsHTML("You can only download one file at a time")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l);
        if (br.containsHTML("You have to wait at least for one hour")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 60 * 60 * 1001l);
        String dllink = br.getRegex("href=\"(http://www\\.filehost\\.tv/get.*?)\">").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        // Limit set to 10 because this is the point where they should check
        // that you download more than usual :D also i coulsn't start more than
        // 10
        return 10;
    }

    public void reset() {
    }

    public void resetPluginGlobals() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }
}
