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
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mp3red.cc" }, urls = { "https?://(?:[a-z0-9]+\\.)?mp3red\\.(?:su|co|me|cc)/\\d+/[a-z0-9\\-]+\\.html" })
public class Mp3redCo extends PluginForHost {
    public Mp3redCo(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:
    /* Extension which will be used if no correct extension is found */
    private static final String  default_extension = ".mp3";
    /* Connection stuff */
    /* 2017-01-26: Buggy servers - disabled resume and chunks to prevent issues. */
    private static final boolean free_resume       = false;
    private static final int     free_maxchunks    = 1;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;
    private boolean              server_issues     = false;

    @SuppressWarnings("deprecation")
    @Override
    public void correctDownloadLink(final DownloadLink link) {
        final String linkpart = new Regex(link.getDownloadURL(), "https?://[^/]+/(.+)").getMatch(0);
        link.setUrlDownload("http://" + this.getHost() + "/" + linkpart);
    }

    @Override
    public String rewriteHost(String host) {
        if (host == null || "mp3red.su".equals(host) || "mp3red.co".equals(host)) {
            return "mp3red.cc";
        }
        return super.rewriteHost(host);
    }

    @Override
    public String getAGBLink() {
        return "http://mp3red.cc/";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String fid = new Regex(link.getDownloadURL(), "https?://[^/]+/(\\d+)/").getMatch(0);
        final String url_filename = new Regex(link.getDownloadURL(), "([^/]+)\\.html$").getMatch(0);
        boolean nice_filename = true;
        String filename = br.getRegex("mp3url_track_data_model\\s*?,\\s*?\"([^<>\"]+)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("id=\"content\">\\s*?<h1>([^<>\"]+)</h1>").getMatch(0);
        }
        if (filename == null) {
            nice_filename = false;
            filename = url_filename;
        }
        filename = fid + "_" + filename;
        dllink = br.getRegex("(/findfile/[^<>\"\\']+)").getMatch(0);
        if (dllink == null) {
            /* 2017-02-03: New */
            dllink = br.getRegex("(/stream/[^<>\"\\']+)").getMatch(0);
        }
        if (dllink == null) {
            /* 2017-02-03: New */
            dllink = br.getRegex("var\\s*?mp3url_track_data_model\\s*?=\\s*?\"(/[^<>\"\\']+)\";").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        final String ext = default_extension;
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        /*
         * Server filenames aren't that nice either (2017-02-26: server filename ==url_filename) so if we have a better filename, set it as
         * filename - if not, server filename will be used later.
         */
        if (nice_filename) {
            link.setFinalFileName(filename);
        } else {
            link.setName(filename);
        }
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
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
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
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
