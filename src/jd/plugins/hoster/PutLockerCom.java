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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "putlocker.com" }, urls = { "http://(www\\.)?putlocker\\.com/file/[A-Z0-9]+" }, flags = { 0 })
public class PutLockerCom extends PluginForHost {

    public PutLockerCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.putlocker.com/page.php?terms";
    }

    private static final String MAINPAGE = "http://www.putlocker.com";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">This file doesn\\'t exist, or has been removed \\.<") || br.getURL().contains("?404")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex fileInfo = br.getRegex("class=\"site-content\">[\t\n\r ]+<h1>(.*?)<strong>\\( (.*?) \\)</strong></h1>");
        String filename = fileInfo.getMatch(0);
        if (filename == null) filename = br.getRegex("<title>PutLocker - (.*?)</title>").getMatch(0);
        String filesize = fileInfo.getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String hash = br.getRegex("<input type=\"hidden\" value=\"([a-z0-9]+)\" name=\"hash\">").getMatch(0);
        if (hash == null) {
            logger.warning("hash is null...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String waittime = br.getRegex("var countdownNum = (\\d+);").getMatch(0);
        int wait = 10;
        if (waittime != null) {
            logger.info("Regexed waittime found, waiting " + waittime + " seconds...");
            wait = Integer.parseInt(waittime);
        }
        sleep(wait * 1001l, downloadLink);
        br.postPage(br.getURL(), "hash=" + Encoding.urlEncode(hash) + "&confirm=Please+wait+for+0+seconds");
        String dllink = getDllink(downloadLink);
        if (dllink == null) {
            logger.warning("dllink is null...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!dllink.startsWith("http")) dllink = MAINPAGE + dllink;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getDllink(DownloadLink downloadLink) throws IOException {
        String dllink = br.getRegex("<a href=\"/gopro\\.php\">Tired of ads and waiting\\? Go Pro\\!</a>[\t\n\rn ]+</div>[\t\n\rn ]+<a href=\"(/.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"(/get_file\\.php\\?download=[A-Z0-9]+\\&key=[a-z0-9]+)\"").getMatch(0);
            if (dllink == null) {
                // Handling for streamlinks
                dllink = br.getRegex("playlist: \\'(/get_file\\.php\\?stream=[A-Z0-9]+)\\'").getMatch(0);
                if (dllink != null) {
                    dllink = MAINPAGE + dllink;
                    downloadLink.setProperty("videolink", dllink);
                    br.getPage(dllink);
                    dllink = br.getRegex("media:content url=\"(http://.*?)\"").getMatch(0);
                    if (dllink == null) dllink = br.getRegex("\"(http://media-b\\d+\\.putlocker\\.com/download/\\d+/.*?)\"").getMatch(0);
                }
            }
        }
        return dllink;
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