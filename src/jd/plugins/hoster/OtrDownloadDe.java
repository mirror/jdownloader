//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "otr-download.de" }, urls = { "http://(www\\.)?otr\\-download\\.de/downloadpopup\\.php\\?option=2\\&file=\\d+" }, flags = { 0 })
public class OtrDownloadDe extends PluginForHost {

    public OtrDownloadDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://otr-download.de/index.php?s=impressum&session=";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(<td width=\"418\"><div align=\"right\" id=\"d_header\"></div></td>|<title></title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<td width=\"418\"><div align=\"right\" id=\"d_header\">([^<>\"\\']+)</div></td>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("getElementById\\(\\'numberCountdown\\'\\)\\.innerHTML = \\'<u><a href=\"\">([^<>\"\\']+)</a>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>([^<>\"\\']+)</title>").getMatch(0);
            }
        }
        String filesize = br.getRegex("Kosten an Traffic: ([^<>\"\\']+)\\)</u>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = downloadLink.getStringProperty("freelink");
        if (dllink != null) {
            try {
                Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty("freelink", Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty("freelink", Property.NULL);
                dllink = null;
            }
        }
        if (dllink == null) {
            if (br.containsHTML("oder die neue Datei würde das Downloadbudget sprengen")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
            if (br.containsHTML("(>Leider können Sie keinen Highspeed Download benutzen|Aber Ihnen stehen die oben angezeigten Möglichkeiten offen an den Downloadlink zu kommen)")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Keine Slots verfügbar!", 5 * 60 * 1000l);
            String dlcontinue = br.getRegex("id=\"now1\"><a href=\"(d[^<>\"\\']+)\"").getMatch(0);
            if (dlcontinue == null) dlcontinue = br.getRegex("\"(downloadpopup\\.php\\?file=\\d+\\&typ=high\\&step=2\\&hash=[a-z0-9]+)\"").getMatch(0);
            if (dlcontinue == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.getPage("" + dlcontinue);
            dllink = br.getRegex("Id\\(\\'numberCountdown\\'\\)\\.innerHTML = \\'<u><a href=\"(http://[^<>\"\\']+)\"").getMatch(0);
            if (dllink == null) dllink = br.getRegex("\"(http://s\\d+\\.otr\\-download\\.de:\\d+/dl\\-high\\-otr/[a-z0-9]+/[a-z0-9]+/[^<>\"\\']+)\"").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("freelink", dllink);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 4;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}