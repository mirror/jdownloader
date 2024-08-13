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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;
import java.util.TimeZone;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.utils.StringUtils;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.downloader.hls.M3U8Playlist;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.translate._JDT;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "zdf.de" }, urls = { "decryptedmediathek://.+" })
public class ZdfDeMediathek extends PluginForHost {
    public static final String PROPERTY_hlsBandwidth     = "hlsBandwidth";
    public static final String PROPERTY_streamingType    = "streamingType";
    public static final String PROPERTY_title            = "title";
    public static final String PROPERTY_tv_show          = "tv_show";
    public static final String PROPERTY_date_formatted   = "date_formatted";
    public static final String PROPERTY_tv_station       = "tv_station";
    public static final String PROPERTY_convert_subtitle = "convertsubtitle";
    private String             dllink                    = null;

    public ZdfDeMediathek(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.VIDEO_STREAMING };
    }

    @Override
    public String getPluginContentURL(final DownloadLink link) {
        /* Expose direct-URLs to user. */
        return getContentURL(link);
    }

    private String getContentURL(final DownloadLink link) {
        return link.getPluginPatternMatcher().replaceFirst("(?i)decrypted://", "http://").replaceFirst("(?i)decryptedmediathek://", "http://");
    }

    @Override
    public String getAGBLink() {
        return "http://zdf.de";
    }

    public static Browser prepBR(final Browser br) {
        br.getHeaders().put("User-Agent", "Opera/9.80 (Linux armv7l; HbbTV/1.1.1 (; Sony; KDL32W650A; PKG3.211EUA; 2013;); ) Presto/2.12.362 Version/12.11");
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return this.requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        dllink = null;
        prepBR(this.br);
        /* New urls 2016-12-21 */
        dllink = getContentURL(link);
        if (StringUtils.isEmpty(dllink)) {
            /* This should never happen! */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // Workaround to prevent finalFileName(set by Decrypter) to be lost on download for non HLS files
        link.setFinalFileName(link.getStringProperty("directName", link.getName()));
        if (!isDownload) {
            if (StringUtils.containsIgnoreCase(dllink, "m3u8")) {
                checkFFProbe(link, "Download a HLS Stream");
                final HLSDownloader downloader = new HLSDownloader(link, br, dllink);
                final int hlsBandwidth = link.getIntegerProperty(ZdfDeMediathek.PROPERTY_hlsBandwidth, -1);
                if (hlsBandwidth > 0) {
                    for (M3U8Playlist playList : downloader.getPlayLists()) {
                        playList.setAverageBandwidth(hlsBandwidth);
                    }
                }
                final long estimatedSize = downloader.getEstimatedSize();
                if (estimatedSize > 0) {
                    link.setDownloadSize(estimatedSize);
                }
            } else {
                basicLinkCheck(br.cloneBrowser(), br.createHeadRequest(dllink), link, link.getFinalFileName(), null);
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        download(link);
    }

    private void download(final DownloadLink link) throws Exception {
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dllink.contains("hinweis_fsk")) {
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
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Dieses Video ist im Sinne des Jugendschutzes nur von 20.00 bis 6.00 Uhr verfügbar.", waitMillisUntilVideoIsAvailable);
        }
        if (dllink.startsWith("rtmp")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Unsupported streaming protocol");
        } else if (dllink.contains("m3u8")) {
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, dllink);
            dl.startDownload();
        } else {
            boolean resume = true;
            int maxChunks = 0;
            if (isSubtitle(link)) {
                br.getHeaders().put("Accept-Encoding", "identity");
                link.setDownloadSize(0);
                resume = false;
                maxChunks = 1;
            }
            br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxChunks);
            handleConnectionErrors(br, dl.getConnection());
            if (this.dl.startDownload()) {
                this.postprocess(link);
            }
        }
    }

    private boolean isSubtitle(final DownloadLink link) {
        final String streamingType = link.getStringProperty(PROPERTY_streamingType);
        if (streamingType != null && streamingType.matches("subtitle.*")) {
            return true;
        } else {
            return false;
        }
    }

    protected boolean looksLikeDownloadableContent(final URLConnectionAdapter con) {
        if (super.looksLikeDownloadableContent(con)) {
            return true;
        } else if (isSubtitleContent(con)) {
            /* Subtitle */
            return true;
        } else {
            return false;
        }
    }

    private static boolean isSubtitleContent(final URLConnectionAdapter con) {
        return con.getResponseCode() == 200 && (StringUtils.containsIgnoreCase(con.getContentType(), "text/xml") || StringUtils.containsIgnoreCase(con.getContentType(), "application/xml") || StringUtils.containsIgnoreCase(con.getContentType(), "text/vtt"));
    }

    private void postprocess(final DownloadLink link) {
        final boolean allowConvertSubtitle;
        if ("subtitle".equalsIgnoreCase(link.getStringProperty(PROPERTY_streamingType))) {
            /* Legacy handling for items added up to revision 48153 as all of them need to be converted from .xml to .srt. */
            allowConvertSubtitle = true;
        } else {
            allowConvertSubtitle = link.getBooleanProperty(PROPERTY_convert_subtitle, false);
        }
        if (this.isSubtitle(link) && getContentURL(link).toLowerCase(Locale.ENGLISH).endsWith(".xml") && allowConvertSubtitle) {
            if (!convertSubtitle(link)) {
                logger.severe("Subtitle conversion failed!");
            } else {
                link.setFinalFileName(link.getFinalFileName().replaceFirst("(?i)\\.xml$", ".srt"));
            }
        }
    }

    private boolean convertSubtitle(final DownloadLink link) {
        final File source = new File(link.getFileOutput());
        final StringBuilder xml = new StringBuilder();
        final String lineseparator = System.getProperty("line.separator");
        Scanner in = null;
        try {
            in = new Scanner(new InputStreamReader(new FileInputStream(source), "UTF-8"));
            while (in.hasNext()) {
                xml.append(in.nextLine() + lineseparator);
            }
        } catch (Exception e) {
            return false;
        } finally {
            in.close();
        }
        boolean success;
        final String xmlContent = xml.toString();
        /* They got two different subtitle formats */
        if (xmlContent.contains("<ebuttm:documentEbuttVersion>") || xmlContent.contains("<tts:documentEbuttVersion>")) {
            success = jd.plugins.hoster.ARDMediathek.convertSubtitleBrDe(this, link, xmlContent, 0);
        } else {
            /* Unknown subtitle type */
            success = false;
        }
        return success;
    }

    /**
     * Converts the ZDF/WDR(some of them) Closed Captions subtitles to SRT subtitles. It runs after the completed download.
     *
     * @return The success of the conversion.
     */
    public static boolean convertSubtitleWdr(final DownloadLink link) {
        final File source = new File(link.getFileOutput());
        BufferedWriter dest = null;
        try {
            File output = new File(source.getAbsolutePath().replace(".xml", ".srt"));
            try {
                if (output.exists() && output.delete() == false) {
                    return false;
                }
                dest = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output), "UTF-8"));
            } catch (IOException e1) {
                return false;
            }
            final StringBuilder xml = new StringBuilder();
            int counter = 1;
            final String lineseparator = System.getProperty("line.separator");
            Scanner in = null;
            try {
                in = new Scanner(new InputStreamReader(new FileInputStream(source), "UTF-8"));
                while (in.hasNext()) {
                    xml.append(in.nextLine() + lineseparator);
                }
            } catch (Exception e) {
                return false;
            } finally {
                in.close();
            }
            /* Subtitle type used in ZdfDeMediathek and WdrDeMediathek, NdrDe */
            final String[][] matches = new Regex(xml.toString(), "<p begin=\"([^<>\"]*)\" end=\"([^<>\"]*)\"[^<>]*?>(.*?)</p>").getMatches();
            try {
                final int starttime = Integer.parseInt(link.getStringProperty("starttime", null));
                for (String[] match : matches) {
                    dest.write(counter++ + lineseparator);
                    final Double start = Double.valueOf(match[0]) + starttime;
                    final Double end = Double.valueOf(match[1]) + starttime;
                    dest.write(convertSubtitleTime(start) + " --> " + convertSubtitleTime(end) + lineseparator);
                    String text = match[2].trim();
                    text = text.replaceAll(lineseparator, " ");
                    text = text.replaceAll("&amp;", "&");
                    text = text.replaceAll("&quot;", "\"");
                    text = text.replaceAll("&#39;", "'");
                    text = text.replaceAll("&apos;", "'");
                    text = text.replaceAll("<br />", lineseparator);
                    text = text.replace("</p>", "");
                    text = text.replace("<span ", "").replace("</span>", "");
                    final String[][] textReplaces = new Regex(text, "(tts:color=\"#([A-Z0-9]+)\">(.*?)($|tts:))").getMatches();
                    if (textReplaces != null && textReplaces.length != 0) {
                        for (final String[] singleText : textReplaces) {
                            final String originalText = singleText[0];
                            final String colorCode = singleText[1].trim();
                            final String plainText = singleText[2].trim();
                            final String completeNewText = "<font color=#" + colorCode + ">" + plainText + "</font>";
                            text = text.replace(originalText, completeNewText);
                        }
                    }
                    dest.write(text + lineseparator + lineseparator);
                }
            } catch (Exception e) {
                return false;
            }
        } finally {
            try {
                dest.close();
            } catch (IOException e) {
            }
        }
        source.delete();
        return true;
    }

    /**
     * Converts the the time of the ZDF format to the SRT format.
     *
     * @param time
     *            . The time from the ZDF XML.
     * @return The converted time as String.
     */
    private static String convertSubtitleTime(Double time) {
        String hour = "00";
        String minute = "00";
        String second = "00";
        String millisecond = "0";
        Integer itime = Integer.valueOf(time.intValue());
        // Hour
        Integer timeHour = Integer.valueOf(itime.intValue() / 3600);
        if (timeHour < 10) {
            hour = "0" + timeHour.toString();
        } else {
            hour = timeHour.toString();
        }
        // Minute
        Integer timeMinute = Integer.valueOf((itime.intValue() % 3600) / 60);
        if (timeMinute < 10) {
            minute = "0" + timeMinute.toString();
        } else {
            minute = timeMinute.toString();
        }
        // Second
        Integer timeSecond = Integer.valueOf(itime.intValue() % 60);
        if (timeSecond < 10) {
            second = "0" + timeSecond.toString();
        } else {
            second = timeSecond.toString();
        }
        // Millisecond
        millisecond = String.valueOf(time - itime).split("\\.")[1];
        if (millisecond.length() == 1) {
            millisecond = millisecond + "00";
        }
        if (millisecond.length() == 2) {
            millisecond = millisecond + "0";
        }
        if (millisecond.length() > 2) {
            millisecond = millisecond.substring(0, 3);
        }
        // Result
        String result = hour + ":" + minute + ":" + second + "," + millisecond;
        return result;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
    }

    @Override
    public String getDescription() {
        return "Lade Video- und Audioinhalte aus der ZDFMediathek herunter";
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return ZdfmediathekConfigInterface.class;
    }

    public static interface ZdfmediathekConfigInterface extends PluginConfigInterface {
        final String text_UseVideoResolutionAsQualityModifierForHTTPVideoStreams = "Use video resolution in http video stream filenames e.g. '1280x720' instead of 'hd'?";
        final String text_FastLinkcheckEnabled                                   = "Enable fast linkcheck?";
        final String text_GrabSubtitleEnabled                                    = "Grab subtitle?";
        final String text_GrabSubtitleForDisabledPeopleEnabled                   = "Grab subtitle for disabled people?";
        final String text_GrabVideoVersionAudioDeskription                       = "Grab video quality 'Audiodeskription'?";
        final String text_GrabVideoVersionOriginalAudio                          = "Grab video quality 'Original sound'?";

        public static class TRANSLATION {
            public String getUseVideoResolutionAsQualityModifierForHTTPVideoStreams_label() {
                return text_UseVideoResolutionAsQualityModifierForHTTPVideoStreams;
            }

            public String getFastLinkcheckEnabled_label() {
                return _JDT.T.lit_enable_fast_linkcheck();
            }

            public String getGrabSubtitleEnabled_label() {
                return text_GrabSubtitleEnabled;
            }

            public String getGrabSubtitleForDisabledPeopleEnabled_label() {
                return text_GrabSubtitleForDisabledPeopleEnabled;
            }

            public String getPreferredSubtitleType_label() {
                return "Preferred subtitle type";
            }

            public String getGrabAudio_label() {
                return _JDT.T.lit_add_audio();
            }

            public String getGrabVideoVersionAudioDeskription_label() {
                return text_GrabVideoVersionAudioDeskription;
            }

            public String getGrabVideoVersionOriginalAudio_label() {
                return text_GrabVideoVersionOriginalAudio;
            }

            public String getGrabBESTEnabled_label() {
                return _JDT.T.lit_add_only_the_best_video_quality();
            }

            public String getOnlyBestVideoQualityOfSelectedQualitiesEnabled_label() {
                return _JDT.T.lit_add_only_the_best_video_quality_within_user_selected_formats();
            }

            public String getAddUnknownQualitiesEnabled_label() {
                return _JDT.T.lit_add_unknown_formats();
            }
        }

        public static final TRANSLATION TRANSLATION = new TRANSLATION();

        @DefaultBooleanValue(false)
        @DescriptionForConfigEntry(text_UseVideoResolutionAsQualityModifierForHTTPVideoStreams)
        @Order(8)
        boolean isUseVideoResolutionAsQualityModifierForHTTPVideoStreams();

        void setUseVideoResolutionAsQualityModifierForHTTPVideoStreams(boolean b);

        @DefaultBooleanValue(true)
        @DescriptionForConfigEntry(text_FastLinkcheckEnabled)
        @Order(9)
        boolean isFastLinkcheckEnabled();

        void setFastLinkcheckEnabled(boolean b);

        @DefaultBooleanValue(false)
        @DescriptionForConfigEntry(text_GrabSubtitleEnabled)
        @Order(10)
        boolean isGrabSubtitleEnabled();

        void setGrabSubtitleEnabled(boolean b);

        @DefaultBooleanValue(false)
        @DescriptionForConfigEntry(text_GrabSubtitleForDisabledPeopleEnabled)
        @Order(11)
        boolean isGrabSubtitleForDisabledPeopleEnabled();

        void setGrabSubtitleForDisabledPeopleEnabled(boolean b);

        public static enum SubtitleType implements LabelInterface {
            WEBVTT {
                @Override
                public String getLabel() {
                    return "WEBVTT (.vtt)";
                }
            },
            XML {
                @Override
                public String getLabel() {
                    return "EBU-TT XML (.xml)";
                }
            },
            SRT {
                @Override
                public String getLabel() {
                    return "SRT [EBU-TT XML converted to srt (.srt)]";
                }
            };
        }

        @AboutConfig
        @DefaultEnumValue("WEBVTT")
        @Order(12)
        @DescriptionForConfigEntry("Preferred subtitle type (only has an effect if grab subtitles is enabled and a subtitle of the selected type is available).")
        SubtitleType getPreferredSubtitleType();

        void setPreferredSubtitleType(final SubtitleType type);

        @DefaultBooleanValue(false)
        @DescriptionForConfigEntry("Grab audio only version if available?")
        @Order(14)
        boolean isGrabAudio();

        void setGrabAudio(boolean b);

        @DefaultBooleanValue(false)
        @DescriptionForConfigEntry(text_GrabVideoVersionAudioDeskription)
        @Order(15)
        boolean isGrabVideoVersionAudioDeskription();

        void setGrabVideoVersionAudioDeskription(boolean b);

        @DefaultBooleanValue(false)
        @DescriptionForConfigEntry(text_GrabVideoVersionOriginalAudio)
        @Order(16)
        boolean isGrabVideoVersionOriginalAudio();

        void setGrabVideoVersionOriginalAudio(boolean b);

        @DefaultBooleanValue(false)
        @Order(20)
        boolean isGrabBESTEnabled();

        void setGrabBESTEnabled(boolean b);

        @AboutConfig
        @DefaultBooleanValue(false)
        @Order(21)
        boolean isOnlyBestVideoQualityOfSelectedQualitiesEnabled();

        void setOnlyBestVideoQualityOfSelectedQualitiesEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(22)
        boolean isAddUnknownQualitiesEnabled();

        void setAddUnknownQualitiesEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(30)
        boolean isGrabHLS170pVideoEnabled();

        void setGrabHLS170pVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(40)
        boolean isGrabHLS270pVideoEnabled();

        void setGrabHLS270pVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(50)
        boolean isGrabHLS360pVideoEnabled();

        void setGrabHLS360pVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(60)
        boolean isGrabHLS480pVideoEnabled();

        void setGrabHLS480pVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(70)
        boolean isGrabHLS570pVideoEnabled();

        void setGrabHLS570pVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(80)
        boolean isGrabHLS720pVideoEnabled();

        void setGrabHLS720pVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(81)
        boolean isGrabHLS1080pVideoEnabled();

        void setGrabHLS1080pVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(90)
        boolean isGrabHTTPMp4LowVideoEnabled();

        void setGrabHTTPMp4LowVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(91)
        boolean isGrabHTTPWebmLowVideoEnabled();

        void setGrabHTTPWebmLowVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(100)
        boolean isGrabHTTPMp4HighVideoEnabled();

        void setGrabHTTPMp4HighVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(101)
        boolean isGrabHTTPWebmHighVideoEnabled();

        void setGrabHTTPWebmHighVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(105)
        boolean isGrabHTTPMp4MediumVideoEnabled();

        void setGrabHTTPMp4MediumVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(106)
        boolean isGrabHTTPWebmMediumVideoEnabled();

        void setGrabHTTPWebmMediumVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(110)
        boolean isGrabHTTPMp4VeryHighVideoEnabled();

        void setGrabHTTPMp4VeryHighVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(111)
        boolean isGrabHTTPWebmVeryHighVideoEnabled();

        void setGrabHTTPWebmVeryHighVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(120)
        boolean isGrabHTTPMp4HDVideoEnabled();

        void setGrabHTTPMp4HDVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(121)
        boolean isGrabHTTPWebmHDVideoEnabled();

        void setGrabHTTPWebmHDVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(122)
        boolean isGrabHTTPMp4FHDVideoEnabled();

        void setGrabHTTPMp4FHDVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(123)
        boolean isGrabHTTPWebmFHDVideoEnabled();

        void setGrabHTTPWebmFHDVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(130)
        boolean isGrabHTTPMp4UHDVideoEnabled();

        void setGrabHTTPMp4UHDVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(131)
        boolean isGrabHTTPWebmUHDVideoEnabled();

        void setGrabHTTPWebmUHDVideoEnabled(boolean b);
    }
}