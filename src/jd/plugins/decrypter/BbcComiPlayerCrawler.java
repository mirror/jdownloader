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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.hls.HlsContainer;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.CrawledLink;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.BbcCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class BbcComiPlayerCrawler extends PluginForDecrypt {
    public BbcComiPlayerCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "bbc.iplayer" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/video/([a-z][a-z0-9]{7})");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final String vpid = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        if (vpid == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final BbcCom hosterPlugin = (BbcCom) this.getNewPluginForHostInstance("bbc.com");
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        /** E.g. json instead of xml: http://open.live.bbc.co.uk/mediaselector/6/select/version/2.0/mediaset/pc/vpid/<vpid>/format/json */
        /**
         * Thanks goes to: https://github.com/rg3/youtube-dl/blob/master/youtube_dl/extractor/bbc.py
         */
        /* 2021-01-12: Website uses "/pc/" instead of "/iptv-all/" */
        br.getPage("https://open.live.bbc.co.uk/mediaselector/6/select/version/2.0/mediaset/iptv-all/vpid/" + vpid + "/format/json");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> root = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final String result = (String) root.get("result");
        if (StringUtils.equalsIgnoreCase(result, "geolocation")) {
            // BbcCom.errorGeoBlocked();
            throw new DecrypterRetryException(RetryReason.GEO, vpid, "This content is not available in your country!");
        }
        /**
         * Look for special VPN "shadow ban": All info and streams are available but we'll run into error 403 when we try to access them.
         */
        int counterError403VPNBlocked = 0;
        final ArrayList<HlsContainer> hlsContainers = new ArrayList<HlsContainer>();
        String qualityString = null;
        final Browser brc = br.cloneBrowser();
        brc.setFollowRedirects(true);
        String relativeUrlForNotificationTitle = null;
        String sourceURL = null;
        try {
            sourceURL = ((CrawledLink) param.getSource()).getSourceUrls()[0];
            relativeUrlForNotificationTitle = new URL(sourceURL).getPath();
        } catch (final Throwable ignore) {
        }
        if (relativeUrlForNotificationTitle == null) {
            relativeUrlForNotificationTitle = br._getURL().getPath();
        }
        final String notificationTitle = "BBC | " + relativeUrlForNotificationTitle;
        Map<Integer, Set<String>> subtitles = new HashMap<Integer, Set<String>>();
        final List<Map<String, Object>> mediaList = (List<Map<String, Object>>) root.get("media");
        this.displayBubbleNotification(notificationTitle, "Looking for valid stream in " + mediaList.size() + " media...");
        for (final Map<String, Object> media : mediaList) {
            final String kind = (String) media.get("kind");
            final List<Map<String, Object>> connections = (List<Map<String, Object>>) media.get("connection");
            if (kind.matches("(?i)audio|video")) {
                for (final Map<String, Object> connection : connections) {
                    final String transferFormat = (String) connection.get("transferFormat");
                    final String protocol = (String) connection.get("protocol");
                    // final String m3u8 = (String) entries.get("c");
                    final String thisHLSMaster = (String) connection.get("href");
                    if (!transferFormat.equalsIgnoreCase("hls")) {
                        /* Skip dash/hds/other */
                        continue;
                    } else if (protocol.equalsIgnoreCase("http")) {
                        /* Qualities are given as both http- and https URLs -> Skip http URLs */
                        continue;
                    } else if (StringUtils.isEmpty(thisHLSMaster)) {
                        continue;
                    }
                    List<HlsContainer> results = new ArrayList<HlsContainer>();
                    /* 2024-09-05: Disabled. */
                    final boolean trySuperHighQualityTrick = false;
                    if (thisHLSMaster.matches(".*/[^/]*?\\.ism(\\.hlsv2\\.ism)?/.*\\.m3u8.*") && trySuperHighQualityTrick) {
                        // rewrite to check for all available formats
                        /* 2021-01-12: TODO: This doesn't work anymore?! */
                        try {
                            final String id = new Regex(thisHLSMaster, "/([^/]*?)\\.ism(\\.hlsv2\\.ism)?/").getMatch(0);
                            final String thisHLSMasterCorrected = thisHLSMaster.replaceFirst("/[^/]*?\\.ism(\\.hlsv2\\.ism)?/.*\\.m3u8", "/" + id + ".ism/" + id + ".m3u8");
                            results = HlsContainer.getHlsQualities(brc, thisHLSMasterCorrected);
                            if (results != null && results.size() > 0) {
                                logger.info("Successfully grabber superhigh HLS qualities");
                            }
                        } catch (final Exception e) {
                            logger.log(e);
                            logger.info("Failed to grab superhigh hls_master: " + brc.getURL());
                        }
                    }
                    if (results == null || results.isEmpty()) {
                        try {
                            results = HlsContainer.getHlsQualities(brc, thisHLSMaster);
                        } catch (final Exception e) {
                            logger.log(e);
                        }
                    }
                    if ((results == null || results.isEmpty()) && brc.getHttpConnection().getResponseCode() == 403) {
                        counterError403VPNBlocked++;
                    } else if (results != null && !results.isEmpty()) {
                        hlsContainers.addAll(results);
                    }
                    if (this.isAbort()) {
                        throw new InterruptedException();
                    }
                }
            } else if (kind.equalsIgnoreCase("captions")) {
                for (final Map<String, Object> connection : connections) {
                    final String priorityStr = connection.get("priority").toString();
                    final String url = connection.get("href").toString();
                    final int prio;
                    if (priorityStr != null && priorityStr.matches("\\d+")) {
                        prio = Integer.parseInt(priorityStr);
                    } else {
                        prio = 0;
                    }
                    Set<String> subtitlelist = subtitles.get(prio);
                    if (subtitlelist == null) {
                        subtitlelist = new HashSet<String>();
                        subtitles.put(prio, subtitlelist);
                    }
                    subtitlelist.add(url);
                }
            }
        }
        if (hlsContainers == null || hlsContainers.isEmpty()) {
            if (counterError403VPNBlocked > 0) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "VPN blocked");
            } else {
                logger.warning("Failed to find any downloadable content");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        /* Find working HLS URL */
        final String configuredPreferredVideoHeightStr = hosterPlugin.getConfiguredVideoHeight();
        final String configuredPreferredVideoFramerateStr = hosterPlugin.getConfiguredVideoFramerate();
        HlsContainer hlscontainer_chosen = null;
        final boolean userWantsBest;
        if (configuredPreferredVideoHeightStr.matches("\\d+")) {
            /* Look for user-preferred quality */
            userWantsBest = false;
            final int configuredPreferredVideoHeight = Integer.parseInt(configuredPreferredVideoHeightStr);
            final String height_for_quality_selection = hosterPlugin.getHeightForQualitySelection(configuredPreferredVideoHeight);
            for (final HlsContainer hlscontainer_temp : hlsContainers) {
                final int height = hlscontainer_temp.getHeight();
                final String height_for_quality_selection_temp = hosterPlugin.getHeightForQualitySelection(height);
                final String framerate = Integer.toString(hlscontainer_temp.getFramerate(25));
                if (height_for_quality_selection_temp.equals(height_for_quality_selection) && framerate.equals(configuredPreferredVideoFramerateStr)) {
                    logger.info("Found user selected quality");
                    hlscontainer_chosen = hlscontainer_temp;
                    break;
                }
            }
            if (hlscontainer_chosen == null) {
                logger.info("Failed to find user selected quality --> Fallback to BEST");
                hlscontainer_chosen = HlsContainer.findBestVideoByBandwidth(hlsContainers);
            }
        } else {
            /* User prefers BEST quality */
            hlscontainer_chosen = HlsContainer.findBestVideoByBandwidth(hlsContainers);
            userWantsBest = true;
        }
        final String urlpartToReplace = "-video=5070000.m3u8";
        if (hosterPlugin.isAttemptToDownloadUnofficialFullHD() && hlscontainer_chosen.getDownloadurl().contains(urlpartToReplace) && userWantsBest) {
            /*
             * 2022-03-14: This can get us an upscaled/higher quality version of that video according to discussion in public ticket:
             * https://github.com/ytdl-org/youtube-dl/issues/30136
             */
            logger.info("Attempting workaround to get 1080p quality even though it is not officially available");
            hlscontainer_chosen.setStreamURL(hlscontainer_chosen.getDownloadurl().replace(urlpartToReplace, "-video=12000000.m3u8"));
            hlscontainer_chosen.setWidth(1920);
            hlscontainer_chosen.setHeight(1080);
            hlscontainer_chosen.setFramerate(50);
        }
        qualityString = String.format("hls_%s@%d", hlscontainer_chosen.getResolution(), hlscontainer_chosen.getFramerate(25));
        final DownloadLink link = new DownloadLink(hosterPlugin, hosterPlugin.getHost(), hlscontainer_chosen.getDownloadurl(), true);
        if (param.getDownloadLink() != null) {
            /* Set properties from previous crawler on new DownloadLink instance e.g. date, title */
            link.setProperties(param.getDownloadLink().getProperties());
        }
        link.setProperty(BbcCom.PROPERTY_QUALITY_IDENTIFICATOR, qualityString);
        link.setFinalFileName(BbcCom.getFilename(link));
        setExtraInformation(link, sourceURL, vpid);
        distribute(link);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        ret.add(link);
        final boolean userWantsSubtitle = hosterPlugin.getPluginConfig().getBooleanProperty(BbcCom.SETTING_CRAWL_SUBTITLE, BbcCom.default_SETTING_CRAWL_SUBTITLE);
        if (userWantsSubtitle && !subtitles.isEmpty()) {
            /* Find first working subtitle as some may be broken or unavailable. */
            /* Sort map according to internal priority */
            this.displayBubbleNotification(notificationTitle, "Checking " + subtitles.size() + " subtitles...");
            final Map<Integer, Set<String>> sortedMap = new TreeMap<Integer, Set<String>>(subtitles);
            final List<String> sortedSubtitles = new ArrayList<String>();
            for (final Map.Entry<Integer, Set<String>> entry : sortedMap.entrySet()) {
                final Set<String> subtitlelist = entry.getValue();
                sortedSubtitles.addAll(subtitlelist);
            }
            /* List is sorted by priority 0-100 but we want the higher value first -> Revert order */
            Collections.reverse(sortedSubtitles);
            String subtitleValidated = null;
            long subtitleSize = 0;
            int index = 0;
            for (final String thisSubtitleURL : sortedSubtitles) {
                logger.info("Checking subtitle " + (index + 1) + "/" + sortedSubtitles.size() + " | URL: " + thisSubtitleURL);
                URLConnectionAdapter con = null;
                try {
                    con = br.openGetConnection(thisSubtitleURL);
                    if (con.isOK()) {
                        subtitleValidated = thisSubtitleURL;
                        subtitleSize = con.getCompleteContentLength();
                        break;
                    }
                } catch (final Throwable e) {
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
                if (this.isAbort()) {
                    throw new InterruptedException();
                }
                logger.info("Skipping invalid subtitle: " + thisSubtitleURL);
                index++;
            }
            if (subtitleValidated != null) {
                final DownloadLink subtitle = new DownloadLink(hosterPlugin, hosterPlugin.getHost(), subtitleValidated, true);
                subtitle.setFinalFileName(link.getFinalFileName().substring(0, link.getFinalFileName().lastIndexOf(".")) + ".ttml");
                if (subtitleSize > 0) {
                    subtitle.setDownloadSize(subtitleSize);
                }
                setExtraInformation(link, sourceURL, vpid);
                ret.add(subtitle);
                distribute(subtitle);
            } else {
                /* All existing subtitles are broken */
                final String text = "All " + subtitles.size() + " subtitles are broken";
                logger.info(text);
                this.displayBubbleNotification(notificationTitle, text);
            }
        }
        return ret;
    }

    private void setExtraInformation(final DownloadLink link, final String sourceURL, final String vpid) {
        link.setAvailable(true);
        if (sourceURL != null) {
            link.setContentUrl(sourceURL);
        }
        link.setProperty(BbcCom.PROPERTY_VIDEOID, vpid);
    }
}
