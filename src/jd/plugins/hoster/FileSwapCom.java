//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

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

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fileswap.com" }, urls = { "http://(www\\.)?fileswap\\.com/dl/[A-Za-z0-9]+/" }, flags = { 0 })
public class FileSwapCom extends PluginForHost {

    public FileSwapCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.fileswap.com/legal/tos.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>The file you requested has not been found or may no longer be available|<title>FileSwap\\.com :  download free</title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<legend>Share This File \\&#187; (.*?)</legend>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>FileSwap\\.com : (.*?) download free</title>").getMatch(0);
        String filesize = br.getRegex("<b>Size:</b>[\r\n\t]+(\\&nbsp\\;)?[\r\n\t]+([\\d\\.]+ [A-Z]{2})[\r\n\t]+").getMatch(1);
        if (filesize == null) filesize = br.getRegex("\\&nbsp;\\&nbsp;\\&nbsp; <b>Size:</b>\\&nbsp;[\t\n\r ]+</td>[\t\n\r ]+<td>(.*?)</td>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML(">The storage node this file is currently on is currently undergoing")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 2 * 60 * 1000l);
        int wait = 1;
        String waittime = br.getRegex("var time=(\\d+);").getMatch(0);
        if (waittime != null) wait = Integer.parseInt(waittime);
        sleep(wait * 1001l, downloadLink);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage("http://www.fileswap.com/ajax_requests.php", "id=" + new Regex(downloadLink.getDownloadURL(), "fileswap\\.com/dl/([A-Za-z0-9]+)/").getMatch(0));
        String dllink = br.toString();
        /* remove newline */
        dllink = dllink.replaceAll("%0D%0A", "");
        dllink = dllink.trim();
        if (!dllink.startsWith("http") || dllink.length() > 500) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -2);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("(There has been an error on the page you tried to access\\.|Support staff has been notified of this error\\.)")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many simultan downloads", 5 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}