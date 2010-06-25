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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ukladaj.sk" }, urls = { "http://[\\w\\.]*?ukladaj\\.sk/\\?fileID=[a-z0-9]+" }, flags = { 0 })
public class UkladajSk extends PluginForHost {

    public UkladajSk(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://ukladaj.sk/index.php?pid=3";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("windows-1250");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">Súbor nebol nájdený\\. Je možné, že nikdy nebol uploadovaný\\. Alebo bol zmazaný\\.</p>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex(">Názov súboru : <span class=\"lila bold dwnLabels\">(.*?)</span>").getMatch(0);
        if (filename == null) filename = br.getRegex("\\&fName=(.*?)\"").getMatch(0);
        String filesize = br.getRegex(">Veľkosť súboru : <span class=\"lila bold dwnLabels\">(.*?)</span>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String downID = br.getRegex("name=\"dwnID\" value=\"(.*?)\"").getMatch(0);
        if (downID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), "dwnID=" + downID, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (!br.containsHTML("name=\"dwnID\"")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}