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
import jd.plugins.components.SiteType.SiteTemplate;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pornziz.com", "xnhub.com" }, urls = { "https?://(?:www\\.)?pornziz\\.com/video/[a-z0-9\\-]+\\-\\d+\\.html", "https?://(?:www\\.)?xnhub\\.com/(?:video/[a-z0-9\\-]+\\-\\d+\\.html|embed/\\d+)" })
public class UnknownPornScript8 extends PluginForHost {
    public UnknownPornScript8(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    /* DEV NOTES */
    /* Porn_plugin */
    /* V0.2 */
    /* Tags: Script, template */
    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "https://www.pornziz.com/static/terms/";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        String fid = new Regex(link.getPluginPatternMatcher(), "https?://[^/]+/embed/(\\d+)").getMatch(0);
        if (fid == null) {
            fid = new Regex(link.getPluginPatternMatcher(), "(\\d+)\\.html$").getMatch(0);
        }
        return fid;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.dllink = null;
        final String extDefault = ".mp4";
        final String urlSlug = new Regex(link.getPluginPatternMatcher(), "(?i)/video/([^/]+)-\\d+\\.html$").getMatch(0);
        if (!link.isNameSet()) {
            if (urlSlug != null) {
                link.setName(Encoding.htmlDecode(urlSlug).replace("-", " ").trim() + extDefault);
            } else {
                link.setName(this.getFID(link) + extDefault);
            }
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (isOffline(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String url_embed = br.getRegex("<iframe[^>]*?src=\"(https?://(?:www\\.)?xnhub\\.com/embed/\\d+)\"[^>]*?></iframe>").getMatch(0);
        if (url_embed != null) {
            /* Typically pornziz.com --> xnhub.com */
            br.getPage(url_embed);
            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("Invalid Video ID")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        final String filename_url = getUrlFilename(link);
        String title = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]+)\">").getMatch(0);
        if (StringUtils.isEmpty(title)) {
            title = filename_url;
        }
        dllink = br.getRegex("<source src=\"(https?[^<>\"]+)\"").getMatch(0);
        if (dllink == null) {
            /*
             * Most of all times they're embedding their own content but sometimes it's "external" --> Should be handled by crawlerplugin
             * and not here!
             */
            String iframe = br.getRegex("<iframe[^<>]+src=\"([^<>\"]+)\"[^<>]+allowfullscreen").getMatch(0);
            if (iframe != null) {
                br.getPage(iframe);
                dllink = br.getRegex("<source src=\"(https?[^<>\"]+)\"").getMatch(0);
            }
        }
        if (dllink == null) {
            logger.info("filename: " + title + ", dllink: " + dllink);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (title != null) {
            title = Encoding.htmlDecode(title);
            title = title.trim();
            link.setFinalFileName(title + extDefault);
        }
        if (!StringUtils.isEmpty(dllink)) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(this.dllink);
                handleConnectionErrors(br, con);
                if (con.getCompleteContentLength() > 0) {
                    if (con.isContentDecoded()) {
                        link.setDownloadSize(con.getCompleteContentLength());
                    } else {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                }
                final String ext = getExtensionFromMimeType(con);
                if (ext != null) {
                    link.setFinalFileName(this.correctOrApplyFileNameExtension(title, "." + ext));
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

    public static boolean isOffline(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 404;
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
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    private void handleConnectionErrors(final Browser br, final URLConnectionAdapter con) throws PluginException, IOException {
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            if (con.getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Video broken?");
            }
        }
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
