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
package jd.plugins.decrypter;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.logging2.LogInterface;
import org.jdownloader.controlling.ffmpeg.json.Stream;
import org.jdownloader.controlling.ffmpeg.json.StreamInfo;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.GenericM3u8;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "atv.at" }, urls = { "https?://(?:www\\.)?atv\\.at/([a-z0-9\\-_]+/[a-z0-9\\-_]+/(?:d|v)\\d+/|tv/[a-z0-9\\-]+/[a-z0-9\\-]+/[a-z0-9\\-]+/[a-z0-9\\-]+)" })
public class AtvAt extends PluginForDecrypt {
    public AtvAt(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String REGEX_HTTP_STREAMING = "(https?://.+/hbbtv)/(\\d+)(_\\d+)?\\.mp4";
    private static final String TYPE_OLD             = "(?i)https?://(?:www\\.)?atv\\.at/([a-z0-9\\-_]+)/([a-z0-9\\-_]+)/(?:d|v)(\\d+)/";
    private static final String TYPE_NEW             = "(?i)https?://(?:www\\.)?atv\\.at/tv/([a-z0-9\\-]+)/([a-z0-9\\-]+)/([a-z0-9\\-]+)/([a-z0-9\\-]+)";

    /**
     * Important note: Via browser the videos are streamed via RTSP.
     *
     * Old URL: http://atv.at/binaries/asset/tvnext_clip/496790/video
     *
     *
     * --> http://b2b.atv.at/binaries/asset/tvnext_clip/496790/video
     */
    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        String seriesname = null;
        String episodename = null;
        String url_seriesname;
        String url_episodename;
        String json_source = null;
        String seasonnumberStr = null;
        String episodenumberStr = null;
        Map<String, Object> entries;
        List<Object> parts = null;
        boolean possiblyGeoBlocked = false;
        if (param.getCryptedUrl().matches(TYPE_OLD)) {
            final Regex linkinfo = new Regex(param.getCryptedUrl(), TYPE_OLD);
            url_seriesname = linkinfo.getMatch(0);
            url_episodename = linkinfo.getMatch(1);
        } else {
            final Regex linkinfo = new Regex(parameter, TYPE_NEW);
            url_seriesname = linkinfo.getMatch(0);
            url_episodename = linkinfo.getMatch(3);
            final String urlSeasonInfo = linkinfo.getMatch(1);
            final String urlEpisodeinfo = linkinfo.getMatch(2);
            seasonnumberStr = new Regex(urlSeasonInfo, "staffel-(\\d+)").getMatch(0);
            episodenumberStr = new Regex(urlEpisodeinfo, "episode-(\\d+)").getMatch(0);
        }
        br.getPage(parameter);
        br.followRedirect();
        if (br.getHttpConnection().getResponseCode() == 404) {
            logger.info("Link offline (404 error): " + parameter);
            final DownloadLink offline = this.createOfflinelink(parameter);
            offline.setFinalFileName(url_seriesname + "_" + url_episodename + ".mp4");
            decryptedLinks.add(offline);
            return decryptedLinks;
        } else if (br.containsHTML("is_geo_ip_blocked\\&quot;:true")) {
            /*
             * We can get the direct links of geo blocked videos anyways - also, this variable only tells if a video is geo blocked at all -
             * this does not necessarily mean that it is blocked in the users'country!
             */
            logger.info("Video might not be available in your country [workaround might be possible though]: " + parameter);
            possiblyGeoBlocked = true;
        }
        br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
        /* Get filename base information */
        try {
            seriesname = this.br.getRegex("\">zurück zu ([^<>\"]*?)<span class=\"ico ico_close\"").getMatch(0);
            episodename = this.br.getRegex("property=\"og:title\" content=\"Folge \\d+ \\- ([^<>\"]*?)\"").getMatch(0);
            episodenumberStr = br.getRegex("class=\"headline\">Folge (\\d+)</h4>").getMatch(0);
            json_source = br.getRegex("var\\s*flashPlayerOptions\\s*=\\s*\"(.*?)\"").getMatch(0);
            if (json_source == null) {
                json_source = br.getRegex("<div class=\"jsb_ jsb_video/FlashPlayer\" data\\-jsb=\"([^\"<>]+)\">").getMatch(0);
            }
            if (json_source != null) {
                json_source = Encoding.htmlDecode(json_source);
                entries = JavaScriptEngineFactory.jsonToJavaMap(json_source);
                entries = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "config/initial_video");
                parts = (List<Object>) entries.get("parts");
            } else {
                /* New 2021-04-15 */
                final String partsJson = br.getRegex("var playlist\\s*=\\s*(\\[.*?\\]);").getMatch(0);
                parts = restoreFromString(partsJson, TypeRef.LIST);
            }
        } catch (final Throwable ignore) {
            logger.log(ignore);
            /* Offline or plugin failure */
            logger.info("There seems to be no downloadable content: " + parameter);
            final DownloadLink offline = this.createOfflinelink(parameter);
            offline.setFinalFileName(url_seriesname + "_" + url_episodename);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        final String urlSeriesnameRemove = new Regex(url_seriesname, "((?:\\-)?staffel\\-\\d+)").getMatch(0);
        final String urlEpisodenameRemove = new Regex(url_episodename, "((?:\\-)?folge\\-\\d+)").getMatch(0);
        short seasonnumber = -1;
        short episodenumber = -1;
        final DecimalFormat df = new DecimalFormat("00");
        if (seriesname == null) {
            /* Fallback to URL information */
            seriesname = url_seriesname.replace("-", " ");
        }
        if (episodename == null) {
            /* Fallback to URL information */
            episodename = url_episodename.replace("-", " ");
        }
        if (urlSeriesnameRemove != null) {
            /* Clean url_seriesname */
            if (seasonnumberStr == null) {
                seasonnumberStr = new Regex(urlSeriesnameRemove, "(\\d+)$").getMatch(0);
            }
            url_seriesname = url_seriesname.replace(urlSeriesnameRemove, "");
        }
        if (urlEpisodenameRemove != null) {
            if (episodenumberStr == null) {
                episodenumberStr = new Regex(urlEpisodenameRemove, "(\\d+)$").getMatch(0);
            }
            /* Clean url_episodename! */
            url_episodename = url_episodename.replace(urlEpisodenameRemove, "");
        }
        if (seasonnumberStr != null && episodenumberStr != null) {
            seasonnumber = Short.parseShort(seasonnumberStr);
            episodenumber = Short.parseShort(episodenumberStr);
        }
        String hybrid_name = seriesname + "_";
        if (seasonnumber > 0 && episodenumber > 0) {
            /* That should be given in most of all cases! */
            hybrid_name += "S" + df.format(seasonnumber) + "E" + df.format(episodenumber) + "_";
        }
        hybrid_name += episodename;
        hybrid_name = Encoding.htmlDecode(hybrid_name);
        hybrid_name = decodeUnicode(hybrid_name);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(hybrid_name);
        int partCounter = 1;
        /* First check for HD HLS URLs */
        final String playlist = br.getRegex("(var\\s*playlist\\s*=\\s*\\[\\s*\\{.*?\\}\\s*\\];\\s*)").getMatch(0);
        final String playlist2 = br.getRegex("(var contentResource\\s*=\\s*\\[.*?\\]);\\s+").getMatch(0);
        final String hdHLSRegEx = "(https://[^/]+/\\d{4}/\\d{2}/HD/\\d+/index\\.m3u8)";
        String[] hdHLSParts = new Regex(playlist, hdHLSRegEx).getColumn(0);
        if ((hdHLSParts == null || hdHLSParts.length == 0) && playlist2 != null) {
            /* 2021-07-05 */
            hdHLSParts = new Regex(playlist2, hdHLSRegEx).getColumn(0);
        }
        if (hdHLSParts != null && hdHLSParts.length > 0) {
            logger.info("Found HD HLS URLs");
            /* Assume that this is our best quality --> Add best of these for each part */
            for (final String hdHLSPart : hdHLSParts) {
                logger.info("Crawling HD HLS quality " + partCounter + " / " + hdHLSParts.length);
                br.getPage(hdHLSPart);
                final List<HlsContainer> hlsContainers = HlsContainer.getHlsQualities(this.br);
                final HlsContainer best = HlsContainer.findBestVideoByBandwidth(hlsContainers);
                final DownloadLink link = this.createDownloadlink(GenericM3u8.createURLForThisPlugin(best.getDownloadurl()));
                String finalname = hybrid_name + "_";
                finalname += "hlsHD_part_";
                finalname += df.format(partCounter) + "_";
                finalname += best.getHeight() + "p";
                finalname += ".mp4";
                link.setFinalFileName(finalname);
                link.setAvailable(true);
                link.setContentUrl(parameter);
                link._setFilePackage(fp);
                decryptedLinks.add(link);
                distribute(link);
                partCounter++;
                if (this.isAbort()) {
                    break;
                }
            }
            return decryptedLinks;
        }
        logger.info("Failed to find HD HLS URLs --> Continuing with older handling");
        /* Reset variable to recycle it */
        partCounter = 1;
        /** Collect existing http URLs */
        String randomHTTPUrl = null;
        List<Object> sources = null;
        ArrayList<Integer> partsWithoutHTTPQuality = new ArrayList<Integer>();
        HashMap<Integer, String> partsWithHTTPQuality = new HashMap<Integer, String>();
        for (final Object parto : parts) {
            Map<String, Object> partInfo = (Map<String, Object>) parto;
            sources = (List<Object>) partInfo.get("sources");
            boolean httpProtocolAvailable = false;
            for (final Object source : sources) {
                Map<String, Object> qualityInfo = (Map<String, Object>) source;
                String src = (String) qualityInfo.get("src");
                if (src == null) {
                    /* 2021-04-15 */
                    src = (String) qualityInfo.get("url");
                }
                if (isSupportedHttpStreamingURL(src)) {
                    httpProtocolAvailable = true;
                    src = fixStreamingURL(src, possiblyGeoBlocked);
                    randomHTTPUrl = src;
                    partsWithHTTPQuality.put(partCounter, src);
                }
            }
            if (!httpProtocolAvailable) {
                partsWithoutHTTPQuality.add(partCounter);
            }
            partCounter++;
        }
        /* Construct & collect missing http URLs based on known pattern */
        if (randomHTTPUrl != null && !partsWithoutHTTPQuality.isEmpty()) {
            final Regex urlParts = new Regex(randomHTTPUrl, REGEX_HTTP_STREAMING);
            final String urlBase = urlParts.getMatch(0);
            final String episodeID = urlParts.getMatch(1);
            for (final int part_number_real : partsWithoutHTTPQuality) {
                final int partIndex = part_number_real - 1;
                final String episodeInURL;
                if (partIndex == 0) {
                    /* E.g. https://multiscreen.atv.cdn.tvnext.tv/2016/10/SD/hbbtv/12345.mp4 */
                    episodeInURL = episodeID;
                } else {
                    /* E.g. https://multiscreen.atv.cdn.tvnext.tv/2016/10/SD/hbbtv/12345_3.mp4 */
                    episodeInURL = episodeID + "_" + part_number_real;
                }
                final String newHTTPUrl = urlBase + "/" + episodeInURL + ".mp4";
                partsWithHTTPQuality.put(part_number_real, newHTTPUrl);
            }
        }
        /* Add all HTTP URLs */
        final Iterator<Entry<Integer, String>> iterator = partsWithHTTPQuality.entrySet().iterator();
        while (iterator.hasNext()) {
            final Entry<Integer, String> entry = iterator.next();
            final int part_number_real = entry.getKey();
            final String url = entry.getValue();
            /* delivery:progressive --> http url */
            /* 2016-10-18: Seems like their http urls always have the same quality */
            /*
             * 2017-01-25: Seems like sometimes, part1 is only available in one single quality which is usually lower than 576p (same for
             * hls) - this is a serverside "issue" and not a JDownloader problem! (See forum 59152 page 7).
             */
            final String quality = "576p";
            final DownloadLink link = this.createDownloadlink(url);
            String finalname = hybrid_name + "_";
            finalname += "http_part_";
            finalname += df.format(part_number_real) + "_";
            finalname += quality;
            finalname += ".mp4";
            link.setFinalFileName(finalname);
            // link.setAvailable(true);
            link.setContentUrl(parameter);
            link._setFilePackage(fp);
            decryptedLinks.add(link);
            distribute(link);
        }
        /* Add all older HLS URLs */
        partCounter = 1;
        final String[] possibleQualities = { "1080", "720", "576", "540", "360", "226" };
        final int possibleQualitiesCount = possibleQualities.length;
        for (final Object parto : parts) {
            entries = (Map<String, Object>) parto;
            sources = (List<Object>) entries.get("sources");
            final boolean expectGeoBlock;
            if (entries.containsKey("is_geo_ip_blocked")) {
                expectGeoBlock = ((Boolean) entries.get("is_geo_ip_blocked")).booleanValue();
            } else {
                expectGeoBlock = false;
            }
            for (final Object source : sources) {
                entries = (Map<String, Object>) source;
                String src;
                if (entries.containsKey("src")) {
                    /* Old */
                    src = (String) entries.get("src");
                    final String protocol = (String) entries.get("protocol");
                    // final String delivery = (String) entries.get("delivery");
                    // final String type = (String) entries.get("type");
                    if (!"http".equalsIgnoreCase(protocol) || src == null || !src.startsWith("http")) {
                        /*
                         * Skip unknown/unsupported streaming protocols e.g. some of their videos still got old rtsp urls e.g.
                         * http://atv.at/bauer-sucht-frau-staffel-13/die-hofwochen-beginnen/d1348313/
                         */
                        continue;
                    }
                } else {
                    /* New 2021-04-15 */
                    src = (String) entries.get("url");
                    final String mimetype = (String) entries.get("mimetype");
                    if (!mimetype.matches("application/x-mpegURL|video/mp4") || src == null || !src.startsWith("http")) {
                        /* Only allow http- and HLS URLs! */
                        continue;
                    }
                }
                src = fixStreamingURL(src, expectGeoBlock);
                /* Some variables we need in that loop below. */
                DownloadLink link = null;
                String quality = null;
                String finalname = null;
                final String part_formatted = df.format(partCounter);
                if (src.contains("chunklist") || src.contains(".m3u8")) {
                    /* Find all hls qualities */
                    if (partsWithHTTPQuality.containsKey(partCounter)) {
                        logger.info("Skipping HLS quality because HTTP is available");
                        continue;
                    }
                    /* 2016-10-18: It is possible to change hls urls to http urls! */
                    this.br.getPage(src);
                    final String[] qualities = this.br.getRegex("BANDWIDTH=").getColumn(-1);
                    final int qualitiesNum = qualities.length;
                    if (!this.br.containsHTML("#EXT-X-STREAM-INF") && !this.br.containsHTML("#EXTINF")) {
                        if (expectGeoBlock) {
                            logger.info("Possible GEO-unlock fail: " + src);
                        }
                        continue;
                    }
                    boolean stop = false;
                    int qualityIndex = 0;
                    for (String line : Regex.getLines(this.br.toString())) {
                        if (!line.startsWith("#")) {
                            if (this.isAbort()) {
                                logger.info("Decryption aborted by user");
                                return decryptedLinks;
                            }
                            long estimatedSize = 0;
                            /* Reset quality value */
                            quality = null;
                            line = line.replaceAll("http(s?)://blocked(\\.|-)", "http$1://");
                            if (qualitiesNum == 0) {
                                stop = true;
                                link = createDownloadlink(this.br.getURL());
                            } else {
                                link = createDownloadlink(br.getURL(line).toString());
                            }
                            link.setContainerUrl(parameter);
                            try {
                                /* try to get the video quality */
                                final HLSDownloader downloader = new HLSDownloader(link, br, link.getDownloadURL()) {
                                    @Override
                                    protected LogInterface initLogger() {
                                        return getLogger();
                                    }
                                };
                                estimatedSize = downloader.getEstimatedSize();
                                final StreamInfo streamInfo = downloader.getProbe();
                                for (Stream s : streamInfo.getStreams()) {
                                    if ("video".equals(s.getCodec_type())) {
                                        quality = s.getHeight() + "p";
                                        break;
                                    }
                                }
                            } catch (Throwable e) {
                                getLogger().log(e);
                                estimatedSize = 0;
                            }
                            finalname = hybrid_name + "_";
                            finalname += "hls_part_";
                            finalname += part_formatted + "_";
                            if (quality == null && qualitiesNum > 0 && qualitiesNum <= possibleQualitiesCount) {
                                /*
                                 * Workaround for possible Ffmpeg issue. We know that the bitrates go from highest to lowest so we can
                                 * assume which quality we have here in case the Ffmpeg method fails!
                                 */
                                quality = possibleQualities[possibleQualitiesCount - qualitiesNum + qualityIndex] + "p";
                            } else if (quality == null) {
                                /* Should never happen! */
                                quality = "unknownp";
                            }
                            if (quality != null) {
                                finalname += quality;
                            }
                            finalname += ".mp4";
                            link.setFinalFileName(finalname);
                            if (estimatedSize > 0) {
                                link.setAvailable(true);
                                link.setDownloadSize(estimatedSize);
                            }
                            link.setContentUrl(parameter);
                            link._setFilePackage(fp);
                            decryptedLinks.add(link);
                            distribute(link);
                            qualityIndex++;
                        }
                        if (stop) {
                            break;
                        }
                    }
                } else {
                    /* Skip http URLs and or unknown qualities. HTTP URLs have already been added before! */
                    continue;
                }
            }
            partCounter++;
            if (this.isAbort()) {
                logger.info("Decryption aborted by user");
                return decryptedLinks;
            }
        }
        if (decryptedLinks.size() == 0 && possiblyGeoBlocked) {
            logger.info("GEO-blocked");
            final DownloadLink offline = this.createOfflinelink(parameter);
            offline.setFinalFileName("GEO_blocked_" + url_seriesname + "_" + url_episodename);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        return decryptedLinks;
    }

    private boolean isSupportedHttpStreamingURL(final String url) {
        return url != null && url.matches(REGEX_HTTP_STREAMING);
    }

    // private boolean isHLSUrl(final String url) {
    // return url != null && url.contains(".m3u8");
    // }
    private String fixStreamingURL(String src, final boolean expectGeoBlock) {
        /* 2020-11-12: Disabled for testing - seems like this doesn't work for all items --> Sometimes leads to timeouts */
        final boolean allowOldGeoBlockedWorkaround = false;
        final String geoblockWorkaroundOld = new Regex(src, "((?:tvnext_clip|video_file)/video/\\d+\\.mp4)").getMatch(0);
        if (expectGeoBlock && geoblockWorkaroundOld != null && allowOldGeoBlockedWorkaround) {
            /* Get around GEO-block - for older videos */
            /*
             * E.g. http://videos-blocked-fallback.atv.cdn.tvnext.tv/tvnext_clip/video/123456.mp4/chunklist.m3u8?ttl=123456&token=123456
             */
            /* E.g. Old url for part 1 only: http://atv.at/bauer-sucht-frau-staffel-13/die-hofwochen-beginnen/d1348313/ */
            /* E.g. old url for all parts: http://atv.at/bauer-sucht-frau-staffel-13/die-hofwochen-beginnen/d1348313/ */
            /* Alternative. http://videos-fallback.atv.cdn.tvnext.tv */
            src = "http://109.68.230.208/vod/fallback/" + geoblockWorkaroundOld + "/index.m3u8";
        } else {
            /* Get around GEO-block - for new content */
            /* 2021-04-15: Still working e.g. original: https://blocked-multiscreen.atv.cdn.tvnext.tv/2021/04/HD/hbbtv/1234567.mp4 */
            src = src.replaceAll("http(s?)://blocked(\\.|-)", "http$1://");
        }
        src = src.replaceAll("&amp;", "&");
        return src;
    }

    private String decodeUnicode(final String s) {
        final Pattern p = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
        String res = s;
        final Matcher m = p.matcher(res);
        while (m.find()) {
            res = res.replaceAll("\\" + m.group(0), Character.toString((char) Integer.parseInt(m.group(1), 16)));
        }
        return res;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}