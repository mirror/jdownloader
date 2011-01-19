//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sendfaile.com" }, urls = { "http://[\\w\\.]*?sendfaile\\.com/download/.*?\\.html" }, flags = { 0 })
public class SendFaileCom extends PluginForHost {

    public SendFaileCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.sendfaile.com/podstrona_regulamin.html";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("PLIK NIE ISTNIEJE\\!")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>SendFaile\\.com - Pobieranie - (.*?)</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<meta name=\"Keywords\" content=\" - Pobieranie - (.*?), hosting plików,").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("\">Nazwa: <b>(.*?)</b>").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("\">Pobierz plik <u><b>(.*?)</b></u>").getMatch(0);
                }
            }
        }
        String filesize = br.getRegex("\">Waga: <u>(.*?)</u>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL().replace("/download/", "/download2/"));
        sleep(20 * 1001l, downloadLink);
        String id = new Regex(downloadLink.getDownloadURL(), "sendfaile\\.com/download/(.*?)/.*?\\.html").getMatch(0);
        if (id == null) id = new Regex(downloadLink.getDownloadURL(), "sendfaile\\.com/download/(.*?)\\.html").getMatch(0);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, "http://www.sendfaile.com/pobierz.php", "id=" + id, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("NIE ODCZEKANO ODPOWIEDNIEGO CZASU\\!")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "No free slots available at the moment");
            if (br.containsHTML("<b>404 Nie ma pliku\\!</b>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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