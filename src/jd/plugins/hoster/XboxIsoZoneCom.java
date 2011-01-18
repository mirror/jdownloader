//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xboxisozone.com" }, urls = { "http://(www\\.)?((xboxisozone|dcisozone|gcisozone|psisozone)\\.com|romgamer\\.com/download)/free/\\d+/(\\d+/)?" }, flags = { 0 })
public class XboxIsoZoneCom extends PluginForHost {

    public XboxIsoZoneCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://xboxisozone.com/faq.php";
    }

    public void correctDownloadLink(DownloadLink link) {
        if (!link.getDownloadURL().contains("www.")) link.setUrlDownload(link.getDownloadURL().replace("http://", "http://www."));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        // No available check possible because this tells the server that i
        // started a download
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        String addedLink = downloadLink.getDownloadURL();
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, addedLink, "", true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            String waittime = br.getRegex("You may download again in approximately:<b> (\\d+) Minutes").getMatch(0);
            if (waittime != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(waittime) * 60 * 1001l);
            if (br.containsHTML(">Free users may only download a maximum of")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
            if (br.containsHTML("(<TITLE>404 Not Found</TITLE>|<H1>Not Found</H1>|<title>404 - Not Found</title>|<h1>404 - Not Found</h1>)")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 4;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}