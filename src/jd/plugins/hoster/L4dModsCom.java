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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "l4dmods.com" }, urls = { "http://[\\w\\.]*?l4dmods\\.com/index\\.php\\?option=com_joomloads(2)?\\&(view=package|controller=package&task=download)\\&(Itemid=[0-9]\\&packageId=[0-9]+|pid=\\d+\\&Itemid=\\d+)" }, flags = { 2 })
public class L4dModsCom extends PluginForHost {

    public L4dModsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.l4dmods.com/forums/ucp.php?mode=register";
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("(view=package|controller=package&task=download)", "view=package"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("<h3></h3>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<h3>(.*?)</h3>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>www\\.L4DMods\\.com - Downloads \\|(.*?)</title>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("class=\"package\">(.*?)</span>").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("class=\"title\">(.*?)</div>").getMatch(0);
                }
            }
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        filename = filename.replaceAll("(\\&quot;|\\&#039;)", "");
        downloadLink.setName(filename.trim());
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        String dlPage = downloadLink.getDownloadURL().replace("view=package", "controller=download&task=ticket");
        br.getPage(dlPage);
        String dllink = br.getRegex("var redirect = '(http://.*?)';").getMatch(0);
        if (dllink == null) dllink = br.getRegex("'(http://dl\\.l4dmods\\..{1,20}\\.de/download\\.php\\?id=\\d+\\&sess=[a-z0-9]+)'").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Ticket Time
        int tt = 25;
        String ttt = new Regex(br.toString(), "countdown\\((\\d+)\\)").getMatch(0);
        if (ttt != null) {
            logger.info("Waittime detected, waiting " + ttt + " seconds from now on...");
            tt = Integer.parseInt(ttt);
        }
        sleep(tt * 1001, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            System.out.print(br.toString());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.setAllowFilenameFromURL(true);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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
