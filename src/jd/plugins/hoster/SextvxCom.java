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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "sextvx.com" }, urls = { "https?://(?:www\\.)?sextvx\\.com/(?:(?:[a-z]{2}/)?video/\\d+/[a-z0-9\\-]+|embed/\\d+)" })
public class SextvxCom extends PluginForHost {
    public SextvxCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:
    /* Connection stuff - too many connections = server returns 404 on download attempt! */
    private static final boolean free_resume    = true;
    private static final int     free_maxchunks = 1;
    private String               dllink         = null;
    private static final String  TYPE_NORMAL    = "(?i)https?://[^/]+/(?:[a-z]{2}/)?video/(\\d+)/([a-z0-9\\-]+)";
    private static final String  TYPE_EMBED     = "(?i)https?://[^/]+/embed/(\\d+)";

    @Override
    public String getAGBLink() {
        return "http://sextvx.com/en/terms";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        if (link.getPluginPatternMatcher() == null) {
            return null;
        } else if (link.getPluginPatternMatcher().matches(TYPE_EMBED)) {
            return new Regex(link.getPluginPatternMatcher(), TYPE_EMBED).getMatch(0);
        } else {
            return new Regex(link.getPluginPatternMatcher(), TYPE_NORMAL).getMatch(0);
        }
    }

    private String getWeakFilename(final DownloadLink link) {
        if (link.getPluginPatternMatcher() == null) {
            return null;
        } else if (link.getPluginPatternMatcher().matches(TYPE_EMBED)) {
            return new Regex(link.getPluginPatternMatcher(), TYPE_EMBED).getMatch(0) + ".mp4";
        } else {
            return new Regex(link.getPluginPatternMatcher(), TYPE_NORMAL).getMatch(1).replace("-", " ") + ".mp4";
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        dllink = null;
        if (!link.isNameSet()) {
            link.setName(getWeakFilename(link));
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (link.getPluginPatternMatcher().matches(TYPE_EMBED)) {
            /* This way we're able to find filenames even for embed URLs. */
            br.getPage("https://www." + this.getHost() + "/en/video/" + this.getFID(link));
        } else {
            br.getPage(link.getPluginPatternMatcher());
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)This video is no longer available")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("class=\"not-available\"")) {
            /* 2021-12-08 */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<h1>([^<>]*?)</h1>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("class=\"block\\-title\">[\t\n\r ]+<h\\d+>([^<>]*?)<").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("itemprop=\"name\">([^<>\"]*?)<").getMatch(0);
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
        if (filename != null) {
            filename = Encoding.htmlDecode(filename);
            filename = filename.trim();
            final String ext = ".mp4";
            if (!filename.endsWith(ext)) {
                filename += ext;
            }
            link.setFinalFileName(filename);
        }
        /* First hit = best quality */
        String source = br.getRegex("<source[^<>']+src=(?:\"|')([^'\"]+)[^>]*video/mp4").getMatch(0);
        if (source != null) {
            br.setFollowRedirects(false);
            source = Encoding.htmlOnlyDecode(source);
            br.getPage(source);
            /* 2017-01-05: 2 different types. */
            final String redirect = br.getRedirectLocation();
            if (redirect != null) {
                dllink = redirect;
            } else {
                dllink = br.getRequest().getHtmlCode();
            }
            br.setFollowRedirects(true);
            if (dllink == null || !dllink.startsWith("http") || dllink.length() > 500) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = Encoding.htmlOnlyDecode(dllink);
            final Browser br2 = br.cloneBrowser();
            br2.getHeaders().put("Referer", "http://" + getHost() + "/static/player/player.swf");
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                handleConnectionErrors(br, con);
                if (con.getCompleteContentLength() > 0) {
                    if (con.isContentDecoded()) {
                        link.setDownloadSize(con.getCompleteContentLength());
                    } else {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
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
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 5;
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
