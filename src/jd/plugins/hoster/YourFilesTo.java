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
        String filename = br.getRegex("Filename:</b></font></td>.*?<td align=.*?width=.*?>(.*?)</td>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        filename = Encoding.deepHtmlDecode(filename);
        String page = Encoding.urlDecode(br.toString(), true);
        String[] links = HTMLParser.getHttpLinks(page, null);
        if (br.containsHTML("var timeout=")) {
            String[] times = br.getRegex("var.*?=.*?(\\d+)").getColumn(0);
            int tt = 30;
            if (times.length > 0) {
                for (String t : times) {
                    if (Integer.parseInt(t) > 10 && Integer.parseInt(t) < tt) tt = Integer.parseInt(t);
                }
            }
            sleep(tt * 1001l, downloadLink);
        }
        boolean found = false;
        for (String link : links) {
            String fakelink = Encoding.deepHtmlDecode(link);
            if (!fakelink.contains(filename)) continue;
            if (br.containsHTML("replace")) {
                String[] replacessuck = br.getRegex("(\\.replace\\(.*?,.*?\\))").getColumn(0);
                if (replacessuck != null) {
                    for (String fckU : replacessuck) {
                        String rpl1 = new Regex(fckU, "replace\\((.*?),.*?\\)").getMatch(0).replace("/", "");
                        String rpl2 = new Regex(fckU, "replace\\(.*?, \"(.*?)\"\\)").getMatch(0);
                        fakelink = fakelink.replace(rpl1, rpl2);
                    }
                }
            }
            Browser brc = br.cloneBrowser();
            dl = BrowserAdapter.openDownload(brc, downloadLink, fakelink);
            if (dl.getConnection().isContentDisposition()) {
                String fakename = Plugin.getFileNameFromHeader(dl.getConnection());
                if (fakename.contains("README.TXT")) {
                    dl.getConnection().disconnect();
                    continue;
                }
                found = true;
                break;
            } else {
                dl.getConnection().disconnect();
            }
        }
        if (!found) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
