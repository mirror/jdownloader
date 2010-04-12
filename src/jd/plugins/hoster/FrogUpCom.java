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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "frogup.com" }, urls = { "http://[\\w\\.]*?frogup\\.com/plik/pokaz/.*?/[0-9]+" }, flags = { 0 })
public class FrogUpCom extends PluginForHost {

    public FrogUpCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.frogup.com/kontakt/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("404\\.gif")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filesize = br.getRegex("Rozmiar pliku:</span> <span class=.*?>(.*?)</span>").getMatch(0);
        String filename = br.getRegex("Pełna nazwa pliku.*?<strong>(.*?)</strong>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (br.containsHTML("Aby obejrzeć i pobrać plik - musisz się")) throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable for registered users");
        // Values to submit to the server
        String id = new Regex(downloadLink.getDownloadURL(), "/pokaz/.*?/([0-9]+)").getMatch(0);
        String name = new Regex(downloadLink.getDownloadURL(), "plik/pokaz/(.*?)/").getMatch(0);
        if (name == null || id == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        name = Encoding.htmlDecode(name.trim().toLowerCase());
        id = Encoding.urlEncode(id);
        // Without this it doesn't work, this is very important!!
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage("http://www.frogup.com/plik/pobierzURL", "name=" + name + "&id=" + id);
        String dllink = br.getRegex("data\":\"(.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -13);
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
