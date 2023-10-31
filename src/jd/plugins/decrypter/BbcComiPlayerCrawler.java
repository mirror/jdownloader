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
import java.util.List;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.CrawledLink;
import jd.http.Browser;
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
        final ArrayList<HlsContainer> hlsContainers = new ArrayList<HlsContainer>();
        String qualityString = null;
        final Browser brc = br.cloneBrowser();
        brc.setFollowRedirects(true);
        /**
         * Look for special VPN "shadow ban": All info and streams are available but we'll run into error 403 when we try to access them.
         */
        int counterError403VPNBlocked = 0;
        /** E.g. json instead of xml: http://open.live.bbc.co.uk/mediaselector/6/select/version/2.0/mediaset/pc/vpid/<vpid>/format/json */
        /**
         * Thanks goes to: https://github.com/rg3/youtube-dl/blob/master/youtube_dl/extractor/bbc.py
         */
        /* 2021-01-12: Website uses "/pc/" instead of "/iptv-all/" */
        this.br.getPage("https://open.live.bbc.co.uk/mediaselector/6/select/version/2.0/mediaset/iptv-all/vpid/" + vpid + "/format/json");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> root = restoreFromString(br.toString(), TypeRef.MAP);
        final String result = (String) root.get("result");
        if (StringUtils.equalsIgnoreCase(result, "geolocation")) {
            // BbcCom.errorGeoBlocked();
            throw new DecrypterRetryException(RetryReason.GEO, vpid, "This content is not available in your country!");
        }
        String subtitleURL = null;
        final List<Map<String, Object>> mediaList = (List<Map<String, Object>>) root.get("media");
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
                    /* 2021-01-12: TODO: Why do we do this replace again? Higher quality? */
                    List<HlsContainer> results = new ArrayList<HlsContainer>();
                    if (thisHLSMaster.matches(".*/[^/]*?\\.ism(\\.hlsv2\\.ism)?/.*\\.m3u8.*")) {
                        // rewrite to check for all available formats
                        /* 2021-01-12: TODO: This doesn't work anymore?! */
                        try {
                            final String id = new Regex(thisHLSMaster, "/([^/]*?)\\.ism(\\.hlsv2\\.ism)?/").getMatch(0);
                            final String thisHLSMasterCorrected = thisHLSMaster.replaceFirst("/[^/]*?\\.ism(\\.hlsv2\\.ism)?/.*\\.m3u8", "/" + id + ".ism/" + id + ".m3u8");
                            results = HlsContainer.getHlsQualities(brc, thisHLSMasterCorrected);
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
                }
            } else if (kind.equalsIgnoreCase("captions")) {
                subtitleURL = JavaScriptEngineFactory.walkJson(connections, "{0}/href").toString();
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
        String sourceURL = null;
        try {
            sourceURL = ((CrawledLink) param.getSource()).getSourceUrls()[0];
        } catch (final Throwable ignore) {
        }
        final DownloadLink link = new DownloadLink(hosterPlugin, hosterPlugin.getHost(), hlscontainer_chosen.getDownloadurl(), true);
        if (param.getDownloadLink() != null) {
            /* Set properties from previous crawler on new DownloadLink instance e.g. date, title */
            link.setProperties(param.getDownloadLink().getProperties());
        }
        link.setProperty(BbcCom.PROPERTY_QUALITY_IDENTIFICATOR, qualityString);
        link.setFinalFileName(BbcCom.getFilename(link));
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        ret.add(link);
        if (subtitleURL != null && hosterPlugin.getPluginConfig().getBooleanProperty(BbcCom.SETTING_CRAWL_SUBTITLE, BbcCom.default_SETTING_CRAWL_SUBTITLE)) {
            final DownloadLink subtitle = new DownloadLink(hosterPlugin, hosterPlugin.getHost(), subtitleURL, true);
            subtitle.setFinalFileName(link.getFinalFileName().substring(0, link.getFinalFileName().lastIndexOf(".")) + ".ttml");
            ret.add(subtitle);
        }
        /* Set additional properties that are supposed to be set on all results */
        for (final DownloadLink crawledresult : ret) {
            crawledresult.setAvailable(true);
            if (sourceURL != null) {
                crawledresult.setContentUrl(sourceURL);
            }
            crawledresult.setProperty(BbcCom.PROPERTY_VIDEOID, vpid);
            if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                /* 2017-04-25: Easy debug for user TODO: Remove once feedback is provided! */
                crawledresult.setComment(crawledresult.getPluginPatternMatcher());
            }
        }
        return ret;
    }
}
