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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mekashare.com" }, urls = { "http://[\\w\\.]*?(mekashare|literack)\\.com/\\d+/.+" }, flags = { 0 })
public class MekaShareCom extends PluginForHost {

    public MekaShareCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://mekashare.com/terms/";
    }

    public void correctDownloadLink(DownloadLink link) {
        // We only use mekashare links!
        link.setUrlDownload(link.getDownloadURL().replace("literack.com", "mekashare.com"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        // TODO:This can also be used good for mass-linkchecking!!
        String checklinks = "http://mekashare.com/check/";
        br.getPage(checklinks);
        br.postPage(checklinks, "links=" + link.getDownloadURL());
        // if (br.containsHTML("you have requested could not be found")) throw
        // new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML(">Inexistent<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = new Regex(link.getDownloadURL(), "mekashare\\.com/\\d+/(.+)").getMatch(0);
        String filesize = br.getRegex("width=\"112\" align=\"left\" style=\"padding:5px\">(.*?)</td>").getMatch(0);
        if (filename == null || filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        filesize = filesize.replace("i", "");
        link.setName(filename.trim());
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL());
        // Ticket Time
        String ttt = br.getRegex("var t =.*?(\\d+);").getMatch(0);
        int tt = 60;
        if (ttt != null) {
            logger.info("Waittime detected, waiting " + ttt + " seconds from now on...");
            tt = Integer.parseInt(ttt);
        }
        if (tt > 101) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, tt * 1001l);
        sleep(tt * 1001, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), "", false, 1);
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
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}