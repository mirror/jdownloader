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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "save.am" }, urls = { "http://[\\w\\.]*?save\\.am/files/[a-z0-9]+/.*?\\.html" }, flags = { 0 })
public class SaveAm extends PluginForHost {

    public SaveAm(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://save.am/terms-of-service/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        // Use english language
        br.getPage("http://save.am/index.php?language=US");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(Sorry, this Download doesnt exist anymore|This file is either removed due to copyright claim or is deleted by the uploader)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("\">Download file(.*?) \\(.*?\\)</h1>").getMatch(0);
        if (filename == null) filename = br.getRegex("action=\"http://save\\.am/files/.*?/(.*?)\\.html\"").getMatch(0);
        String filesize = br.getRegex("\">Download file.*? \\((.*?)\\)</h1>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        String ttt = br.getRegex("var time = (\\d+)\\.").getMatch(0);
        int tt = 60;
        if (ttt != null) {
            logger.info("Waittime detected, waiting " + ttt.trim() + " seconds from now on...");
            tt = Integer.parseInt(ttt);
        }
        sleep(tt * 1001, downloadLink);
        br.postPage(downloadLink.getDownloadURL(), "section=benefit&did=0");
        br.postPage(downloadLink.getDownloadURL(), "section=waitingtime&did=0");
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            checkErrors(downloadLink);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            checkErrors(downloadLink);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public void checkErrors(DownloadLink theLink) throws NumberFormatException, PluginException {
        // Add more errors here!
        if (br.containsHTML("(Sorry, you cant  download more then 1 at time|Upgrade to Premium)")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultanious downloads", 10 * 60 * 1000l);

    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}