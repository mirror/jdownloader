//jDownloader - Downloadmanager
//Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.config.MediathekProperties;
import org.jdownloader.plugins.components.config.WatchboxDeConfigInterface;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.MediathekHelper;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "watchbox.de" }, urls = { "https?://(?:www\\.)?watchbox\\.de/(?:serien|filme)/[^<>\"]+\\d+\\.html" })
public class WatchboxDe extends PluginForDecrypt {
    private final HashMap<String, DownloadLink> foundQualitiesMap   = new HashMap<String, DownloadLink>();
    ArrayList<DownloadLink>                     decryptedLinks      = new ArrayList<DownloadLink>();
    /* Important: Keep this updated & keep this in order: Highest --> Lowest */
    private final List<String>                  all_known_qualities = Arrays.asList("hls_3104000_540", "hls_1620000_540", "hls_984000_360", "hls_666000_360", "hls_380000_180", "hls_221000_144");
    private final Map<String, Long>             heigth_to_bitrate   = new HashMap<String, Long>();
    {
        heigth_to_bitrate.put("144", 221000l);
        heigth_to_bitrate.put("180", 380000l);
        /* keep in mind that sometimes there are two versions for 360 and/or 540! All versions in this map are the higher ones! */
        heigth_to_bitrate.put("360", 984000l);
        heigth_to_bitrate.put("540", 3104000l);
    }
    private String subtitleLink   = null;
    private String parameter      = null;
    private String title          = null;
    private String show           = null;
    private String description    = null;
    private int    seasonnumber   = -1;
    private int    episodenumber  = -1;
    private long   date_timestamp = -1;
    private String contentID      = null;

    public WatchboxDe(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("deprecation")
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final WatchboxDeConfigInterface cfg = PluginJsonConfig.get(org.jdownloader.plugins.components.config.WatchboxDeConfigInterface.class);
        final List<String> selectedQualities = new ArrayList<String>();
        final boolean addHLS144 = cfg.isGrabHLS144pVideoEnabled();
        final boolean addHLS180 = cfg.isGrabHLS180pVideoEnabled();
        final boolean addHLS360Lower = cfg.isGrabHLS360pLowerVideoEnabled();
        final boolean addHLS360 = cfg.isGrabHLS360pVideoEnabled();
        final boolean addHLS540Lower = cfg.isGrabHLS540pVideoEnabled();
        final boolean addHLS540 = cfg.isGrabHLS540pVideoEnabled();
        if (addHLS144) {
            selectedQualities.add("hls_" + heigth_to_bitrate.get("144") + "_144");
        }
        if (addHLS180) {
            selectedQualities.add("hls_" + heigth_to_bitrate.get("180") + "_180");
        }
        if (addHLS360Lower) {
            selectedQualities.add("hls_666000_360");
        }
        if (addHLS360) {
            selectedQualities.add("hls_" + heigth_to_bitrate.get("360") + "_360");
        }
        if (addHLS540Lower) {
            selectedQualities.add("hls_1620000_540");
        }
        if (addHLS540) {
            selectedQualities.add("hls_" + heigth_to_bitrate.get("540") + "_540");
        }
        crawl();
        handleUserQualitySelection(selectedQualities);
        if (decryptedLinks == null) {
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }
        if (decryptedLinks.size() == 0) {
            logger.info("Failed to find any links");
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        return decryptedLinks;
    }

    public static boolean isOffline(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 404;
    }

    /** Last revision with old handling: 38658 */
    private void crawl() throws Exception {
        br.getPage(parameter);
        if (isOffline(this.br)) {
            this.decryptedLinks.add(this.createOfflinelink(this.parameter));
            return;
        }
        this.contentID = br.getRegex("videoId\\s*?:\\s*?\"(\\d+)\"").getMatch(0);
        /*
         * 2018-04-11: E.g. hls:
         * https://vodwbusohls.secure.footprint.net/proxy/manifest-wb-clear/at-ch-de/<videoID>-1-12332.ism/fairplay.m3u8
         */
        /* 2018-04-11: E.g. dash: https://vodwbusodash.secure.footprint.net/proxy/manifest-wb-clear/at-ch-de/<videoID>-1-12332.ism/.mpd */
        final String hls_master = br.getRegex("hls\\s*?:\\s*?\\'(http[^<>\"\\']*?)\\'").getMatch(0);
        if (StringUtils.isEmpty(hls_master) || StringUtils.isEmpty(this.contentID)) {
            logger.warning("Decrypter broken for link: " + parameter);
            throw new DecrypterException("Plugin broken");
        }
        /* Tags: schema.org Supported/known types: TVEpisode, Movie */
        final String[] jsonSchemaOrgSchemas = br.getRegex("<script[^>]*?type=\"application/ld\\+json\"[^>]*?>(.*?)</script>").getColumn(0);
        for (final String jsonSchemaOrgSchema : jsonSchemaOrgSchemas) {
            try {
                final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(jsonSchemaOrgSchema);
                final String type = (String) entries.get("@type");
                if (StringUtils.isEmpty(type) || !type.matches("TVEpisode|Movie")) {
                    /* Skip invalid objects */
                    continue;
                }
                title = (String) entries.get("name");
                description = (String) entries.get("description");
                final String date = (String) JavaScriptEngineFactory.walkJson(entries, "releasedEvent/startDate");
                date_timestamp = TimeFormatter.getMilliSeconds(date, "yyyy-MM-dd", Locale.GERMAN);
                if ("TVEpisode".equalsIgnoreCase(type)) {
                    /* Series information should be available */
                    this.show = (String) JavaScriptEngineFactory.walkJson(entries, "partOfSeries/name");
                    this.seasonnumber = (int) JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(entries, "partOfSeason/seasonNumber"), -1);
                    this.episodenumber = (int) JavaScriptEngineFactory.toLong(entries.get("episodeNumber"), -1);
                }
                break;
            } catch (final Throwable e) {
            }
        }
        /* 2018-04-11: So far not given */
        subtitleLink = null;
        if (StringUtils.isEmpty(title)) {
            /* Fallback */
            title = this.contentID;
        }
        addHLS(hls_master);
    }

