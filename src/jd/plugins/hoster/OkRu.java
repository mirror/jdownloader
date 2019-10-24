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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "ok.ru" }, urls = { "https?://(?:[A-Za-z0-9]+\\.)?ok\\.ru/(?:video|videoembed|web-api/video/moviePlayer|live)/(\\d+(-\\d+)?)" })
public class OkRu extends PluginForHost {
    public OkRu(PluginWrapper wrapper) {
        super(wrapper);
        this.setConfigElements();
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
        return new Regex(link.getPluginPatternMatcher(), "/(\\d+(-\\d+)?)$").getMatch(0);
    }

    /* DEV NOTES */
    // Porn_plugin
    // Tags:
    // protocol: no https
    // other:
    /* Connection stuff */
    private static final boolean free_resume         = true;
    private static final int     free_maxchunks      = 0;
    private static final int     free_maxdownloads   = -1;
    private final String         PREFER_480P         = "PREFER_480P";
    private String               dllink              = null;
    private boolean              download_impossible = false;
    private boolean              paidContent         = false;

    public static void prepBR(final Browser br) {
        /* Use mobile website to get http urls. */
        /* 2019-10-15: Do not use mobile User-Agent anymore! */
        // br.getHeaders().put("User-Agent", "Mozilla/5.0 (Linux; U; Android 2.2.1; en-us; Nexus One Build/FRG83) AppleWebKit/533.1 (KHTML,
        // like Gecko) Version/4.0 Mobile");
        // with jd default lang we get non english (homepage) or non russian responses (mobile)
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        br.setFollowRedirects(true);
    }

