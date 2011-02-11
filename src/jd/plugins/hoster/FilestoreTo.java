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
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filestore.to" }, urls = { "http://[\\w\\.]*?filestore\\.to/\\?d=[A-Z0-9]+" }, flags = { 0 })
public class FilestoreTo extends PluginForHost {

    public FilestoreTo(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(2000l);

    }

    @Override
    public void init() {
        Browser.setRequestIntervalLimitGlobal(getHost(), 500);
    }

    @Override
    public String getAGBLink() {
        return "http://www.filestore.to/?p=terms";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        String url = downloadLink.getDownloadURL();
        String downloadName = null;
        String downloadSize = null;
        for (int i = 1; i < 10; i++) {
            try {
                br.getPage(url);
            } catch (Exception e) {
                continue;
            }
            if (!br.containsHTML("<strong>Download-Datei wurde nicht gefunden</strong")) {
                downloadName = Encoding.htmlDecode(br.getRegex("\">Dateiname:</td>.*?<td colspan=\"2\" style=\"color:.*?;\">(.*?)</td>").getMatch(0));
                downloadSize = (br.getRegex("\">Dateigr\\&ouml;\\&szlig;e:</td>.*?<td width=\"\\d+\" style=\".*?\">(.*?)</td>").getMatch(0));
                if (downloadName != null) {
                    downloadLink.setName(downloadName);
                    if (downloadSize != null) downloadLink.setDownloadSize(SizeFormatter.getSize(downloadSize.replaceAll(",", "\\.")));
                    return AvailableStatus.TRUE;
                }
            }
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String sid = br.getRegex("name=\"sid\" value=\"(.*?)\"").getMatch(0);
        String fid = new Regex(downloadLink.getDownloadURL(), "filestore\\.to/\\?d=([A-Z0-9]+)").getMatch(0);
        // String ajaxFun = "http://filestore.to/ajax/download.php?a=1&f=" + fid
        // + "&s=" + sid;
        // br.getPage(ajaxFun);
        String ajaxDownload = "http://filestore.to/ajax/download.php?d=" + fid + "&s=" + sid;
        br.getPage(ajaxDownload);
        br.setFollowRedirects(true);
        String dllink = br.toString().trim();
        if (!dllink.startsWith("http://")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 2000;
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
