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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wrzuc.to" }, urls = { "http://[\\w\\.]*?wrzuc\\.to/plik/\\w+/.+" }, flags = { 0 })
public class WrzucTo extends PluginForHost {

    public WrzucTo(PluginWrapper wrapper) {
        super(wrapper);
        br.setFollowRedirects(true);
    }

    // @Override
    public String getAGBLink() {
        return "http://www.wrzuc.to/strona/regulamin";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());

        String filesize = br.getRegex(Pattern.compile("<tbody class=\"info\">.*<tr>.*<td>(.*?)</td>", Pattern.DOTALL)).getMatch(0);
        if (filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String name = br.getRegex(Pattern.compile("<div id=\"file_info\">.*<strong>(.*?)</strong><br />", Pattern.DOTALL)).getMatch(0);
        if (name == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(name.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {

        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        String link = br.getRegex(Pattern.compile("<a href=\"(.*?)\">Pobierz plik | Download File</a>")).getMatch(0);
        dl = jd.plugins.BrowserAdapter.openDownload(br,downloadLink, link, false, 1);
        dl.startDownload();
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return this.getMaxSimultanDownloadNum();
    }

    // @Override
    public void reset() {
    }

    // @Override
    public void resetPluginGlobals() {
    }

    // @Override
    public void resetDownloadlink(DownloadLink link) {
        // TODO Auto-generated method stub
    }
}