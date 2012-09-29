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
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "own3d.tv" }, urls = { "http://[\\w\\.]*?own3d\\.tv/(.*?/)?(video|watch)/\\d+" }, flags = { 0 })
public class Own3dTvTv extends PluginForHost {

    private String dllink = null;

    public Own3dTvTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.own3d.tv/content/1/#policy_terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null) {
            if (br.getRedirectLocation().equals("http://www.own3d.tv/video//")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            downloadLink.setUrlDownload(br.getRedirectLocation());
            br.getPage(downloadLink.getDownloadURL());
        }
        String filename = br.getRegex("class=\"chanPlayerTitle fnt20\"><span class=\"fntB\">([^<>\"]*?)</span>").getMatch(0);

        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String hdLink = br.getRegex("\\'(videos/HD/\\d+/[^<>\"]*?)\\'").getMatch(0);
        String sdLink = br.getRegex("\\'(videos/SD/\\d+/[^<>\"]*?)\\'").getMatch(0);
        String temporaryID = br.getRegex("queryString: escape\\(\\'\\?(.*?)\\&").getMatch(0);
        if (temporaryID == null || sdLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String firstPart = hdLink;
        if (firstPart == null || firstPart.contains("/',")) firstPart = sdLink;
        dllink = "http://owned.vo.llnwd.net/v1/" + firstPart + "?" + temporaryID;
        filename = filename.trim();
        downloadLink.setFinalFileName(filename + ".mp4");
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(dllink);
            if (!con.getContentType().contains("html"))
                downloadLink.setDownloadSize(con.getLongContentLength());
            else
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
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
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}