    private void addHLS(final String hls_master) throws Exception {
        final Browser hlsBR = br.cloneBrowser();
        hlsBR.getPage(hls_master);
        final List<HlsContainer> allHlsContainers = HlsContainer.getHlsQualities(hlsBR);
        for (final HlsContainer hlscontainer : allHlsContainers) {
            if (!hlscontainer.isVideo()) {
                /* 2018-04-11: For now, skip that one AAC mp4a.40.2 Container they have. */
                continue;
            }
            final String final_download_url = hlscontainer.getDownloadurl();
            addQuality(final_download_url, hlscontainer.getBandwidth(), hlscontainer.getWidth(), hlscontainer.getHeight());
        }
    }

    /* Returns quality identifier String, compatible with quality selection values. Format: protocol_bitrateCorrected_heightCorrected */
    private String getQualityIdentifier(final String directurl, long bitrate, int width, int height) {
        final String protocol;
        if (directurl.contains("m3u8")) {
            protocol = "hls";
        } else {
            protocol = "http";
        }
        /* Use this for quality selection as real resolution can be slightly different than the values which our users can select. */
        final int height_corrected = getHeightForQualitySelection(height);
        final long bitrate_corrected;
        if (bitrate > 0) {
            bitrate_corrected = getBitrateForQualitySelection(bitrate, directurl);
        } else {
            bitrate_corrected = getDefaultBitrateForHeight(height_corrected);
        }
        final String qualityStringForQualitySelection = protocol + "_" + bitrate_corrected + "_" + height_corrected;
        return qualityStringForQualitySelection;
    }

