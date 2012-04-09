//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision: 15600 $", interfaceVersion = 2, names = { "nekaka.com" }, urls = { "https?://(www\\.)?nekaka\\.com/d/[a-zA-Z0-9\\-\\_]{10}" }, flags = { 0 })
public class NekakaCom extends PluginForHost {

    // DEV NOTES:
    // protocol: http & https
    // free: resumes, one chunk, maxdl unlimited
    // captchatype: null

    public NekakaCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.nekaka.com";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.setCookie("http://www.nekaka.com/", "lang", "en");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(?i)invalid file link")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("(?i)<nobr>([^<>]+) ?</nobr>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("(?i)Download File  \\- ([^<>]+)</title>").getMatch(0);
            if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String filesize = br.getRegex("(?i)([\\d\\.]+ ?(GB|MB))").getMatch(0);
        link.setName(filename.trim());
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = br.getRegex("(?i)(https?://([\\w\\-\\.]+)?nekaka\\.com/files/download/\\d+)").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
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
    public void resetDownloadlink(DownloadLink link) {
    }

}