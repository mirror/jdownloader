//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class SrfCh extends PluginForHost {
    @SuppressWarnings("deprecation")
    public SrfCh(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "srf.ch" });
        ret.add(new String[] { "rts.ch" });
        ret.add(new String[] { "rsi.ch" });
        ret.add(new String[] { "rtr.ch" });
        ret.add(new String[] { "swissinfo.ch" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/play/(.+)\\?(?:id=[A-Za-z0-9\\-]+|urn=[A-Za-z0-9\\-:]+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "http://www.srf.ch/allgemeines/impressum";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        this.br.setAllowedResponseCodes(new int[] { 410 });
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 410) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<meta name=\"title\" content=\"([^<>]*?) \\- Play [^\"]+\"").getMatch(0);
        if (filename == null) {
            /* Fallback: Obtain filename from url */
            filename = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
            filename = filename.replace("-", " ");
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename.trim()) + ".mp4";
        filename = encodeUnicode(filename);
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        /* 2020-07-29: E.g. "blockReason" : "ENDDATE" or "blockReason" : "GEOBLOCK" */
        String blockReason = br.getRegex("geoblock\\s*?:\\s*?\\{\\s*?(?:audio|video)\\:\\s*?\"([^\"]+)\"").getMatch(0);
        final String domainpart = new Regex(link.getDownloadURL(), "https?://(?:www\\.)?([A-Za-z0-9\\.]+)\\.ch/").getMatch(0);
        final String videoid = new Regex(link.getDownloadURL(), "\\?id=([A-Za-z0-9\\-]+)").getMatch(0);
        final String channelname = convertDomainPartToShortChannelName(domainpart);
        /* xml also possible: http://il.srgssr.ch/integrationlayer/1.0/<channelname>/srf/video/play/<videoid>.xml */
        final boolean useV2 = true;
        /* 2020-09-09: New URLs will contain this parameter */
        String urn = UrlQuery.parse(link.getPluginPatternMatcher()).get("urn");
        if (StringUtils.isEmpty(urn)) {
            /* E.g. for older URLs --> We have to generate that parameter on our own. */
            urn = "urn:" + channelname + ":video:" + videoid;
        }
        if (useV2) {
            this.br.getPage("https://il.srgssr.ch/integrationlayer/2.0/mediaComposition/byUrn/" + urn + ".json?onlyChapters=true&vector=portalplay");
        } else {
            this.br.getPage("http://il.srgssr.ch/integrationlayer/1.0/ue/" + channelname + "/video/play/" + videoid + ".json");
        }
        String url_http_download = null;
        String url_hls_master = null;
        String url_rtmp = null;
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        ArrayList<Object> ressourcelist;
        try {
            if (useV2) {
                final Map<String, String> hlsMap = new HashMap<String, String>();
                final Map<String, String> mp4Map = new HashMap<String, String>();
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "chapterList/{0}");
                if (StringUtils.isEmpty(blockReason)) {
                    /* 2020-07-29: New */
                    blockReason = (String) entries.get("blockReason");
                }
                // final String id = (String) entries.get("id");
                ressourcelist = (ArrayList<Object>) entries.get("resourceList");
                for (final Object ressourceO : ressourcelist) {
                    entries = (LinkedHashMap<String, Object>) ressourceO;
                    final String protocol = (String) entries.get("protocol");
                    if (protocol.equals("HTTP")) {
                        final String url = (String) entries.get("url");
                        final String quality = (String) entries.get("quality");
                        if (StringUtils.isNotEmpty(url)) {
                            mp4Map.put(quality, url);
                        }
                    } else if (protocol.equals("HLS")) {
                        final String url = (String) entries.get("url");
                        final String quality = (String) entries.get("quality");
                        if (StringUtils.isNotEmpty(url)) {
                            hlsMap.put(quality, url);
                        }
                    } else {
                        /* Skip unsupported protocol */
                        logger.info("Skipping protocol: " + entries);
                        continue;
                    }
                }
                if (hlsMap.size() > 0) {
                    if (hlsMap.containsKey("HD")) {
                        url_hls_master = hlsMap.get("HD");
                    } else if (hlsMap.containsKey("SD")) {
                        url_hls_master = hlsMap.get("SD");
                    } else {
                        logger.info("unknown qualities(hls):" + hlsMap);
                        url_hls_master = hlsMap.entrySet().iterator().next().getValue();
                    }
                }
                if (mp4Map.size() > 0) {
                    if (mp4Map.containsKey("HD")) {
                        url_http_download = mp4Map.get("HD");
                    } else if (hlsMap.containsKey("SD")) {
                        url_http_download = mp4Map.get("SD");
                    } else {
                        logger.info("unknown qualities(mp4):" + mp4Map);
                        url_http_download = mp4Map.entrySet().iterator().next().getValue();
                    }
                }
                if (!StringUtils.isEmpty(url_hls_master)) {
                    /* Sign URL */
                    String acl = new Regex(url_hls_master, "https?://[^/]+(/.+\\.csmil)").getMatch(0);
                    if (acl == null) {
                        logger.warning("Failed to find acl");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    acl += "/*";
                    br.getPage("https://player.rts.ch/akahd/token?acl=" + acl);
                    entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                    entries = (LinkedHashMap<String, Object>) entries.get("token");
                    /* 2019-08-09: TODO: Cleanup this encoding mess ... */
                    String authparams = (String) entries.get("authparams");
                    if (StringUtils.isEmpty(authparams)) {
                        logger.warning("Failed to find authparams");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    authparams = new Regex(authparams, "hdnts=(.+)").getMatch(0);
                    authparams = URLEncode.encodeURIComponent(authparams);
                    authparams = authparams.replace("*", "%2A");
                    url_hls_master += "&hdnts=" + authparams;
                    final String param_caption = new Regex(url_hls_master, "caption=([^\\&]+)").getMatch(0);
                    if (param_caption != null) {
                        String param_caption_new = param_caption;
                        param_caption_new = Encoding.htmlDecode(param_caption_new);
                        param_caption_new = URLEncode.encodeURIComponent(param_caption_new);
                        param_caption_new = param_caption_new.replace("%3D", "=");
                        url_hls_master = url_hls_master.replace(param_caption, param_caption_new);
                    }
                }
            } else {
                entries = (LinkedHashMap<String, Object>) entries.get("Video");
                LinkedHashMap<String, Object> temp = null;
                /* Try to find http downloadurl (not always available) */
                try {
                    ressourcelist = (ArrayList) JavaScriptEngineFactory.walkJson(entries, "Downloads/Download");
                    for (final Object streamtypeo : ressourcelist) {
                        temp = (LinkedHashMap<String, Object>) streamtypeo;
                        final String protocol = (String) temp.get("@protocol");
                        if (protocol == null) {
                            continue;
                        }
                        if (protocol.equals("HTTP")) {
                            url_http_download = this.findBestQualityForStreamtype(temp);
                            break;
                        }
                    }
                } catch (final Exception e) {
                    logger.log(e);
                }
                /* Try to find hls master (usually available) */
                try {
                    ressourcelist = (ArrayList) JavaScriptEngineFactory.walkJson(entries, "Playlists/Playlist");
                    for (final Object streamtypeo : ressourcelist) {
                        temp = (LinkedHashMap<String, Object>) streamtypeo;
                        final String protocol = (String) temp.get("@protocol");
                        if (protocol == null) {
                            continue;
                        }
                        if (protocol.equals("HTTP-HLS")) {
                            url_hls_master = this.findBestQualityForStreamtype(temp);
                            break;
                        }
                    }
                } catch (final Exception e) {
                    logger.log(e);
                }
                /* Try to find rtmp url (sometimes available, sometimes the only streamtype available) */
                try {
                    ressourcelist = (ArrayList) JavaScriptEngineFactory.walkJson(entries, "Playlists/Playlist");
                    for (final Object streamtypeo : ressourcelist) {
                        temp = (LinkedHashMap<String, Object>) streamtypeo;
                        final String protocol = (String) temp.get("@protocol");
                        if (protocol == null) {
                            continue;
                        }
                        if (protocol.equals("RTMP")) {
                            url_rtmp = findBestQualityForStreamtype(temp);
                            break;
                        }
                    }
                } catch (final Exception e) {
                    logger.log(e);
                }
            }
        } catch (final Exception e) {
            if (!StringUtils.isEmpty(blockReason)) {
                contentBlocked(blockReason);
            } else {
                throw e;
            }
        }
        if (url_http_download == null && url_hls_master == null && url_rtmp == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (url_http_download != null) {
            /* Prefer http download */
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, url_http_download, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                if (br.getHttpConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 3 * 60 * 1000l);
                } else if (br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 3 * 60 * 1000l);
                }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
            }
            this.dl.startDownload();
        } else if (url_hls_master != null) {
            /* Prefer hls over rtmp but sometimes only one of both is available. */
            /* 2019-08-09: These headers are not necessarily needed */
            logger.info("Downloading hls");
            br.getHeaders().put("Accept", "*/*");
            br.getHeaders().put("Origin", "https://player.rts.ch");
            br.getHeaders().put("Sec-Fetch-Site", "cross-site");
            this.br.getPage(url_hls_master);
            /* 2019-08-09: This will also return error 403 for wrongly signed URLs! */
            if (this.br.getHttpConnection().getResponseCode() == 403) {
                contentBlocked(blockReason);
            }
            // try {
            // this.br.cloneBrowser().getPage("http://il.srgssr.ch/integrationlayer/1.0/ue/" + channelname + "/video/" + videoid +
            // "/clicked.xml");
            // } catch (final Throwable e) {
            // }
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
            if (hlsbest == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String url_hls = hlsbest.getDownloadurl();
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, url_hls);
            dl.startDownload();
        } else {
            logger.info("Downloading rtmp");
            final String app = "ondemand";
            final String playpath = new Regex(url_rtmp, app + "/" + "(.+)\\.flv$").getMatch(0);
            try {
                dl = new RTMPDownload(this, link, url_rtmp);
            } catch (final NoClassDefFoundError e) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "RTMPDownload class missing");
            }
            /* Setup rtmp connection */
            jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
            rtmp.setPageUrl(link.getDownloadURL());
            rtmp.setUrl(url_rtmp);
            rtmp.setPlayPath(playpath);
            rtmp.setApp("ondemand");
            rtmp.setFlashVer("WIN 18,0,0,232");
            /* Hash is wrong (static) but server will accept it anyways so good enough for us for now :) */
            rtmp.setSwfUrl("http://tp.srgssr.ch/assets/lib/srg-technical-player/f2ff86c6a1f230060e46122086a7326f-player.swf");
            rtmp.setResume(true);
            ((RTMPDownload) dl).startDownload();
        }
    }

    private void contentBlocked(final String blockReason) throws PluginException {
        final String userReadableBlockedReason;
        if (StringUtils.isEmpty(blockReason)) {
            userReadableBlockedReason = "Unknown";
        } else if (blockReason.equalsIgnoreCase("ENDDATE")) {
            userReadableBlockedReason = "Content is not available anymore (expired)";
        } else if (blockReason.equalsIgnoreCase("GEOBLOCK")) {
            userReadableBlockedReason = "GEO-blocked";
        } else {
            userReadableBlockedReason = "Unknown reason:" + blockReason;
        }
        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Content not downloadable because " + userReadableBlockedReason);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private String findBestQualityForStreamtype(LinkedHashMap<String, Object> temp) {
        final String[] possibleQualities = { "HD", "HQ", "MQ", "SQ", "SD" };
        String best_quality = null;
        ArrayList<Object> ressourcelist2 = (ArrayList) temp.get("url");
        for (final String possible_quality : possibleQualities) {
            for (final Object hlso : ressourcelist2) {
                temp = (LinkedHashMap<String, Object>) hlso;
                final String quality = (String) temp.get("@quality");
                if (quality == null) {
                    continue;
                }
                if (quality.equals(possible_quality)) {
                    /* We found the best available quality */
                    best_quality = (String) temp.get("text");
                    return best_quality;
                }
            }
        }
        return best_quality;
    }

    private String convertDomainPartToShortChannelName(final String input) {
        final String output;
        if (input.equals("play.swissinfo")) {
            output = "swi";
        } else {
            output = input;
        }
        return output;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}