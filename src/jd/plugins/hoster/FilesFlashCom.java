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
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filesflash.com" }, urls = { "http://(www\\.)?filesflash\\.com/[a-z0-9]+" }, flags = { 0 })
public class FilesFlashCom extends PluginForHost {

    public FilesFlashCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://filesflash.com/tos.php";
    }

    private static final String IPBLOCKED = "(>Your IP address is already downloading another link|Please wait for that download to finish\\.|Free users may only download one file at a time\\.)";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>That is not a valid url\\.<|>That file is not available for download\\.<)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex(">Filename: (.*?)<br").getMatch(0);
        String filesize = br.getRegex("Size: (.*?)</td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String token = br.getRegex("<input type=\"hidden\" name=\"token\" value=\"(.*?)\"/>").getMatch(0);
        if (token == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.postPage("http://filesflash.com/freedownload.php", "token=" + token + "&freedl=+Free+Download+");
        if (br.containsHTML(IPBLOCKED)) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "IP already downloading", 10 * 60 * 1000l);
        if (br.containsHTML("(>That file is too big for free downloading.| Max allowed size for free downloads is)")) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.filesflashcom.only4premium", "Only downloadable for premium users"));
        String dllink = br.getRegex("<div id=\"link\" style=\"display:none\"><a href=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://[a-z0-9]+\\.filesflash\\.com/[a-z0-9]+/[a-z0-9]+/.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String wait = br.getRegex("count=(\\d+);").getMatch(0);
        int waittime = 45;
        if (wait != null) waittime = Integer.parseInt(wait);
        // Normal waittime is 45 seconds, if waittime > 2 Minutes reconnect
        if (waittime > 120) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittime * 1001l);
        sleep(waittime * 1001l, downloadLink);
        br.setFollowRedirects(false);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(IPBLOCKED)) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "IP already downloading", 10 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}