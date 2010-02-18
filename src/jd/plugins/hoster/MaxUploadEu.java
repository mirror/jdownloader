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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "maxupload.eu" }, urls = { "http://[\\w\\.]*?maxupload\\.eu/../\\d+" }, flags = { 0 })
public class MaxUploadEu extends PluginForHost {

    public MaxUploadEu(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(100l);
    }

    @Override
    public String getAGBLink() {
        return "http://www.maxupload.eu/en/terms";
    }

    public String fileno;

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        fileno = new Regex(downloadLink.getDownloadURL(), "maxupload.eu/../(\\d+)").getMatch(0);
        if (fileno == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("http://www.maxupload.eu/en/" + fileno);
        String filename = br.getRegex("class=\"fname\"><strong>(.*?)</strong>").getMatch(0);
        String filesize = br.getRegex("size:</span> (.*?)<br").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(Encoding.htmlDecode(filename.trim()));
        downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        String getlink;
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        getlink = br.getRegex("a rel=\"nofollow\" href=\"(.*?)\"").getMatch(0);
        if (getlink == null) getlink = "http://www.maxupload.eu/download.php?id=" + fileno;
        br.setFollowRedirects(true);
        // this.sleep(3000, downloadLink); // uncomment when they introduce
        // waittime
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, getlink, false, 1);
        URLConnectionAdapter con = dl.getConnection();
        if (!con.isOK()) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
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
