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
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.appwork.utils.StringUtils;
import org.jdownloader.downloader.hds.HDSDownloader;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hds.HDSContainer;
import org.jdownloader.plugins.components.hls.HlsContainer;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "orf.at" }, urls = { "" })
public class ORFMediathek extends PluginForHost {
    private static final String TYPE_AUDIO                      = "(?i)https?://ooe\\.orf\\.at/radio/stories/(\\d+)/";
    /* Variables related to plugin settings */
    public static final String  Q_SUBTITLES                     = "Q_SUBTITLES";
    public static final boolean Q_SUBTITLES_default             = true;
    public static final String  Q_THUMBNAIL                     = "Q_THUMBNAIL";
    public static final boolean Q_THUMBNAIL_default             = true;
    public static final String  Q_BEST                          = "Q_BEST_2";
    public static final boolean Q_BEST_default                  = true;
    public static final String  Q_VERYLOW                       = "Q_VERYLOW";
    public static final boolean Q_VERYLOW_default               = true;
    public static final String  Q_LOW                           = "Q_LOW";
    public static final boolean Q_LOW_default                   = true;
    public static final String  Q_MEDIUM                        = "Q_MEDIUM";
    public static final String  Q_HIGH                          = "Q_HIGH";
    public static final String  Q_VERYHIGH                      = "Q_VERYHIGH";
    public static final String  VIDEO_SEGMENTS                  = "VIDEO_SEGMENTS";
    public static final String  SETTING_PREFER_VIDEO_GAPLESS    = "VIDEO_GAPLESS";
    public static final String  HTTP_STREAM                     = "HTTP_STREAM";
    public static final String  HLS_STREAM                      = "HLS_STREAM";
    public static final String  HDS_STREAM                      = "HDS_STREAM_2024_02_20";
    public static final String  PROPERTY_TITLE                  = "title";
    public static final String  PROPERTY_VIDEO_POSITION         = "video_position";
    public static final String  PROPERTY_VIDEO_POSITION_MAX     = "video_position_max";
    public static final String  PROPERTY_INTERNAL_QUALITY       = "directQuality";
    public static final String  PROPERTY_STREAMING_TYPE         = "streamingType";
    public static final String  PROPERTY_CONTENT_TYPE           = "contentType";
    public static final String  PROPERTY_QUALITY_HUMAN_READABLE = "directFMT";
    public static final String  PROPERTY_SEGMENT_ID             = "segment_id";
    public static final String  PROPERTY_VIDEO_ID               = "video_id";
    public static final String  PROPERTY_DELIVERY               = "delivery";
    public static final String  PROPERTY_DIRECTURL              = "directURL";
    public static final String  PROPERTY_SOURCEURL              = "mainlink";
    public static final String  PROPERTY_AGE_RESTRICTED         = "age_restricted";
    public static String        CONTENT_TYPE_IMAGE              = "image";
    public static String        CONTENT_TYPE_SUBTITLE           = "subtitle";
    public static String        CONTENT_TYPE_VIDEO              = "video";

