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

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;

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
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "free-sex-video.net", "hotclips24.com", "pornclipsxxx.com", "al4a.com" }, urls = { "https?://(?:www\\.)?free-sex-video\\.net/video/[a-z0-9\\-]+\\d+\\.html", "https?://(?:www\\.)?hotclips24\\.com/video/[a-z0-9\\-]+\\d+\\.html", "https?://(?:www\\.)?pornclipsxxx\\.com/video/[a-z0-9\\-]+\\d+\\.html", "https?://(?:www\\.)?al4a\\.com/video/[a-z0-9\\-]+\\d+\\.html" })
public class FluidPlayer extends PluginForHost {
    public FluidPlayer(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags: fluidplayer.com
    // protocol: no https
    // other:
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 1;    // https://svn.jdownloader.org/issues/83286
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;
    private boolean              server_issues     = false;

    @Override
    public String getAGBLink() {
        return "http://hotclips24.com/tos";
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        // link.setPluginPatternMatcher(link.getPluginPatternMatcher().replace("free-sex-video.net/", "hotclips24.com/"));
        // Not the same: (https://svn.jdownloader.org/issues/85655)
        // https://free-sex-video.net/video/-51455.html
        // https://hotclips24.com/video/-51455.html
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = null;
        correctDownloadLink(link);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String url_filename = new Regex(link.getDownloadURL(), "/video/([a-z0-9\\-]*\\-\\d+)\\.html").getMatch(0).replace("-", " ");
        String filename = br.getRegex("title: '([^<>\"']*?)',").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>([^<>]*?)(at (Free Sex Video|HotClips24))?</title>").getMatch(0);
        }
        if (filename == null) {
            filename = url_filename;
        }
        dllink = br.getRegex("'(?:file|video)'\\s*:\\s*'(http[^<>\"]*?)'").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\\s+(?:file|url):\\s*(\"|')(http[^<>\"]*?)\\1").getMatch(1);
            if (dllink == null) {
                dllink = br.getRegex("<source src=\"(https?://[^<>\"]*?)\" type=(\"|')video/(?:mp4|flv)\\2").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("property=\"og:video\" content=\"(http[^<>\"]*?)\"").getMatch(0);
                    if (dllink == null) {
                        dllink = br.getRegex("var defFile = '(http[^<>\"']*?)';").getMatch(0);
                        if (dllink == null) {
                            /* Mobile format */
                            dllink = br.getRegex("var mobFile = '(http[^<>\"']*?)';").getMatch(0);
                            if (dllink == null) {
                                dllink = br.getRegex("(https?://media\\d+\\.free\\-sex\\-video\\.net/media/videos/[^<>\"]*?\\.mp4)").getMatch(0);
                            }
                        }
                    }
                }
            }
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        final String ext = ".mp4";
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        link.setFinalFileName(filename);
        if (!StringUtils.isEmpty(dllink)) {
            dllink = Encoding.htmlDecode(dllink);
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openHeadConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    link.setDownloadSize(con.getLongContentLength());
                } else {
                    server_issues = true;
                }
                /* 2019-08-21: Not required */
                // link.setProperty("directlink", dllink);
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
        } else if (StringUtils.isEmpty(dllink)) {
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
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.FluidPlayer;
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
