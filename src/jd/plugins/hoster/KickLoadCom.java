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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "kickload.com" }, urls = { "http://[\\w\\.]*?kickload\\.com/file/\\d+/.+" }, flags = { 0 })
public class KickLoadCom extends PluginForHost {
    private static final String MAINPAGE = "http://kickload.com/";

    public KickLoadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://kickload.com/tos/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie(MAINPAGE, "lang", "en");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(<h2>This file couldn.t be found\\!</h2>|exit o has been deleted\\.)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>Download (.*?) \\(.*?\\) - kickload\\.com</title>").getMatch(0);
        if (filename == null) filename = br.getRegex("<h2>(.*?)</h2>").getMatch(0);
        String filesize = br.getRegex("<title>Download .*? \\(([0-9\\.]+ .*?)\\) - kickload\\.com</title>").getMatch(0);
        if (filesize == null) filesize = br.getRegex("class=\"download_details\">File size: (.*?) -  Downloads").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(Regex.getSize(filesize));
        String md5 = br.getRegex("id=\"md5\">\\((.*?)\\)").getMatch(0);
        if (md5 != null) link.setMD5Hash(md5.trim());
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.postPage(downloadLink.getDownloadURL(), "free_download=1&free_download1.x=&free_download1.y=&free_download1=1");
        if (br.containsHTML("(ou are already downloading a file\\.|Please, wait until the file has been loaded\\.</h2>)")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads", 10 * 60 * 1000l);
        String postPage = br.getRegex("\"(http://srv\\d+\\.kickload\\.com/download\\.php\\?ticket=.*?)\"").getMatch(0);
        String ticket = br.getRegex("\\?ticket=(.*?)\"").getMatch(0);
        if (ticket == null) ticket = br.getRegex("name=\"ticket\" id=\"ticket\" value=\"(.*?)\"").getMatch(0);
        if (ticket == null || postPage == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, postPage, "ticket=" + ticket + "&x=&y=", false, 1);
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
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}