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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fileshaker.com" }, urls = { "http://[\\w\\.]*?fileshaker\\.com/.+" }, flags = { 0 })
public class FileshakerCom extends PluginForHost {

    public FileshakerCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.fileshaker.com/terms";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Sorry, File not found\\!")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // Sometimes the page is buggy, filename is not shown but download works
        String filename = br.getRegex("<div class=\"file\\-name\\-text\">(.*?) \\- \\d+").getMatch(0);
        if (filename != null) {
            downloadLink.setName(Encoding.htmlDecode(filename.trim()));
        } else {
            logger.info("Filename not found for link: " + downloadLink.getDownloadURL());
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, "http://www.fileshaker.com/download/do_download", "key=" + new Regex(downloadLink.getDownloadURL(), "fileshaker\\.com/(.*?)").getMatch(0), false, 1);
        if (dl.getConnection().getResponseCode() != 200 && dl.getConnection().getResponseCode() != 206 || dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            // This should never happen
            if (br.containsHTML(">Log in to your Account<")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Too many simultan downloads", 5 * 60 * 1000l);
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 5 * 60 * 1000l);
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
