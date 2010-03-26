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

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "madshare.com" }, urls = { "http://[\\w\\.]*?madshare\\.com/en/download/[a-zA-Z0-9]+/" }, flags = { 0 })
public class MadShareCom extends PluginForHost {

    public MadShareCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.madshare.com/en/terms-and-conditions.html";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(parameter.getDownloadURL());
        if (br.getRedirectLocation() != null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>MadShare.com.*?Download(.*?)</title>").getMatch(0);
        if (filename == null) filename = br.getRegex("<h2>(.*?)\\(<span").getMatch(0);
        String filesize = br.getRegex("class=\"size\">(.*?)</span>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize + "Bytes"));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        requestFileInformation(link);
        String freelink = br.getRegex("\"(free-download/.*?/.*?)\"").getMatch(0);
        if (freelink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("http://www.madshare.com/" + freelink);
        String id = br.getRegex("id:'(.*?)'").getMatch(0);
        if (id == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("http://www.madshare.com/api/get-download-id?id=" + id);
        // Usually happens when you try to start more than 1 dl at the same time
        if (br.containsHTML("dailyLimitExceeded")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 60 * 1000l);
        String server = br.getRegex("server.*?'(.*?)'").getMatch(0);
        String key = br.getRegex("key.*?'(.*?)'").getMatch(0);
        if (server == null || key == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String dllink = "http://" + server + "/download/" + key;
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        int tt = 60;
        String ttt = br.getRegex("class=\"counter\" style='font-size:48px;color:#f15a22;'>(\\d+)</span><br />seconds left</b>").getMatch(0);
        if (ttt != null) {
            logger.info("Waittime detected, waiting " + ttt.trim() + " seconds from now on...");
            tt = Integer.parseInt(ttt);
        }
        tt = tt + 1;
        sleep(tt * 1001, link);
        br.setDebug(true);
        jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1).startDownload();
        if ((dl.getConnection().getContentType().contains("html"))) {
            String check = br.getURL();
            if (check.contains("freeWait") || check.contains("freeStarted")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
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

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

}
