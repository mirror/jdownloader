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

import java.util.ArrayList;
import java.util.List;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.plugins.decrypter.BoysfoodComCrawler;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class BoysfoodCom extends PluginForHost {
    public BoysfoodCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    /* DEV NOTES */
    // Tags: Porn plugin
    // other:
    private String              dllink      = null;
    private static final String TYPE_EMBED  = "(?:https?://[^/]+)?/embed/(\\d+)/?";
    private static final String TYPE_NORMAL = "(?:https?://[^/]+)?/(?:free-porn-videos|videos)/(\\d+)/([a-z0-9\\-]+)";

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "boysfood.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:(?:free-porn-videos|videos)/\\d+/[a-z0-9\\-]+|embed/\\d+/?)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://www.boysfood.com/page/tos/";
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
        if (link == null || link.getPluginPatternMatcher() == null) {
            return null;
        } else if (link.getPluginPatternMatcher().matches(TYPE_EMBED)) {
            return new Regex(link.getPluginPatternMatcher(), TYPE_EMBED).getMatch(0);
        } else {
            return new Regex(link.getPluginPatternMatcher(), TYPE_NORMAL).getMatch(0);
        }
    }

    // private String getURLTitle(final DownloadLink link) {
    // return getURLTitleCleaned(link.getPluginPatternMatcher());
    // }
    public static String getURLTitleCleaned(final String url) {
        String title = new Regex(url, TYPE_NORMAL).getMatch(1);
        if (title != null) {
            return title.replace("-", " ").trim();
        } else {
            return null;
        }
    }

    private String getWeakFilename(final DownloadLink link) {
        final String urlTitle = getURLTitleCleaned(link.getPluginPatternMatcher());
        if (urlTitle != null) {
            return urlTitle + ".mp4";
        } else {
            return this.getFID(link) + ".mp4";
        }
    }

    @Override
    public boolean isResumeable(DownloadLink link, final Account account) {
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        if (!link.isNameSet()) {
            link.setName(getWeakFilename(link));
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (BoysfoodComCrawler.isOfflineStatic(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.getURL().matches(TYPE_EMBED)) {
            final String realVideoURL = br.getRegex(TYPE_NORMAL).getMatch(-1);
            if (realVideoURL == null || !realVideoURL.contains(this.getFID(link))) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            link.setPluginPatternMatcher(br.getURL(realVideoURL).toString());
            br.getPage(realVideoURL);
        }
        final String titleByURL = getURLTitleCleaned(br.getURL());
        if (titleByURL != null) {
            link.setFinalFileName(titleByURL + ".mp4");
        }
        dllink = br.getRegex("Download:\\s*<a href=\"(https?://[^\"]+)").getMatch(0);
        if (dllink == null) {
            // 2024-04-24
            dllink = br.getRegex("\"contentUrl\": \"(https?://[^\"]+)").getMatch(0);
        }
        if (!StringUtils.isEmpty(dllink)) {
            /* Fix encoding */
            this.dllink = Encoding.htmlOnlyDecode(this.dllink);
            if (!isDownload) {
                basicLinkCheck(br.cloneBrowser(), br.createHeadRequest(dllink), link, link.getFinalFileName(), ".mp4");
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, null), 0);
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.KernelVideoSharing;
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