    public ORFMediathek(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public String getAGBLink() {
        return "http://orf.at";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        if (link == null || link.getPluginPatternMatcher() == null) {
            return super.getLinkID(link);
        }
        final Regex typeAudio = new Regex(link.getPluginPatternMatcher(), TYPE_AUDIO);
        if (typeAudio.patternFind()) {
            return "orfmediathek://audio/" + typeAudio.getMatch(0);
        } else {
            return "orfmediathek://playlist/" + link.getStringProperty(PROPERTY_SEGMENT_ID) + "/contentType/" + getContentType(link) + "/" + link.getStringProperty(PROPERTY_VIDEO_ID) + "/delivery/" + link.getStringProperty(PROPERTY_DELIVERY) + "/streamingtype/" + link.getStringProperty(PROPERTY_STREAMING_TYPE) + "/quality/" + link.getStringProperty(PROPERTY_QUALITY_HUMAN_READABLE);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws IOException, PluginException {
        URLConnectionAdapter con = null;
        String dllink = null;
        if (link.getPluginPatternMatcher().matches(TYPE_AUDIO)) {
            br.getPage(link.getPluginPatternMatcher());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String filename = br.getRegex("role=\"article\">[\t\n\r ]+<h1>([^<>]*?)</h1>").getMatch(0);
            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
            }
            filename = Encoding.htmlDecode(filename).trim();
            filename += ".mp3";
            link.setFinalFileName(filename);
            final String audioID = br.getRegex("data\\-audio=\"(\\d+)\"").getMatch(0);
            if (audioID == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage("http://bits.orf.at/filehandler/static-api/json/current/data.json?file=" + audioID);
            dllink = br.getRegex("\"url\":\"(https?[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            try {
                con = br.openGetConnection(dllink);
                this.handleConnectionErrors(br, link, con);
                if (!looksLikeDownloadableContent(con, link)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
                link.setProperty(PROPERTY_DIRECTURL, dllink);
                return AvailableStatus.TRUE;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        } else {
            final String deprecatedPreSetFilename = link.getStringProperty("directName");
            if (deprecatedPreSetFilename != null) {
                link.setFinalFileName(deprecatedPreSetFilename);
            } else {
                link.setFinalFileName(getFormattedVideoFilename(link));
            }
        }
        if (isSubtitle(link) || isImage(link) || isVideoProgressiveStream(link)) {
            final Browser br2 = br.cloneBrowser();
            dllink = link.getStringProperty(PROPERTY_DIRECTURL);
            if (dllink == null) {
                /* Invalid item (this should never happen!). */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            try {
                con = br2.openHeadConnection(dllink);
                handleConnectionErrors(br2, link, con);
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    private void handleConnectionErrors(final Browser br, final DownloadLink link, final URLConnectionAdapter con) throws PluginException, IOException {
        if (!this.looksLikeDownloadableContent(con, link)) {
            br.followConnection(true);
            if (con.getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                handleURLBasedErrors(br, link);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken file?");
            }
        }
        handleURLBasedErrors(br, link);
    }

    private void handleURLBasedErrors(final Browser br, final DownloadLink link) throws PluginException {
        if (isAgeRestrictedByCurrentTime(br.getURL())) {
            // Account.setNextDayAsTempTimeout
            /*
             * E.g. progressive:
             * https://apasfpd.sf.apa.at/gp/online/14ed5a0157632458580f9bc7bfd1feba/1708297200/Jugendschutz0600b2000_Q8C.mp4
             */
            /* E.g. HLS: https://apasfiis.sf.apa.at/gp_nas/_definst_/nas/gp/online/Jugendschutz0600b2000_Q8C.mp4/playlist.m3u8 */
            // Last-Modified: Mon, 18 Mar 2019 23:11:36 GMT
            final long browserDateTimestamp = br.getCurrentServerTime(System.currentTimeMillis());
            final Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT+1"));
            if (browserDateTimestamp != -1) {
                c.setTime(new Date(browserDateTimestamp));
            }
            c.set(c.HOUR_OF_DAY, 20);
            c.set(c.MINUTE, 0);
            c.set(c.SECOND, 0);
            final long tsLater = c.getTimeInMillis();
            final long timeUntilLater = tsLater - System.currentTimeMillis();
            final long waitMillisUntilVideoIsAvailable;
            if (timeUntilLater > 0) {
                waitMillisUntilVideoIsAvailable = timeUntilLater;
            } else {
                /**
                 * This should never happen. Either server time is wrong/offset or user has wrong local OS time. </br>
                 * Video should already be available -> Wait static wait time
                 */
                waitMillisUntilVideoIsAvailable = 30 * 60 * 1000;
            }
            link.setProperty(ORFMediathek.PROPERTY_AGE_RESTRICTED, true);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Dieses Video ist im Sinne des Jugendschutzes nur von 20.00 bis 6.00 Uhr verfügbar.", waitMillisUntilVideoIsAvailable);
        } else {
            final String errortextGeoBlocked1 = "Error 403: GEO-blocked content or video temporarily unavailable via this streaming method. Check your orf.at plugin settings.";
            final String errortextGeoBlocked2 = "GEO-blocked";
            if (br.getHttpConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_FATAL, errortextGeoBlocked1);
            } else if (StringUtils.containsIgnoreCase(br.getURL(), "geoprotection_")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, errortextGeoBlocked2);
            } else if (StringUtils.containsIgnoreCase(br.getURL(), "nicht_verfuegbar_hr")) {
                /* 2023-11-27 */
                throw new PluginException(LinkStatus.ERROR_FATAL, errortextGeoBlocked2);
            }
        }
    }

    public static boolean isAgeRestrictedByCurrentTime(final String url) {
        return StringUtils.containsIgnoreCase(url, "/Jugendschutz");
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        download(link);
    }

    @SuppressWarnings("deprecation")
    private void download(final DownloadLink link) throws Exception {
        final String dllink = link.getStringProperty(PROPERTY_DIRECTURL);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (isSubtitle(link)) {
            /* Workaround for old downloadcore bug that can lead to incomplete files */
            br.getHeaders().put("Accept-Encoding", "identity");
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
            handleConnectionErrors(br, link, dl.getConnection());
            dl.startDownload();
        } else if ("hls".equals(link.getStringProperty(PROPERTY_DELIVERY)) && dllink.contains("playlist.m3u8")) {
            /* HLS playlist which should contain only one quality (for older items from tvthek.orf.at). */
            checkFFmpeg(link, "Download a HLS Stream");
            br.getPage(dllink);
            handleURLBasedErrors(br, link);
            final HlsContainer best = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(br));
            if (best == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = new HLSDownloader(link, br, best.getDownloadurl());
            dl.startDownload();
        } else if ("hls".equals(link.getStringProperty(PROPERTY_DELIVERY))) {
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, dllink);
            dl.startDownload();
        } else if ("hds".equals(link.getStringProperty(PROPERTY_DELIVERY))) {
            br.getPage(dllink);
            handleURLBasedErrors(br, link);
            final List<HDSContainer> all = HDSContainer.getHDSQualities(br);
            if (all == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final HDSContainer hit = HDSContainer.findBestVideoByResolution(all);
            if (hit == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            hit.write(link);
            final HDSDownloader dl = new HDSDownloader(link, br, hit.getFragmentURL()) {
                @Override
                protected URLConnectionAdapter onNextFragment(URLConnectionAdapter connection, int fragmentIndex) throws IOException, PluginException {
                    if (fragmentIndex == 1 && StringUtils.containsIgnoreCase(connection.getRequest().getLocation(), "geoprotection_")) {
                        /* GEO-blocked during download --> This should be a rare occurence */
                        connection.disconnect();
                        throw new PluginException(LinkStatus.ERROR_FATAL, "GEO-blocked");
                    }
                    return super.onNextFragment(connection, fragmentIndex);
                }
            };
            this.dl = dl;
            dl.setEstimatedDuration(hit.getDuration());
            dl.startDownload();
        } else if (StringUtils.startsWithCaseInsensitive(dllink, "rtmp")) {
            /* 2023-11-27: This should never happen */
            throw new PluginException(LinkStatus.ERROR_FATAL, "Unsupported protocol rtmp(e)");
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
            this.handleConnectionErrors(br, link, dl.getConnection());
            dl.startDownload();
        }
    }

    public static String getFormattedVideoFilename(final DownloadLink link) {
        final String ext;
        final boolean isImage = isImage(link);
        if (isSubtitle(link)) {
            ext = ".srt";
        } else if (isImage) {
            ext = ".jpeg";
        } else {
            ext = ".mp4";
        }
        final String title = link.getStringProperty(PROPERTY_TITLE);
        final int position = link.getIntegerProperty(PROPERTY_VIDEO_POSITION, -1);
        final int positionMax = link.getIntegerProperty(PROPERTY_VIDEO_POSITION_MAX, -1);
        final String streamingType = link.getStringProperty(PROPERTY_STREAMING_TYPE);
        final String delivery = link.getStringProperty(PROPERTY_DELIVERY);
        final String playlistID = link.getStringProperty(PROPERTY_VIDEO_ID);
        final String segmentID = link.getStringProperty(PROPERTY_SEGMENT_ID);
        final String fmtHumanReadable = link.getStringProperty(PROPERTY_QUALITY_HUMAN_READABLE);
        String indexStr = "";
        if (position != -1 && positionMax > 1) {
            indexStr = new DecimalFormat("00").format(position) + "_";
        }
        String filename;
        if (isImage) {
            filename = indexStr + title;
        } else {
            filename = indexStr + title + "@" + streamingType + delivery;
            filename += "_" + playlistID + "_" + segmentID;
            filename += "@" + fmtHumanReadable;
        }
        filename += ext;
        return filename;
    }

    public static String getContentType(final DownloadLink link) {
        final String streamingType = link.getStringProperty(PROPERTY_STREAMING_TYPE);
        if (StringUtils.equalsIgnoreCase(streamingType, "subtitle")) {
            /* For items added until including revision 48529. */
            return CONTENT_TYPE_SUBTITLE;
        } else {
            return link.getStringProperty(PROPERTY_CONTENT_TYPE);
        }
    }

    public static boolean isSubtitle(final DownloadLink link) {
        if (StringUtils.equalsIgnoreCase(getContentType(link), CONTENT_TYPE_SUBTITLE)) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isVideoProgressiveStream(final DownloadLink link) {
        if ("http".equals(link.getStringProperty(PROPERTY_STREAMING_TYPE)) && StringUtils.equalsIgnoreCase("progressive", link.getStringProperty("delivery"))) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isImage(final DownloadLink link) {
        if (StringUtils.equalsIgnoreCase(getContentType(link), CONTENT_TYPE_IMAGE)) {
            return true;
        } else {
            return false;
        }
    }

    protected boolean looksLikeDownloadableContent(final URLConnectionAdapter con, final DownloadLink link) {
        if (super.looksLikeDownloadableContent(con)) {
            return true;
        } else if (isSubtitle(link) && isSubtitleContent(con)) {
            return true;
        } else if (isSubtitle(link) && con.isOK()) {
            /* 2023-08-15 e.g. https://api-tvthek.orf.at/assets/subtitles/0158/88/3bca2b4fb96099bfd35871d61a63ab06342eacc6.srt */
            logger.info("Subtitle is missing [correct] content-type header: " + con.getURL());
            return true;
        } else {
            return false;
        }
    }

    private static boolean isSubtitleContent(final URLConnectionAdapter con) {
        return con.getResponseCode() == 200 && (StringUtils.containsIgnoreCase(con.getContentType(), "text/xml") || StringUtils.containsIgnoreCase(con.getContentType(), "text/vtt"));
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public String getDescription() {
        return "JDownloader's ORF Plugin helps downloading videos from on.orf.at. ORF provides different video qualities and types of media.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_SUBTITLES, "Download subtitle").setDefaultValue(Q_SUBTITLES_default));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_THUMBNAIL, "Download thumbnail").setDefaultValue(Q_THUMBNAIL_default));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        final ConfigEntry bestonly = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_BEST, "Load Best Version ONLY").setDefaultValue(Q_BEST_default);
        getConfig().addEntry(bestonly);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_VERYLOW, "Load very low version").setDefaultValue(Q_VERYLOW_default).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_LOW, "Load low version").setDefaultValue(Q_LOW_default).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_MEDIUM, "Load medium version").setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_HIGH, "Load high version").setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_VERYHIGH, "Load very high version").setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SETTING_PREFER_VIDEO_GAPLESS, "Prefer gapless video").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), VIDEO_SEGMENTS, "Load video segments").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), HTTP_STREAM, "Load http streams").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), HLS_STREAM, "Load hls streams").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), HDS_STREAM, "Load hds streams (unavailable since 2024-02-20)").setDefaultValue(false).setEnabled(false));
    }
}