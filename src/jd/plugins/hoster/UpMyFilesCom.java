//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "up-myfiles.com" }, urls = { "http://(www\\.)?up-myfiles\\.com/kleeja/do\\.php\\?filename=\\d+.+" }, flags = { 0 })
public class UpMyFilesCom extends PluginForHost {

    public UpMyFilesCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://up-myfiles.com/kleeja/go.php?go=rules";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("<li>File cannot be found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex(">File name</td>[\r\n\t ]+<td>(.*?)</td>").getMatch(0);
        String filetype = br.getRegex(">File type</td>[\r\n\t ]+<td[^>]+>(.*?)</td>").getMatch(0);
        if (filetype == null) filetype = downloadLink.getDownloadURL().substring(downloadLink.getDownloadURL().lastIndexOf("."));
        String filesize = br.getRegex(">File size</td>[\r\n\t ]+<td[^>]+>(.*?)</td>").getMatch(0);
        if (filename != null) downloadLink.setName(filename.trim() + (filetype != null ? "." + filetype.trim() : ""));
        if (filename != null) downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        int wait = 30;
        String waitTimer = br.getRegex("var timer = (\\d+);").getMatch(0);
        if (waitTimer != null) wait = Integer.parseInt(waitTimer);
        sleep(wait * 1001, downloadLink);
        String dllink = downloadLink.getDownloadURL().replace("filename=", "downf=");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
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