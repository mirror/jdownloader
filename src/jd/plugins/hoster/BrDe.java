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
package jd.plugins.hoster;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Scanner;
import java.util.TimeZone;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.translate._JDT;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "br-online.de" }, urls = { "http://brdecrypted\\-online\\.de/\\?format=(mp4|xml)\\&quality=\\d+x\\d+\\&hash=[a-z0-9]+" })
public class BrDe extends PluginForHost {
    public BrDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String  dllink             = null;
    private boolean geo_or_age_blocked = false;
    private boolean server_issues      = false;

    @Override
    public String getAGBLink() {
        return "https://www.br.de/unternehmen/service/kontakt/index.html";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        final String startLink = downloadLink.getStringProperty("mainlink");
        if (downloadLink.getBooleanProperty("offline", false) || startLink == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dllink = downloadLink.getStringProperty("direct_link", null);
        geo_or_age_blocked = false;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(startLink);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = downloadLink.getStringProperty("plain_filename", null);
        dllink = Encoding.htmlDecode(dllink.trim());
        downloadLink.setFinalFileName(filename);
        URLConnectionAdapter con = null;
        try {
            br.getHeaders().put("Accept-Encoding", "identity");
            con = br.openGetConnection(dllink);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                if (con.getResponseCode() == 403) {
                    /* E.g. content is not available before 10PM (Germany). */
                    geo_or_age_blocked = true;
                } else {
                    server_issues = true;
                }
            }
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (geo_or_age_blocked) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "GEO-or age blocked", 30 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("Accept-Encoding", "identity");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (this.dl.startDownload()) {
            this.postprocess(downloadLink);
        }
    }

    private void postprocess(final DownloadLink downloadLink) {
        if ("subtitle".equals(downloadLink.getStringProperty("streamingType", null))) {
            if (!convertSubtitle(downloadLink)) {
                logger.severe("Subtitle conversion failed!");
            } else {
                downloadLink.setFinalFileName(downloadLink.getStringProperty("plain_filename", null).replace(".xml", ".srt"));
            }
        }
    }

    /**
     * Converts the BR Closed Captions subtitles to SRT subtitles. It runs after the completed download.
     *
     * @return The success of the conversion.
     */
    public boolean convertSubtitle(final DownloadLink downloadlink) {
        final File source = new File(downloadlink.getFileOutput());
        BufferedWriter dest;
        try {
            dest = new BufferedWriter(new FileWriter(new File(source.getAbsolutePath().replace(".xml", ".srt"))));
        } catch (IOException e1) {
            return false;
        }
        final StringBuilder xml = new StringBuilder();
        int counter = 1;
        final String lineseparator = System.getProperty("line.separator");
        Scanner in = null;
        try {
            in = new Scanner(new FileReader(source));
            while (in.hasNext()) {
                xml.append(in.nextLine() + lineseparator);
            }
        } catch (Exception e) {
            return false;
        } finally {
            in.close();
        }
        final String xmlContent = xml.toString();
        final boolean success = convertSubtitleBrOnlineDe(downloadlink, xmlContent, 0);
        return success;
    }

    /**
     * Converts the BR Closed Captions subtitles to SRT subtitles. It runs after the completed download.
     *
     * @return The success of the conversion.
     */
    public static boolean convertSubtitleBrOnlineDe(final DownloadLink downloadlink, final String xmlContent, long offset_reduce_milliseconds) {
        final File source = new File(downloadlink.getFileOutput());
        BufferedWriter dest;
        try {
            dest = new BufferedWriter(new FileWriter(new File(source.getAbsolutePath().replace(".xml", ".srt"))));
        } catch (IOException e1) {
            return false;
        }
        int counter = 1;
        final String lineseparator = System.getProperty("line.separator");
        try {
            /* Find hex color text --> code assignments */
            final HashMap<String, String> color_codes = new HashMap<String, String>();
            final String[][] found_color_codes = new Regex(xmlContent, "xml:id=\"([A-Za-z0-9]+)\"[^>]+tts:color=\"(#[A-Z0-9]+)\"").getMatches();
            if (found_color_codes != null && found_color_codes.length != 0) {
                for (final String[] color_info : found_color_codes) {
                    color_codes.put(color_info[0], color_info[1]);
                }
            }
            /* empty subtitle|subtitle with text */
            final String[] matches = new Regex(xmlContent, "(<tt:p[^>]*?xml:id=\"[A-Za-z0-9]+\".*?(?:end=\"\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\" />|</tt:p>))").getColumn(0);
            boolean offsetSet = false;
            for (final String info : matches) {
                dest.write(counter++ + lineseparator);
                final String startString = new Regex(info, "begin=\"(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\"").getMatch(0);
                final String endString = new Regex(info, "end=\"(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\"").getMatch(0);
                final Regex startInfo = new Regex(info, "begin=\"(\\d{2})(:\\d{2}:\\d{2})\\.(\\d{3})\"");
                // final Regex endInfo = new Regex(info, "end=\"(\\d{2})(:\\d{2}:\\d{2})\\.(\\d{3})\"");
                final String start_hours_source_string = startInfo.getMatch(0);
                // final String end_hours_source_string = endInfo.getMatch(0);
                final int start_hours_source = Integer.parseInt(start_hours_source_string);
                // final int end_hours_source = Integer.parseInt(end_hours_source_string);
                long start_milliseconds = timeStringToMilliseconds(startString);
                long end_milliseconds = timeStringToMilliseconds(endString);
                if (start_hours_source >= 9 && counter == 2 && !offsetSet) {
                    /* 1st case - correct offset hardcoded 10 hours */
                    offset_reduce_milliseconds = 10 * 60 * 60 * 1000l;
                    offsetSet = true;
                } else if (start_hours_source > 0 && counter == 2 && !offsetSet) {
                    /* 2nd case - correct offset dynamically */
                    offset_reduce_milliseconds = start_milliseconds;
                    offsetSet = true;
                }
                if (start_hours_source == 0 && counter == 2 && !offsetSet && offset_reduce_milliseconds != 0) {
                    /* Given offset is wrong --> Correct that */
                    offset_reduce_milliseconds = 0;
                    offsetSet = true;
                } else if (offset_reduce_milliseconds != 0) {
                    /* Correct offset via given offset_reduce_hours */
                    start_milliseconds -= offset_reduce_milliseconds;
                    /* Errorhandling for negative start values - should not happen with end values */
                    if (start_milliseconds < 0) {
                        start_milliseconds = 0;
                    }
                    end_milliseconds -= offset_reduce_milliseconds;
                    offsetSet = true;
                }
                final DateFormat output_date_format = new SimpleDateFormat("HH:mm:ss,SSS");
                /* Important or we will always have one hour too much! */
                output_date_format.setTimeZone(TimeZone.getTimeZone("GMT"));
                final String start_formatted = output_date_format.format(start_milliseconds);
                final String end_formatted = output_date_format.format(end_milliseconds);
                dest.write(start_formatted + " --> " + end_formatted + lineseparator);
                final String[][] texts = new Regex(info, "<tt:span[^>]*?style=\"([A-Za-z0-9]+)\">([^<>]*?)</tt:span>").getMatches();
                String text = "";
                int line_counter = 1;
                for (final String[] textinfo : texts) {
                    final String color = textinfo[0];
                    final String colorcode = color_codes.get(color);
                    String line = textinfo[1];
                    text += "<font color=" + colorcode + ">" + line + "</font>";
                    /* Add linebreak as long as we're not at the last line of this statement */
                    if (line_counter != texts.length) {
                        text += lineseparator;
                    }
                    line_counter++;
                }
                text = text.replaceAll("&apos;", "\\\\u0027");
                text = HTMLEntities.unhtmlentities(text);
                text = HTMLEntities.unhtmlAmpersand(text);
                text = HTMLEntities.unhtmlAngleBrackets(text);
                text = HTMLEntities.unhtmlSingleQuotes(text);
                text = HTMLEntities.unhtmlDoubleQuotes(text);
                dest.write(text + lineseparator + lineseparator);
            }
        } catch (Exception e) {
            return false;
        } finally {
            try {
                dest.close();
            } catch (IOException e) {
            }
        }
        source.delete();
        return true;
    }

    public static long timeStringToMilliseconds(final String timeString) {
        final Regex timeRegex = new Regex(timeString, "(\\d{2}):(\\d{2}):(\\d{2})\\.(\\d{3})");
        final String timeHours = timeRegex.getMatch(0);
        final String timeMinutes = timeRegex.getMatch(1);
        final String timeSeconds = timeRegex.getMatch(2);
        final String timeMilliseconds = timeRegex.getMatch(3);
        final long timeMillisecondsComplete = Long.parseLong(timeHours) * 60 * 60 * 1000 + Long.parseLong(timeMinutes) * 60 * 1000 + Long.parseLong(timeSeconds) * 1000 + Long.parseLong(timeMilliseconds);
        return timeMillisecondsComplete;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public String getDescription() {
        return "JDownloader's BR Plugin helps downloading videoclips from br.de. You can choose between different video qualities.";
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return BrDeConfigInterface.class;
    }

    public static interface BrDeConfigInterface extends PluginConfigInterface {
        public static class TRANSLATION {
            public String getFastLinkcheckEnabled_label() {
                return _JDT.T.lit_enable_fast_linkcheck();
            }

            public String getGrabSubtitleEnabled_label() {
                return _JDT.T.lit_add_subtitles();
            }

            public String getGrabAudio_label() {
                return _JDT.T.lit_add_audio();
            }

            public String getGrabBESTEnabled_label() {
                return _JDT.T.lit_add_only_the_best_video_quality();
            }

            public String getOnlyBestVideoQualityOfSelectedQualitiesEnabled_label() {
                return _JDT.T.lit_add_only_the_best_video_quality_within_user_selected_formats();
            }
            // public String getAddUnknownQualitiesEnabled_label() {
            // return _JDT.T.lit_add_unknown_formats();
            // }
        }

        public static final TRANSLATION TRANSLATION = new TRANSLATION();

        @DefaultBooleanValue(true)
        @Order(9)
        boolean isFastLinkcheckEnabled();

        void setFastLinkcheckEnabled(boolean b);

        @DefaultBooleanValue(false)
        @Order(10)
        boolean isGrabSubtitleEnabled();

        void setGrabSubtitleEnabled(boolean b);

        @DefaultBooleanValue(false)
        @Order(20)
        boolean isGrabBESTEnabled();

        void setGrabBESTEnabled(boolean b);

        @AboutConfig
        @DefaultBooleanValue(false)
        @Order(21)
        boolean isOnlyBestVideoQualityOfSelectedQualitiesEnabled();

        void setOnlyBestVideoQualityOfSelectedQualitiesEnabled(boolean b);

        // @DefaultBooleanValue(true)
        // @Order(21)
        // boolean isAddUnknownQualitiesEnabled();
        //
        // void setAddUnknownQualitiesEnabled(boolean b);
        @DefaultBooleanValue(true)
        @Order(90)
        boolean isGrabHTTPMp4XSVideoEnabled();

        void setGrabHTTPMp4XSVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(100)
        boolean isGrabHTTPMp4SVideoEnabled();

        void setGrabHTTPMp4SVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(110)
        boolean isGrabHTTPMp4MVideoEnabled();

        void setGrabHTTPMp4MVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(111)
        boolean isGrabHTTPMp4LVideoEnabled();

        void setGrabHTTPMp4LVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(112)
        boolean isGrabHTTPMp4XLVideoEnabled();

        void setGrabHTTPMp4XLVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(113)
        boolean isGrabHTTPMp4XXLVideoEnabled();

        void setGrabHTTPMp4XXLVideoEnabled(boolean b);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
