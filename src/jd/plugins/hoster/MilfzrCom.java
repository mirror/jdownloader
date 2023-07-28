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

import java.util.List;
import java.util.Map;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
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
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "milfzr.com" }, urls = { "https?://(?:www\\.)?milfzr\\.com/(?:[A-Za-z0-9\\-%]+/?|\\?p=\\d+)" })
public class MilfzrCom extends PluginForHost {
    public MilfzrCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    /* DEV NOTES */
    // Tags: Porn plugin
    // protocol: no https
    // other:
    /* Extension which will be used if no correct extension is found */
    private static final String default_extension  = ".mp4";
    /* Connection stuff */
    private static final int    free_maxdownloads  = -1;
    private String              dllink             = null;
    private final String        PROPERTY_CONTENTID = "contentid";
    private final String        PATTERN_NORMAL     = "(?i)https?://(?:www\\.)?milfzr\\.com/([A-Za-z0-9\\-%]+)/?";
    private final String        PATTERN_SHORT      = "(?i)https?://(?:www\\.)?milfzr\\.com/\\?p=(\\d+)";

    @Override
    public String getAGBLink() {
        return "https://milfzr.com/";
    }

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
        final String storedContentid = link.getStringProperty(PROPERTY_CONTENTID);
        if (storedContentid != null) {
            return storedContentid;
        } else {
            return new Regex(link.getPluginPatternMatcher(), PATTERN_SHORT).getMatch(0);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final String fid = this.getFID(link);
        final String urlSlug = new Regex(link.getPluginPatternMatcher(), PATTERN_NORMAL).getMatch(0);
        if (!link.isNameSet()) {
            if (urlSlug != null) {
                link.setName(Encoding.htmlDecode(urlSlug).trim() + ".mp4");
            } else {
                link.setName(fid + ".mp4");
            }
        }
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        final String videoThumbnail = PluginJSonUtils.getJson(br, "posterImage");
        final String fidFromHTML = br.getRegex("rel='shortlink'[^>]*href='https?://[^/]+/\\?p=(\\d+)").getMatch(0);
        if (br.getHttpConnection().getResponseCode() == 403) {
            /* Bad responsecode */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 404) {
            /* Bad responsecode */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.getHttpConnection().getContentType().contains("html")) {
            /* Bad Content-Type e.g. https://milfzr.com/wp-json */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!this.canHandle(br.getURL())) {
            /*
             * Redirect to unsupported URL e.g. https://milfzr.com/wp-admin -->
             * https://milfzr.com/wp-login.php?redirect_to=https%3A%2F%2Fmilfzr.com%2Fwp-admin%2F&reauth=1
             */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getRequest().getHtmlCode().length() < 100) {
            /* Invalid response */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (fidFromHTML == null) {
            logger.warning("Failed to find internal ID for this item");
        } else if (fid == null) {
            link.setProperty(PROPERTY_CONTENTID, fidFromHTML);
        }
        String title = br.getRegex("<h1 class=\"entry-title\"[^>]*>([^<>\"]+)</h1>").getMatch(0);
        if (title == null) {
            title = HTMLSearch.searchMetaTag(br, "og:title");
        }
        /* RegExes sometimes used for streaming */
        final String jssource = br.getRegex("sources(?:\")?\\s*?:\\s*?(\\[.*?\\])").getMatch(0);
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
                    dllink_temp = (String) entries.get("file");
                    quality_temp_o = entries.get("label");
                    if (quality_temp_o != null && quality_temp_o instanceof Number) {
                        quality_temp = JavaScriptEngineFactory.toLong(quality_temp_o, 0);
                    } else if (quality_temp_o != null && quality_temp_o instanceof String) {
                        quality_temp_str = (String) quality_temp_o;
                        if (quality_temp_str.matches("\\d+p")) {
                            /* E.g. '360p' */
                            quality_temp = Long.parseLong(new Regex(quality_temp_str, "(\\d+)p").getMatch(0));
                        } else if (quality_temp_str.equalsIgnoreCase("full")) {
                            /* Small hack to prefer such URLs */
                            quality_temp = 10000;
                        } else {
                            /* Bad / Unsupported format */
                            continue;
                        }
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
        if (StringUtils.isEmpty(dllink)) {
            dllink = br.getRegex("\\'(?:file|video)\\'[\t\n\r ]*?:[\t\n\r ]*?\\'(http[^<>\"]*?)\\'").getMatch(0);
        }
        if (StringUtils.isEmpty(dllink)) {
            dllink = br.getRegex("<source src=\"(https?://[^<>\"]*?)\" type=(?:\"|\\')video/(?:mp4|flv)(?:\"|\\')").getMatch(0);
        }
        if (StringUtils.isEmpty(dllink)) {
            dllink = br.getRegex("property=\"og:video\" content=\"(http[^<>\"]*?)\"").getMatch(0);
        }
        if (title != null) {
            title = Encoding.htmlDecode(title);
            title = title.trim();
            String ext;
            if (!StringUtils.isEmpty(dllink)) {
                ext = getFileNameExtensionFromString(dllink, default_extension);
                if (ext != null && !ext.matches("\\.(?:flv|mp4)")) {
                    ext = default_extension;
                }
            } else {
                ext = default_extension;
            }
            if (!title.endsWith(ext)) {
                title += ext;
            }
            link.setFinalFileName(title);
        }
        if (StringUtils.isEmpty(dllink) && videoThumbnail == null) {
            /* No video content e.g. https://milfzr.com/tags/ or https://milfzr.com/news/ */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!StringUtils.isEmpty(dllink)) {
            // dllink = Encoding.htmlDecode(dllink);
            if (dllink.contains("//videos-up")) {
                dllink = dllink.replace("//videos-up", "//milfzr.com/videos-up");
            }
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                handleConnectionErrors(br, con);
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        } else {
            /* We cannot be sure whether we have the correct extension or not! */
            link.setName(title);
        }
        return AvailableStatus.TRUE;
    }

    private void handleConnectionErrors(final Browser br, final URLConnectionAdapter con) throws Exception {
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            if (con.getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
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
