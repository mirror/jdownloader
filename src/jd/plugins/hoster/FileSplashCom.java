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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filesplash.com" }, urls = { "http://[\\w\\.]*?(filesplash|hitlisted)\\.com/[0-9a-z]{12}" }, flags = { 0 })
public class FileSplashCom extends PluginForHost {

    public FileSplashCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://filesplash.com/tos.html";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://www.filesplash.com", "lang", "english");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("No such file")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("No such user exist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("File Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("><td nowrap>(.*?)</td></tr>").getMatch(0);
        String filesize = br.getRegex("\\(([0-9]+ bytes)\\)").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename);
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        String dllink = br.getRegex("\"(http://cherryload\\.us/files/.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("<br>.*?<br>.*?<a href=\"(http.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String passCode = null;
        // if (br.containsHTML("name=\"password\"")) {
        // if (downloadLink.getStringProperty("pass", null) == null) {
        // passCode = Plugin.getUserInput("Password?", downloadLink);
        // } else {
        // /* gespeicherten PassCode holen */
        // passCode = downloadLink.getStringProperty("pass", null);
        // }
        // DLForm.put("password", passCode);
        // }
        // Ticket Time
        String ttt = br.getRegex("countdown\">.*?(\\d+).*?</span>").getMatch(0);
        if (ttt != null) {
            logger.info("Waittime detected, waiting " + ttt.trim() + " seconds from now on...");
            int tt = Integer.parseInt(ttt);
            sleep(tt * 1001, downloadLink);
        }
        jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (!(dl.getConnection().isContentDisposition())) {
            br.followConnection();
            if (br.containsHTML("Wrong password")) {
                logger.warning("Wrong password!");
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        dl.startDownload();
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