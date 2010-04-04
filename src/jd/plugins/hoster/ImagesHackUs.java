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
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imageshack.us" }, urls = { "http://[\\w\\.]*?img[0-9]{1,4}\\.imageshack\\.us/i/[a-z0-9]+\\.[a-zA-Z0-9]{2,4}/" }, flags = { 0 })
public class ImagesHackUs extends PluginForHost {

    public ImagesHackUs(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://reg.imageshack.us/content.php?page=rules";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (br.getRedirectLocation() != null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String finallink = br.getRegex("/rss\\+xml\" href=\"(.*?)\\.comments\\.xml\"").getMatch(0);
        if (finallink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        URLConnectionAdapter con = br.openGetConnection(finallink);
        if (con.getContentType().contains("html")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(getFileNameFromHeader(con));
        link.setDownloadSize(con.getContentLength());
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        String finallink = br.getRegex("/rss\\+xml\" href=\"(.*?)\\.comments\\.xml\"").getMatch(0);
        if (finallink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // More is possible but 1 chunk is good to prevent errors
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finallink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    // More is possible but 1 is good to prevent errors
    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}