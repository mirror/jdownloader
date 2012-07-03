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
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "eroprofile.com" }, urls = { "http://(www\\.)?eroprofile\\.com/m/(videos/view/|photos/view/)[A-Za-z0-9\\-]+" }, flags = { 0 })
public class EroProfileCom extends PluginForHost {

    public EroProfileCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String DLLINK = null;

    @Override
    public String getAGBLink() {
        return "http://www.eroprofile.com/p/help/termsOfUse";
    }

    private static final String VIDEOLINK = "http://(www\\.)?eroprofile\\.com/m/videos/view/[A-Za-z0-9\\-]+";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("http://eroprofile.com/", "lang", "en");
        br.getPage(downloadLink.getDownloadURL());
        if (downloadLink.getDownloadURL().matches(VIDEOLINK)) {
            if (br.containsHTML("(>Video not found|>The video could not be found|<title>EroProfile</title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            String filename = getFilename();
            DLLINK = br.getRegex("file:\\'(http://[^<>\"]*?)\\'").getMatch(0);
            if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            DLLINK = Encoding.htmlDecode(DLLINK);
            String ext = DLLINK.substring(DLLINK.lastIndexOf("."));
            if (ext == null || ext.length() > 5) ext = ".m4v";
            downloadLink.setFinalFileName(filename + ext);
        } else {
            if (br.containsHTML("(>Photo not found|>The photo could not be found|<title>EroProfile</title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            String filename = getFilename();
            DLLINK = br.getRegex("<div class=\"photo\"><a href=\"(/[^<>\"]*?)\"").getMatch(0);
            if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            DLLINK = "http://www.eroprofile.com" + DLLINK;
            String ext = DLLINK.substring(DLLINK.lastIndexOf("."));
            if (ext == null || ext.length() > 5) ext = ".jpg";
            downloadLink.setFinalFileName(filename + ext);
        }
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
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
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getFilename() throws PluginException {
        String filename = br.getRegex("<tr><th>Title:</th><td>([^<>\"]*?)</td></tr>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>EroProfile \\- ([^<>\"]*?)</title>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        return Encoding.htmlDecode(filename.trim());
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
