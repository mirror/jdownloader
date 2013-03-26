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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filesmall.com" }, urls = { "http://(www\\.)?filesmall\\.com/[A-Za-z0-9]+/download\\.html" }, flags = { 0 })
public class FilesMallCom extends PluginForHost {

    public FilesMallCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.filesmall.com/terms.html";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">File Not Found, may be deleted by user or administrator")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<p>File Name: ([^<>\"]*?)</p>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>([^<>\"]*?) \\| FilesMall\\.com</title>").getMatch(0);
        String filesize = br.getRegex("<p>File Size: ([^<>\"]*?)</p>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String action = br.getRegex("<p class=\"bottom\">[\t\r\n ]+<form action=\"(http://[^<>\"]*?)\"").getMatch(0);
        if (action == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.postPage(action, "");
        if (!br.containsHTML("freecap\\.php")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        for (int i = 0; i <= 5; i++) {
            final String code = getCaptchaCode("http://www.filesmall.com/freecap.php", downloadLink);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, action, "download=1&word=" + code, false, 1);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                if (br.containsHTML("freecap\\.php")) continue;
            }
            break;
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("freecap\\.php")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return true;
    }
}