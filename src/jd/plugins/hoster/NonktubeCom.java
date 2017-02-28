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
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.http.requests.HeadRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "nonktube.com" }, urls = { "https?://(www\\.)?nonktube\\.com/(?:porn/)?video/\\d+/[a-z0-9\\-]+" })
public class NonktubeCom extends PluginForHost {

    public NonktubeCom(PluginWrapper wrapper) {
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
        return "http://www.nonktube.com/static/terms";
    }

    public void correctDownloadLink(DownloadLink link) {
        // 20170228 Referrer should be https
        link.setUrlDownload(link.getDownloadURL().replace("http:", "https:"));
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        final String fid = new Regex(downloadLink.getDownloadURL(), "/video/(\\d+)/").getMatch(0);
        dllink = null;
        this.setBrowserExclusive();
        downloadLink.setLinkID(fid);
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        String redirect = this.br.getRedirectLocation();
        if (redirect != null && ((!redirect.contains("nonktube.com/") || !redirect.contains("/video/")) && redirect.contains("out.php"))) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.br.setFollowRedirects(true);
        if (redirect != null) {
            if (redirect.contains("u=")) {
                redirect = new Regex(redirect, ".*?u=(http.+)").getMatch(0);
            } else {
                this.br.getPage(redirect);
            }
        }
        String filename;
        if (true) {
            /* Faster way */
            br.getPage("http://www.nonktube.com/media/nuevo/config.php?key=" + fid + "--");
            if (br.containsHTML("Invalid video|<title>NONK Tube<") || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("<title><\\!\\[CDATA\\[([^<>\"]*?)\\]\\]></title>").getMatch(0);
        } else {
            if (br.containsHTML("data-dismiss=\"alert\"") || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("<title>([^<>\"]*?) \\- NonkTube\\.com</title>").getMatch(0);
            br.getPage("http://www.nonktube.com/media/nuevo/config.php?key=" + fid + "--");
        }
        if (filename == null) {
            filename = new Regex(downloadLink.getDownloadURL(), "nonktube\\.com/video/\\d+/([a-z0-9\\-]+)").getMatch(0);
        }
        dllink = br.getRegex("<file>(https?://[^<>\"]*?)</file>").getMatch(0);
        if (filename == null || dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlDecode(dllink);
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        final String ext = getFileNameExtensionFromString(dllink, ".mp4");
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
                HeadRequest headRequest = new HeadRequest(dllink);
                headRequest.getHeaders().put("Range", "bytes=0-");
                con = br2.openRequestConnection(headRequest);
            } catch (final BrowserException e) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (!con.getContentType().contains("html") && con.isOK()) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            downloadLink.setProperty("directlink", dllink);
            return AvailableStatus.TRUE;
        } finally {
            try {
                if (con != null) {
                    con.disconnect();
                }
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        downloadLink.setProperty("ServerComaptibleForByteRangeRequest", true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, free_resume, free_maxchunks);
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

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    private boolean isJDStable() {
        return System.getProperty("jd.revision.jdownloaderrevision") == null;
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
