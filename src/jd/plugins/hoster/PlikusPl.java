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

    public String getAGBLink() {
        return "http://www.osemka.pl/html/regulamin/#regulamin_plikus";
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    public int getTimegapBetweenConnections() {
        return 2500;
    }

    @SuppressWarnings("deprecation")
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (br.getRedirectLocation() != null) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
        }
        final String linkurl = downloadLink.getDownloadURL().replace("plik,", "pobierz,").replaceAll(",.*,", ",");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, linkurl, false, 1);
        dl.startDownload();
    }

    @SuppressWarnings("deprecation")
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws PluginException, IOException {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("itemprop=\"name\">([^<>\"]*?)<").getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setName(filename);
        return AvailableStatus.TRUE;
    }

    public void reset() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }

    public void resetPluginGlobals() {

    }

}