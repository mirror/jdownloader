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
import java.util.Random;

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
        // They got captchas but they don't check if you enter em valid so lets
        // enter random stuff
        String randomstuff = new Regex(downloadLink.getDownloadURL(), "fileserve.com/file/(.+)").getMatch(0) + new Random().nextInt(10);
        br.postPage(downloadLink.getDownloadURL(), "captcha=" + randomstuff);
        sleep(60 * 1001l, downloadLink);
        br.postPage(downloadLink.getDownloadURL(), "downloadLink=show");
        br.postPage(downloadLink.getDownloadURL(), "download=normal");
        String dllink = br.getRedirectLocation();
        // Waittime
        String ttt = br.getRegex("<p>Wait (\\d+) seconds before next download</p>").getMatch(0);
        if (ttt != null) {
            logger.info("Waittime detected, waiting " + ttt.trim() + " seconds from now on...");
            int tt = Integer.parseInt(ttt);
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Wait some seconds till retry", tt * 1001l);
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