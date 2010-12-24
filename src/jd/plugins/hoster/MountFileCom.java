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
import jd.http.RandomUserAgent;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mountfile.com" }, urls = { "http://[\\w\\.]*?mountfile\\.com/file/[a-z0-9]+/[a-z0-9]+" }, flags = { 0 })
public class MountFileCom extends PluginForHost {

    public MountFileCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.mountfile.com/user/terms.html";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("share.", ""));
    }

    private static final String MAINPAGE = "http://mountfile.com/";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setCookie("http://mountfile.com/", "language", "en_us");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("File not found or System under maintanence\\.")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?) - MountFile\\.com - Free File Hosting and Sharing, Permanently Save </title>").getMatch(0);
        if (filename == null) filename = br.getRegex("<h2 class=\"title\">Downloading:\\&nbsp;\\&nbsp;(.*?)</h2>").getMatch(0);
        String filesize = br.getRegex("File Size: <b>(.*?)</b><br>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String bigWaittime = br.getRegex("id=\"down_interval\">(\\d+)</span>").getMatch(0);
        if (bigWaittime == null) bigWaittime = br.getRegex("style=\"font-size: 28px; color: green;\">(\\d+)</span> minutes</span>").getMatch(0);
        if (bigWaittime != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(bigWaittime) * 60 * 1001l);
        if (br.containsHTML(">You reached your hourly traffic limit")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1001l);
        // The little waittime is skippable atm.
        // int waitThis = 30;
        // String littleWaittime =
        // br.getRegex(">\\&nbsp;\\&nbsp;(\\d+) seconds").getMatch(0);
        // if (littleWaittime != null) {
        // logger.info("Regexed waittime: " + littleWaittime + " seconds");
        // waitThis = Integer.parseInt(littleWaittime);
        // }
        // sleep(waitThis * 1001l, downloadLink);
        String userid = new Regex(downloadLink.getDownloadURL(), "mountfile\\.com/file/(.*?)/").getMatch(0);
        String fileid = new Regex(downloadLink.getDownloadURL(), "mountfile\\.com/file/.*?/(.+)").getMatch(0);
        if (userid == null || fileid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("http://mountfile.com/file/down/" + userid + "/" + fileid + ".html");
        String vid = br.getRegex("name=\"vid\" value=\"(.*?)\"").getMatch(0);
        String vid1 = br.getRegex("setCookie\\(\"vid1\", \"(.*?)\"").getMatch(0);
        String vid2 = br.getRegex("setCookie\\(\"vid2\", \"(.*?)\"").getMatch(0);
        if (vid == null || vid1 == null || vid2 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Those cookies are important, no downloadstart without them!
        br.setCookie(MAINPAGE, "vid1", vid1);
        br.setCookie(MAINPAGE, "vid2", vid2);
        String postData = "module=fileService&action=downfile&userId=" + userid + "&fileId=" + fileid + "&vid=" + vid;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, "http://dl1.mountfile.com/view", postData, false, 1);
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