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
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "eroxia.com" }, urls = { "https?://(?:www\\.)?eroxia\\.com/(?:video/[a-z0-9\\-]+-\\d+\\.html|embed/\\d+)" })
public class EroxiaCom extends PluginForHost {
    public EroxiaCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.XXX };
    }

    /* Using playerConfig script */
    /* Tags: playerConfig.php */
    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "http://www.eroxia.com/contact.php";
    }

    private static final String TYPE_NORMAL = "https?://[^/]+/video/([a-z0-9\\-]+)-(\\d+)\\.html";
    private static final String TYPE_EMBED  = "https?://[^/]+/embed/(\\d+)";

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
        if (link != null && link.getPluginPatternMatcher() != null) {
            if (link.getPluginPatternMatcher().matches(TYPE_NORMAL)) {
                return new Regex(link.getPluginPatternMatcher(), TYPE_NORMAL).getMatch(1);
            } else {
                return new Regex(link.getPluginPatternMatcher(), TYPE_EMBED).getMatch(0);
            }
        } else {
            return null;
        }
    }

    private String getWeakFilename(final DownloadLink link) {
        final String weakTitle;
        if (link.getPluginPatternMatcher().matches(TYPE_NORMAL)) {
            weakTitle = new Regex(link.getPluginPatternMatcher(), TYPE_NORMAL).getMatch(0).replace("-", " ");
        } else {
            weakTitle = new Regex(link.getPluginPatternMatcher(), TYPE_EMBED).getMatch(0);
        }
        return weakTitle + ".mp4";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        if (!link.isNameSet()) {
            link.setName(this.getWeakFilename(link));
        }
        br.setFollowRedirects(true);
        /* 2021-08-06: Important else we might not be able to find downloadurls! */
        final boolean forceEmbedHandling = true;
        if (forceEmbedHandling) {
            br.getPage("https://www." + this.getHost() + "/embed/" + this.getFID(link));
            if (br.containsHTML("Invalid Video ID") || br.toString().length() <= 100) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } else {
            br.getPage(link.getPluginPatternMatcher());
            if (!this.canHandle(this.br.getURL()) || br.containsHTML("(?i)Tube</title>|error\">(Page you are|We're sorry)")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        String filename;
        if (link.getPluginPatternMatcher().matches(TYPE_NORMAL)) {
            filename = new Regex(link.getPluginPatternMatcher(), TYPE_NORMAL).getMatch(0).replace("-", " ");
        } else {
            filename = this.getFID(link);
        }
        // Try to find direct link first
        dllink = br.getRegex("\\&file=(http[^<>\"]*?)\\&").getMatch(0);
        /* This method does not work for all items - only for .mp4 items with hash as internal filename! */
        final boolean allowCreateVideoUrlFromThumbnail = false;
        if (dllink == null && allowCreateVideoUrlFromThumbnail) {
            /* 2021-08-06: Seems like they somehow block us? But luckily we can generate valid streamingURLs out of their thumbnail URLs. */
            final String thumbnailURLPart = br.getRegex("(https?://media\\.eroxia\\.com/thumbs/[^\"]*?/[a-f0-9]{32}\\.mp4)").getMatch(0);
            if (thumbnailURLPart != null) {
                dllink = thumbnailURLPart.replaceFirst("/thumbs/", "/");
            }
        }
        if (dllink == null) {
            /* 2020-10-19 */
            dllink = br.getRegex("<source src=\"(https?://[^/]+/videos/[^\"]+\\.mp4)\"").getMatch(0);
        }
        if (dllink == null) {
            /* Old code */
            dllink = br.getRegex("url: \\'(http://[^<>\"]*?)\\'").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("<source[^>]*\\s+src=(\"|'|)(.*?)\\1").getMatch(1);
                if (dllink == null) {
                    // No direct link there -> 2nd way
                    dllink = br.getRegex("(http://(www\\.)?eroxia\\.com/playerConfig\\.php\\?[^<>\"/\\&]*?)\"").getMatch(0);
                    if (dllink != null) {
                        br.getPage(Encoding.htmlDecode(dllink));
                        dllink = br.getRegex("flvMask:(http://[^<>\"]*?);").getMatch(0);
                        if (dllink == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                    }
                }
            }
        }
        dllink = Encoding.htmlDecode(dllink);
        link.setFinalFileName(Encoding.htmlDecode(filename).trim() + ".mp4");
        if (!StringUtils.isEmpty(this.dllink)) {
            URLConnectionAdapter con = null;
            try {
                final Browser brc = br.cloneBrowser();
                brc.setFollowRedirects(true);
                con = brc.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (this.dllink == null) {
            if (!br.containsHTML("id=\"thisPlayer\"")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "No player available (?)");
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.UnknownPornScript4;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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
