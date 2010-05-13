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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fileflyer.com" }, urls = { "http://[\\w\\.]*?fileflyer\\.com/view/[\\w]+" }, flags = { 0 })
public class FileFlyerCom extends PluginForHost {

    public FileFlyerCom(PluginWrapper wrapper) {
        super(wrapper);
        br.setFollowRedirects(true);
    }

    public String getAGBLink() {
        return "http://www.fileflyer.com/legal/terms.aspx";
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        String filesize = br.getRegex(Pattern.compile("<a id=\"ItemsList_ctl00_size\">(.*?)</a>", Pattern.CASE_INSENSITIVE)).getMatch(0);
        String name = br.getRegex(Pattern.compile("<a id=\"ItemsList_ctl00_file\".*\\ title=\"(.*?)\".*?class=", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (name == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        downloadLink.setName(name.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize));
        if (br.containsHTML("Access to old files is available to premium users only")) downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.FileFlyerCom.errors.Only4Premium", "Only downloadable for premium users"));
        if (br.containsHTML("(class=\"handlink\">Expired</a>|class=\"handlink\">Removed</a>)"))
            return AvailableStatus.FALSE;
        else
            return AvailableStatus.TRUE;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (br.containsHTML("Access to old files is available to premium users only")) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.FileFlyerCom.errors.Only4Premium", "Only downloadable for premium users"));
        if (br.containsHTML("access to the service may be unavailable for a while")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "No free slots available", 30 * 60 * 1000l);
        String linkurl = br.getRegex(Pattern.compile("<a id=\"ItemsList_ctl00_img\".*href=\"(.*)\">")).getMatch(0);
        if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        linkurl = Encoding.htmlDecode(linkurl);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, linkurl, true, 0);
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void reset() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }
}