    private DownloadLink addQuality(final String directurl, long bandwidth, int width, int height) {
        /* Errorhandling */
        final String ext;
        if (directurl == null || ((width == 0 || height == 0) && !directurl.contains(".mp3"))) {
            /* Skip items with bad data. */
            return null;
        } else if (directurl.contains("mp3")) {
            ext = "mp3";
        } else {
            ext = "mp4";
        }
        final String protocol;
        if (directurl.contains("m3u8")) {
            protocol = "hls";
        } else {
            protocol = "http";
        }
        final WatchboxDeConfigInterface cfg = PluginJsonConfig.get(org.jdownloader.plugins.components.config.WatchboxDeConfigInterface.class);
        final String qualityStringForQualitySelection = getQualityIdentifier(directurl, bandwidth, width, height);
        final DownloadLink link = createDownloadlink(directurl.replaceAll("https?://", getHost() + "decrypted://"));
        final MediathekProperties data = link.bindData(MediathekProperties.class);
        data.setSourceHost(getHost());
        data.setChannel("WATCHBOX");
        data.setTitle(this.title);
        data.setResolution(width + "x" + height);
        data.setBandwidth(bandwidth);
        data.setProtocol(protocol);
        data.setReleaseDate(date_timestamp);
        data.setFileExtension(ext);
        if (!StringUtils.isEmpty(this.show)) {
            data.setShow(this.show);
        }
        if (this.seasonnumber > -1) {
            data.setSeasonNumber(this.seasonnumber);
        }
        if (this.episodenumber > -1) {
            data.setEpisodeNumber(this.episodenumber);
        }
        link.setFinalFileName(MediathekHelper.getMediathekFilename(link, data, false, true));
        link.setContentUrl(this.parameter);
        link.setProperty("itemId", this.contentID);
        if (description != null) {
            link.setComment(description);
        }
        if (cfg.isFastLinkcheckEnabled()) {
            link.setAvailable(true);
        }
        foundQualitiesMap.put(qualityStringForQualitySelection, link);
        return link;
    }

