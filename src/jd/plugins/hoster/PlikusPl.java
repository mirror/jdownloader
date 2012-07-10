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
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "plikus.pl" }, urls = { "http://[\\w\\.]*?plikus\\.pl/plik,*.+\\.html" }, flags = { 0 })
public class PlikusPl extends PluginForHost {

    public PlikusPl(PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(5000l);
    }

    // @Override
    public String getAGBLink() {
        return "http://www.osemka.pl/html/regulamin/#regulamin_plikus";
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    // @Override
    /*
     * public String getVersion() { return getVersion("$Revision$"); }
     */

    // @Override
    public int getTimegapBetweenConnections() {
        return 2500;
    }

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        // Form form = br.getForm(0);
        br.setFollowRedirects(false);
        if (br.getRedirectLocation() != null) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
        // dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, form,
        // true, 0);
        String linkurl = downloadLink.getDownloadURL().replace("plik,", "pobierz,").replaceAll(",.*,", ",");

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, linkurl);
        dl.startDownload();
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException, IOException {
        setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        // not used anymore
        // String filesize =
        // br.getRegex("<p><b>Rozmiar pliku:</b>(.*?)</p>").getMatch(0);
        String filename = br.getRegex("<a class=\"btn_download\" href=\"/pobierz,(.*?)\\.html\">[\n\t\r]+<span class=\"action\">Pobierz teraz!</span>").getMatch(0);
        // String filename =
        // br.getRegex("<h1><img src=.*?/>(.*?)</h1>").getMatch(0);
        if (filename == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        downloadLink.setName(downloadLink.getName().replace("plik,", "").replaceAll(",.{3}+\\.html", ""));
        return AvailableStatus.TRUE;
    }

    // @Override
    public void reset() {
    }

    // @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    // @Override
    public void resetPluginGlobals() {

    }

}