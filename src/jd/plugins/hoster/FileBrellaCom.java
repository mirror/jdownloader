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
import java.util.Random;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filebrella.com" }, urls = { "http://[\\w\\.]*?filebrella\\.com/download/[a-z0-9]+" }, flags = { 0 })
public class FileBrellaCom extends PluginForHost {

    public FileBrellaCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.filebrella.com/terms.php";
    }

    private static final String CAPTCHATEXT   = "images/verification\\.php";
    private static final String CAPTCHAFAILED = "status success=\"false\">";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(<title>FileBrella \\[File Deleted\\]</title>|<div class=\"title\">File Deleted</div>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<div class=\"title\">.*?</div>[\t\n\r ]+<div>[a-z0-9]+_(.*?)</div>").getMatch(0);
        if (filename == null || filename.equals("")) filename = br.getRegex("<title>FileBrella \\[[a-z0-9]+_(.*?)\\]</title>").getMatch(0);
        String filesize = br.getRegex("/> Filesize: (.*?)</div>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String fileID = new Regex(downloadLink.getDownloadURL(), "filebrella\\.com/download/(.+)").getMatch(0);
        if (!br.containsHTML(CAPTCHATEXT) || fileID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        for (int i = 0; i <= 3; i++) {
            String postLink = "http://www.filebrella.com/xml/captchaSubmit.php?code=" + getCaptchaCode("http://www.filebrella.com/images/verification.php", downloadLink) + "&fid=" + fileID + "&reqID=%6s" + 100000 + new Random().nextInt(900000);
            br.postPage(postLink, "");
            if (br.containsHTML(CAPTCHAFAILED)) continue;
            break;
        }
        if (br.containsHTML(CAPTCHAFAILED)) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String dllink = br.getRegex("link=\"(http.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://download\\d+\\.filebrella\\.com:\\d+/[a-z0-9]+/[a-z0-9]+/.*?)\"").getMatch(0);
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
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}