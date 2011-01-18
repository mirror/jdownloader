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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ehow.com" }, urls = { "http://[\\w\\.]*?.ehow\\.com/video_\\d+_.*?\\.html" }, flags = { 0 })
public class EHowCom extends PluginForHost {

    public EHowCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String dllink = null;
    private boolean hd = true;

    @Override
    public String getAGBLink() {
        return "http://www.ehow.com/terms_use.aspx";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null) {
            if (br.getRedirectLocation().contains("Error.aspx\\?404=true")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (br.containsHTML("This video does not exist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<meta name=\"title\" content=\"(.*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("var articleTitle = \"(.*?)\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<h1 class=\"Heading1a\" style=\"clear:both;\">(.*?)</h1>").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("class=\"itemContent playing jsVideoTitle\">(.*?)</a>").getMatch(0);
                    if (filename == null) filename = br.getRegex("var title = \"(.*?)\"").getMatch(0);
                }
            }

        }
        getDllink();
        if (filename == null || dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        filename = filename.trim();
        downloadLink.setFinalFileName(filename + ".flv");
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = br2.openGetConnection(dllink);
        if (!con.getContentType().contains("html"))
            downloadLink.setDownloadSize(con.getLongContentLength());
        else
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (hd)
            downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.EHowCom.hdAvailable", "Download is available in HD"));
        else
            downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.EHowCom.hdNotAvailable", "Download is only available in SD"));
        return AvailableStatus.TRUE;
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

    public void getDllink() throws Exception {
        dllink = br.getRegex("'(http://cdn-viper\\.demandvideo\\.com/media/[a-z0-9-]+/flashHD/[a-z0-9-]+\\.flv)'").getMatch(0);
        if (dllink == null) {
            hd = false;
            dllink = br.getRegex("(http://cdn-viper\\.demandvideo\\.com/media/[a-z0-9-]+/flash/[a-z0-9-]+\\.flv)").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("id: '(http://.*?\\.flv)'").getMatch(0);
            }
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
