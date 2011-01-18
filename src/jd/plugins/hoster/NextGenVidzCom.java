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

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "nextgenvidz.com" }, urls = { "http://[\\w\\.]*?nextgenvidz\\.com/view/\\d+" }, flags = { 0 })
public class NextGenVidzCom extends PluginForHost {

    public NextGenVidzCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://nextgenvidz.com/terms.html";
    }

    private static final String DIRECTLINKREGEX1 = "so\\.addVariable\\(\\'file\\',\\'(http.*?)\\'\\);";
    private static final String DIRECTLINKREGEX2 = "\\'(http://sv\\d+\\.nextgenvidz\\.com/video/[a-u0-9]+/[a-u0-9]+/.*?)\\'";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.postPage(link.getDownloadURL(), "continue=1");
        if (br.containsHTML("No htmlCode read")) return AvailableStatus.UNCHECKABLE;
        if (br.containsHTML(">This Video Does not exist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>NextGenVidz.com :: Viewing \"(.*?)\"</title>").getMatch(0);
        if (filename == null) filename = br.getRegex("property=\"og:title\" content=\"(.*?)\"").getMatch(0);
        String filesize = br.getRegex("/>FileSize: (.*?)<br").getMatch(0);
        if (filename == null && filesize == null) {
            if (br.getRegex(DIRECTLINKREGEX1).getMatch(0) == null && br.getRegex(DIRECTLINKREGEX2).getMatch(0) == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            return AvailableStatus.TRUE;
        }
        if (filename != null) link.setFinalFileName(filename.trim());
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML("No htmlCode read")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
        String dllink = br.getRegex(DIRECTLINKREGEX1).getMatch(0);
        if (dllink == null) dllink = br.getRegex(DIRECTLINKREGEX2).getMatch(0);
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