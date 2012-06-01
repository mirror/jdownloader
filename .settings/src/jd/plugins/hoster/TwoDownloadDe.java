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
import java.util.Random;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision: 16611 $", interfaceVersion = 2, names = { "2download.de" }, urls = { "http://(www\\.)?2download\\.de/download\\-[a-z0-9]+\\.php" }, flags = { 0 })
public class TwoDownloadDe extends PluginForHost {

    public TwoDownloadDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.2download.de/disclaimer.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>Diese Datei ist nicht mehr verfügbar|i>Die Datei wurde in den letzten|>Die Datei wurde gesperrt)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final Regex fileInfo = br.getRegex("<div style=\"margin: 10px; word\\-break: break\\-all; word\\-wrap: break\\-word;\"><font size=\"5\" color=\"#81A3E2\">[\t\n\r ]+<b>([^<>\"]*?)</b></font><br/>[\t\n\r ]+<font size=\"4\" color=\"#81A3E2\">(\\d+(\\.\\d+)?(,\\d+)? KB)");
        String filename = fileInfo.getMatch(0);
        if (filename == null) filename = br.getRegex("<title>Download ([^<>\"]*?)</title>").getMatch(0);
        String filesize = fileInfo.getMatch(1);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize.replace(".", "").replace(",", ".")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String dllink = "http://www.2download.de/download.php?id=" + new Regex(downloadLink.getDownloadURL(), "([a-z0-9]*?)\\.php$").getMatch(0);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, "submit=1&x=" + new Random().nextInt(10) + "&y=" + new Random().nextInt(10), false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
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