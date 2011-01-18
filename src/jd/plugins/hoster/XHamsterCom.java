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
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xhamster.com" }, urls = { "http://[\\w\\.]*?xhamster\\.com/movies/[0-9]+/.*?\\.html" }, flags = { 0 })
public class XHamsterCom extends PluginForHost {

    public XHamsterCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://xhamster.com/terms.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Video Not found") || br.containsHTML("403 Forbidden")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<meta name=\"description\" content=\"(.*?)\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<meta name=\"keywords\" content=\"(.*?)\"").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("height=\"26\" width=.*?align=left>\\&nbsp;(.*?)</th>").getMatch(0);
                    if (filename == null) {
                        filename = br.getRegex("<B>Description:</B></td>.*?<td width=[0-9]+>(.*?)</td>").getMatch(0);
                    }
                }
            }
        }
        String ending = br.getRegex("'type':'(.*?)'").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        filename = Encoding.htmlDecode(filename.trim());
        if (ending != null) {
            downloadLink.setFinalFileName(filename + "." + ending);
        } else {
            downloadLink.setName(filename);
        }
        String dllink = getDllink();
        if (!br.openGetConnection(dllink).getContentType().contains("html")) downloadLink.setDownloadSize(br.getHttpConnection().getLongContentLength());
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        // Access the page again to get a new direct link because by checking
        // the availibility the first linkisn't valid anymore
        br.getPage(downloadLink.getDownloadURL());
        String dllink = getDllink();
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public String getDllink() throws IOException, PluginException {
        String server = br.getRegex("'srv': '(.*?)'").getMatch(0);
        String type = br.getRegex("'type':'(.*?)'").getMatch(0);
        String file = br.getRegex("'file': '(.*?)'").getMatch(0);
        if (server == null || type == null || file == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String dllink = server + "/" + type + "2/" + file;
        return dllink;
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
