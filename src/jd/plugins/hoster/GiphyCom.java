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
import java.util.Map;

import org.appwork.storage.TypeRef;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class GiphyCom extends PluginForHost {
    public GiphyCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 1;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;
    private boolean              server_issues     = false;
    private final String         PATTERN_NORMAL    = "https?://[^/]+/gifs/([A-Za-z0-9\\-]+-)?([A-Za-z0-9]+)";
    private final String         PATTERN_DIRECT    = "https?://media\\d*\\.giphy\\.com/media/([A-Za-z0-9]+)";

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "giphy.com" });
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
            ret.add("https?://(?:www\\.|media\\d*\\.)?" + buildHostsPatternPart(domains) + "/(gifs/([A-Za-z0-9\\-]+-)?[A-Za-z0-9]+|media/[A-Za-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://support.giphy.com/hc/en-us/articles/360020027752-GIPHY-Terms-of-Service";
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
        if (link.getPluginPatternMatcher() == null) {
            return null;
        } else if (link.getPluginPatternMatcher().matches(PATTERN_DIRECT)) {
            return new Regex(link.getPluginPatternMatcher(), PATTERN_DIRECT).getMatch(0);
        } else {
            return new Regex(link.getPluginPatternMatcher(), PATTERN_NORMAL).getMatch(0);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String fid = getFID(link);
        if (!link.isNameSet()) {
            link.setName(this.getFID(link) + ".mp4");
        }
        if (link.getPluginPatternMatcher().matches("(?i).*/media/.+")) {
            // rewrite direct/media urls to normal url format
            br.getPage("https://" + this.getHost() + "/gifs/" + fid);
            if (!br.getURL().contains(fid) || !this.canHandle(br.getURL())) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            link.setPluginPatternMatcher(br.getURL());
        }
        String title = null;
        dllink = null;
        server_issues = false;
        final boolean useOembedAPI = true;
        if (useOembedAPI) {
            br.getPage("https://" + this.getHost() + "/services/oembed?url=" + Encoding.urlEncode(link.getPluginPatternMatcher()));
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
            this.dllink = (String) entries.get("url");
            final String titleTmp = (String) entries.get("title");
            if (!StringUtils.isEmpty(titleTmp)) {
                title = titleTmp.replaceAll("(\\s*-\\s*Find\\s*&\\s*Share\\s*on\\s*GIPHY)", "");
            }
        } else {
            br.getPage(link.getPluginPatternMatcher());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            dllink = br.getRegex("property=\"og:image\" content=\"(https://[^\"]+)").getMatch(0);
        }
        if (title != null) {
            link.setFinalFileName(this.applyFilenameExtension(title, ".gif"));
        }
        if (!StringUtils.isEmpty(dllink)) {
            dllink = Encoding.htmlDecode(dllink);
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(this.dllink);
                if (!this.looksLikeDownloadableContent(con)) {
                    server_issues = true;
                } else {
                    if (con.getCompleteContentLength() > 0) {
                        if (con.isContentDecoded()) {
                            link.setDownloadSize(con.getCompleteContentLength());
                        } else {
                            link.setVerifiedFileSize(con.getCompleteContentLength());
                        }
                    }
                    if (title != null) {
                        link.setFinalFileName(this.correctOrApplyFileNameExtension(title, con));
                    }
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
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
            }
        }
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