    private void handleUserQualitySelection(List<String> selectedQualities) {
        /* We have to re-add the subtitle for the best quality if wished by the user */
        HashMap<String, DownloadLink> finalSelectedQualityMap = new HashMap<String, DownloadLink>();
        final WatchboxDeConfigInterface cfg = PluginJsonConfig.get(org.jdownloader.plugins.components.config.WatchboxDeConfigInterface.class);
        if (cfg.isGrabBESTEnabled()) {
            /* User wants BEST only */
            finalSelectedQualityMap = findBESTInsideGivenMap(this.foundQualitiesMap);
        } else {
            final boolean grabUnknownQualities = cfg.isAddUnknownQualitiesEnabled();
            final boolean grab_best_out_of_user_selection = cfg.isOnlyBestVideoQualityOfSelectedQualitiesEnabled();
            boolean atLeastOneSelectedItemExists = false;
            for (final String quality : all_known_qualities) {
                if (selectedQualities.contains(quality) && foundQualitiesMap.containsKey(quality)) {
                    atLeastOneSelectedItemExists = true;
                }
            }
            if (!atLeastOneSelectedItemExists) {
                /* Only logger */
                logger.info("Possible user error: User selected only qualities which are not available --> Adding ALL");
            } else if (selectedQualities.size() == 0) {
                /* Errorhandling for bad user selection */
                logger.info("User selected no quality at all --> Adding ALL qualities instead");
                selectedQualities = all_known_qualities;
            }
            final Iterator<Entry<String, DownloadLink>> it = foundQualitiesMap.entrySet().iterator();
            while (it.hasNext()) {
                final Entry<String, DownloadLink> entry = it.next();
                final String quality = entry.getKey();
                final DownloadLink dl = entry.getValue();
                final boolean isUnknownQuality = !all_known_qualities.contains(quality);
                if (isUnknownQuality) {
                    logger.info("Found unknown quality: " + quality);
                    if (grabUnknownQualities) {
                        logger.info("Adding unknown quality: " + quality);
                        finalSelectedQualityMap.put(quality, dl);
                    }
                } else if (selectedQualities.contains(quality) || !atLeastOneSelectedItemExists) {
                    /* User has selected this particular quality OR we have to add it because user plugin settings were bad! */
                    finalSelectedQualityMap.put(quality, dl);
                }
            }
            /* Check if user maybe only wants the best quality inside his selected videoqualities. */
            if (grab_best_out_of_user_selection) {
                finalSelectedQualityMap = findBESTInsideGivenMap(finalSelectedQualityMap);
            }
        }
        /* Finally add selected URLs */
        final Iterator<Entry<String, DownloadLink>> it = finalSelectedQualityMap.entrySet().iterator();
        while (it.hasNext()) {
            final Entry<String, DownloadLink> entry = it.next();
            final DownloadLink dl = entry.getValue();
            /* 2018-04-11: No subtitles available so far */
            if (cfg.isGrabSubtitleEnabled() && !StringUtils.isEmpty(subtitleLink)) {
                final String plain_name = dl.getStringProperty("plain_name", null);
                final String subtitle_filename = plain_name + ".xml";
                final DownloadLink dl_subtitle = createDownloadlink(subtitleLink.replaceAll("https?://", getHost() + "decrypted://"));
                final MediathekProperties data_src = dl.bindData(MediathekProperties.class);
                final MediathekProperties data_subtitle = dl_subtitle.bindData(MediathekProperties.class);
                data_subtitle.setStreamingType("subtitle");
                data_subtitle.setSourceHost(data_src.getSourceHost());
                data_subtitle.setProtocol(data_src.getProtocol() + "sub");
                data_subtitle.setResolution(data_src.getResolution());
                data_subtitle.setTitle(data_src.getTitle());
                data_subtitle.setFileExtension("xml");
                dl_subtitle.setAvailable(true);
                dl_subtitle.setFinalFileName(subtitle_filename);
                dl_subtitle.setProperty("mainlink", parameter);
                dl_subtitle.setProperty("itemId", dl.getProperty("itemId", null));
                dl_subtitle.setContentUrl(parameter);
                decryptedLinks.add(dl_subtitle);
            }
            decryptedLinks.add(dl);
        }
        if (all_known_qualities.isEmpty()) {
            logger.info("Failed to find any quality at all");
        }
        if (decryptedLinks.size() > 1) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(title);
            fp.addLinks(decryptedLinks);
        }
    }

    private HashMap<String, DownloadLink> findBESTInsideGivenMap(final HashMap<String, DownloadLink> map_with_all_qualities) {
        HashMap<String, DownloadLink> newMap = new HashMap<String, DownloadLink>();
        DownloadLink keep = null;
        if (map_with_all_qualities.size() > 0) {
            for (final String quality : all_known_qualities) {
                keep = map_with_all_qualities.get(quality);
                if (keep != null) {
                    newMap.put(quality, keep);
                    break;
                }
            }
        }
        if (newMap.isEmpty()) {
            /* Failover in case of bad user selection or general failure! */
            newMap = map_with_all_qualities;
        }
        return newMap;
    }

    /* Returns default videoBitrate for width values. */
    private long getDefaultBitrateForHeight(final int height) {
        final String height_str = Integer.toString(height);
        long bitrateVideo;
        if (heigth_to_bitrate.containsKey(height_str)) {
            bitrateVideo = heigth_to_bitrate.get(height_str);
        } else {
            /* Unknown or audio */
            bitrateVideo = 0;
        }
        return bitrateVideo;
    }

    /**
     * Given width may not always be exactly what we have in our quality selection but we need an exact value to make the user selection
     * work properly!
     */
    private int getHeightForQualitySelection(final int height) {
        final int heightelect;
        if (height > 0 && height <= 160) {
            heightelect = 144;
        } else if (height > 160 && height <= 250) {
            heightelect = 180;
        } else if (height > 250 && height <= 500) {
            heightelect = 360;
        } else if (height > 500 && height <= 700) {
            heightelect = 540;
        } else {
            /* Either unknown quality or audio (0x0) */
            heightelect = height;
        }
        return heightelect;
    }

    /**
     * Given bandwidth may not always be exactly what we have in our quality selection but we need an exact value to make the user selection
     * work properly!
     */
    private long getBitrateForQualitySelection(final long bandwidth, final String directurl) {
        final long bandwidthselect;
        if (bandwidth > 0 && bandwidth <= 300000) {
            /* 144p */
            bandwidthselect = 221000;
        } else if (bandwidth > 300000 && bandwidth <= 450000) {
            /* 180p */
            bandwidthselect = 380000;
        } else if (bandwidth > 450000 && bandwidth <= 800000) {
            /* 360p lower */
            bandwidthselect = 666000;
        } else if (bandwidth > 800000 && bandwidth <= 1400000) {
            /* 360p */
            bandwidthselect = 984000;
        } else if (bandwidth > 1400000 && bandwidth <= 2800000) {
            /* 540p lower */
            bandwidthselect = 1620000;
        } else if (bandwidth > 2800000 && bandwidth <= 3800000) {
            /* 540p */
            bandwidthselect = 3104000;
        } else {
            /* Probably unknown quality */
            bandwidthselect = bandwidth;
        }
        return bandwidthselect;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}