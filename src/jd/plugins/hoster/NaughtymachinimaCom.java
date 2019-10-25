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
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "naughtymachinima.com" }, urls = { "https?://(?:www\\.)?naughtymachinima\\.com/video/\\d+(?:/[a-z0-9\\-_]+)?" })
public class NaughtymachinimaCom extends PluginForHost {
    public NaughtymachinimaCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:
    /* Extension which will be used if no correct extension is found */
    private static final String  default_Extension = ".mp4";
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;
    private boolean              server_issues     = false;
    private boolean              privatecontent    = false;

    @Override
    public String getAGBLink() {
        return "http://www.naughtymachinima.com/static/terms";
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replace("http://", "https://"));
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        correctDownloadLink(link);
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || this.br.getURL().contains("/error/") || this.br.containsHTML("This video cannot be found\\.")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* 2016-09-01: This check is not needed yet! */
        // privatecontent = this.br.containsHTML(">This is a private video");
        privatecontent = false;
        final String fid = new Regex(link.getDownloadURL(), "/video/(\\d+)/").getMatch(0);
        final String url_name = new Regex(link.getDownloadURL(), "([a-z0-9\\-_]+)$").getMatch(0);
        String filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]+)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>([^<>\"]+) \\- Naughty Machinima</title>").getMatch(0);
        }
        if (filename == null) {
            filename = url_name;
        }
        final String videos[] = br.getRegex("src\\s*=\\s*\"([^\"]+/media/videos/[^\"]+" + fid + "[^\"]*\\.mp4)").getColumn(0);
        if (videos != null) {
            int size = -1;
            for (final String video : videos) {
                String resolution = new Regex(video, "_(\\d+)p\\.mp4").getMatch(0);
                if (resolution == null && StringUtils.containsIgnoreCase(video, "/hd/")) {
                    resolution = "720";
                }
                if (resolution == null && StringUtils.containsIgnoreCase(video, "/iphone/")) {
                    resolution = "360";
                }
                if (size == -1 || resolution == null || Integer.parseInt(resolution) > size) {
                    size = resolution != null ? Integer.parseInt(resolution) : -1;
                    dllink = video;
                }
            }
        }
        if (filename == null || dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        final String ext = getFileNameExtensionFromString(dllink, default_Extension);
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        if (dllink != null) {
            link.setFinalFileName(filename);
            if (!(Thread.currentThread() instanceof SingleDownloadController)) {
                final Browser br2 = br.cloneBrowser();
                // In case the link redirects to the finallink
                br2.setFollowRedirects(true);
                final URLConnectionAdapter con = br2.openHeadConnection(dllink);
                try {
                    if (con.getResponseCode() == 200 && !con.getContentType().contains("text")) {
                        link.setDownloadSize(con.getLongContentLength());
                        link.setProperty("directlink", dllink);
                    } else {
                        server_issues = true;
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
        } else {
            /* We cannot be sure whether we have the correct extension or not! */
            link.setName(filename);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (privatecontent) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Private video");
        } else if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
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
