//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fisierulmeu.ro" }, urls = { "http://[\\w\\.]*?fisierulmeu\\.ro/.+/.+\\.html" }, flags = { 0 })
public class FisierulMeuRo extends PluginForHost {

    public FisierulMeuRo(PluginWrapper wrapper) {
        super(wrapper);

    }

    // @Override
    public String getAGBLink() {
        return "http://fisierulmeu.ro/termeni.html";
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    // @Override
    /*
     * /* public String getVersion() { return getVersion("$Revision$"); }
     */

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);

        String downloadUrl = br.getRegex(Pattern.compile("action=!!!(.*?)!!!")).getMatch(0);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadUrl, true, 1);
        dl.startDownload();
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        this.setBrowserExclusive();
        if (br.containsHTML("Ne pare rau, dar acest fisier are o adresa gresita.") || br.containsHTML("404 Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String name = br.getRegex(Pattern.compile("Numele Fisierului:</b>(.*?)</div>", Pattern.DOTALL)).getMatch(0).trim();
        if (name == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filesize = br.getRegex(Pattern.compile("<div class=\"dwn_text\"><b>Marimea Fisierului:</b>(.*?)</div>", Pattern.DOTALL)).getMatch(0).trim();
        if (filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(name);
        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));

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