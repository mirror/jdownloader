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
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hostuje.net" }, urls = { "http://[\\w\\.]*?hostuje\\.net/file\\.php\\?id=[a-zA-Z0-9]+" }, flags = { 0 })
public class HostUjeNet extends PluginForHost {

    public HostUjeNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://hostuje.net/regulamin.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws PluginException, IOException {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:34.0) Gecko/20100101 Firefox/34.0");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML(">Podany plik nie został odnaleziony")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = br.getRegex("name=\"name\" value=\"([^<>\"]*?)\"").getMatch(0);
        final String filesize = br.getRegex("<b>Rozmiar:</b>([^<>\"]*?)<br>").getMatch(0);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setName(Encoding.htmlDecode(filename).trim());
        downloadLink.setDownloadSize(SizeFormatter.getSize(Encoding.htmlDecode(filesize)));
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        final String fidhash = new Regex(downloadLink.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
        final String k = br.getRegex("name=\"k\" value=\"([^<>\"]*?)\"").getMatch(0);
        final String mimetype = br.getRegex("name=\"mime\" value=\"([^<>\"]*?)\"").getMatch(0);
        if (k == null || mimetype == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // br.getPage("http://hostuje.net/obraz.php");
        // br.getPage("http://hostuje.net/dll_kody.js.php?new7a&flash=0");
        br.postPage(br.getURL(), "REG=1&OK=1&hasz=" + fidhash + "&id=" + fidhash + "&name=" + Encoding.urlEncode(downloadLink.getName()) + "&mime=" + Encoding.urlEncode(mimetype) + "&k=" + k);
        String link = null;
        link = br.getRedirectLocation();
        if (link == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, link, false, 1);
        dl.startDownload();
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return true;
    }
}