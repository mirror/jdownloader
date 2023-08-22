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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.JoinPeerTubeOrg;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class PeerTubeCrawler extends PluginForDecrypt {
    public PeerTubeCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.GENERIC, LazyPlugin.FEATURE.VIDEO_STREAMING };
    }

    /** Returns content of getPluginDomains as single dimensional Array. */
    public static ArrayList<String> getAllSupportedPluginDomainsFlat() {
        ArrayList<String> allDomains = new ArrayList<String>();
        for (final String[] domains : JoinPeerTubeOrg.getPluginDomains()) {
            for (final String singleDomain : domains) {
                allDomains.add(singleDomain);
            }
        }
        return allDomains;
    }

    public static String[] getAnnotationNames() {
        return new String[] { "joinpeertube.org" };
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(JoinPeerTubeOrg.getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(JoinPeerTubeOrg.getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:a|c)/([^/]+)/videos");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String host = Browser.getHost(param.getCryptedUrl(), true);
        final String channelName = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        final String channelNameUrlencoded = Encoding.urlEncode(channelName);
        br.getHeaders().put("Accept", "application/json, text/plain, */*");
        br.getHeaders().put("Referer", "https://" + host + "/c/" + channelNameUrlencoded + "/videos?s=1");
        br.getPage("https://" + host + "/api/v1/video-channels/" + channelNameUrlencoded);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> channel = restoreFromString(br.toString(), TypeRef.MAP);
        br.getPage("/api/v1/video-channels/" + channelName + "/videos?start=0&count=0&sort=-publishedAt");
        final Map<String, Object> channelVideosInfo = restoreFromString(br.toString(), TypeRef.MAP);
        final int totalNumberofVideos = ((Number) channelVideosInfo.get("total")).intValue();
        if (totalNumberofVideos == 0) {
            final DownloadLink dummy = this.createOfflinelink(param.getCryptedUrl(), "EMPTY_CHANNEL_" + channelName, "This channel contains no videos.");
            decryptedLinks.add(dummy);
            return decryptedLinks;
        }
        final String channelDescription = (String) channel.get("description");
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(channelName);
        if (!StringUtils.isEmpty(channelDescription)) {
            fp.setComment(channelDescription);
        }
        final int maxItemsPerRequest = 25;
        int page = 1;
        int index = 0;
        final Set<String> dupes = new HashSet<String>();
        final PluginForHost plg = this.getNewPluginForHostInstance(this.getHost());
        channelLoop: do {
            final UrlQuery query = new UrlQuery();
            query.add("start", Integer.toString(index));
            query.add("count", Integer.toString(maxItemsPerRequest));
            query.add("sort", "-publishedAt");
            query.add("skipCount", "true");
            query.add("nsfw", "both");
            br.getPage("/api/v1/video-channels/" + channelNameUrlencoded + "/videos?" + query.toString());
            final Map<String, Object> channelVideosResponse = restoreFromString(br.toString(), TypeRef.MAP);
            final List<Map<String, Object>> videoMaps = (List<Map<String, Object>>) channelVideosResponse.get("data");
            for (final Map<String, Object> videoMap : videoMaps) {
                String videoURL = (String) videoMap.get("url");
                if (videoURL == null) {
                    /* Not always given e.g. visionon.tv */
                    final String embedPath = (String) videoMap.get("embedPath");
                    if (!StringUtils.isEmpty(embedPath)) {
                        videoURL = br.getURL(embedPath).toString();
                    }
                }
                if (!plg.canHandle(videoURL)) {
                    logger.warning("Stopping because: Found unsupported URL! Developer work needed! URL: " + videoURL);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else if (!dupes.add(videoURL)) {
                    /* Additional fail-safe */
                    logger.info("Stopping because: Detected dupe");
                    break channelLoop;
                }
                final DownloadLink video = this.createDownloadlink(videoURL);
                JoinPeerTubeOrg.parseMetadataAndSetFilename(video, videoMap);
                video.setAvailable(true);
                video._setFilePackage(fp);
                decryptedLinks.add(video);
                distribute(video);
            }
            logger.info("Crawled page " + page + " | Found items so far: " + decryptedLinks.size() + "/" + totalNumberofVideos);
            if (decryptedLinks.size() >= totalNumberofVideos) {
                logger.info("Stopping because: Found all items");
                break;
            } else if (videoMaps.size() < maxItemsPerRequest) {
                /* Additional fail-safe */
                logger.info("Stopping because: Reached last page");
                break;
            } else if (this.isAbort()) {
                break;
            }
            page++;
            index += videoMaps.size();
        } while (true);
        if (decryptedLinks.size() < totalNumberofVideos) {
            /* This should never happen */
            logger.warning("Some videos are missing: " + (totalNumberofVideos - decryptedLinks.size()));
        }
        return decryptedLinks;
    }
}
