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

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "flyflv.com" }, urls = { "https?://(?:www\\.)?flyflv\\.com/movies/(\\d+(?:/[A-Za-z0-9\\-_]+)?|player/\\d+)" })
public class FlyflvCom extends PluginForHost {
    public FlyflvCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
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

    @Override
    public String getAGBLink() {
        return "http://www.flyflv.com/site/page/terms";
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
        return new Regex(link.getPluginPatternMatcher(), "(?i)/movies/(?:player/)?(\\d+)").getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        final String extDefault = ".mp4";
        final String fid = getFID(link);
        String fallbackTitle = new Regex(link.getPluginPatternMatcher(), "(?i)movies/\\d+/(.+)").getMatch(0);
        if (fallbackTitle != null) {
            fallbackTitle = fallbackTitle.replace("-", " ").trim();
        } else {
            fallbackTitle = fid;
        }
        if (!link.isNameSet()) {
            link.setName(fallbackTitle + extDefault);
        }
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage("https://www." + this.getHost() + "/movies/" + fid);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* Check if crawler set filename as this website does not provide good filenames! */
        String title = link.getFinalFileName();
        if (title == null) {
            title = br.getRegex("<h1 itemprop=\"name\">([^<>\"]+)</h1>").getMatch(0);
        }
        if (title == null) {
            title = fallbackTitle;
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
        if (title == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        title = Encoding.htmlDecode(title).trim();
        final String ext = getFileNameExtensionFromString(dllink, extDefault);
        link.setFinalFileName(this.applyFilenameExtension(title, ext));
        if (!StringUtils.isEmpty(this.dllink) && !isDownload) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                handleConnectionErrors(br, con);
                link.setVerifiedFileSize(con.getCompleteContentLength());
                link.setFinalFileName(this.correctOrApplyFileNameExtension(title, con));
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    private void handleConnectionErrors(final Browser br, final URLConnectionAdapter con) throws Exception {
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            if (con.getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (StringUtils.isEmpty(this.dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        handleConnectionErrors(br, dl.getConnection());
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
