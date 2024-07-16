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
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLSearch;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class YoupicCom extends PluginForHost {
    public YoupicCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_HOST };
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    public int getMaxChunks(final DownloadLink link, final Account account) {
        return 1;
    }

    private String       dllink             = null;
    private final String PROPERTY_TITLE     = "title";
    private final String PROPERTY_FULL_NAME = "full_name";
    private final String PROPERTY_USERNAME  = "user";

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

    private static final Pattern PATTERN_OLD = Pattern.compile("image/(\\d+)(/([\\w\\-]+))?");
    private static final Pattern PATTERN_NEW = Pattern.compile("([\\w\\-]+)/([0-9]{15,})/([0-9]{15,})");

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(" + PATTERN_OLD + "|" + PATTERN_NEW + ")");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://" + getHost() + "/termconditions";
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
        String fid = new Regex(link.getPluginPatternMatcher(), PATTERN_NEW).getMatch(2);
        if (fid == null) {
            fid = new Regex(link.getPluginPatternMatcher(), PATTERN_OLD).getMatch(0);
        }
        return fid;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = null;
        final String extDefault = ".jpg";
        if (!link.isNameSet()) {
            final String urlSlug = new Regex(link.getPluginPatternMatcher(), PATTERN_OLD).getMatch(2);
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
        final String appB64 = br.getRegex("\"app\"\\), \"([^\"]+)").getMatch(0);
        if (appB64 != null) {
            /* TODO: Fix this, encoding is wrong */
            System.out.println(Encoding.Base64Decode(appB64));
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
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, null), this.getMaxChunks(link, null));
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
        return Integer.MAX_VALUE;
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