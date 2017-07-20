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
import java.util.List;
import java.util.Map;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.ffmpeg.json.StreamInfo;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imdb.com" }, urls = { "https?://(?:www\\.)?imdb\\.com/(?:video/(?!imdblink|internet\\-archive)[\\w\\-]+/vi\\d+|[A-Za-z]+/[a-z]{2}\\d+/mediaviewer/rm\\d+)" })
public class ImDbCom extends PluginForHost {
    private String              dllink         = null;
    private boolean             server_issues  = false;
    private boolean             mature_content = false;
    private static final String IDREGEX        = "(vi\\d+)$";
    private static final String TYPE_VIDEO     = "https?://(?:www\\.)?imdb\\.com/video/[\\w\\-]+/(vi|screenplay/)\\d+";
    private static final String TYPE_PHOTO     = "https?://(?:www\\.)?imdb\\.com/.+/mediaviewer/.+";

    public ImDbCom(final PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void correctDownloadLink(final DownloadLink link) {
        if (link.getDownloadURL().matches(TYPE_VIDEO)) {
            link.setUrlDownload("http://www.imdb.com/video/screenplay/" + new Regex(link.getDownloadURL(), IDREGEX).getMatch(0));
        }
    }

    @Override
    public String getAGBLink() {
        return "http://www.imdb.com/help/show_article?conditions";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        this.dllink = null;
        this.server_issues = false;
        this.mature_content = false;
        setBrowserExclusive();
        br.setFollowRedirects(true);
        final String downloadURL = downloadLink.getDownloadURL();
        this.br.setLoadLimit(this.br.getLoadLimit() * 3);
        br.getPage(downloadURL);
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String ending = null;
        String filename = null;
        if (downloadURL.matches(TYPE_PHOTO)) {
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            boolean newWay = false;
            String json = this.br.getRegex("(\\{\"mediaViewerModel.+\\})").getMatch(0);
            if (json == null) {
                /* 2017-07-18 */
                json = this.br.getRegex("IMDbReactInitialState\\.push\\((\\{.*?\\})\\);\\s+").getMatch(0);
                newWay = true;
            }
            Map<String, Object> entries = getJsonMap(JavaScriptEngineFactory.jsonToJavaMap(json));
            if (newWay) {
                /* 2017-07-18 */
                final String id_main = new Regex(downloadLink.getDownloadURL(), "([a-z]{2}\\d+)/mediaviewer").getMatch(0);
                entries = getJsonMap(JavaScriptEngineFactory.walkJson(entries, "mediaviewer/galleries/" + id_main));
            } else {
                entries = getJsonMap(entries.get("mediaViewerModel"));
            }
            /* Now let's find the specific object ... */
            final String idright = new Regex(downloadLink.getDownloadURL(), "(rm\\d+)").getMatch(0);
            String idtemp = null;
            final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("allImages");
            for (final Object imageo : ressourcelist) {
                entries = getJsonMap(imageo);
                idtemp = (String) entries.get("id");
                if (idtemp != null && idtemp.equalsIgnoreCase(idright)) {
                    filename = (String) entries.get("altText");
                    dllink = (String) entries.get("src");
                    break;
                }
            }
            if (filename == null) {
                /* Fallback to url-filename */
                filename = new Regex(downloadURL, "imdb\\.com/[^/]+/(.+)").getMatch(0).replace("/", "_");
            }
            if (filename == null || dllink == null || !dllink.startsWith("http")) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (dllink.contains("@@")) {
                final String qualityPart = dllink.substring(dllink.lastIndexOf("@@") + 2);
                if (qualityPart != null) {
                    dllink = dllink.replace(qualityPart, "");
                }
            }
            filename = Encoding.htmlDecode(filename.trim());
            final String fid = new Regex(downloadLink.getDownloadURL(), "rm(\\d+)").getMatch(0);
            String artist = br.getRegex("itemprop=\\'url\\'>([^<>\"]*?)</a>").getMatch(0);
            if (artist != null) {
                filename = Encoding.htmlDecode(artist.trim()) + "_" + fid + "_" + filename;
            } else {
                filename = fid + "_" + filename;
            }
            ending = getFileNameExtensionFromString(dllink, ".jpg");
        } else {
            /*
             * get the fileName from main download link page because fileName on the /player subpage may be wrong
             */
            filename = br.getRegex("<title>(.*?) \\- IMDb</title>").getMatch(0);
            // br.getPage(downloadURL + "/player");
            if (br.containsHTML("(<title>IMDb Video Player: </title>|This video is not available\\.)")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (filename == null) {
                filename = br.getRegex("<title>IMDb Video Player: (.*?)</title>").getMatch(0);
            }
            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // br.getPage("http://www.imdb.com/video/imdb/" + new Regex(downloadLink.getDownloadURL(), IDREGEX).getMatch(0) +
            // "/player?uff=3");
            br.getPage("http://www.imdb.com/video/user/" + new Regex(downloadLink.getDownloadURL(), IDREGEX).getMatch(0) + "/imdb/single?vPage=1");
            this.mature_content = this.br.containsHTML("why=maturevideo");
            if (!this.mature_content) {
                final String json = this.br.getRegex("<script class=\"imdb\\-player\\-data\" type=\"text/imdb\\-video\\-player\\-json\">([^<>]+)<").getMatch(0);
                LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json);
                final ArrayList<Object> ressourcelist = (ArrayList<Object>) JavaScriptEngineFactory.walkJson(entries, "videoPlayerObject/video/videoInfoList");
                String dllink_http = null;
                String dllink_hls_master = null;
                for (final Object videoo : ressourcelist) {
                    entries = (LinkedHashMap<String, Object>) videoo;
                    final String dllink_temp = (String) entries.get("videoUrl");
                    if (dllink_temp == null || !dllink_temp.startsWith("http")) {
                        continue;
                    }
                    if (dllink_temp.contains(".m3u8")) {
                        dllink_hls_master = dllink_temp;
                    } else {
                        dllink_http = dllink_temp;
                    }
                }
                /* 2017-07-18: Prefer hls as it contains higher qualities */
                if (!StringUtils.isEmpty(dllink_hls_master)) {
                    dllink = dllink_hls_master;
                } else {
                    dllink = dllink_http;
                }
            }
            filename = filename.trim();
            ending = ".mp4";
        }
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ending);
        if (dllink != null && dllink.contains(".m3u8")) {
            /* hls */
            /* Access HLS master */
            br.getPage(dllink);
            final List<HlsContainer> allQualities = HlsContainer.getHlsQualities(this.br);
            final HlsContainer hlsBest = HlsContainer.findBestVideoByBandwidth(allQualities);
            HlsContainer finalCandidate = null;
            final String configuredResolution = getConfiguredVideoResolution();
            final long configuredBandwidth = getConfiguredVideoBandwidth();
            if (configuredResolution.equalsIgnoreCase("BEST")) {
                finalCandidate = hlsBest;
            } else {
                for (final HlsContainer hlstemp : allQualities) {
                    if (hlstemp.getResolution().equalsIgnoreCase(configuredResolution) && hlstemp.getBandwidth() == configuredBandwidth) {
                        logger.info("Found User-Selection");
                        finalCandidate = hlstemp;
                    }
                }
                if (finalCandidate == null) {
                    logger.info("Failed to find configured quality --> Falling back to BEST");
                    finalCandidate = hlsBest;
                } else {
                    logger.info("Quality selection successful");
                }
            }
            dllink = finalCandidate.getDownloadurl();
            checkFFProbe(downloadLink, "Download a HLS Stream");
            final HLSDownloader downloader = new HLSDownloader(downloadLink, br, dllink);
            final StreamInfo streamInfo = downloader.getProbe();
            if (streamInfo == null) {
                server_issues = true;
            } else {
                final long estimatedSize = downloader.getEstimatedSize();
                if (estimatedSize > 0) {
                    downloadLink.setDownloadSize(estimatedSize);
                }
            }
        } else if (this.dllink != null) {
            /* http */
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
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

    /* Simple wrapper due to unexpected LinkedHashMap/HashMap results. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getJsonMap(final Object jsono) {
        if (jsono instanceof Map) {
            return (Map<String, Object>) jsono;
        } else {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (this.mature_content) {
            /* Mature content --> Only viewable for registered users */
            logger.info("Video with mature content --> Only downloadable for registered users");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } else if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dllink.contains(".m3u8")) {
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            dl = new HLSDownloader(downloadLink, br, dllink);
            dl.startDownload();
        } else {
            int maxChunks = 0;
            if (downloadLink.getDownloadURL().matches(TYPE_VIDEO)) {
                maxChunks = 1;
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, maxChunks);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    private String getConfiguredVideoResolution() {
        final int selection = this.getPluginConfig().getIntegerProperty(SELECTED_VIDEO_FORMAT, 0);
        final String selectedFormat = FORMATS[selection];
        if (selectedFormat.contains("x")) {
            final String resolution = selectedFormat.split("@")[0];
            return resolution;
        } else {
            /* BEST selection */
            return selectedFormat;
        }
    }

    private long getConfiguredVideoBandwidth() {
        final int selection = this.getPluginConfig().getIntegerProperty(SELECTED_VIDEO_FORMAT, 0);
        final String selectedFormat = FORMATS[selection];
        if (selectedFormat.contains("x")) {
            final String bandwidth = selectedFormat.split("@")[1];
            return Long.parseLong(bandwidth);
        } else {
            /* BEST selection */
            return 0;
        }
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), SELECTED_VIDEO_FORMAT, FORMATS, "Select preferred quality:").setDefaultValue(0));
    }

    /* The list of qualities displayed to the user */
    private final String[] FORMATS               = new String[] { "BEST", "1920x1080@8735000", "1280x720@5632000", "1280x720@3480000", "704x396@2366000", "640x360@1638000", "640x360@1114000", "512x288@777000", "480x270@532000", "320x180@326000" };
    private final String   SELECTED_VIDEO_FORMAT = "SELECTED_VIDEO_FORMAT";

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}