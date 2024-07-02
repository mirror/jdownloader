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
import java.util.regex.Pattern;

import org.appwork.storage.TypeRef;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tvigle.ru" }, urls = { "https?://cloud\\.tvigle\\.ru/video/\\d+|https?://www\\.tvigle\\.ru/video/[a-z0-9\\-]+/" })
public class TvigleRu extends PluginForHost {
    public TvigleRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.VIDEO_STREAMING };
    }

    /* Connection stuff */
    private static final boolean free_resume    = true;
    private static final int     free_maxchunks = 0;
    private String               dllink         = null;
    private static final String  type_embedded  = "(?i)https?://cloud\\.tvigle\\.ru/video/\\d+";
    private static final String  type_normal    = "(?i)https?://www\\.tvigle\\.ru/video/[a-z0-9\\-]+/";

    @Override
    public String getAGBLink() {
        return "http://www.tvigle.ru/";
    }

    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        dllink = null;
        String filename;
        long filesize = 0;
        final String[] qualities = { "1080p", "720p", "480p", "360p", "240p", "180p" };
        String videoID = downloadLink.getStringProperty("videoID", null);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String extDefault = ".mp4";
        if (videoID == null) {
            if (downloadLink.getDownloadURL().matches(type_embedded)) {
                videoID = new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0);
            } else {
                br.getPage(downloadLink.getDownloadURL());
                if (br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                videoID = br.getRegex("var cloudId = \\'(\\d+)\\';").getMatch(0);
                if (videoID == null) {
                    videoID = br.getRegex("class=\"video-preview current_playing\" id=\"(\\d+)\"").getMatch(0);
                }
                if (videoID == null) {
                    videoID = br.getRegex("api/v1/video/(\\d+)").getMatch(0);
                }
                if (videoID == null) {
                    /* 2020-11-30 */
                    videoID = br.getRegex("cloud\\.tvigle\\.ru/video/(\\d+)").getMatch(0);
                }
                if (videoID == null) {
                    /* 2024-07-02 */
                    videoID = br.getRegex("\"first_video_id\":(\\d+)").getMatch(0);
                }
            }
            if (videoID == null) {
                if (!br.containsHTML(Pattern.quote(br._getURL().getPath()))) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            downloadLink.setName(videoID + extDefault);
        }
        br.getPage("/api/video/" + videoID + "/");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.getHttpConnection().getContentType().contains("application/json")) {
            /* No json response */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        Map<String, Object> api_data = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        api_data = (Map<String, Object>) api_data.get("playlist");
        api_data = (Map<String, Object>) ((List) api_data.get("items")).get(0);
        final Object error_object = api_data.get("errorType");
        if (error_object != null) {
            final long error_code = ((Number) error_object).longValue();
            if (error_code == 1 || error_code == 7) { // "errorType": 7, "isGeoBlocked": true
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Map<String, Object> videos = (Map<String, Object>) api_data.get("videos");
        final Map<String, Object> videolinks_map = (Map<String, Object>) videos.get("mp4");
        final Map<String, Object> video_files_size = (Map<String, Object>) api_data.get("video_files_size");
        final Map<String, Object> video_files_size_map = (Map<String, Object>) video_files_size.get("mp4");
        for (final String quality : qualities) {
            dllink = (String) videolinks_map.get(quality);
            if (dllink != null) {
                filesize = ((Number) video_files_size_map.get(quality)).longValue();
                break;
            }
        }
        filename = (String) api_data.get("title");
        if (filename == null || dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String ext = getFileNameExtensionFromString(dllink, extDefault);
        filename = this.applyFilenameExtension(filename, ext);
        downloadLink.setFinalFileName(filename);
        downloadLink.setDownloadSize(filesize);
        downloadLink.setProperty("videoID", videoID);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
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
