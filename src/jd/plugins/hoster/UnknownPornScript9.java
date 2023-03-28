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

import java.util.Map;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

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
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "winporn.com", "proporn.com", "vivatube.com", "tubeon.com", "viptube.com", "hd21.com", "iceporn.com", "nuvid.com", "yeptube.com" }, urls = { "https?://(?:www\\.)?winporn\\.com/(?:[a-z]{2}/)?video/\\d+(?:/[a-z0-9\\-]+)?", "https?://(?:www\\.)?proporn\\.com/(?:[a-z]{2}/)?video/\\d+(?:/[a-z0-9\\-]+)?", "https?://(?:www\\.)?vivatube\\.com/(?:[a-z]{2}/)?video/\\d+(?:/[a-z0-9\\-]+)?", "https?://(?:www\\.)?tubeon\\.com/(?:[a-z]{2}/)?video/\\d+(?:/[a-z0-9\\-]+)?", "https?://(?:www\\.)?viptube\\.com/(?:[a-z]{2}/)?video/\\d+(?:/[a-z0-9\\-]+)?", "https?://(?:www\\.)?hd21\\.com/(?:[a-z]{2}/)?video/\\d+(?:/[a-z0-9\\-]+)?", "https?://(?:www\\.)?iceporn\\.com/(?:[a-z]{2}/)?video/\\d+(?:/[a-z0-9\\-]+)?", "https?://(?:www\\.)?nuvid\\.com/(?:[a-z]{2}/)?video/\\d+(?:/[a-z0-9\\-]+)?",
        "https?://(?:www\\.)?yeptube\\.com/(?:[a-z]{2}/)?video/\\d+(?:/[a-z0-9\\-]+)?" })
public class UnknownPornScript9 extends PluginForHost {
    public UnknownPornScript9(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    /* Similar sites but they use a different 'player_config' URL: drtuber.com, viptube.com */
    /* Connection stuff */
    private final boolean free_resume       = true;
    private int           free_maxchunks    = 0;
    private final int     free_maxdownloads = -1;
    private String        dllink            = null;
    private boolean       server_issues     = false;

    @Override
    public String getAGBLink() {
        return "http://www.winporn.com/static/terms";
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
        return new Regex(link.getPluginPatternMatcher(), "/video/(\\d+)").getMatch(0);
    }

    /** Items with different FUIDs but same filenames should not get treated as mirrors! */
    @Override
    public String getMirrorID(DownloadLink link) {
        final String fid = getFID(link);
        if (link != null && StringUtils.equals(getHost(), link.getHost()) && fid != null) {
            return getHost() + "://" + fid;
        } else {
            return super.getMirrorID(link);
        }
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = null;
        server_issues = false;
        link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("class=\"notifications__item notifications__item-error\"") || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String url_filename = new Regex(link.getDownloadURL(), "([a-z0-9\\-]+)$").getMatch(0);
        /* Access player json */
        final String videoid = PluginJSonUtils.getJson(br, "vid");
        if (StringUtils.isEmpty(videoid)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        String config_url = br.getRegex("config_url\\s*:\\s*'(.*?)'").getMatch(0);
        String embed = br.getRegex("embed\\s*:\\s*(\\d+)").getMatch(0);
        if (config_url == null) {
            /* Fallback */
            config_url = "/player_config_json/";
        } else {
            config_url = config_url.replace("\\", "");
        }
        if (embed == null) {
            embed = "0";
        }
        br.getPage(String.format("%s?vid=%s&aid=&domain_id=&embed=%s&ref=&check_speed=0", config_url, videoid, embed));
        final Map<String, Object> map = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        // final long has_hq = JavaScriptEngineFactory.toLong(map.get("has_hq"), 1);
        /* Most reliable way to find filename */
        String filename = (String) map.get("title");
        if (filename == null) {
            filename = url_filename;
        }
        /* Prefer hq */
        dllink = (String) JavaScriptEngineFactory.walkJson(map, "files/hq");
        if (dllink == null) {
            dllink = (String) JavaScriptEngineFactory.walkJson(map, "files/lq");
        }
        filename = filename.trim();
        final String ext = getFileNameExtensionFromString(dllink, ".mp4");
        link.setFinalFileName(Encoding.htmlDecode(filename) + ext);
        URLConnectionAdapter con = null;
        try {
            final Browser brc = br.cloneBrowser();
            con = brc.openHeadConnection(dllink);
            if (con.getResponseCode() == 404) {
                /*
                 * Small workaround for buggy servers that redirect and fail if the Referer is wrong then. Examples: hdzog.com
                 */
                brc.followConnection(true);
                final String redirect_url = con.getRequest().getUrl();
                con = brc.openHeadConnection(redirect_url);
            }
            if (looksLikeDownloadableContent(con)) {
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
            } else {
                server_issues = true;
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
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
        if (link.getDownloadURL().contains("iceporn") || link.getDownloadURL().contains("viptube")) {
            free_maxchunks = 1; // https://svn.jdownloader.org/issues/84428, /84735
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
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
        return SiteTemplate.UnknownPornScript9;
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
