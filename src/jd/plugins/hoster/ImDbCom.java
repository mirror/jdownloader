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
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imdb.com" }, urls = { "https?://(?:www\\.)?imdb\\.com/((video|videoplayer)/([\\w\\-]+/)?vi\\d+|[A-Za-z]+/[a-z]{2}\\d+/mediaviewer/rm\\d+)" })
public class ImDbCom extends PluginForHost {
    private String              dllink         = null;
    private boolean             server_issues  = false;
    private boolean             mature_content = false;
    private static final String IDREGEX        = "(vi\\d+)$";
    private static final String TYPE_VIDEO     = "https?://(?:www\\.)?imdb\\.com/(?:video|videoplayer)/[\\w\\-]+/(vi|screenplay/)\\d+";
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
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "(\\d+)$").getMatch(0);
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
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.dllink = null;
        this.server_issues = false;
        this.mature_content = false;
        setBrowserExclusive();
        br.setFollowRedirects(true);
        this.br.setLoadLimit(this.br.getLoadLimit() * 3);
        br.getPage(link.getPluginPatternMatcher());
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String ending = null;
        String filename = null;
        if (link.getPluginPatternMatcher().matches(TYPE_PHOTO)) {
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /* 2020-11-03 */
            final String json = br.getRegex("id=\"__NEXT_DATA__\" type=\"application/json\">(\\{.*?)</script>").getMatch(0);
            // final String id_main = new Regex(link.getDownloadURL(), "([a-z]{2}\\d+)/mediaviewer").getMatch(0);
            Map<String, Object> entries = JSonStorage.restoreFromString(json, TypeRef.HASHMAP);
            /* Now let's find the specific object ... */
            final String idright = new Regex(link.getDownloadURL(), "(rm\\d+)").getMatch(0);
            String idtemp = null;
            final ArrayList<Object> ressourcelist = (ArrayList<Object>) JavaScriptEngineFactory.walkJson(entries, "props/urqlState/{0}/data/title/images/edges");
            for (final Object imageo : ressourcelist) {
                entries = getJsonMap(imageo);
                entries = getJsonMap(entries.get("node"));
                idtemp = (String) entries.get("id");
                if (idtemp != null && idtemp.equalsIgnoreCase(idright)) {
                    filename = (String) JavaScriptEngineFactory.walkJson(entries, "titles/{0}/titleText/text");
                    dllink = (String) entries.get("url");
                    break;
                }
            }
            if (filename == null) {
                /* Fallback to fid */
                filename = this.getFID(link);
            }
            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (dllink == null || !dllink.startsWith("http")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /* 2020-02-17: Not required anymore? This may cripple final downloadurls! */
            // if (dllink.contains("@@")) {
            // final String qualityPart = dllink.substring(dllink.lastIndexOf("@@") + 2);
            // if (qualityPart != null) {
            // dllink = dllink.replace(qualityPart, "");
            // }
            // }
            filename = Encoding.htmlDecode(filename.trim());
            final String fid = new Regex(link.getDownloadURL(), "rm(\\d+)").getMatch(0);
            String artist = br.getRegex("itemprop=\\'url\\'>([^<>\"]*?)</a>").getMatch(0);
            if (artist != null) {
                filename = Encoding.htmlDecode(artist.trim()) + "_" + fid + "_" + filename;
            } else {
                filename = fid + "_" + filename;
            }
            ending = getFileNameExtensionFromString(dllink, ".jpg");
        } else {
            /* Video */
            /*
             * get the fileName from main download link page because fileName on the /player subpage may be wrong
             */
            // br.getPage(downloadURL + "/player");
            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(<title>IMDb Video Player: </title>|This video is not available\\.)")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
            final String json = br.getRegex("\\.querySelector\\(\\'#imdb-video-root-[^\\']+'\\),\\s*(\\{.+\\})\\];").getMatch(0);
            Map<String, Object> entries = JSonStorage.restoreFromString(json, TypeRef.HASHMAP);
            /* json inside json */
            final String json2 = (String) JavaScriptEngineFactory.walkJson(entries, "playbackData/{0}");
            final List<Object> videoObjects = JSonStorage.restoreFromString(json2, TypeRef.LIST);
            String dllink_http = null;
            String dllink_hls_master = null;
            for (final Object videoO : videoObjects) {
                entries = (Map<String, Object>) videoO;
                final List<Object> qualitiesO = (List<Object>) entries.get("videoLegacyEncodings");
                for (final Object qualityO : qualitiesO) {
                    entries = (Map<String, Object>) qualityO;
                    final String mimeType = (String) entries.get("mimeType");
                    final String url = (String) entries.get("url");
                    // final String definition = (String) entries.get("definition"); // E.g. AUTO, 480p, SD
                    if (StringUtils.isEmpty(mimeType) || StringUtils.isEmpty(url)) {
                        /* Skip invalid items */
                        continue;
                    }
                    if (mimeType.equalsIgnoreCase("video/mp4")) {
                        dllink_http = url;
                    } else if (mimeType.equalsIgnoreCase("application/x-mpegurl")) {
                        dllink_hls_master = url;
                    } else {
                        logger.info("Unsupported mimeType: " + mimeType);
                    }
                }
                /* 2020-11-03: Stop after first element - we expect this to contain only one element anyways! */
                break;
            }
            if (filename == null) {
                filename = this.getFID(link);
            } else {
                filename = this.getFID(link) + "_" + filename;
            }
            if (!StringUtils.isEmpty(dllink_hls_master)) {
                dllink = dllink_hls_master;
            } else {
                dllink = dllink_http;
            }
            filename = filename.trim();
            ending = ".mp4";
        }
        link.setFinalFileName(Encoding.htmlDecode(filename) + ending);
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
            checkFFProbe(link, "Download a HLS Stream");
            final HLSDownloader downloader = new HLSDownloader(link, br, dllink);
            final StreamInfo streamInfo = downloader.getProbe();
            if (streamInfo == null) {
                server_issues = true;
            } else {
                final long estimatedSize = downloader.getEstimatedSize();
                if (estimatedSize > 0) {
                    link.setDownloadSize(estimatedSize);
                }
            }
        } else if (this.dllink != null) {
            /* http */
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    link.setDownloadSize(con.getLongContentLength());
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