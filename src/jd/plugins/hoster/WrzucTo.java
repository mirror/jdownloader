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
import jd.http.RandomUserAgent;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wrzuc.to" }, urls = { "http://[\\w\\.]*?wrzuc\\.to/.+(\\.wt|\\.html)" }, flags = { 0 })
public class WrzucTo extends PluginForHost {

    public WrzucTo(PluginWrapper wrapper) {
        super(wrapper);
        br.setFollowRedirects(true);
    }

    public String getAGBLink() {
        return "http://www.wrzuc.to/strona/regulamin";
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://www.wrzuc.to", "language", "en");
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        String filesize = br.getRegex(Pattern.compile("class=\"info\">.*?<tr>.*?<td>(.*?)</td>", Pattern.DOTALL)).getMatch(0);
        String name = br.getRegex(Pattern.compile("<div id=\"file_info\">.*<strong>(.*?)</strong><br />", Pattern.DOTALL)).getMatch(0);
        if (name == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(name.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String dllink = br.getRegex(("DwnlButton\">.*?<a href=\"(.*?)\">Download file")).getMatch(0);
        // To the original Coder of this plugin: Please check if the dllink is
        // null, else JD will show a "browser Fehler null" and then we have
        // confused users ;)
        if (dllink == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT); }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    public void reset() {
    }

    public void resetPluginGlobals() {
    }

    public void resetDownloadlink(DownloadLink link) {

    }
}