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
import java.util.LinkedHashMap;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tvnow.de" }, urls = { "https?://(?:www\\.)?tvnow\\.(?:at|ch|de)/.+|https?://link\\.tvnow\\.de/\\?f=\\d+\\&e=\\d+" })
public class TvnowDe extends PluginForDecrypt {
    public TvnowDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        String formatID = null;
        String url_showname;
        String url_singleEpisodeName = null;
        String singleEpisodeID = null;
        boolean isMovie;
        String url_old = null;
        String url_new = null;
        String stationName;
        /* E.g. tvnow.de/shows/bla | tvnow.de/serien/bla */
        if (parameter.matches(jd.plugins.hoster.TvnowDe.TYPE_MOVIE_NEW)) {
            stationName = null;
            url_showname = new Regex(parameter, "([^/]+)$").getMatch(0);
            formatID = new Regex(url_showname, ".+\\-(\\d+)$").getMatch(0);
            isMovie = true;
            url_showname = jd.plugins.hoster.TvnowDe.cleanupShowTitle(url_showname);
        } else if (jd.plugins.hoster.TvnowDe.isMovie_old(parameter)) {
            /* Old movies linkformat */
            url_old = parameter;
            final Regex urlInfo = new Regex(parameter, "https?://[^/]+/([^/]+)/([^/]+)");
            stationName = urlInfo.getMatch(0);
            url_showname = urlInfo.getMatch(1);
            isMovie = true;
        } else if (parameter.matches("https?://[^/]+/[^/]+/[^/]+")) {
            /* 2018-12-12: New linkformat */
            stationName = null;
            url_showname = new Regex(parameter, "/([^/]+)$").getMatch(0);
            formatID = new Regex(url_showname, ".+\\-(\\d+)$").getMatch(0);
            url_showname = jd.plugins.hoster.TvnowDe.cleanupShowTitle(url_showname);
            isMovie = false;
        } else if (parameter.matches(jd.plugins.hoster.TvnowDe.TYPE_DEEPLINK)) {
            formatID = new Regex(parameter, "f=(\\d+)").getMatch(0);
            singleEpisodeID = new Regex(parameter, "e=(\\d+)").getMatch(0);
            url_showname = null;
            stationName = null;
            br.getPage(parameter);
            /* Additional offline-check */
            if (br.getHttpConnection().getResponseCode() == 404) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            url_old = br.getRegex("webLink = \\'(https?://[^/]+/[^<>\"\\']+)\\'").getMatch(0);
            if (url_old != null) {
                isMovie = jd.plugins.hoster.TvnowDe.isMovie_old(url_old);
                if (isMovie) {
                    final Regex urlInfo = new Regex(url_old, "tvnow\\.de/([^/]+)/([^/]+)");
                    stationName = urlInfo.getMatch(0);
                    url_showname = urlInfo.getMatch(1);
                } else {
                    /* We can only forward series directly to the host-plugin - we have to crawl movies via the main loop below! */
                    final Regex urlInfo = new Regex(url_old, "tvnow\\.de/([^/]+)/([^/]+)/(.+)");
                    stationName = urlInfo.getMatch(0);
                    url_showname = urlInfo.getMatch(1);
                    url_singleEpisodeName = urlInfo.getMatch(2);
                }
            } else {
                isMovie = false;
            }
        } else if (jd.plugins.hoster.TvnowDe.isSeriesSingleEpisodeNew(parameter)) {
            /* New single-series-episode linkformat */
            final Regex urlInfo = new Regex(parameter, jd.plugins.hoster.TvnowDe.TYPE_SERIES_SINGLE_EPISODE_NEW);
            stationName = null;
            url_showname = urlInfo.getMatch(0);
            formatID = new Regex(url_showname, "\\-(\\d+)$").getMatch(0);
            final String url_singleEpisodeNameTmp = urlInfo.getMatch(1);
            if (url_singleEpisodeNameTmp != null && !url_singleEpisodeNameTmp.matches("\\d{4}\\-\\d{2}")) {
                /* Make sure that this is actually an episodeName and not e.g. just a date. */
                url_singleEpisodeName = urlInfo.getMatch(1);
                singleEpisodeID = new Regex(url_singleEpisodeName, ".+\\-(\\d+)$").getMatch(0);
                url_singleEpisodeName = jd.plugins.hoster.TvnowDe.cleanupEpisodeTitle(url_singleEpisodeName);
            }
            url_showname = jd.plugins.hoster.TvnowDe.cleanupShowTitle(url_showname);
            isMovie = false;
        } else if (parameter.matches(jd.plugins.hoster.TvnowDe.TYPE_SERIES_NEW)) {
            /* New series linkformat */
            final Regex urlInfo = new Regex(parameter, jd.plugins.hoster.TvnowDe.TYPE_SERIES_NEW);
            stationName = null;
            url_showname = urlInfo.getMatch(0);
            formatID = new Regex(url_showname, ".+\\-(\\d+)$").getMatch(0);
            url_showname = jd.plugins.hoster.TvnowDe.cleanupShowTitle(url_showname);
            isMovie = false;
        } else {
            /* Old linkformat/other */
            url_old = parameter;
            Regex urlInfo = new Regex(parameter, "https?://[^/]+/([^/]+)/([^/]+)/([^/]+).*?");
            stationName = urlInfo.getMatch(0);
            url_showname = urlInfo.getMatch(1);
            url_singleEpisodeName = urlInfo.getMatch(2);
            final boolean showname_url_is_unsafe = url_showname == null || (new Regex(url_showname, "(\\-\\d+){1}$").matches() && !new Regex(url_showname, "(\\-\\d+){2}$").matches());
            if (showname_url_is_unsafe) {
                logger.info("Expecting redirect from old linktype to new linktype");
                br.setFollowRedirects(false);
                br.getPage(url_old);
                /* Old linkformat should redirect to new linkformat */
                url_new = br.getRedirectLocation();
                /*
                 * We accessed the main-URL so it makes sense to at least check for a 404 at this stage to avoid requestion potentially dead
                 * URLs again via API!
                 */
                if (br.getHttpConnection().getResponseCode() == 404) {
                    decryptedLinks.add(this.createOfflinelink(parameter));
                    return decryptedLinks;
                } else if (url_new == null) {
                    logger.warning("Redirect to new linktype failed");
                    return null;
                }
                logger.info("URL_old: " + parameter + " | URL_new: " + url_new);
                /* Find the values we need. */
                urlInfo = new Regex(url_new, "https?://[^/]+/[^/]+/([^/]*?)/([^/]+/)?(.+)");
                url_showname = urlInfo.getMatch(0);
                url_singleEpisodeName = urlInfo.getMatch(2);
                if (StringUtils.isEmpty(url_showname) || StringUtils.isEmpty(url_singleEpisodeName)) {
                    logger.warning("Failed to extract urlInfo from URL_new");
                    return null;
                }
                singleEpisodeID = new Regex(url_singleEpisodeName, ".+\\-(\\d+)$").getMatch(0);
                url_singleEpisodeName = jd.plugins.hoster.TvnowDe.cleanupEpisodeTitle(url_singleEpisodeName);
            }
            formatID = new Regex(url_showname, ".+\\-(\\d+)$").getMatch(0);
            url_showname = jd.plugins.hoster.TvnowDe.cleanupShowTitle(url_showname);
            isMovie = false;
        }
        String formatTitle = null;
        LinkedHashMap<String, Object> entries = null;
        if (formatID == null) {
            /* Required for old URLs which contain showname and stationName but not the formatID. */
            if (StringUtils.isEmpty(stationName) || StringUtils.isEmpty(url_showname)) {
                /* This request is impossible without the correct stationName. */
                logger.warning("Failed to find stationName - probably invalid/offline URL");
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            jd.plugins.hoster.TvnowDe.prepBRAPI(this.br);
            /* First we need to find the ID of whatever the user added */
            br.getPage(jd.plugins.hoster.TvnowDe.API_BASE + "/formats/seo?fields=id,title,hasFreeEpisodes,isGeoblocked&name=" + url_showname + ".php&station=" + stationName);
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            if (br.getHttpConnection().getResponseCode() == 404) {
                /* Rare case */
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            // final boolean hasFreeEpisodes = ((Boolean)entries.get("hasFreeEpisodes")).booleanValue();
            // final boolean isGeoblocked = ((Boolean)entries.get("isGeoblocked")).booleanValue();
            formatID = Long.toString(JavaScriptEngineFactory.toLong(entries.get("id"), 0));
            formatTitle = (String) entries.get("title");
        }
        if (formatID.equals("0")) {
            /* Should never happen */
            logger.warning("Failed to find itemID");
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        if (StringUtils.isEmpty(formatID) || StringUtils.isEmpty(url_showname)) {
            logger.warning("Failed to find formatID");
            return null;
        }
        if (url_singleEpisodeName != null) {
            /* Single item --> Hosterplugin */
            final String contentURL;
            if (formatID != null && singleEpisodeID != null) {
                contentURL = generateDeeplinkURL(formatID, singleEpisodeID);
            } else {
                contentURL = url_old;
            }
            if (contentURL == null) {
                /* This should never happen */
                logger.warning("contentURL is null");
                return null;
            }
            final DownloadLink dl = this.createDownloadlink(contentURL);
            /* Very important! */
            jd.plugins.hoster.TvnowDe.storeUrlPartInfo(dl, url_showname, url_singleEpisodeName, stationName, formatID, singleEpisodeID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        if (StringUtils.isEmpty(formatTitle)) {
            formatTitle = url_showname;
        }
        final int maxItemsPerPage = 100;
        int page = 1;
        int numberofItemsGrabbedTmp;
        int numberofItemsGrabbedTotal = 0;
        /* TODO: Add functionality if requested by user. */
        int maxNumberofItemsToGrab = -1;
        boolean done = false;
        do {
            numberofItemsGrabbedTmp = 0;
            br.getPage(jd.plugins.hoster.TvnowDe.API_BASE + "/movies?fields=*&filter=%7B%22FormatId%22:" + formatID + "%7D&maxPerPage=" + maxItemsPerPage + "&order=BroadcastStartDate+desc&page=" + page);
            if (br.getHttpConnection().getResponseCode() == 404) {
                logger.info("WTF: 404 during crawl-process");
                return null;
            }
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("items");
            for (final Object videoO : ressourcelist) {
                entries = (LinkedHashMap<String, Object>) videoO;
                final String episodeID = Long.toString(JavaScriptEngineFactory.toLong(entries.get("id"), -1));
                final String seasonnumber = Long.toString(JavaScriptEngineFactory.toLong(entries.get("season"), -1));
                final String episodenumber = Long.toString(JavaScriptEngineFactory.toLong(entries.get("episode"), -1));
                final String videoSeoName = (String) entries.get("seoUrl");
                final String broadcastStartDate = (String) entries.get("broadcastStartDate");
                final String broadcastStartDate_important_part = new Regex(broadcastStartDate, "^(\\d{4}\\-\\d{2})").getMatch(0);
                /* An URL which is rarely used on their website but this one will always lead us to their website! */
                /* Structure: http://link.tvnow.de/?f=<formatID>&e=<episodeID> */
                final String deeplinkUrl = (String) entries.get("deeplinkUrl");
                final String thisStationName;
                if (stationName != null) {
                    thisStationName = stationName;
                } else {
                    thisStationName = (String) entries.get("cornerLogo");
                }
                if (StringUtils.isEmpty(videoSeoName) || episodeID.equals("-1")) {
                    logger.warning("Failed to find thisStationName or videoSeoName");
                    return null;
                }
                // if (thisStationName.equalsIgnoreCase("none")) {
                // /* E.g. stuff which is only available online but not on TV. */
                // logger.info("DEBUG");
                // }
                final boolean creation_of_old_linktype_possible = url_old != null || (jd.plugins.hoster.TvnowDe.isValidTvStation(thisStationName));
                final boolean useNewContentURL = true;
                final boolean useDeeplinkAsContentURL = false;
                String contentURL;
                /*
                 * 2018-12-18: Both linktypes are still officially supported via website but the old linktype does not work for content
                 * which does not actually get aired on TV as the tvStationName is part of the old URL.
                 */
                if (useNewContentURL || !creation_of_old_linktype_possible) {
                    /* Different URLs for movies- and series */
                    final String showname_with_formatID = url_showname + "-" + formatID;
                    final String videoSeoName_with_episodeID = videoSeoName + "-" + episodeID;
                    if (isMovie) {
                        contentURL = String.format("https://www.tvnow.de/filme/%s", videoSeoName_with_episodeID);
                    } else {
                        if (seasonnumber.equals("-1") || episodenumber.equals("-1")) {
                            /* This should never happen! */
                            return null;
                        }
                        if (seasonnumber.matches("\\d{4}")) {
                            contentURL = String.format("https://www.tvnow.de/serien/%s/%s/episode-%s-%s", showname_with_formatID, broadcastStartDate_important_part, episodenumber, videoSeoName_with_episodeID);
                        } else {
                            contentURL = String.format("https://www.tvnow.de/serien/%s/staffel-%s/episode-%s-%s", showname_with_formatID, seasonnumber, episodenumber, videoSeoName_with_episodeID);
                        }
                    }
                } else if (useDeeplinkAsContentURL) {
                    if (StringUtils.isEmpty(deeplinkUrl)) {
                        logger.warning("Failed to find deeplinkUrl");
                        return null;
                    }
                    contentURL = deeplinkUrl;
                } else {
                    /* Old layout: https://www.tvnow.de/<tvStationName>/<showname>/<videoSeoName> (different for movies) */
                    if (url_old != null && ressourcelist.size() == 1) {
                        /* Easiest case - valid old_url is simply given and we can use it because user only added a single object. */
                        contentURL = url_old;
                    } else {
                        if (isMovie) {
                            contentURL = String.format("https://www.tvnow.de/%s/%s", thisStationName, url_showname);
                        } else {
                            contentURL = String.format("https://www.tvnow.de/%s/%s/%s", thisStationName, url_showname, videoSeoName);
                        }
                    }
                }
                final DownloadLink dl = this.createDownloadlink(contentURL);
                jd.plugins.hoster.TvnowDe.parseInformation(dl, entries, thisStationName, formatTitle);
                /* Very important! */
                jd.plugins.hoster.TvnowDe.storeUrlPartInfo(dl, url_showname, videoSeoName, thisStationName, formatID, episodeID);
                decryptedLinks.add(dl);
                distribute(dl);
                numberofItemsGrabbedTmp++;
                numberofItemsGrabbedTotal++;
                /* Did we crawl the max number of items the user wanted to have? */
                done = maxNumberofItemsToGrab > 0 && numberofItemsGrabbedTotal >= maxNumberofItemsToGrab;
            }
            if (!done) {
                /* Did we reach the last page? */
                done = numberofItemsGrabbedTmp < maxItemsPerPage;
            }
            page++;
        } while (!done && !this.isAbort());
        // String fpName = br.getRegex("").getMatch(0);
        // if (fpName != null) {
        // final FilePackage fp = FilePackage.getInstance();
        // fp.setName(Encoding.htmlDecode(fpName.trim()));
        // fp.addLinks(decryptedLinks);
        // }
        return decryptedLinks;
    }

    @Override
    public DownloadLink createDownloadlink(final String url) {
        return super.createDownloadlink(url.replaceAll("https?://", "tvnowdecrypted://"));
    }

    public static String generateDeeplinkURL(final String formatID, final String episodeID) {
        if (formatID == null || episodeID == null) {
            return null;
        }
        return String.format("http://link.tvnow.de/?f=%s&e=%s", formatID, episodeID);
    }
}
