//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fsishare.com" }, urls = { "http://(www\\.)?fsishare\\.com/videopage/.*?\\.html" }, flags = { 0 })
public class FsiShareCom extends PluginForHost {

    public FsiShareCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://fsishare.com";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        // Max 5 connections at all, using more will result in disconnection or
        // network errors
        return 1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = br.getRegex("<div id=\"downloadbutton\" style=\"display: none;\">[\t\n\r ]+<p class=\"style7\" align=\"left\"><a[\t\n\r ]+href=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://(www\\.)?fsishare\\.com/mmsclips/.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Waittime can still be skipped
        // String waittime =
        // br.getRegex("link will appear in (\\d+) seconds").getMatch(0);
        // if (waittime == null) waittime =
        // br.getRegex("var c = (\\d+);").getMatch(0);
        // int wait = 30;
        // if (waittime != null) wait = Integer.parseInt(waittime);
        // sleep(wait * 1001l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -5);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        // Often slow servers
        br.setReadTimeout(60 * 1000);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>404 Not Found<|>Not Found<)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("Name :([^<>\"]+)</p>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>FSI Share \\-(.*?)</title>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Filesize is now always displayed
        String fileSize = br.getRegex(">Video Size : (.*?)</p>").getMatch(0);
        if (fileSize != null) link.setDownloadSize(SizeFormatter.getSize(fileSize));
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".3gp");
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}