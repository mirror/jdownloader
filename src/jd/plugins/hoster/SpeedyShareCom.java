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
import java.util.regex.Pattern;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "speedyshare.com" }, urls = { "http://[\\w\\.]*?speedyshare\\.com/files/[0-9]+/.+" }, flags = { 0 })
public class SpeedyShareCom extends PluginForHost {

    public SpeedyShareCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(2000l);
    }

    @Override
    public String getAGBLink() {
        return "http://www.speedyshare.com/terms.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        String url = downloadLink.getDownloadURL();
        br.getPage(url);
        String downloadName = Encoding.htmlDecode(br.getRegex(Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE)).getMatch(0));
        String downloadSize = (br.getRegex(Pattern.compile("class=result>File size(.*?),", Pattern.CASE_INSENSITIVE)).getMatch(0));
        if (br.containsHTML("(This file has been deleted for the following reason|File not found)") || downloadName == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(downloadName);
        if (downloadSize != null)
            downloadLink.setDownloadSize(Regex.getSize(downloadSize.replaceAll(",", "\\.")));
        else
            logger.warning("Filesizeregex for speedyshare.com is broken!");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        requestFileInformation(downloadLink);
        if (br.containsHTML("The one-hour limit has been reached. Wait")) {
            String wait[] = br.getRegex("id=minwait1>(\\d+):(\\d+)</span> minutes").getRow(0);
            long waittime = 1000l * 60 * Long.parseLong(wait[0]) + 1000 * Long.parseLong(wait[1]);
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittime);
        }
        /* Link holen */
        String linkpart0 = new Regex(downloadLink.getDownloadURL(), "(speedyshare\\.com/files/[0-9]+/)").getMatch(0);
        String linkpart1 = new Regex(downloadLink.getDownloadURL(), "speedyshare\\.com/files/[0-9]+/(.+)").getMatch(0);
        if (linkpart0 == null || linkpart1 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String linkurl = "http://www." + linkpart0 + "download/" + linkpart1;
        /* Datei herunterladen */
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, linkurl, true, 0);
        URLConnectionAdapter con = dl.getConnection();
        if (con.getResponseCode() != 200 && con.getResponseCode() != 206) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 5 * 60 * 1000l);
        }
        if (con.getContentType().contains("html")) {
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
