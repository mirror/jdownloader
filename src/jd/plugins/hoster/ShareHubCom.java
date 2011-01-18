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

import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sharehub.com" }, urls = { "http://(go.sharehub.com|sharehub.me|follow.to|kgt.com|krt.com)/.*" }, flags = { 0 })
public class ShareHubCom extends PluginForHost {

    public ShareHubCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String getAGBLink() {
        return "http://sharehub.com/tos.php";
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        br.setDebug(true);
        AvailableStatus av = requestFileInformation(downloadLink);
        if (av != AvailableStatus.TRUE) throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED);
        Form form = br.getForm(0);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        form.setProperty("x", (int) (Math.random() * 134) + "");
        form.setProperty("y", (int) (Math.random() * 25) + "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, form, false, 1);
        dl.setFilesizeCheck(false);
        dl.startDownload();

    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("<h1>File not found!</h1>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex(Pattern.compile("<h1 id=\"fileName\">(.*?)</h1>", Pattern.CASE_INSENSITIVE)).getMatch(0));
        String filesize = br.getRegex("<td><strong>File size:</strong></td>.*?<td>(.*?)</td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void reset() {
    }

    public void resetDownloadlink(DownloadLink link) {

    }

}
