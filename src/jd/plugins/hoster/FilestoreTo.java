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
import jd.nutils.encoding.Encoding;
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
        // All other browsers seem to be blocked
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; de; rv:1.9.2.17) Gecko/20110420 Firefox/3.6.17");
        String url = downloadLink.getDownloadURL();
        String downloadName = null;
        String downloadSize = null;
        for (int i = 1; i < 5; i++) {
            try {
                br.getPage(url);
            } catch (Exception e) {
                continue;
            }
            if (br.containsHTML(">Download\\-Datei wurde nicht gefunden<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (br.containsHTML("Entweder wurde die Datei von unseren Servern entfernt oder der Download-Link war")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            downloadName = br.getRegex(">Datei:</td>[\t\n\r ]+<td colspan=\"2\" style=\"[a-z0-9:;#\\- ]+\">(.*?)</td>").getMatch(0);
            downloadSize = br.getRegex("<td width=\"220\" style=\"[a-z0-9:;#\\- ]+\">(.*?)</td>").getMatch(0);
            if (downloadName != null) {
                downloadLink.setName(Encoding.htmlDecode(downloadName));
                if (downloadSize != null) downloadLink.setDownloadSize(SizeFormatter.getSize(downloadSize.replaceAll(",", "\\.")));
                return AvailableStatus.TRUE;
            }

        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String gamer = br.getRegex("name=\"downid\" value=\"(.*?)\">").getMatch(0);
        if (gamer == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String waittime = br.getRegex("Bitte warte (\\d+) Sekunden und starte dann").getMatch(0);
        int wait = 10;
        if (waittime != null) if (Integer.parseInt(waittime) < 61) wait = Integer.parseInt(waittime);
        sleep(wait * 1001l, downloadLink);
        // If plugin breaks most times this link is changed
        String ajaxDownload = "http://filestore.to/ajax/download.php?Download=" + gamer;
        br.getPage(ajaxDownload);
        if (br.containsHTML("(Da hat etwas nicht geklappt|Wartezeit nicht eingehalten|Versuche es erneut)")) {
            logger.warning("FATAL waittime error!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
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
