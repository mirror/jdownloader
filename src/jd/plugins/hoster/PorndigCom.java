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

import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.config.PorndigComConfig;
import org.jdownloader.plugins.components.config.PorndigComConfig.PreferredQuality;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "porndig.com" }, urls = { "https?://(?:www\\.)?porndig\\.com/videos/(\\d+)/([a-z0-9\\-]+)\\.html" })
public class PorndigCom extends PluginForHost {
    public PorndigCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other: porn plugin
    /* Connection stuff */
    private static final boolean free_resume                   = true;
    private static final int     free_maxchunks                = 0;
    private static final int     free_maxdownloads             = -1;
    private static final String  PROPERTY_CHOSEN_VIDEO_QUALITY = "quality";
    private final String         PROPERTY_DIRECTURL            = "directurl";

    @Override
    public String getAGBLink() {
        return "https://www.porndig.com/tos";
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
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final String urlTitle = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(1);
        link.setFinalFileName(urlTitle.replace("-", " ") + ".mp4");
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String directurl = this.findDirecturl(br, link);
        link.setProperty(PROPERTY_DIRECTURL, directurl);
        return AvailableStatus.TRUE;
    }

    /** Returns directurl of video and does a deeper offline-check. */
    private String findDirecturl(final Browser br, final DownloadLink link) throws Exception {
        String directurl = this.getOfficialDownloadurlAndSetFilesize(br, link);
        if (directurl != null) {
            logger.info("Found official downloadurl");
        } else {
            logger.info("Failed to find official downloadurl --> Looking for stream downloadlink");
            final String url_embed = this.br.getRegex("<iframe[^<>]*?src=\"(http[^<>\"]+player/[^<>\"]+)\"").getMatch(0);
            if (url_embed == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(url_embed);
            if (br.containsHTML("(?i)>\\s*This video has been removed")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String jssource = br.getRegex("\"sources\"\\s*:\\s*(\\[.*?\\])").getMatch(0);
            if (jssource != null) {
                try {
                    Map<String, Object> entries = null;
                    long qualityBest = 0;
                    final List<Object> ressourcelist = (List) JavaScriptEngineFactory.jsonToJavaObject(jssource);
                    final PreferredQuality userPreferredQuality = getConfiguredQuality(link);
                    for (final Object videoo : ressourcelist) {
                        entries = (Map<String, Object>) videoo;
                        final String dllinkTmp = (String) entries.get("src");
                        final long qualityNumberTemp = ((Number) entries.get("res")).longValue();
                        final String qualityNameTmp = (String) entries.get("label");
                        if (StringUtils.isEmpty(dllinkTmp) || qualityNumberTemp == 0 || StringUtils.isEmpty(qualityNameTmp)) {
                            continue;
                        }
                        if (userPreferredQuality == this.gerPreferredQualityByString(qualityNameTmp, false)) {
                            logger.info("Found user preferred quality: " + qualityNameTmp);
                            directurl = dllinkTmp;
                            break;
                        }
                        if (qualityNumberTemp > qualityBest) {
                            qualityBest = qualityNumberTemp;
                            directurl = dllinkTmp;
                        }
                    }
                    if (!StringUtils.isEmpty(directurl)) {
                        logger.info("Handling for multiple video stream sources succeeded");
                    }
                } catch (final Throwable e) {
                    logger.log(e);
                    logger.info("BEST handling for multiple video source failed");
                }
            } else {
                // <video> <source
                int best = 0;
                String hls = null;
                final String[] sources = br.getRegex("<source[^>]+>").getColumn(-1);
                if (sources != null && sources.length > 0) {
                    for (final String source : sources) {
                        final String url = new Regex(source, "src\\s*=\\s*('|\")(.*?)\\1").getMatch(1);
                        if (url.contains(".m3u8")) {
                            // typically this is single entry.
                            hls = url;
                            continue;
                        }
                        final String label = new Regex(source, "label\\s*=\\s*('|\")(\\d+)p?\\1").getMatch(1);
                        if (label == null || url == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        final int p = Integer.parseInt(label);
                        if (best < p) {
                            directurl = url;
                            best = p;
                        }
                    }
                    // prefer non hls over hls, as hls core can't chunk at this stage.
                    if (directurl == null && hls != null) {
                        // hls has multiple qualities....
                        final Browser br2 = br.cloneBrowser();
                        br2.getPage(hls);
                        final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(br2));
                        if (hlsbest == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        directurl = hlsbest.getDownloadurl();
                    }
                }
            }
        }
        return directurl;
    }

    private String getOfficialDownloadurlAndSetFilesize(final Browser br, final DownloadLink link) throws PluginException {
        final String[] htmls = br.getRegex("<a href=\"[^\"]+/download/[^\"]+\"[^>]*><span class=\"link_name\">.*?</a>").getColumn(-1);
        long bestFilesize = -1;
        String bestDownloadurl = null;
        String bestqualityName = null;
        final PreferredQuality userPreferredQuality = getConfiguredQuality(link);
        for (final String html : htmls) {
            final String url = new Regex(html, "\"(https?://[^/]+/download/[^\"]+)\"").getMatch(0);
            String qualityName = new Regex(html, "class=\"link_name\">\\s*([A-Za-z0-9 ]+)").getMatch(0);
            final String filesizeStr = new Regex(html, "class=\"file_size\">(\\d+ [A-Z]{1,5})</span>").getMatch(0);
            if (url == null || qualityName == null || filesizeStr == null) {
                /* Skip invalid items */
                continue;
            }
            /* "UDH 4K " --> "UHD 4K" */
            qualityName = qualityName.trim();
            final long filesizeTmp = SizeFormatter.getSize(filesizeStr);
            if (filesizeTmp > bestFilesize) {
                bestFilesize = filesizeTmp;
                bestDownloadurl = url;
                bestqualityName = qualityName;
            }
            final PreferredQuality quality = this.gerPreferredQualityByString(qualityName, false);
            if (quality == userPreferredQuality) {
                logger.info("Found user preferred quality: " + qualityName);
                saveChosenQuality(link, qualityName);
                link.setDownloadSize(filesizeTmp);
                return url;
            }
        }
        if (bestDownloadurl == null) {
            /* Probably website layout changes and fix required. */
            logger.warning("Failed to find any official downloads");
            return null;
        } else {
            logger.info("Using BEST quality:" + bestqualityName);
            saveChosenQuality(link, bestqualityName);
            link.setDownloadSize(bestFilesize);
            return bestDownloadurl;
        }
    }

    private void saveChosenQuality(final DownloadLink link, final String qualityName) {
        if (qualityName != null && DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            link.setComment(qualityName);
        }
        link.setProperty(PROPERTY_CHOSEN_VIDEO_QUALITY, qualityName);
    }

    public PreferredQuality getChosenQualityIdentifier(final DownloadLink link) throws PluginException {
        if (link.hasProperty(PROPERTY_CHOSEN_VIDEO_QUALITY)) {
            final String quality = link.getStringProperty(PROPERTY_CHOSEN_VIDEO_QUALITY, null);
            return gerPreferredQualityByString(quality, true);
        } else {
            return null;
        }
    }

    private PreferredQuality gerPreferredQualityByString(final String quality, final boolean throwExceptionOnUnknownQuality) throws PluginException {
        if (quality == null) {
            return PreferredQuality.BEST;
        } else if ("270p".equals(quality)) {
            return PreferredQuality.Q270P;
        } else if ("360p".equals(quality)) {
            return PreferredQuality.Q360P;
        } else if ("540p".equals(quality)) {
            return PreferredQuality.Q540P;
        } else if ("720p".equals(quality)) {
            return PreferredQuality.Q720P;
        } else if ("1080p".equals(quality)) {
            return PreferredQuality.Q1080P;
        } else if ("UHD 4K".equals(quality)) {
            /* 4K iodentifier for official downloads */
            return PreferredQuality.UHD4K;
        } else if ("4K".equals(quality)) {
            /* 4K identifier for stream downloads */
            return PreferredQuality.UHD4K;
        } else if (throwExceptionOnUnknownQuality) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unknown quality:" + quality);
        } else {
            return null;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        final String dllink = link.getStringProperty(PROPERTY_DIRECTURL);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (dllink.contains("m3u8") || dllink.contains("//ahhls.") || dllink.contains("media=hls")) {
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, dllink);
            dl.startDownload();
        } else {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, free_resume, free_maxchunks);
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
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return PorndigComConfig.class;
    }

    protected PreferredQuality getConfiguredQuality(final DownloadLink link) throws PluginException {
        /* Return last used quality if available. */
        final PreferredQuality cfgquality = getChosenQualityIdentifier(link);
        if (cfgquality != null) {
            return cfgquality;
        } else {
            /* Return currently selected preferred quality. */
            return PluginJsonConfig.get(PorndigComConfig.class).getPreferredQuality();
        }
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
        link.removeProperty(PROPERTY_CHOSEN_VIDEO_QUALITY);
    }
}
