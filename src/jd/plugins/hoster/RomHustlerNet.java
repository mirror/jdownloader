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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "romhustler.net" }, urls = { "http://(www\\.)?romhustler\\.net/rom/[^<>\"/]+/[^<>\"/]+(/[^<>\"/]+)?" }, flags = { 0 })
public class RomHustlerNet extends PluginForHost {

    public RomHustlerNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String getAGBLink() {
        return "http://romhustler.net/disclaimer.php";
    }

    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        final String dlink = br.getRegex("\"(/download/\\d+)\"").getMatch(0);
        if (dlink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage(dlink);

        boolean skipWaittime = true;
        if (!skipWaittime) {
            int wait = 8;
            final String waittime = br.getRegex("start=\"(\\d+)\"></span>").getMatch(0);
            if (waittime != null) wait = Integer.parseInt(waittime);
            sleep(wait * 1001l, downloadLink);
        }

        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getPage("http://romhustler.net/link/" + new Regex(dlink, "(\\d+)$").getMatch(0) + "?_=" + System.currentTimeMillis());
        String finallink = br.toString().trim();
        if (finallink == null || !finallink.startsWith("http://") || finallink.length() > 500) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        br.getHeaders().put("X-Requested-With", null);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finallink, false, -2);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        String filename = dl.getConnection().getURL().toString();
        filename = Encoding.htmlDecode(filename.substring(filename.lastIndexOf("/") + 1));
        if (filename != null && filename.contains(downloadLink.getName())) {
            downloadLink.setFinalFileName(filename);
        }
        dl.startDownload();
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException, IOException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML(">404 \\- Page got lost")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String name = br.getRegex("<title>Download ([^<>\"]*?) Rom / Iso").getMatch(0);
        if (name == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        downloadLink.setName(Encoding.htmlDecode(name.trim()));
        return AvailableStatus.TRUE;
    }

    public void reset() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }

    public void resetPluginGlobals() {
    }

}