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

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rapidgator.net" }, urls = { "http://[\\w\\.]*?rapidgator\\.net/(files/dl/)?\\d+/.*?\\.html" }, flags = { 0 })
public class RapidGatorNet extends PluginForHost {

    public RapidGatorNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://rapidgator.net/?content=terms";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("files/dl/", ""));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("UTF-8");
        br.setCookie("http://rapidgator.net/", "language", "en");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(<h3>File not found</h3>|<h1>Error 404</h1>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<p><b>(.*?)</b>").getMatch(0);
        if (filename == null) filename = br.getRegex("onclick=\"initPremiumDl\\(\\&#039;/files/dlpremium/\\d+/(.*?)\\.html\\&#039;\\);\"").getMatch(0);
        String filesize = br.getRegex("style=\"color:#8E908F;\">(.*?)</font>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        Regex someImportantStuff = br.getRegex("onclick=\"initCountdown\\(\\&#039;(.*?)\\&#039;, \\&#039;(.*?)\\&#039;, (.*?)\\&#039;, \\&#039;(.*?)\\&#039;, (.*?)\\&#039;/ajax/initdl\\&#\\d+;\\);\"");
        String fileid = someImportantStuff.getMatch(0);
        String file_hash = someImportantStuff.getMatch(1);
        String old_sid = someImportantStuff.getMatch(3);
        if (fileid == null || file_hash == null || old_sid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        String postData = "file_id=" + fileid + "&file_hash=" + file_hash + "&mode=free&old_sid=" + old_sid;
        br.postPage("http://rapidgator.net/ajax/initdl", postData);
        String dllink = br.getRegex("<b>Ссылка для скачивания:</b>[\r\t\n <td><tr></tr></td>]+<a href=\"(.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://dl\\d+\\.rapidgator\\.net/api/index\\.php\\?sid=[a-z0-9]+)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}