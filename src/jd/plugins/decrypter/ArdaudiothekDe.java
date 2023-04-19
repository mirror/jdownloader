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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class ArdaudiothekDe extends PluginForDecrypt {
    public ArdaudiothekDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "ardaudiothek.de" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(sendung/[\\w\\-]+/\\d+/?|episode/[\\w\\-]+/[\\w\\-]+/[\\w\\-]+/\\d+/?)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String episodeTargetID = null;
        final boolean doSpecialHandlingToFindSingleEpisodePosition = true;
        br.setFollowRedirects(true);
        List<Map<String, Object>> episodes = null;
        int infiniteLoopPreventionCounter = -1;
        FilePackage fp = null;
        String urlToAccess = param.getCryptedUrl();
        Map<String, Object> publicationService = null;
        int page = 0;
        final int maxItemsPerPage = 12;
        do {
            page++;
            infiniteLoopPreventionCounter++;
            br.getPage(urlToAccess);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            urlToAccess = null;
            final String json = br.getRegex("type=\"application/json\">([^<]+)<").getMatch(0);
            final Map<String, Object> entries = restoreFromString(json, TypeRef.MAP);
            final Map<String, Object> data = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "props/pageProps/initialData/data");
            final Map<String, Object> result = (Map<String, Object>) data.get("result");
            final Map<String, Object> podcastEpisode = (Map<String, Object>) data.get("item");
            if (result == null && podcastEpisode == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (result != null) {
                /* This means we got a podcast with multiple episodes. */
                final String podcastID = result.get("id").toString();
                final Map<String, Object> pageInfo = (Map<String, Object>) JavaScriptEngineFactory.walkJson(result, "items/pageInfo");
                final int totalNumberofItems = ((Number) result.get("numberOfElements")).intValue();
                publicationService = (Map<String, Object>) result.get("publicationService");
                final String podcastTitle = result.get("title").toString();
                final String podcastDescription = (String) result.get("description");
                if (fp == null) {
                    fp = FilePackage.getInstance();
                    fp.setName(podcastTitle);
                    fp.setComment(podcastDescription);
                }
                final Map<String, Object> items = (Map<String, Object>) result.get("items");
                episodes = (List<Map<String, Object>>) items.get("nodes");
                final ArrayList<DownloadLink> results = this.processEpisodes(episodes, totalNumberofItems, episodeTargetID, 0);
                fp.addLinks(results);
                ret.addAll(results);
                logger.info("Crawled page " + page + " | Found items: " + ret.size() + "/" + totalNumberofItems);
                if (episodes.size() == maxItemsPerPage && Boolean.TRUE.equals(pageInfo.get("hasNextPage"))) {
                    ret.addAll(this.crawlPagination(podcastID, maxItemsPerPage, maxItemsPerPage, totalNumberofItems, fp));
                    break;
                } else {
                    logger.info("Stopping because: There is only one page");
                    break;
                }
            } else {
                /* Single episode */
                publicationService = (Map<String, Object>) podcastEpisode.get("publicationService");
                episodeTargetID = podcastEpisode.get("id").toString();
                if (doSpecialHandlingToFindSingleEpisodePosition) {
                    /*
                     * Access main podcast URL but then later only pick this episode so we get to know the position of that single added
                     * episode.
                     */
                    final Map<String, Object> programSet = (Map<String, Object>) podcastEpisode.get("programSet");
                    urlToAccess = programSet.get("path").toString();
                    logger.info("Accessing main podcast URL in order to find context of single wished episode: " + urlToAccess);
                    continue;
                } else {
                    episodes = new ArrayList<Map<String, Object>>();
                    episodes.add(podcastEpisode);
                    ret.addAll(processEpisodes(episodes, 1, episodeTargetID, 0));
                    break;
                }
            }
        } while ((infiniteLoopPreventionCounter <= 1 && urlToAccess != null));
        // String tvRadioStationTitle = null;
        // if (publicationService != null) {
        // tvRadioStationTitle = publicationService.get("title").toString();
        // }
        return ret;
    }

    private ArrayList<DownloadLink> crawlPagination(final String podcastID, final int maxItemsPerPage, int offset, final int totalNumberofItems, final FilePackage fp) throws IOException, PluginException {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final Browser brc = br.cloneBrowser();
        brc.getHeaders().put("Accept", "*/*");
        /* Starts from 2 as page 1 = website -> html code and gets crawled in beforehand. */
        int page = 1;
        do {
            page++;
            brc.getPage("https://api.ardaudiothek.de/graphql?query=query%20ProgramSetEpisodesQuery(%24id%3AID!%2C%24offset%3AInt!%2C%24count%3AInt!)%7Bresult%3AprogramSet(id%3A%24id)%7Bitems(offset%3A%24offset%20first%3A%24count%20orderBy%3APUBLISH_DATE_DESC%20filter%3A%7BisPublished%3A%7BequalTo%3Atrue%7D%7D)%7BpageInfo%7BhasNextPage%20endCursor%7Dnodes%7Bid%20title%20publishDate%20summary%20duration%20path%20image%7Burl%20url1X1%20description%20attribution%7DprogramSet%7Bid%20title%20path%20publicationService%7Btitle%20genre%20path%20organizationName%7D%7Daudios%7Burl%20downloadUrl%20allowDownload%7D%7D%7D%7D%7D&variables=%7B%22id%22%3A%22" + podcastID + "%22%2C%22offset%22%3A" + offset + "%2C%22count%22%3A" + maxItemsPerPage + "%7D");
            final Map<String, Object> entries = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
            final Map<String, Object> items = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "data/result/items");
            final Map<String, Object> pageInfo = (Map<String, Object>) items.get("pageInfo");
            final List<Map<String, Object>> episodes = (List<Map<String, Object>>) items.get("nodes");
            final ArrayList<DownloadLink> results = this.processEpisodes(episodes, totalNumberofItems, null, offset);
            if (fp != null) {
                fp.addLinks(results);
            }
            ret.addAll(results);
            logger.info("Crawled page " + page + " | Found items: " + ret.size() + "/" + totalNumberofItems);
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (Boolean.FALSE.equals(pageInfo.get("hasNextPage"))) {
                logger.info("Stopping because: Reached last page");
                break;
            } else if (episodes.size() < maxItemsPerPage) {
                /* Double fail-safe */
                logger.info("Stopping because: Current page contains less items than " + maxItemsPerPage);
                break;
            } else {
                /* Access next page */
                offset += episodes.size();
                continue;
            }
        } while (true);
        return ret;
    }

    private ArrayList<DownloadLink> processEpisodes(final List<Map<String, Object>> episodes, final int totalNumberofEpisodes, final String episodeTargetID, final int offset) throws PluginException, IOException {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final int padLength = StringUtils.getPadLength(totalNumberofEpisodes);
        int position = offset + 1;
        for (final Map<String, Object> episode : episodes) {
            final String episodeID = episode.get("id").toString();
            final String episodeSummary = (String) episode.get("summary");
            /* Is available if we open the single URL to an episode. */
            final String episodeFullDescription = (String) episode.get("description");
            final List<Map<String, Object>> audios = (List<Map<String, Object>>) episode.get("audios");
            /* Find direct-url: We prefer original downloadurl and only use streaming URL as fallback. */
            String urlStreaming = null;
            String urlDownload = null;
            for (final Map<String, Object> audio : audios) {
                if ((Boolean) audio.get("allowDownload")) {
                    urlDownload = audio.get("downloadUrl").toString();
                } else {
                    urlStreaming = audio.get("url").toString();
                }
            }
            if (StringUtils.isEmpty(urlStreaming) && StringUtils.isEmpty(urlDownload)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final DownloadLink link = this.createDownloadlink(urlDownload != null ? urlDownload : urlStreaming);
            if (episodes.size() == 1) {
                link.setFinalFileName(episode.get("title") + ".mp3");
            } else {
                link.setFinalFileName(StringUtils.formatByPadLength(padLength, position) + " - " + episode.get("title") + ".mp3");
            }
            link.setAvailable(true);
            if (!StringUtils.isEmpty(episodeFullDescription)) {
                link.setComment(episodeFullDescription);
            } else if (!StringUtils.isEmpty(episodeSummary)) {
                link.setComment(episodeSummary);
            }
            /* Estimate filesize based on bitrate of 160kb/s */
            final Number durationSeconds = (Number) episode.get("duration");
            if (durationSeconds != null) {
                link.setDownloadSize((durationSeconds.longValue() * 160 * 1024) / 8);
            }
            final String path = (String) episode.get("path");
            if (!StringUtils.isEmpty(path)) {
                link.setContentUrl(br.getURL(path).toString());
            }
            if (StringUtils.equals(episodeID, episodeTargetID)) {
                /* User wants to have one specific episode only */
                ret.clear();
                ret.add(link);
                break;
            } else {
                ret.add(link);
            }
            position++;
        }
        return ret;
    }
}
