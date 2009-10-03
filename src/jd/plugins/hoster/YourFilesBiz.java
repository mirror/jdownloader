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
import jd.parser.html.HTMLParser;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "yourfiles.biz" }, urls = { "http://[\\w\\.]*?yourfiles\\.(biz|to)/\\?d=[\\w]+" }, flags = { 0 })
public class YourFilesBiz extends PluginForHost {

    public YourFilesBiz(PluginWrapper wrapper) {
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
        String filename = br.getRegex("<b>File name:</b></td>\\s+<td align=left width=[0-9]+%>(.*?)</td>").getMatch(0);
        String filesize = br.getRegex("File size:</b></td>\\s+<td align=left>(.*?)</td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String filename = br.getRegex("<b>File name:</b></td>\\s+<td align=left width=[0-9]+%>(.*?)</td>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        filename = filename.replace("(", "%2528");
        filename = filename.replace(")", "%2529");
        filename = filename.replace("'", "%27");
        filename = filename.replace(" ", "%20");
        String page = Encoding.urlDecode(br.toString(), true);
        String[] links = HTMLParser.getHttpLinks(page, null);
        if (br.containsHTML("var timeout=")) {
            int tt = Integer.parseInt(br.getRegex("var timeout='(\\d+)';").getMatch(0));
            sleep(tt * 1001l, downloadLink);
        }
        boolean found = false;
        for (String link : links) {
            if (!link.contains(filename)) continue;
            Browser brc = br.cloneBrowser();
            dl = BrowserAdapter.openDownload(brc, downloadLink, link);
            if (dl.getConnection().isContentDisposition()) {
                String fakename = Plugin.getFileNameFromHeader(dl.getConnection());
                if (fakename.contains("README.TXT") || !fakename.equals(filename))continue;
                found = true;
                break;
            } else {
                dl.getConnection().disconnect();
            }
        }
        if (!found) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
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
