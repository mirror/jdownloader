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
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filestore.to" }, urls = { "http://[\\w\\.]*?filestore\\.to/\\?d=[\\w]+" }, flags = { 0 })
public class FilestoreTo extends PluginForHost {

    public FilestoreTo(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(2000l);
        Browser.setRequestIntervalLimitGlobal(getHost(), 500);
    }

    @Override
    public String getAGBLink() {
        return "http://www.filestore.to/rules.php?setlang=en";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setCookie("http://www.filestore.to", "yab_mylang", "en");
        String url = downloadLink.getDownloadURL();
        String downloadName = null;
        String downloadSize = null;
        for (int i = 1; i < 10; i++) {
            try {
                br.getPage(url);
            } catch (Exception e) {
                continue;
            }
            if (!br.containsHTML("Your requested file is not found")) {
                downloadName = Encoding.htmlDecode(br.getRegex(Pattern.compile("Download: (.*)</td>", Pattern.CASE_INSENSITIVE)).getMatch(0));
                downloadSize = (br.getRegex(Pattern.compile("<td align=left width=\"76%\">(.*? [\\w]{2,})</td>", Pattern.CASE_INSENSITIVE)).getMatch(0));
                if (!(downloadName == null || downloadSize == null)) {
                    downloadLink.setName(downloadName);
                    downloadLink.setDownloadSize(Regex.getSize(downloadSize.replaceAll(",", "\\.")));
                    return AvailableStatus.TRUE;
                }
            }
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String page = Encoding.urlDecode(br.toString(), true);
        String[] links = HTMLParser.getHttpLinks(page, null);
        boolean found = false;
        for (String link : links) {
            if (!new Regex(link, ".*?.getfile\\.php.*?$").matches()) continue;
            Browser brc = br.cloneBrowser();
            dl = BrowserAdapter.openDownload(brc, downloadLink, link);
            if (dl.getConnection().isContentDisposition()) {
                found = true;
                break;
            } else
                dl.getConnection().disconnect();
        }
        if (found == false) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl.startDownload();
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 2000;
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
