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

import org.appwork.utils.StringUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "flyflv.com" }, urls = { "https?://(?:www\\.)?flyflv\\.com/movies/(\\d+(?:/[A-Za-z0-9\\-_]+)?|player/\\d+)" })
public class FlyflvCom extends PluginForHost {
    public FlyflvCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;
    private boolean              server_issues     = false;

    @Override
    public String getAGBLink() {
        return "http://www.flyflv.com/site/page/terms";
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        final String linkpart = new Regex(link.getPluginPatternMatcher(), "/(\\d+(/.+)?)$").getMatch(0);
        link.setPluginPatternMatcher("https://www.flyflv.com/movies/" + linkpart);
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
        return new Regex(link.getPluginPatternMatcher(), "/movies/(?:player/)?(\\d+)").getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        return requestFileInformation(link, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws IOException, PluginException {
        final String fid = getFID(link);
        String fallbackTitle = new Regex(link.getPluginPatternMatcher(), "movies/\\d+/(.+)").getMatch(0);
        if (fallbackTitle == null) {
            fallbackTitle = fid;
        }
        if (!link.isNameSet()) {
            link.setName(fallbackTitle + ".mp4");
        }
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* Check if crawler set filename as this website does not provide good filenames! */
        String filename = link.getFinalFileName();
        if (filename == null) {
            filename = br.getRegex("<h1 itemprop=\"name\">([^<>\"]+)</h1>").getMatch(0);
        }
        if (filename == null) {
            filename = fallbackTitle;
        }
        dllink = br.getRegex("\\'(?:file|video)\\'[\t\n\r ]*?:[\t\n\r ]*?\\'(http[^<>\"]*?)\\'").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("<source src=(?:'|\")([^<>'\"]*?)(?:'|\")").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("property=\"og:video\" content=\"(http[^<>\"]*?)\"").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("var fileUrl[\t\n\r ]*?=[\t\n\r ]*?\"(http[^<>\"\\']+)\"").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename).trim();
        filename = encodeUnicode(filename);
        final String ext = getFileNameExtensionFromString(dllink, ".mp4");
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        link.setFinalFileName(filename);
        if (!StringUtils.isEmpty(this.dllink) && !isDownload) {
            URLConnectionAdapter con = null;
            try {
                /* Do NOT use Head-Request for this website!! */
                con = br.openGetConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 1000) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
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
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (StringUtils.isEmpty(this.dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Important else we might run into error 403. */
        this.sleep(1000, link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
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
