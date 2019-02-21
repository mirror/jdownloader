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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "sextvx.com" }, urls = { "http://(?:www\\.)?sextvx\\.com/[a-z]{2}/video/\\d+/[a-z0-9\\-]+" })
public class SextvxCom extends PluginForHost {
    public SextvxCom(PluginWrapper wrapper) {
        super(wrapper);
    }
    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:

    /* Connection stuff - too many connections = server returns 404 on download attempt! */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 1;
    private static final int     free_maxdownloads = 5;
    private String               dllink            = null;

    @Override
    public String getAGBLink() {
        return "http://sextvx.com/en/terms";
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        final Regex urlregex = new Regex(link.getDownloadURL(), "sextvx\\.com/[a-z]{2}/video/(\\d+)/(.+)");
        final String fid = urlregex.getMatch(0);
        link.setUrlDownload("http://sextvx.com/en/video/" + fid + "/" + urlregex.getMatch(1));
        link.setLinkID(fid);
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("This video is no longer available")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<h1>([^<>]*?)</h1>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("class=\"block\\-title\">[\t\n\r ]+<h\\d+>([^<>]*?)<").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("itemprop=\"name\">([^<>\"]*?)<").getMatch(0);
        }
        if (filename == null) {
            filename = new Regex(downloadLink.getDownloadURL(), "([a-z0-9\\-]+)$").getMatch(0);
        }
        // if (filename == null) {
        // filename = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        // }
        final Regex ajaxdataregex = br.getRegex("path=\"(?:\\d+,)?(\\d+)\\.([^<>\"]*?)\"");
        String server = ajaxdataregex.getMatch(0);
        String path = ajaxdataregex.getMatch(1);
        if (server == null) {
            server = this.br.getRegex("path=\"(\\d+)[0-9,\\.]*?(\\d+/[^<>\"]*?)\"").getMatch(0);
        }
        if (path == null) {
            path = this.br.getRegex("path=\"[0-9,\\.]+(\\d+/[^<>\"]*?)\"").getMatch(0);
        }
        String flux = this.br.getRegex("(/flux[^\"]+)").getMatch(0);
        if ((flux == null || flux.contains("'")) && server != null && path != null) {
            path = path.replace(".", ",").replace("/", ",");
            flux = "/flux?d=web.flv&s=" + server + "&p=" + path;
        }
        String source = br.getRegex("<source[^<>']+src='([^']+)'").getMatch(0);
        if (filename == null || source == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setFollowRedirects(false);
        br.getPage(source);
        /* 2017-01-05: 2 different types. */
        final String redirect = br.getRedirectLocation();
        if (redirect != null) {
            dllink = redirect;
        } else {
            dllink = br.toString();
        }
        br.setFollowRedirects(true);
        if (dllink == null || !dllink.startsWith("http") || dllink.length() > 500) {
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
        br2.getHeaders().put("Referer", "http://sextvx.com/static/player/player.swf");
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            try {
                con = br.openHeadConnection(dllink);
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