    public static LinkedHashMap<String, Object> getFlashVars(final Browser br) {
        String playerJsonSrc = br.getRegex("data-module=\"OKVideo\" data-options=\"([^<>]+)\" data-player-container-id=").getMatch(0);
        if (playerJsonSrc == null) {
            return null;
        }
        try {
            playerJsonSrc = playerJsonSrc.replace("&quot;", "\"");
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(playerJsonSrc);
            entries = (LinkedHashMap<String, Object>) entries.get("flashvars");
            String metadataUrl = (String) entries.get("metadataUrl");
            String metadataSrc = (String) entries.get("metadata");
            if (StringUtils.isEmpty(metadataSrc) && metadataUrl != null) {
                metadataUrl = Encoding.htmlDecode(metadataUrl);
                br.postPage(metadataUrl, "st.location=AutoplayLayerMovieRBlock%2FanonymVideo%2Fanonym");
                metadataSrc = br.toString();
            }
            // final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("");
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(metadataSrc);
            return entries;
        } catch (final Throwable e) {
            return null;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        dllink = null;
        download_impossible = false;
        this.setBrowserExclusive();
        prepBR(this.br);
        final String fid = this.getFID(link);
        br.getPage("https://" + this.getHost() + "/video/" + fid);
        /* Offline or private video */
        if (isOffline(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = null;
        LinkedHashMap<String, Object> entries = getFlashVars(this.br);
        if (entries != null) {
            filename = (String) JavaScriptEngineFactory.walkJson(entries, "movie/title");
            final Object paymentInfo = entries.get("paymentInfo");
            if (paymentInfo != null) {
                /* User needs account and has to pay to download/view such content. */
                this.paidContent = true;
            } else {
                final String[] qualities = { "hd", "sd", "low", "lowest", "mobile" };
                final String lowQualityName = "sd";
                final boolean preferLowQuality = this.getPluginConfig().getBooleanProperty(PREFER_480P, false);
                LinkedHashMap<String, Object> httpQualityInfo = null;
                final Object httpQualitiesO = entries.get("videos");
                if (httpQualitiesO != null) {
                    int maxQuality = 0;
                    final ArrayList<Object> httpQualities = (ArrayList<Object>) httpQualitiesO;
                    for (final Object httpQ : httpQualities) {
                        httpQualityInfo = (LinkedHashMap<String, Object>) httpQ;
                        final String quality = (String) httpQualityInfo.get("name");
                        final String url = (String) httpQualityInfo.get("url");
                        if (StringUtils.isEmpty(quality) || StringUtils.isEmpty(url)) {
                            continue;
                        }
                        final int currentQuality;
                        if (quality.equalsIgnoreCase("hd")) {
                            currentQuality = 100;
                        } else if (quality.equalsIgnoreCase("sd")) {
                            currentQuality = 80;
                        } else if (quality.equalsIgnoreCase("low")) {
                            currentQuality = 60;
                        } else if (quality.equalsIgnoreCase("lowest")) {
                            currentQuality = 40;
                        } else {
                            /* Mobile or other > 0 */
                            currentQuality = 1;
                        }
                        if (preferLowQuality && quality.equalsIgnoreCase(lowQualityName)) {
                            dllink = url;
                            break;
                        } else if (currentQuality > maxQuality) {
                            dllink = url;
                            maxQuality = currentQuality;
                        }
                    }
                }
                if (StringUtils.isEmpty(dllink)) {
                    /* Prefer http - only use HLS if http is not available! */
                    dllink = (String) entries.get("hlsManifestUrl");
                }
                // final String statusText = (String) JavaScriptEngineFactory.walkJson(entries, "movie/statusText");
                // if ("Not paid".equalsIgnoreCase(statusText)) {
                // /* User needs account and has to pay to download/view such content. */
                // this.paidContent = true;
                // }
            }
        }
        if (StringUtils.isEmpty(filename)) {
            /* Fallback */
            filename = fid;
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        if (br.containsHTML("class=\"fierr\"") || br.containsHTML(">Access to this video is restricted")) {
            if (!link.isNameSet()) {
                link.setName(filename + ".mp4");
            }
            download_impossible = true;
            return AvailableStatus.TRUE;
        }
        // final String url_quality = new Regex(dllink, "(st.mq=\\d+)").getMatch(0);
        // if (url_quality != null) {
        // /* st.mq: 2 = 480p (mobile format), 3=?, 4=? 5 = highest */
        // if (this.getPluginConfig().getBooleanProperty(PREFER_480P, false)) {
        // dllink = dllink.replace(url_quality, "st.mq=2");
        // } else {
        // /* Prefer highest quality available */
        // dllink = dllink.replace(url_quality, "st.mq=5");
        // }
        // }
        final String ext = ".mp4";
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        link.setFinalFileName(filename);
        /* Only check filesize during linkcheck to avoid double-http-requests */
        if (!StringUtils.isEmpty(dllink) && !isDownload) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (con.isContentDisposition()) {
                    link.setDownloadSize(con.getLongContentLength());
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

    public static boolean isOffline(final Browser br) throws IOException {
        // class=\"empty\" is NOT an indication for an offline video!
        if (br.containsHTML("error-page\"") || br.getHttpConnection().getResponseCode() == 404) {
            return true;
        }
        /* Offline or private video */
        if (br.containsHTML(">Access to this video has been restricted|>Access to the video has been restricted") || br.getURL().contains("/main/st.redirect/")) {
            return true;
        }
        if (br.getURL().contains("?")) {
            /* Redirect --> Offline! */
            return true;
        }
        // video blocked | video not found
        if (br.containsHTML(">Видеоролик заблокирован<|>Видеоролик не найден<")) {
            return true;
        }
        if (br.containsHTML(">Video has not been found</div") || br.containsHTML(">Video hasn't been found</div")) {
            return true;
        }
        // offline due to copyright claim
        if (br.containsHTML("<div class=\"empty\"")) {
            final String vid = new Regex(br.getURL(), "(\\d+)$").getMatch(0);
            // mobile page .... get standard browser
            final Browser br2 = new Browser();
            br2.setFollowRedirects(true);
            br2.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
            br2.getPage(br.createGetRequest("/video/" + vid));
            if (br2.containsHTML(">Video has been blocked due to author's rights infingement<|>The video is blocked<|>Group, where this video was posted, has not been found")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getAGBLink() {
        return "https://ok.ru/";
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        if (download_impossible) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download impossible - video corrupted?", 3 * 60 * 60 * 1000l);
        } else if (this.paidContent) {
            throw new AccountRequiredException();
        } else if (StringUtils.isEmpty(dllink)) {
            logger.warning("Failed to find final downloadurl");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dllink.contains(".m3u8")) {
            br.getPage(dllink);
            HlsContainer chosenQuality = null;
            final List<HlsContainer> qualities = HlsContainer.getHlsQualities(br);
            for (final HlsContainer quality : qualities) {
                final int bandwidth = quality.getBandwidth();
                final boolean isSDQuality = bandwidth > 1000000 && bandwidth < 2000000;
                if (this.getPluginConfig().getBooleanProperty(PREFER_480P, false) && isSDQuality) {
                    chosenQuality = quality;
                    break;
                }
            }
            if (chosenQuality == null) {
                chosenQuality = HlsContainer.findBestVideoByBandwidth(qualities);
            }
            dllink = chosenQuality.getDownloadurl();
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, dllink);
            dl.startDownload();
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
            if (!dl.getConnection().isContentDisposition()) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
            }
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, this.getPluginConfig(), PREFER_480P, "Prefer download of 480p version instead of the highest video quality?").setDefaultValue(false));
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
