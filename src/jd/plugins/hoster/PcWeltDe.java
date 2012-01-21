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
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pcwelt.de" }, urls = { "http://(www\\.)?pcwelt\\.de/downloads/[^<>\"]+\\-\\d+\\.html" }, flags = { 0 })
public class PcWeltDe extends PluginForHost {

    public PcWeltDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.pcwelt.de/news/Impressum-975146.html";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (!br.getURL().contains("/downloads/")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<h1 class=\"headline\">(.*?)</h1>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<div class=\"boxed\">[\t\n\r ]+<div class=\"left\">(.*?)</div>").getMatch(0);
            if (filename == null) filename = br.getRegex("<title>([^<>\"]+) Download \\-").getMatch(0);
        }
        String filesize = br.getRegex(">Dateigr&ouml;\\&szlig;e:</th>[\t\n\r ]+<td class=\"col\\-\\d+\">(.*?)</td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (!filesize.equals("-")) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String continueLink = br.getRegex("<div class=\"unavailable\">[\t\n\r ]+<div class=\"text\">[\t\n\r ]+<a href=\"(/[^<>\"]+)\"").getMatch(0);
        if (continueLink == null) continueLink = br.getRegex("\"(/downloads/[^<>\"]+\\-starten\\-\\d+\\.html)\"").getMatch(0);
        if (continueLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("http://www.pcwelt.de" + continueLink + "?rate=0&page=2&bid=0");
        String dllink = br.getRegex("<a id=\"dl_link_third\" href=\"(http://[^<>\"]+)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://download\\.pcwelt\\.de/area_release/files/[^<>\"]+)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (!dllink.contains("pcwelt")) throw new PluginException(LinkStatus.ERROR_FATAL, "Nicht downloadbar: externe Downloadquelle");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}