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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "3plus.tv" }, urls = { "https?://(?:www\\.)?3plus.tv/episode/[a-z0-9\\-]+/[a-z0-9\\-]+" })
public class ThreeplusTv extends PluginForHost {

    public ThreeplusTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:

    /* Extension which will be used if no correct extension is found */
    private static final String  default_extension = ".mp4";
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;

    private String               dllink            = null;
    private boolean              server_issues     = false;

    @Override
    public String getAGBLink() {
        return "http://www.3plus.tv/impressum";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        String sdnPlayoutId = getsdnPlayoutId();
        final String vastid = this.br.getRegex("vastid=(\\d+)").getMatch(0);
        if (br.getHttpConnection().getResponseCode() == 404 || sdnPlayoutId == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String url_filename = new Regex(link.getDownloadURL(), "/episode/(.+)").getMatch(0).replace("/", "_");
        String filename = null;
        if (filename == null) {
            filename = url_filename;
        }
        String player_get_parameters = "?timestamp=0&key=0&js=true&autoplay=true&container=sdnPlayer_player&width=100%25&height=100%25&protocol=http&token=0&jscallback=sdnPlaylistBridge";
        if (vastid != null) {
            player_get_parameters += "&vastid=" + vastid;
        }
        this.br.getPage("http://playout.3qsdn.com/" + sdnPlayoutId + player_get_parameters);
        sdnPlayoutId = getsdnPlayoutId();
        if (sdnPlayoutId != null) {
            /* First ID goes to second ID --> Access that */
            this.br.getPage("/" + sdnPlayoutId + player_get_parameters);
        }
        final String[] qualities = { "hd1080p", "hd720p", "mediumlarge", "medium", "small" };
        for (final String possibleQuality : qualities) {
            dllink = this.br.getRegex("src\\s*?:\\s*?\\'(http[^<>\"\\']+format=progressive[^<>\"\\']*?)\\',\\s*?type\\s*?:\\s*?\\'video/mp4\\',\\s*?quality\\s*?:\\s*?\\'" + possibleQuality + "\\'").getMatch(0);
            if (dllink != null) {
                break;
            }
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        final String ext;
        if (dllink != null) {
            ext = getFileNameExtensionFromString(dllink, default_extension);
        } else {
            ext = default_extension;
        }
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        if (dllink != null) {
            dllink = Encoding.htmlDecode(dllink);
            link.setFinalFileName(filename);
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openHeadConnection(dllink);
                if (!con.getContentType().contains("html")) {
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
        } else {
            /* We cannot be sure whether we have the correct extension or not! */
            link.setName(filename);
        }
        return AvailableStatus.TRUE;
    }

    private String getsdnPlayoutId() {
        return this.br.getRegex("sdnPlayoutId\\s*?(?:=|:)\\s*?(?:\"|\\')([^\"\\']+)(?:\"|\\')").getMatch(0);
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (this.br.containsHTML("Dieses Video steht nur für Silverlight und Flash")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Cannot download Silverlight/DRM protected content");
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
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
