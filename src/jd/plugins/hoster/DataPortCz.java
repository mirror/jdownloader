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
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dataport.cz" }, urls = { "http://[\\w\\.]*?dataport\\.cz/file/\\d+/.+" }, flags = { 0 })
public class DataPortCz extends PluginForHost {

    public DataPortCz(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://dataport.cz/pravidla-pouziti/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(<h2>Litujeme, soubor nebyl nalezen\\.</h2>|Prosím zkontrolujte překlepy v adrese\\.)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?) :: DataPort\\.cz - Neomezený download server</title>").getMatch(0);
        if (filename == null) filename = br.getRegex("<td>Název souboru:</td>[\n\r\t ]+<td><strong>(.*?)</strong></td>").getMatch(0);
        String filesize = br.getRegex("<td>Velikost souboru:</td>[\n\r\t ]+<td>(.*?)</td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Set final filename here because server sends us bad filenames
        link.setFinalFileName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = br.getRegex("<td><a href=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://www\\d+\\.dataport\\.cz/download\\.php\\?uid=\\d+)\"").getMatch(0);
        if (dllink == null) {
            logger.warning("dllink is null...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
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