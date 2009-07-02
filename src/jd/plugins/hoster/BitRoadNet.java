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
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.HostPlugin;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(names = { "bitroad.net"}, urls ={ "http://[\\w\\.]*?(bitroad\\.net|filemashine\\.com)/download/[A-Fa-f0-9]+"}, flags = {0})
public class BitRoadNet extends PluginForHost {

    public BitRoadNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public String getAGBLink() {
        return "http://bitroad.net/tmpl/terms.php";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("file is not found") || br.containsHTML("Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("name=\"name\" value=\"(.*?)\"").getMatch(0);
        String size = br.getRegex("<h1>.*\\[\\s(.*?)\\s\\]</h1>").getMatch(0);
        if (filename == null || size == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(size));
        return AvailableStatus.TRUE;
    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        if (br.containsHTML("Downloading is in process from your IP")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED,10*60*1001);
        Form dl1 = br.getFormbyProperty("id", "Premium");
        if (dl1 == null) dl1 = br.getFormbyProperty("name", "Premium");
        if (dl1 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.submitForm(dl1);
        String url = br.getRedirectLocation();
        if (url == null) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        dl = br.openDownload(downloadLink, url, false, 1);
        dl.startDownload();
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    // @Override
    public void reset() {
    }

    // @Override
    public void resetPluginGlobals() {
    }

    // @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
