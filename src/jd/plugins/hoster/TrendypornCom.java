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

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class TrendypornCom extends antiDDoSForHost {
    public TrendypornCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    /* DEV NOTES */
    // Tags: Porn plugin
    // other:
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;
    private boolean              server_issues     = false;

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "trendyporn.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:embed/\\d+|video/([a-z0-9\\-]+)?-\\d+\\.html)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://www.trendyporn.com/static/tos.html";
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

    private static final String TYPE_NORMAL = "https?://[^/]+/video/([a-z0-9\\-]+)?-(\\d+)\\.html";
    private static final String TYPE_EMBED  = "https?://[^/]+/embed/(\\d+)";

    private String getWeakFilename(final DownloadLink link) {
        String weakTitle;
        if (link.getPluginPatternMatcher().matches(TYPE_NORMAL)) {
            weakTitle = new Regex(link.getPluginPatternMatcher(), TYPE_NORMAL).getMatch(0);
            if (weakTitle != null) {
                weakTitle = weakTitle.replace("-", " ").trim();
            }
        } else {
            weakTitle = new Regex(link.getPluginPatternMatcher(), TYPE_EMBED).getMatch(0);
        }
        return weakTitle + ".mp4";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        if (!link.isNameSet()) {
            link.setName(getWeakFilename(link));
        }
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (link.getPluginPatternMatcher().matches(TYPE_EMBED)) {
            /* Access dummy-URL so we're able to find a meaningful filename inside HTML code */
            getPage("https://www." + this.getHost() + "/video/-" + this.getFID(link) + ".html");
        } else {
            getPage(link.getPluginPatternMatcher());
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename;
        final String filenameURLFromHTML = br.getRegex(TYPE_NORMAL).getMatch(0);
        if (filenameURLFromHTML != null) {
            filename = filenameURLFromHTML.replace("-", " ").trim();
        } else if (link.getPluginPatternMatcher().matches(TYPE_NORMAL)) {
            /* Fallback */
            filename = new Regex(link.getPluginPatternMatcher(), TYPE_NORMAL).getMatch(0).replace("-", " ").trim();
        } else {
            /* Last chance fallback */
            filename = this.getFID(link);
        }
        /* RegExes sometimes used for streaming */
        final String jssource = br.getRegex("sources(?:\")?\\s*:\\s*(\\[.*?\\])").getMatch(0);
        if (jssource != null) {
            try {
                Map<String, Object> entries = null;
                Object quality_temp_o = null;
                long quality_temp = 0;
                String quality_temp_str = null;
                long quality_best = 0;
                String dllink_temp = null;
                final List<Object> ressourcelist = (List) JavaScriptEngineFactory.jsonToJavaObject(jssource);
                for (final Object videoo : ressourcelist) {
                    entries = (Map<String, Object>) videoo;
                    dllink_temp = (String) entries.get("src");
                    quality_temp_o = entries.get("label");
                    if (quality_temp_o != null && quality_temp_o instanceof Number) {
                        quality_temp = JavaScriptEngineFactory.toLong(quality_temp_o, 0);
                    } else if (quality_temp_o != null && quality_temp_o instanceof String) {
                        quality_temp_str = (String) quality_temp_o;
                        if (quality_temp_str.matches("\\d+p.*?")) {
                            /* E.g. '360p' */
                            quality_temp = Long.parseLong(new Regex(quality_temp_str, "(\\d+)p").getMatch(0));
                        } else {
                            /* Bad / Unsupported format */
                            continue;
                        }
                    } else if (ressourcelist.size() == 1) {
                        /* Only one quality available --> Make sure we're using that */
                        quality_temp = 1;
                    }
                    if (StringUtils.isEmpty(dllink_temp) || quality_temp == 0) {
                        continue;
                    } else if (dllink_temp.contains(".m3u8")) {
                        /* Skip hls */
                        continue;
                    }
                    if (quality_temp > quality_best) {
                        quality_best = quality_temp;
                        dllink = dllink_temp;
                    }
                }
                if (!StringUtils.isEmpty(dllink)) {
                    logger.info("BEST handling for multiple video source succeeded");
                }
            } catch (final Throwable e) {
                logger.info("BEST handling for multiple video source failed");
            }
        }
        if (dllink == null) {
            dllink = br.getRegex("source src=\"(https?://[^\"]+)\"[^>]*type=.video/mp4").getMatch(0);
        }
        final String ext = ".mp4";
        if (filename != null) {
            filename = Encoding.htmlDecode(filename);
            filename = filename.trim();
            if (!filename.endsWith(ext)) {
                filename += ext;
            }
            link.setFinalFileName(filename);
        }
        if (!StringUtils.isEmpty(dllink)) {
            URLConnectionAdapter con = null;
            try {
                con = openAntiDDoSRequestConnection(br, br.createHeadRequest(dllink));
                if (!this.looksLikeDownloadableContent(con)) {
                    server_issues = true;
                } else {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
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