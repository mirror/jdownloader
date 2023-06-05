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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "vidyard.com" }, urls = { "https?://(?:[A-Za-z0-9]+\\.)?vidyard\\.com/watch/([A-Za-z0-9\\-_]+)" })
public class VidyardCom extends PluginForHost {
    public VidyardCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.VIDEO_STREAMING };
    }
    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:

    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;
    private boolean              server_issues     = false;

    @Override
    public String getAGBLink() {
        return "https://www.vidyard.com/terms/";
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

    @SuppressWarnings({ "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        if (!link.isNameSet()) {
            link.setName(getFID(link) + ".mp4");
        }
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage("http://" + Browser.getHost(link.getPluginPatternMatcher(), true) + "/watch/" + this.getFID(link));
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String fallback_sd_url = this.br.getRegex("property=\"og:video\" content=\"(https[^<>\"]+)\"").getMatch(0);
        this.br.getPage("https://play.vidyard.com/player/" + this.getFID(link) + ".json");
        final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.getRequest().getHtmlCode());
        final Map<String, Object> videomapv1 = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "payload/chapters/{0}");
        final Map<String, Object> videomapv2 = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "payload/vyContext/chapterAttributes/{0}");
        if (videomapv1 == null && videomapv2 == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String title = null;
        String description = null;
        final List<Map<String, Object>> ressourcelist;
        if (videomapv1 != null) {
            title = (String) videomapv1.get("name");
            description = (String) videomapv1.get("description");
            ressourcelist = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(videomapv1, "sources/mp4");
        } else {
            final Map<String, Object> video_data = (Map<String, Object>) videomapv2.get("video_data");
            title = (String) video_data.get("name");
            description = (String) video_data.get("description");
            ressourcelist = (List<Map<String, Object>>) videomapv2.get("video_files");
        }
        if (!StringUtils.isEmpty(description) && link.getComment() == null) {
            link.setComment(description);
        }
        if (ressourcelist == null || ressourcelist.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (fallback_sd_url == null) {
            fallback_sd_url = (String) entries.get("sd_url");
        }
        if (fallback_sd_url == null) {
            fallback_sd_url = (String) entries.get("sd_unsecure_url");
        }
        String profile = null;
        String url_temp = null;
        boolean stop = false;
        final String[] supportedQualities = { "full_hd", "hd", "720p", "480p", "360p", "sd", "mp3" };
        for (final String quality : supportedQualities) {
            for (final Map<String, Object> qualityMap : ressourcelist) {
                profile = (String) qualityMap.get("profile");
                url_temp = (String) qualityMap.get("url");
                if (StringUtils.isEmpty(url_temp)) {
                    url_temp = (String) qualityMap.get("unsecure_url");
                }
                if (profile == null || url_temp == null) {
                    continue;
                }
                if (profile.equals(quality)) {
                    stop = true;
                    dllink = url_temp;
                    break;
                }
            }
            if (stop) {
                break;
            }
        }
        if (dllink == null) {
            /* Last chance */
            dllink = fallback_sd_url;
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String ext;
        if (StringUtils.equalsIgnoreCase(profile, "mp3")) {
            ext = ".mp3";
        } else {
            ext = ".mp4";
        }
        if (!StringUtils.isEmpty(title)) {
            title = Encoding.htmlDecode(title);
            title = title.trim();
            title = encodeUnicode(title);
            link.setFinalFileName(title + ext);
        }
        if (!StringUtils.isEmpty(dllink)) {
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
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
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
