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

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
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
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "javhd.com" }, urls = { "" })
public class JavhdCom extends PluginForHost {
    public JavhdCom(PluginWrapper wrapper) {
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
    private static final String  default_extension = ".mp4";
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 1;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;
    private final String         TYPE_OLD          = "(?i)https?://[^/]+/[a-z]{2}/id/(\\d+)/([a-z0-9\\-]+)";
    private final String         TYPE_OLD_2        = "(?i)https?://[^/]+/[a-z]{2}/tourjoin\\?id=(\\d+)";

    @Override
    public String getAGBLink() {
        return "https://javhd.com/en/terms";
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
        String fid = new Regex(link.getPluginPatternMatcher(), TYPE_OLD).getMatch(0);
        if (fid == null) {
            fid = new Regex(link.getPluginPatternMatcher(), "(\\d+)$").getMatch(0);
        }
        return fid;
    }

    private String getTitleFromURL(final DownloadLink link) {
        String title = new Regex(link.getPluginPatternMatcher(), TYPE_OLD).getMatch(1);
        if (title != null) {
            return title.replace("-", " ").trim();
        } else {
            return null;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final String videoid = this.getFID(link);
        if (!link.isNameSet()) {
            link.setName(videoid + default_extension);
        }
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String videoID1 = new Regex(br.getURL(), "(?i)/video/(\\d+)").getMatch(0);
        final String videoID2 = new Regex(br.getURL(), "(?i)/id/(\\d+)").getMatch(0);
        if (videoID1 == null && videoID2 == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (videoID1 != null) {
            br.getPage("/en/player_api?videoId=" + videoID1 + "&is_trailer=1");
        } else {
            br.getPage("/en/player/" + videoID2 + "?is_trailer=1");
        }
        final Map<String, Object> jsonroot = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final String id = jsonroot.get("id").toString();
        if (!StringUtils.equals(id, videoID1) && !StringUtils.equals(id, videoID2)) {
            /* Offline = all values will be null */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        try {
            Object quality_temp_o = null;
            long quality_temp = 0;
            String quality_temp_str = null;
            long quality_best = 0;
            String dllink_temp = null;
            final List<Object> ressourcelist = (List) jsonroot.get("sources");
            for (final Object videoo : ressourcelist) {
                final Map<String, Object> entries = (Map<String, Object>) videoo;
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
        final String titleFromURL = this.getTitleFromURL(link);
        String title = (String) jsonroot.get("title");
        if (StringUtils.isEmpty(title)) {
            /* Fallback */
            title = titleFromURL;
        }
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
        link.setFinalFileName(title + ext);
        if (!StringUtils.isEmpty(dllink)) {
            if (StringUtils.endsWithCaseInsensitive(dllink, "black_cap.mp4")) {
                /* Example: https://javhd.com/en/tourjoin?id=30453 */
                throw new PluginException(LinkStatus.ERROR_FATAL, "Broken video or no trailer available");
            }
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                connectionErrorhandling(con);
                link.setVerifiedFileSize(con.getCompleteContentLength());
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    private void connectionErrorhandling(final URLConnectionAdapter con) throws Exception {
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
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        this.connectionErrorhandling(dl.getConnection());
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
