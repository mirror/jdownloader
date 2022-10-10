//jDownloader - Downloadmanager
//Copyright (C) 2017  JD-Team support@jdownloader.org
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.controller.LazyPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class JpgChurch extends PluginForHost {
    public JpgChurch(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_HOST };
    }

    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;
    private final String         PROPERTY_USER     = "user";

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "jpg.church" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/img/([^/\\?#]+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://jpg.church/page/tos";
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
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        if (!link.isNameSet()) {
            link.setName(this.correctOrApplyFileNameExtension(this.getFID(link).replaceAll("-+", " "), null));
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final boolean useOembed = true;
        String title = null;
        if (useOembed) {
            final UrlQuery query = new UrlQuery();
            query.add("url", URLEncode.encodeURIComponent(link.getPluginPatternMatcher()));
            query.add("format", "json");
            br.getPage("https://" + this.getHost() + "/oembed/?" + query.toString());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            if (entries == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            title = (String) entries.get("title");
            final String thumbnailURL = (String) entries.get("url");
            if (thumbnailURL == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Remove part of this URL to get the full image. */
            this.dllink = thumbnailURL.replaceFirst("\\.md\\.(jpe?g|webp|gif)$", ".$1");
            final String author = (String) entries.get("author");
            if (author != null) {
                link.setProperty(PROPERTY_USER, author);
            }
            br.setCurrentURL(link.getPluginPatternMatcher());
        } else {
            br.getPage(link.getPluginPatternMatcher());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            title = br.getRegex("property\\s*=\\s*\"og:title\" content\\s*=\\s*\"([^\"]+)\"").getMatch(0);
            dllink = br.getRegex("property\\s*=\\s*\"og:image\" content\\s*=\\s*\"(https?://[^\"]+)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("<link rel\\s*=\\s*\"image_src\" href\\s*=\\s*\"(https?://[^\"]+)\">").getMatch(0);
            }
            final String author = br.getRegex("username\\s*:\\s*\"([^\"]+)\"").getMatch(0);
            if (author != null) {
                link.setProperty(PROPERTY_USER, author);
            }
        }
        if (!StringUtils.isEmpty(title)) {
            final String ext = getFileNameExtensionFromURL(dllink);
            title = Encoding.htmlDecode(title).trim();
            if (ext == null) {
                link.setName(this.correctOrApplyFileNameExtension(title, null));
            } else {
                link.setFinalFileName(this.correctOrApplyFileNameExtension(title, ext));
            }
        }
        if (!StringUtils.isEmpty(dllink)) {
            final Browser brc = br.cloneBrowser();
            if (checkDownloadableRequest(link, brc, brc.createHeadRequest(dllink), 0, true) == null) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Video broken?");
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (StringUtils.isEmpty(dllink)) {
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
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.CheveretoImageHosting;
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