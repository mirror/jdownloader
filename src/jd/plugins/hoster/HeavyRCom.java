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

import org.jdownloader.plugins.components.antiDDoSForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "heavy-r.com" }, urls = { "https?://(?:www\\.)?heavy\\-r\\.com/video/\\d+(?:/[^/]*/?)?" })
public class HeavyRCom extends antiDDoSForHost {

    public HeavyRCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String NICE_HOST         = "reavy-r.com";
    private static final String NICE_HOSTproperty = NICE_HOST.replaceAll("(\\.|-)", "");
    private static final String NORESUME          = NICE_HOSTproperty + "NORESUME";

    private String              dllink            = null;

    @Override
    public String getAGBLink() {
        return "http://www.heavy-r.com/index.php?page=terms";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String url = downloadLink.getDownloadURL();
        if (url.matches(".+\\d+$")) {
            url = url + "/x/";
        }
        getPage(url);
        if (br.containsHTML("404 \\- Not Found")) {
            getPage(url);
        }
        if (br.containsHTML("Video not found\\!") || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("id=\"videotitle\">[\t\n\r ]+<h1>([^<>\"]*?)</h1>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        }
        dllink = br.getRegex("file: \'(https?://[^<>\"]*?)\'").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("source\\s*type=\"video/.*?\"\\s*src=\"(https?://[^<>\"]*?)\"").getMatch(0);
        }
        if (filename == null || dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlDecode(dllink);
        filename = filename.trim();
        String ext = getFileNameExtensionFromString(dllink);
        if (ext == null || ext.length() > 5) {
            ext = ".mp4";
        }
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ext);
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            br2.getHeaders().put("Range", "bytes=" + 0 + "-");
            con = openConnection(br2, dllink);
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

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        boolean resume = true;
        int maxchunks = 0;
        if (downloadLink.getBooleanProperty(HeavyRCom.NORESUME, false)) {
            resume = false;
        }
        if (!resume) {
            maxchunks = 1;
        }
        downloadLink.setProperty("ServerComaptibleForByteRangeRequest", true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resume, maxchunks);
        if (dl.getConnection().getResponseCode() == 416) {
            logger.info("Resume impossible, disabling it for the next try");
            downloadLink.setChunksProgress(null);
            downloadLink.setProperty(HeavyRCom.NORESUME, Boolean.valueOf(true));
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private URLConnectionAdapter openConnection(final Browser br, final String directlink) throws IOException {
        URLConnectionAdapter con;
        if (isJDStable()) {
            con = br.openGetConnection(directlink);
        } else {
            con = br.openHeadConnection(directlink);
        }
        return con;
    }

    private boolean isJDStable() {
        return System.getProperty("jd.revision.jdownloaderrevision") == null;
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
