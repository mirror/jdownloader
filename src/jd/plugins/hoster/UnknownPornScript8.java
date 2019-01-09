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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pornziz.com", "vidxporn.com", "xnhub.com" }, urls = { "https?://(?:www\\.)?pornziz\\.com/video/[a-z0-9\\-]+\\-\\d+\\.html", "https?://(?:www\\.)?vidxporn\\.com/video/[a-z0-9\\-]+\\-\\d+\\.html", "https?://(?:www\\.)?xnhub\\.com/(?:video/[a-z0-9\\-]+\\-\\d+\\.html|embed/\\d+)" })
public class UnknownPornScript8 extends PluginForHost {
    public UnknownPornScript8(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    /* Porn_plugin */
    /* V0.2 */
    /* Tags: Script, template */
    private String  dllink      = null;
    private boolean serverissue = false;

    @Override
    public String getAGBLink() {
        return "http://www.pornziz.com/static/terms/";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        serverissue = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String url_embed = br.getRegex("<iframe[^>]*?src=\"(https?://(?:www\\.)?xnhub\\.com/embed/\\d+)\"[^>]*?></iframe>").getMatch(0);
        if (url_embed != null) {
            /* Typically pornziz.com or vidxporn.com --> xnhub.com */
            br.getPage(url_embed);
            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("Invalid Video ID")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        final String filename_url = getUrlFilename(downloadLink);
        String filename = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]+)\">").getMatch(0);
        if (StringUtils.isEmpty(filename)) {
            filename = filename_url;
        }
        dllink = br.getRegex("<source src=\"(https?[^<>\"]+)\"").getMatch(0);
        if (dllink == null) {
            String iframe = br.getRegex("<iframe[^<>]+src=\"([^<>\"]+)\"[^<>]+allowfullscreen").getMatch(0);
            if (iframe != null) {
                br.getPage(iframe);
                dllink = br.getRegex("<source src=\"(https?[^<>\"]+)\"").getMatch(0);
            }
        }
        if (filename == null || dllink == null) {
            logger.info("filename: " + filename + ", dllink: " + dllink);
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
        URLConnectionAdapter con = null;
        try {
            con = br.openHeadConnection(dllink);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                serverissue = true;
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    private String getUrlFilename(final DownloadLink dl) {
        final String result;
        if (new Regex(dl.getPluginPatternMatcher(), ".+/video/.+\\.html$").matches()) {
            result = new Regex(dl.getPluginPatternMatcher(), "/([^/]+)\\.html$").getMatch(0);
        } else {
            /* Embed urls */
            result = new Regex(dl.getPluginPatternMatcher(), "(\\d+)$").getMatch(0);
        }
        return result;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (serverissue) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.UnknownPornScript8;
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
