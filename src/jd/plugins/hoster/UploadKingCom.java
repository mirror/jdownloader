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
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

//Nearly the same code as plugin UploadHereCom
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploadking.com" }, urls = { "http://(www\\.)?uploadking\\.com/[A-Z0-9]+" }, flags = { 0 })
public class UploadKingCom extends PluginForHost {

    public UploadKingCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.uploadking.com/terms";
    }

    private static final String TEMPORARYUNAVAILABLE         = "(>Unfortunately, this file is temporarily unavailable|> \\- The server the file is residing on is currently down for maintenance)";
    private static final String TEMPORARYUNAVAILABLEUSERTEXT = "This file is temporary unavailable!";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.setCustomCharset("utf-8");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(TEMPORARYUNAVAILABLE)) {
            link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.uploadherecom.temporaryunavailable", TEMPORARYUNAVAILABLEUSERTEXT));
            return AvailableStatus.TRUE;
        }
        if (br.containsHTML("(>Unfortunately, this file is unavailable|> \\- Invalid link|> \\- The file has been deleted)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex infoWhileLimitReached = br.getRegex(">You are currently downloading (.*?) \\((\\d+.*?)\\)\\. Please ");
        String filename = br.getRegex("\">File: <b>(.*?)</b>").getMatch(0);
        if (filename == null) filename = infoWhileLimitReached.getMatch(0);
        String filesize = br.getRegex("\">Size: <b>(.*?)</b>").getMatch(0);
        if (filesize == null) filesize = infoWhileLimitReached.getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML(TEMPORARYUNAVAILABLE)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.uploadherecom.temporaryunavailable", TEMPORARYUNAVAILABLEUSERTEXT), 60 * 60 * 1000l);
        if (br.containsHTML("(>You are currently downloading|this download, before starting another\\.</font>)")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads", 5 * 60 * 1000l);
        String dllink = br.getRegex("\" id=\"dlbutton\"><a href=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://www\\d+\\.uploadking\\.com:\\d+/files/[A-Z0-9]+/.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // More connections possible but doesn't work for all links
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -11);
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