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
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLSearch;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class YoupicCom extends PluginForHost {
    public YoupicCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_HOST };
    }

    /* Connection stuff */
    private static final boolean free_resume        = true;
    private static final int     free_maxchunks     = 1;
    private static final int     free_maxdownloads  = -1;
    private String               dllink             = null;
    private final String         PROPERTY_TITLE     = "title";
    private final String         PROPERTY_FULL_NAME = "full_name";
    private final String         PROPERTY_USERNAME  = "user";

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "youpic.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/image/(\\d+)(/([\\w\\-]+))?");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://youpic.com/termconditions";
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
        dllink = null;
        final String extDefault = ".jpg";
        if (!link.isNameSet()) {
            final String urlSlug = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(2);
            if (urlSlug != null) {
                link.setName(urlSlug.replace("-", " ").trim() + extDefault);
            } else {
                link.setName(this.getFID(link) + extDefault);
            }
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = HTMLSearch.searchMetaTag(br, "og:title");
        dllink = br.getRegex("\"(https?://cdn\\.[^/]+/large/[^\"]+)\"").getMatch(0);
        if (title != null) {
            title = Encoding.htmlDecode(title);
            title = title.replaceFirst("(?i)\\s*on YouPic", "");
            final Regex moreinfo = new Regex(title, " by (.+)");
            final String fullName = moreinfo.getMatch(0);
            if (fullName != null) {
                link.setProperty(PROPERTY_FULL_NAME, fullName);
                title = title.replace(moreinfo.getMatch(-1), "");
            } else {
                logger.warning("Failed to find fullName");
            }
            title = title.trim();
            link.setProperty(PROPERTY_TITLE, title);
            link.setFinalFileName(this.applyFilenameExtension(title, extDefault));
        }
        final String username = br.getRegex("/photographer/([\\w\\-]+)").getMatch(0);
        if (username != null) {
            link.setProperty(PROPERTY_USERNAME, username);
        } else {
            logger.warning("Failed to find username");
        }
        if (!StringUtils.isEmpty(dllink)) {
            dllink = dllink.replaceFirst("(?i)/large/", "/huge/"); // Higher image quality
            basicLinkCheck(br.cloneBrowser(), br.createGetRequest(dllink), link, title, extDefault);
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
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    @Override
    protected boolean looksLikeDownloadableContent(final URLConnectionAdapter con) {
        /* Special: For working image direct-URLs they return nearly no headers at all. Also no content-type header. */
        final String contentType = con.getContentType();
        if (con.isOK() && !StringUtils.containsIgnoreCase(contentType, "html") && con.getCompleteContentLength() > 0) {
            return true;
        } else {
            return false;
        }
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