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
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "zetshare.com" }, urls = { "http://[\\w\\.]*?zetshare\\.com/(download/|url/)?download\\.php\\?file=[0-9a-zA-Z]+" }, flags = { 0 })
public class ZetshareCom extends PluginForHost {

    public ZetshareCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.zetshare.com/download/index.php?page=tos";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        String url = downloadLink.getDownloadURL();
        br.getPage(url);

        if (br.containsHTML("Invalid download link")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String waittime = ((br.getRegex(Pattern.compile("trying to download again too soon![ ]*Wait (.*?)\\.<BR>"))).getMatch(0));
        if (waittime != null) {
            String downloadName = new Regex(url, "\\?file=(.+)").getMatch(0);
            if (downloadName != null) downloadLink.setName(downloadName.trim());
            downloadLink.getLinkStatus().setStatusText("Waiting-Ticket! No FileCheck possible!");
            return AvailableStatus.TRUE;
        }

        String downloadName = new Regex(url, "\\?file=(.+)").getMatch(0);
        String downloadSize = (br.getRegex(Pattern.compile("File Size : </b>(.*?)<br />", Pattern.CASE_INSENSITIVE)).getMatch(0));
        if (downloadSize == null || downloadName == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(downloadName.trim());
        downloadLink.setDownloadSize(SizeFormatter.getSize(downloadSize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        requestFileInformation(downloadLink);
        String waittime = null;
        while ((waittime = ((br.getRegex(Pattern.compile("trying to download again too soon![ ]*Wait (.*?)\\.<BR>"))).getMatch(0))) != null) {
            if (waittime != null) {
                this.sleep(TimeFormatter.getMilliSeconds(waittime), downloadLink);
                requestFileInformation(downloadLink);
            }
        }
        /* Link holen */
        String linkurl = Encoding.htmlDecode(new Regex(br, Pattern.compile("<a href=\"(http://www\\.zetshare\\.com/(download/|url/)*?download2\\.php\\?a=.*?)\"><", Pattern.CASE_INSENSITIVE)).getMatch(0));
        if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, linkurl);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 10;
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
