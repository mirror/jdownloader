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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Random;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tvigle.ru" }, urls = { "http://cloud\\.tvigle\\.ru/video/\\d+|http://www\\.tvigle\\.ru/video/[a-z0-9\\-]+/" })
public class TvigleRu extends PluginForHost {
    public TvigleRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;
    private static final String  type_embedded     = "http://cloud\\.tvigle\\.ru/video/\\d+";
    private static final String  type_normal       = "http://www\\.tvigle\\.ru/video/[a-z0-9\\-]+/";

    @Override
    public String getAGBLink() {
        return "http://www.tvigle.ru/";
    }

    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        String filename;
        long filesize = 0;
        final String[] qualities = { "1080p", "720p", "480p", "360p", "240p", "180p" };
        LinkedHashMap<String, Object> api_data;
        String videoID = downloadLink.getStringProperty("videoID", null);
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
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
            }
            if (videoID == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            downloadLink.setName(videoID + ".mp4");
        }
        int partner_id = new Random().nextInt(12);
        if (partner_id == 0) {
            partner_id = 1;
        }
        /* partner_id = number between 0 and 18 */
        br.getPage("http://cloud.tvigle.ru/api/play/video/" + videoID + "/?partner_id=" + partner_id);
        if (br.getHttpConnection().getResponseCode() == 404 || !br.getHttpConnection().getContentType().equals("application/json")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        api_data = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        api_data = (LinkedHashMap<String, Object>) api_data.get("playlist");
        api_data = (LinkedHashMap<String, Object>) ((ArrayList) api_data.get("items")).get(0);
        final Object error_object = api_data.get("errorType");
        if (error_object != null) {
            final long error_code = ((Number) error_object).longValue();
            if (error_code == 1 || error_code == 7) { // "errorType": 7, "isGeoBlocked": true
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final LinkedHashMap<String, Object> videos = (LinkedHashMap<String, Object>) api_data.get("videos");
        final LinkedHashMap<String, Object> videolinks_map = (LinkedHashMap<String, Object>) videos.get("mp4");
        final LinkedHashMap<String, Object> video_files_size = (LinkedHashMap<String, Object>) api_data.get("video_files_size");
        final LinkedHashMap<String, Object> video_files_size_map = (LinkedHashMap<String, Object>) video_files_size.get("mp4");
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
        filename = encodeUnicode(filename);
        final String ext = getFileNameExtensionFromString(dllink, ".mp4");
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        downloadLink.setFinalFileName(filename);
        downloadLink.setDownloadSize(filesize);
        downloadLink.setProperty("videoID", videoID);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, free_resume, free_maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                if (isJDStable()) {
                    con = br2.openGetConnection(dllink);
                } else {
                    con = br2.openHeadConnection(dllink);
                }
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    private boolean isJDStable() {
        return System.getProperty("jd.revision.jdownloaderrevision") == null;
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
