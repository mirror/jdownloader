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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "vine.co" }, urls = { "https?://(www\\.)?vine\\.co/v/[A-Za-z0-9]+" }, flags = { 0 })
public class VineCo extends PluginForHost {

    public VineCo(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String DLLINK = null;

    @Override
    public String getAGBLink() {
        return "https://vine.co/terms";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        this.br.setAllowedResponseCodes(410);
        br.getPage(downloadLink.getDownloadURL().replace("http://", "https://"));
        final int responsecode = this.br.getHttpConnection().getResponseCode();
        if (br.containsHTML(">Page not found") || responsecode == 404 || responsecode == 410) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String fid = downloadLink.getDownloadURL().substring(downloadLink.getDownloadURL().lastIndexOf("/") + 1);
        downloadLink.setLinkID(fid);
        String filename = br.getRegex("property=\"?og:title\"? content=\"?(.*?)\"?><meta").getMatch(0);
        DLLINK = br.getRegex("property=\"twitter:player:stream\" content=\"(https?://[^<>\"]*?)\"").getMatch(0);
        if (DLLINK == null) {
            DLLINK = br.getRegex("contentUrl\" : \"(https?://[^<>\"]*?)\"").getMatch(0);
        }
        if (filename == null || DLLINK == null) {
            logger.info("filename: " + filename + ", DLLINK: " + DLLINK);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        DLLINK = Encoding.htmlDecode(DLLINK);
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        /* Include linkid in filename to avoid false positive duplicate! */
        filename = fid + "_" + filename + ".mp4";
        downloadLink.setFinalFileName(filename);
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openHeadConnection(DLLINK);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        // We only download small files, chunkload makes no sense
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 1);
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
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
