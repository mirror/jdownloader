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

import org.appwork.utils.StringUtils;

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
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "throatfucktube.com" }, urls = { "https?://(?:www\\.)?throatfucktube\\.com/video/[A-Za-z0-9\\-]+\\-\\d+\\.html" })
public class ThroatfucktubeCom extends PluginForHost {
    public ThroatfucktubeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    /* Porn_plugin */
    // other:
    /* Extension which will be used if no correct extension is found */
    private static final String  type_1            = "^https?://(?:www\\.)?[^/]+/(?:videos?|free)/[A-Za-z0-9\\-]+\\-\\d+\\.html$";
    /* E.g. chopsticktube.com */
    private static final String  type_2            = "^https?://(?:www\\.)?[^/]+/video/\\d+/[A-Za-z0-9\\-]+\\-\\d+\\.html$";
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 1;
    private static final int     free_maxdownloads = 1;
    private String               dllink            = null;
    private boolean              server_issues     = false;

    @Override
    public String getAGBLink() {
        return "https://throatfucktube.com/static/terms";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String url_filename = null;
        if (link.getDownloadURL().matches(type_1)) {
            url_filename = new Regex(link.getDownloadURL(), "/(?:videos?|free)/([a-z0-9\\-]+)\\-\\d+\\.html").getMatch(0).replace("-", " ");
        } else {
            url_filename = new Regex(link.getDownloadURL(), "/video/\\d+/([a-z0-9\\-]+)\\.html").getMatch(0).replace("-", " ");
        }
        String filename = this.br.getRegex("<h2 class=\"page\\-title\">([^<>\"]*?)</h2>").getMatch(0);
        if (filename == null) {
            filename = url_filename;
        }
        final String html_clip = this.br.getRegex("urls\\.push\\(\\{(.*?)\\}\\);").getMatch(0);
        if (html_clip != null) {
            /* TODO: Check if some sites can have multiple formats/qualities --> Always find the highest bitrate! */
            dllink = new Regex(html_clip, "file:\"(http[^<>\"]*?)\"").getMatch(0);
        } else {
            dllink = br.getRegex("file:\"(http[^<>\"]*?)\"").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        final String ext = dllink != null ? (getFileNameExtensionFromString(dllink, ".mp4")) : ".mp4";
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        link.setFinalFileName(filename);
        final boolean checkForFilesize = false;
        if (checkForFilesize && dllink != null) {
            dllink = Encoding.htmlDecode(dllink);
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    link.setDownloadSize(con.getCompleteContentLength());
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
        } else if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
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
        return SiteTemplate.UnknownPornScript2;
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
