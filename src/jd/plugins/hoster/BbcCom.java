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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bbc.com" }, urls = { "http://bbcdecrypted/[a-z][a-z0-9]{7}" })
public class BbcCom extends PluginForHost {
    public BbcCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://www.bbc.com/terms/";
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
        return new Regex(link.getPluginPatternMatcher(), "/([^/]+)$").getMatch(0);
    }

    private String             hls_url                     = null;
    int                        numberofFoundMedia          = 0;
    public static final String PROPERTY_TITLE_FROM_CRAWLER = "decrypterfilename";
    public static final String PROPERTY_TITLE              = "title";
    public static final String PROPERTY_DATE               = "date";
    public static final String PROPERTY_TV_BRAND           = "brand";

    /** E.g. json instead of xml: http://open.live.bbc.co.uk/mediaselector/6/select/version/2.0/mediaset/pc/vpid/<vpid>/format/json */
    /**
     * Thanks goes to: https://github.com/rg3/youtube-dl/blob/master/youtube_dl/extractor/bbc.py
     *
     * @throws Exception
     */
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final String filenameBase = this.getFilenameBase(link);
        if (!link.isNameSet()) {
            link.setName(filenameBase + ".mp4");
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final ArrayList<HlsContainer> hlsContainers = new ArrayList<HlsContainer>();
        String qualityString = null;
        final Browser brc = br.cloneBrowser();
        brc.setFollowRedirects(true);
        /**
         * Look for special VPN "shadow ban": All info and streams are available but we'll run into error 403 when we try to access them.
         */
        int counterError403 = 0;
        boolean vpnBlocked = false;
        /* 2021-01-12: Website uses "/pc/" instead of "/iptv-all/" */
        this.br.getPage("https://open.live.bbc.co.uk/mediaselector/6/select/version/2.0/mediaset/iptv-all/vpid/" + getFID(link) + "/format/json");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> root = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final String result = (String) root.get("result");
        if (StringUtils.equalsIgnoreCase(result, "geolocation")) {
            errorGeoBlocked();
        }
        final List<Map<String, Object>> mediaList = (List<Map<String, Object>>) root.get("media");
        for (final Map<String, Object> media : mediaList) {
            final String kind = (String) media.get("kind");
            if (!kind.matches("audio|video")) {
                continue;
            }
            final List<Map<String, Object>> connections = (List<Map<String, Object>>) media.get("connection");
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
                    counterError403++;
                    vpnBlocked = true;
                } else if (results != null && !results.isEmpty()) {
                    hlsContainers.addAll(results);
                }
            }
        }
        if (hlsContainers == null || hlsContainers.isEmpty()) {
            if (vpnBlocked) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "VPN blocked");
            }
            logger.warning("Failed any downloadable content");
            return AvailableStatus.TRUE;
        }
        /* Find working HLS URL */
        final String configuredPreferredVideoHeightStr = getConfiguredVideoHeight();
        final String configuredPreferredVideoFramerateStr = getConfiguredVideoFramerate();
        HlsContainer hlscontainer_chosen = null;
        final boolean userWantsOver720p;
        if (configuredPreferredVideoHeightStr.matches("\\d+")) {
            /* Look for user-preferred quality */
            final int configuredPreferredVideoHeight = Integer.parseInt(configuredPreferredVideoHeightStr);
            if (configuredPreferredVideoHeight > 720 && (configuredPreferredVideoFramerateStr == null || Integer.parseInt(configuredPreferredVideoFramerateStr) >= 50)) {
                userWantsOver720p = true;
            } else {
                userWantsOver720p = false;
            }
            final String height_for_quality_selection = getHeightForQualitySelection(configuredPreferredVideoHeight);
            for (final HlsContainer hlscontainer_temp : hlsContainers) {
                final int height = hlscontainer_temp.getHeight();
                final String height_for_quality_selection_temp = getHeightForQualitySelection(height);
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
            userWantsOver720p = true;
        }
        final String urlpartToReplace = "-video=5070000.m3u8";
        if (isAttemptToDownloadUnofficialFullHD() && hlscontainer_chosen.getDownloadurl().contains(urlpartToReplace) && userWantsOver720p) {
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
        link.setFinalFileName(getFilenameBase(link) + "_" + qualityString + ".mp4");
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            /* 2017-04-25: Easy debug for user TODO: Remove once feedback is provided! */
            link.setComment(hlscontainer_chosen.getDownloadurl());
        }
        hls_url = hlscontainer_chosen.getDownloadurl();
        return AvailableStatus.TRUE;
    }

    private String getFilenameBase(final DownloadLink link) {
        if (link.hasProperty(PROPERTY_TITLE_FROM_CRAWLER)) {
            return link.getStringProperty(PROPERTY_TITLE_FROM_CRAWLER);
        } else if (link.hasProperty(PROPERTY_TITLE)) {
            String filenameBase = link.getStringProperty(PROPERTY_TITLE);
            if (link.hasProperty(PROPERTY_TV_BRAND)) {
                filenameBase = link.getStringProperty(PROPERTY_TV_BRAND) + "_" + filenameBase;
            }
            if (link.hasProperty(PROPERTY_DATE)) {
                filenameBase = link.getStringProperty(PROPERTY_DATE) + "_" + filenameBase;
            }
            return filenameBase;
        } else {
            /* Fallback */
            return getFID(link);
        }
    }

    private void errorGeoBlocked() throws PluginException {
        throw new PluginException(LinkStatus.ERROR_FATAL, "GEO-Blocked and/or account required");
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        checkFFmpeg(link, "Download a HLS Stream");
        dl = new HLSDownloader(link, br, hls_url);
        dl.startDownload();
    }

    /**
     * Given width may not always be exactly what we have in our quality selection but we need an exact value to make the user selection
     * work properly!
     */
    private String getHeightForQualitySelection(final int height) {
        final String heightselect;
        if (height > 0 && height <= 200) {
            heightselect = "170";
        } else if (height > 200 && height <= 300) {
            heightselect = "270";
        } else if (height > 300 && height <= 400) {
            heightselect = "360";
        } else if (height > 400 && height <= 500) {
            heightselect = "480";
        } else if (height > 500 && height <= 600) {
            heightselect = "570";
        } else if (height > 600 && height <= 800) {
            heightselect = "720";
        } else if (height > 800 && height <= 1080) {
            heightselect = "1080";
        } else {
            /* Either unknown quality or audio (0x0) */
            heightselect = Integer.toString(height);
        }
        return heightselect;
    }

    // @SuppressWarnings({ "static-access" })
    // private String formatDate(String input) {
    // final long date;
    // if (input.matches("\\d+")) {
    // date = Long.parseLong(input) * 1000;
    // } else {
    // final Calendar cal = Calendar.getInstance();
    // input += cal.get(cal.YEAR);
    // date = TimeFormatter.getMilliSeconds(input, "E '|' dd.MM.yyyy", Locale.GERMAN);
    // }
    // String formattedDate = null;
    // final String targetFormat = "yyyy-MM-dd";
    // Date theDate = new Date(date);
    // try {
    // final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
    // formattedDate = formatter.format(theDate);
    // } catch (Exception e) {
    // /* prevent input error killing plugin */
    // formattedDate = input;
    // }
    // return formattedDate;
    // }
    private String getConfiguredVideoFramerate() {
        final int selection = this.getPluginConfig().getIntegerProperty(SETTING_SELECTED_VIDEO_FORMAT, 0);
        final String selectedResolution = FORMATS[selection];
        if (selectedResolution.contains("x")) {
            final String framerate = selectedResolution.split("@")[1];
            return framerate;
        } else {
            /* BEST selection */
            return selectedResolution;
        }
    }

    private String getConfiguredVideoHeight() {
        final int selection = this.getPluginConfig().getIntegerProperty(SETTING_SELECTED_VIDEO_FORMAT, 0);
        final String selectedResolution = FORMATS[selection];
        if (selectedResolution.contains("x")) {
            final String height = new Regex(selectedResolution, "\\d+x(\\d+)").getMatch(0);
            return height;
        } else {
            /* BEST selection */
            return selectedResolution;
        }
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), SETTING_SELECTED_VIDEO_FORMAT, FORMATS, "Select preferred video resolution:").setDefaultValue(0));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SETTING_ATTEMPT_FHD_WORKAROUND, "Try to download 1080p 50fps streams if not officially available?\r\nWarning: This may lead to download failures!").setDefaultValue(default_SETTING_ATTEMPT_FHD_WORKAROUND));
    }

    /* The list of qualities displayed to the user */
    private static final String[] FORMATS                                = new String[] { "BEST", "1920x1080@50", "1920x1080@25", "1280x720@50", "1280x720@25", "1024x576@50", "1024x576@25", "768x432@50", "768x432@25", "640x360@25", "480x270@25", "320x180@25" };
    private static final String   SETTING_SELECTED_VIDEO_FORMAT          = "SELECTED_VIDEO_FORMAT";
    private static final String   SETTING_ATTEMPT_FHD_WORKAROUND         = "ATTEMPT_FDH_WORKAROUND";
    private static final boolean  default_SETTING_ATTEMPT_FHD_WORKAROUND = false;

    private boolean isAttemptToDownloadUnofficialFullHD() {
        return this.getPluginConfig().getBooleanProperty(SETTING_ATTEMPT_FHD_WORKAROUND, default_SETTING_ATTEMPT_FHD_WORKAROUND);
    }

    /** Delete this after 01-2023 */
    @Deprecated
    public List<HlsContainer> requestFileInformationOLD(final DownloadLink link) throws Exception {
        if (!link.isNameSet()) {
            link.setName(getFID(link) + ".mp4");
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String vpid = new Regex(link.getPluginPatternMatcher(), "bbcdecrypted/(.+)").getMatch(0);
        long filesize_temp = 0;
        // final ArrayList<String> hlsMasters = new ArrayList<String>();
        final List<HlsContainer> hlsContainers = new ArrayList<HlsContainer>();
        String qualityString = null;
        final Browser brc = br.cloneBrowser();
        brc.setFollowRedirects(true);
        /**
         * Look for special VPN "shadow ban": All info and streams are available but we'll run into error 403 when we try to access them.
         */
        boolean vpnBlocked = false;
        /* 2021-01-12: This code is not used anymore but please do NOT delete it! */
        /* HLS - try that first as it will give us higher bitrates */
        this.br.getPage("https://open.live.bbc.co.uk/mediaselector/5/select/version/2.0/mediaset/iptv-all/vpid/" + vpid);
        if (!this.br.getHttpConnection().isOK()) {
            /* RTMP #1 */
            /* 403 or 404 == geoblocked|offline|needsRTMP */
            /* Fallback e.g. vpids: p01dvmbh, b06s1fj9 */
            /* Possible "device" strings: "pc", "iptv-all", "journalism-pc" */
            this.br.getPage("https://open.live.bbc.co.uk/mediaselector/5/select/version/2.0/mediaset/pc/vpid/" + vpid);
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String transferformat = null;
        // int filesize_max = 0;
        int bitrate_max = 0;
        int bitrate_temp = 0;
        /* Allow audio download if there is no video available at all --> We probably have a podcast then. */
        final boolean allowAudio = !this.br.containsHTML("kind=\"video\"");
        /* Find BEST possible quality throughout different streaming protocols. */
        final String media[] = this.br.getRegex("<media(.*?)</media>").getColumn(0);
        numberofFoundMedia = media.length;
        final HashSet<String> testedM3U8 = new HashSet<String>();
        for (final String mediasingle : media) {
            final String kind = new Regex(mediasingle, "kind=\"([a-z]+)\"").getMatch(0);
            final String bitrate_str = new Regex(mediasingle, "bitrate=\"(\\d+)\"").getMatch(0);
            final String filesize_str = new Regex(mediasingle, "media_file_size=\"(\\d+)\"").getMatch(0);
            if (bitrate_str == null) {
                /* Skip faulty items. */
                continue;
            } else if (!"video".equalsIgnoreCase(kind) && !("audio".equalsIgnoreCase(kind) && allowAudio)) {
                /* E.g. skip 'captions' [subtitles] and skip audio content if video content is available. */
                continue;
            }
            final String[] connections = new Regex(mediasingle, "(<connection.*?)/>").getColumn(0);
            if (connections == null || connections.length == 0) {
                /* Whatever - skip such a case */
                continue;
            }
            bitrate_temp = Integer.parseInt(bitrate_str);
            /* Filesize is not always given */
            if (filesize_str != null) {
                filesize_temp = Long.parseLong(filesize_str);
            } else {
                filesize_temp = 0;
            }
            if (bitrate_temp >= bitrate_max) {
                bitrate_max = bitrate_temp;
                /* Every protocol can have multiple 'mirrors' or even sub-protocols (http --> dash, hls, hds, directhttp) */
                for (final String connection : connections) {
                    transferformat = new Regex(connection, "transferFormat=\"([a-z]+)\"").getMatch(0);
                    if (transferformat == null) {
                        /* 2018-11-16: E.g. very old content (usually rtmp-only, flash-player also required via browser!) */
                        transferformat = new Regex(connection, "protocol=\"([A-Za-z]+)\"").getMatch(0);
                    }
                    if (transferformat == null || !transferformat.equals("hls")) {
                        /* Skip unsupported protocols */
                        continue;
                    }
                    String thisHLSMaster = new Regex(connection, "\"(https?://[^<>\"]+\\.m3u8[^<>\"]*?)\"").getMatch(0);
                    if (thisHLSMaster == null || !testedM3U8.add(thisHLSMaster)) {
                        continue;
                    }
                    List<HlsContainer> foundQualities = new ArrayList<HlsContainer>();
                    thisHLSMaster = Encoding.htmlDecode(thisHLSMaster);
                    if (thisHLSMaster.matches(".*/[^/]*?\\.ism(\\.hlsv2\\.ism)?/.*\\.m3u8.*")) {
                        // rewrite to check for all available formats
                        try {
                            final String id = new Regex(thisHLSMaster, "/([^/]*?)\\.ism(\\.hlsv2\\.ism)?/").getMatch(0);
                            final String rewrittenHLSURL = thisHLSMaster.replaceFirst("/[^/]*?\\.ism(\\.hlsv2\\.ism)?/.*\\.m3u8", "/" + id + ".ism/" + id + ".m3u8");
                            foundQualities = HlsContainer.getHlsQualities(brc, rewrittenHLSURL);
                        } catch (final Exception e) {
                            logger.log(e);
                        }
                    }
                    if (foundQualities == null || foundQualities.isEmpty()) {
                        try {
                            foundQualities = HlsContainer.getHlsQualities(brc, thisHLSMaster);
                        } catch (final Exception e) {
                            logger.log(e);
                        }
                    }
                    if ((foundQualities == null || foundQualities.isEmpty()) && brc.getHttpConnection().getResponseCode() == 403) {
                        vpnBlocked = true;
                    } else if (foundQualities != null && !foundQualities.isEmpty()) {
                        hlsContainers.addAll(foundQualities);
                    }
                }
            }
        }
        if (vpnBlocked) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "VPN blocked");
        }
        final String filenameBase = this.getFilenameBase(link);
        if (qualityString != null) {
            link.setFinalFileName(filenameBase + "_" + qualityString + ".mp4");
        } else {
            link.setName(filenameBase + ".mp4");
        }
        if (filesize_temp > 0) {
            /* 2017-04-25: Changed from BEST by filesize to BEST by bitrate --> Filesize is not always given for BEST bitrate */
            link.setDownloadSize(filesize_temp);
        }
        return hlsContainers;
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