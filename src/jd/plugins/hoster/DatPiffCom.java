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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "datpiff.com" }, urls = { "http://(www\\.)?datpiff\\.com/.*?-download\\.php\\?id=[a-z0-9]+" }, flags = { 0 })
public class DatPiffCom extends PluginForHost {

    public DatPiffCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.datpiff.com/terms";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>Download Unavailable<|>A zip file has not yet been generated<)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>Download Mixtape \\&quot;(.*?)\\&quot;</title>").getMatch(0);
        if (filename == null) filename = br.getRegex("<span>Download Mixtape<em>(.*?)</em></span>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML(">you must be logged in to download mixtapes<")) throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable via account");
        String fileID = new Regex(downloadLink.getDownloadURL(), "download\\.php\\?id=(.+)").getMatch(0);
        if (fileID == null || !br.containsHTML("download-mixtape\\.php")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.postPage("http://www.datpiff.com/download-mixtape.php", "id=" + fileID + "&x=84&y=11");
        String dllink = br.getRedirectLocation();
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // Server doesn't send the correct filename directly, filename fix also
        // doesn't work so we have to do it this way
        downloadLink.setFinalFileName(getFileNameFromHeader(dl.getConnection()));
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