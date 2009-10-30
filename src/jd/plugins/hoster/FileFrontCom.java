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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filefront.com" }, urls = { "http://[\\w\\.]*?filefront\\.com/[0-9]+" }, flags = { 0 })
public class FileFrontCom extends PluginForHost {

    public FileFrontCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://aup.legal.filefront.com/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        br.setFollowRedirects(false);
        if (br.containsHTML("Error 404") || br.containsHTML("<h1>File is Unavailable.</h1>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("File name: </td> <td> <div style=\"width:.*?;\">(.*?)</").getMatch(0);
        String filesize = br.getRegex("File size: </td> <td>(.*?)</td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        filesize = filesize.trim();
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        String nextpageurl = br.getRegex("\"(http://www\\.filefront\\.com/thankyou\\.php\\?f=.*?k=.*?)\"").getMatch(0);
        if (nextpageurl == null) {
            nextpageurl = br.getRegex("age_gate_[0-9]\" style=\"\"><a href=\"(.*?)\"").getMatch(0);
        }
        if (nextpageurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage(nextpageurl);
        String linkurl = br.getRegex("Your download will begin.*?<a href=\"(.*?)\"").getMatch(0);
        if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, linkurl, true, 0);
        dl.startDownload();

    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
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
