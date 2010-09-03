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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "yourfiles.to" }, urls = { "http://[\\w\\.]*?yourfiles\\.(biz|to)/\\?d=[\\w]+" }, flags = { 0 })
public class YourFilesTo extends PluginForHost {

    public YourFilesTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://yourfiles.biz/rules.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("http://yourfiles.to/", "yab_mylang", "en");
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Your requested file is not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("Filename:</b></font></td>.*?<td align=.*?width=.*?>(.*?)</td>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
        }
        String filesize = br.getRegex("File size:</b></td>.*?<td align=.*?>(.*?)</td>").getMatch(0);
        if (filesize == null) filesize = br.getRegex("File.*?size.*?:.*?</b>(.*?)<b><br>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        if (filesize != null) {
            downloadLink.setDownloadSize(Regex.getSize(filesize.trim()));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);

        String link = br.getRegex("var bla = 'http://http:/(.*?)';").getMatch(0);
        link = Encoding.urlDecode(link.replace("dumdidum", ""), true);
        link = Encoding.urlDecode(link.replace("dumdidu", ""), true);

        Browser brc = br.cloneBrowser();
        dl = BrowserAdapter.openDownload(brc, downloadLink, link);
        /* Workaround für fehlerhaften Filename Header */
        String name = Plugin.getFileNameFromHeader(dl.getConnection());
        if (name != null) downloadLink.setFinalFileName(Encoding.deepHtmlDecode(name));
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
