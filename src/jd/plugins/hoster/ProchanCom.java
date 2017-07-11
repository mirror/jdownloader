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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "prochan.com" }, urls = { "https?://(?:\\w+\\.)?prochan\\.com/(view\\?p=|embed\\?f=|v/)[A-Za-z0-9\\-_]+" })
public class ProchanCom extends PluginForHost {

    public ProchanCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Porn_plugin
    // Tags:
    // protocol: no https
    // other:

    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;

    private String               dllink            = null;

    @Override
    public String getAGBLink() {
        return "http://www.prochan.com/tos";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        dllink = null;
        final String fid = getFID(downloadLink);
        downloadLink.setLinkID(fid);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        /* Using embed, we can view mature content without having to log in. */
        br.getPage(Request.getLocation("//www.prochan.com/view?t=" + fid, br.createGetRequest(downloadLink.getPluginPatternMatcher())));
        if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getLongContentLength() == 1 || "thing not found".equals(br.toString()) || br.containsHTML("File not found or deleted")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = fid;
        dllink = br.getRegex("('|\"|\\s*)(?:\\W*file|\\W*url)\\1:(\"|'|\\s*)(http[^\"'<>]*?)\\2").getMatch(2);
        if (dllink == null) {
            dllink = br.getRegex("<source src=('|\"|\\s*)(https?://[^\"'<>]*?)\\1[^>]*type=('|\"|\\s*)video/(?:mp4|flv)\\3").getMatch(1);
            if (dllink == null) {
                dllink = br.getRegex("property=\"og:video\" content=\"(http[^\"'<>]*?)\"").getMatch(0);
            }
        }
        if (filename == null || dllink == null) {
            // they have blank and offline content and don't say so.. do not throw defect.. let users report broken plugin manually.
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dllink = Encoding.htmlDecode(dllink);
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        String ext = getFileNameExtensionFromString(dllink, ".flv");
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        downloadLink.setFinalFileName(filename);
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            try {
                con = br2.openHeadConnection(dllink);
            } catch (final BrowserException e) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            downloadLink.setProperty("directlink", dllink);
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, free_resume, free_maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e) {
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @SuppressWarnings("deprecation")
    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "([A-Za-z0-9\\-_]+)$").getMatch(0);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
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
