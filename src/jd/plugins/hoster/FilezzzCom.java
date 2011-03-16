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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filezzz.com" }, urls = { "http://[\\w\\.]*?filezzz\\.com/download/[0-9]+/" }, flags = { 0 })
public class FilezzzCom extends PluginForHost {

    public FilezzzCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(1000l);
    }

    @Override
    public String getAGBLink() {
        return "http://www.filezzz.com/terms/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        String url = downloadLink.getDownloadURL();
        br.getPage(url);
        if (br.containsHTML("not found!")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        br.getPage(url);
        String downloadName = Encoding.htmlDecode(br.getRegex(Pattern.compile("<font size=6>(.*?)</font>", Pattern.CASE_INSENSITIVE)).getMatch(0));
        String downloadSize = (br.getRegex(Pattern.compile("file size (.*?)\\)", Pattern.CASE_INSENSITIVE)).getMatch(0));
        if (downloadName == null || downloadSize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(downloadName.trim());
        downloadLink.setDownloadSize(SizeFormatter.getSize(downloadSize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        requestFileInformation(downloadLink);
        // this.sleep(4000l, downloadLink);
        /* Link holen */
        String linkurl = br.getRegex("<br>\\s+<br>\\s+<a href=\"(.*?)\"").getMatch(0);
        if (linkurl == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, linkurl, true, -2);
        URLConnectionAdapter con = dl.getConnection();
        if (con.getResponseCode() != 200 && con.getResponseCode() != 206) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 5 * 60 * 1000l);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 3;
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
