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

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLSearch;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "al4a.com" }, urls = { "https?://(?:www\\.)?al4a\\.com/(?:video/[a-z0-9\\-]+-\\d+\\.html|embed/\\d+)" })
public class Al4aCom extends PluginForHost {
    public Al4aCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    /* DEV NOTES */
    // Tags: fluidplayer.com
    // protocol: no https
    // other:
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;
    private final String         PATTERN_NORMAL    = "(?i)https?://[^/]+/video/([a-z0-9\\-]+)-(\\d+)\\.html";
    private final String         PATTERN_EMBED     = "(?i)https?://[^/]+/embed/(\\d+)";

    @Override
    public String getAGBLink() {
        return "https://www.al4a.com/contact#terms";
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
        String fid = new Regex(link.getPluginPatternMatcher(), PATTERN_NORMAL).getMatch(1);
        if (fid == null) {
            fid = new Regex(link.getPluginPatternMatcher(), PATTERN_EMBED).getMatch(0);
        }
        return fid;
    }

    private String getTitleFromURL(final String url) {
        String urlSlug = new Regex(url, PATTERN_NORMAL).getMatch(0);
        if (urlSlug != null) {
            return urlSlug.replace("-", " ").trim();
        } else {
            return null;
        }
    }

    private String getContentURL(final DownloadLink link) {
        if (link.getPluginPatternMatcher().matches(PATTERN_EMBED)) {
            /* Do this so we can find a human readable title in the end. */
            return "https://www." + this.getHost() + "/video/-" + this.getFID(link) + ".html";
        } else {
            return link.getPluginPatternMatcher();
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        dllink = null;
        final String extDefault = ".mp4";
        String titleFromURL = getTitleFromURL(link.getPluginPatternMatcher());
        if (!link.isNameSet()) {
            if (titleFromURL != null) {
                link.setName(titleFromURL + extDefault);
            } else {
                link.setName(this.getFID(link) + extDefault);
            }
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(getContentURL(link));
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (titleFromURL == null) {
            /* E.g. for embed URLs which will redirect to normal URLs. */
            titleFromURL = getTitleFromURL(br.getURL());
        }
        String title = HTMLSearch.searchMetaTag(br, "og:title");
        if (title == null) {
            title = titleFromURL;
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
                        }
                    }
                }
            }
        }
        if (title != null) {
            title = Encoding.htmlDecode(title);
            title = title.trim();
            link.setName(title + extDefault);
        }
        if (!StringUtils.isEmpty(dllink) && !isDownload) {
            basicLinkCheck(br.cloneBrowser(), br.createHeadRequest(dllink), link, title, extDefault);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        if (StringUtils.isEmpty(dllink)) {
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
