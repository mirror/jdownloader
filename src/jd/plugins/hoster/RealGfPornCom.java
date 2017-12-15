//    jDownloader - Downloadmanager
//    Copyright (C) 2010  JD-Team support@jdownloader.org
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
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "realgfporn.com" }, urls = { "https?://(?:www\\.)?realgfporn\\.com/videos/[a-z0-9\\-_]+\\d+\\.html" })
public class RealGfPornCom extends PluginForHost {
    public RealGfPornCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    /* Porn_plugin */
    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "http://www.realgfporn.com/DMCA.html";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        if (downloadLink.getBooleanProperty("offline", false)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getURL().equals("http://www.realgfporn.com/") || br.containsHTML("<title>Free Amateur and Homemade Porn Videos  \\– Real Girlfriend Porn</title>") || br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("class=\"deleted\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<h3 class=\"video_title\">(.*?)</h3>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>(.*?)( - Real Girlfriend Porn)?</title>").getMatch(0);
        }
        dllink = br.getRegex("\\(\\'file\\',\\'(http://.*?)\\'\\)").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\\'(http://media\\d+\\.realgfporn\\.com/videos/.*?)\\'").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\\&file=(http://(www\\.)realgfporn\\.com/videos/.*?)\\&height=").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("<param name=\"filename\" value=\"(http://.*?)\"").getMatch(0);
                    if (dllink == null) {
                        dllink = br.getRegex("file\\s*:\\s*(\"|'|)(https?://.*?)\\1").getMatch(1);
                    }
                }
            }
        }
        if (filename == null || dllink == null) {
            logger.info("filename: " + filename + ", dllink: " + dllink);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlDecode(dllink);
        filename = filename.trim();
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename.trim()) + getFileNameExtensionFromString(dllink, ".mp4"));
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(dllink);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
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
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}