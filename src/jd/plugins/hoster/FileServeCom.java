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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fileserve.com" }, urls = { "http://[\\w\\.]*?fileserve\\.com/file/[a-zA-Z0-9]+" }, flags = { 0 })
public class FileServeCom extends PluginForHost {

    public FileServeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.fileserve.com/terms.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(The file could not be found|Please check the download link)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("fileserve\\.com/file/[a-zA-Z0-9]+/(.*?)</b>").getMatch(0);
        String filesize = br.getRegex("</b> \\((.*?)\\)").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        if (filesize != null) link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        if (!br.containsHTML("captica.php")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        for (int i = 0; i <= 3; i++) {
            String captchaUrl = "http://www.fileserve.com/captica.php" + "";
            String code = getCaptchaCode(captchaUrl, downloadLink);
            br.postPage(downloadLink.getDownloadURL(), "captcha=" + code);
            if (br.containsHTML("captica.php")) continue;
            break;
        }
        // Ticket Time
        int tt = 60;
        String ttt = br.getRegex("id=\"timmer\">(\\d+)</span").getMatch(0);
        if (ttt != null) {
            logger.info("Waittime detected, waiting " + ttt.trim() + " seconds from now on...");
            tt = Integer.parseInt(ttt);
        }
        sleep(tt * 1001, downloadLink);
        br.postPage(downloadLink.getDownloadURL(), "downloadLink=show");
        br.postPage(downloadLink.getDownloadURL(), "download=normal");
        String dllink = br.getRedirectLocation();
        // Waittime
        String reconTime = br.getRegex("<p>Wait (\\d+) seconds before next download</p>").getMatch(0);
        if (reconTime != null) {
            logger.info("Waittime detected, waiting " + reconTime + " seconds from now on...");
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Wait some seconds till retry", Integer.parseInt(reconTime) * 1001l);
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
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
        return 2;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}