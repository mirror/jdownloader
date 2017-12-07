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
import java.util.Scanner;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "ndr.de" }, urls = { "http://ndrdecrypted\\.de/\\d+" })
public class NdrDe extends PluginForHost {
    public NdrDe(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "https://www.ndr.de/service/impressum/index.html";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME       = true;
    private static final int     FREE_MAXCHUNKS    = 0;
    private static final int     FREE_MAXDOWNLOADS = -1;
    private static final String  Q_SUBTITLES       = "Q_SUBTITLES";
    private static final String  Q_BEST            = "Q_BEST";
    private static final String  Q_LOW             = "Q_LOW";
    private static final String  Q_HIGH            = "Q_HIGH";
    private static final String  Q_VERYHIGH        = "Q_VERYHIGH";
    private static final String  Q_HD              = "Q_HD";
    public static final String   FAST_LINKCHECK    = "FAST_LINKCHECK";
    private String               DLLINK            = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getStringProperty("mainlink", null));
        DLLINK = link.getStringProperty("directlink", null);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = link.getStringProperty("decryptedfilename", null);
        link.setFinalFileName(filename);
        URLConnectionAdapter con = null;
        try {
            try {
                con = br.openHeadConnection(DLLINK);
            } catch (final BrowserException e) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (!con.getContentType().contains("html")) {
                link.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, boolean resumable, int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (downloadLink.getStringProperty("streamingType", null).equals("subtitle")) {
            br.getHeaders().put("Accept-Encoding", "identity");
            downloadLink.setDownloadSize(0);
            resumable = false;
            maxchunks = 1;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (br.getHttpConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 3 * 60 * 1000l);
            } else if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 3 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
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
                downloadLink.setFinalFileName(downloadLink.getStringProperty("decryptedfilename", null).replace(".xml", ".srt"));
            }
        }
    }

    /**
     * Converts the ZDF/WDR(some of them) Closed Captions subtitles to SRT subtitles. It runs after the completed download.
     *
     * @return The success of the conversion.
     */
    public static boolean convertSubtitle(final DownloadLink downloadlink) {
        final File source = new File(downloadlink.getFileOutput());
        boolean success = false;
        BufferedWriter dest = null;
        try {
            File output = new File(source.getAbsolutePath().replace(".xml", ".srt"));
            try {
                if (output.exists() && output.delete() == false) {
                    return false;
                }
                dest = new BufferedWriter(new FileWriter(output));
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
            if (xmlContent.contains("<ebuttm:documentEbuttVersion>")) {
                success = jd.plugins.hoster.BrDe.convertSubtitleBrOnlineDe(downloadlink, xmlContent, 0);
            } else {
                /* Subtitle type used in ZdfDeMediathek and WdrDeMediathek, NdrDe */
                final String[][] matches = new Regex(xmlContent, "<p begin=\"([^<>\"]*)\" end=\"([^<>\"]*)\" tts:textAlign=\"center\">?(.*?)</p>").getMatches();
                try {
                    final int starttime = 0;
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
                        final String[][] textReplaces = new Regex(text, "color=\"#([A-Z0-9]+)\">(.*?)($|tts:)").getMatches();
                        if (textReplaces != null && textReplaces.length != 0) {
                            for (final String[] singleText : textReplaces) {
                                final String colorCode = singleText[0].trim();
                                final String plainText = singleText[1].trim();
                                final String completeNewText = "<font color=#" + colorCode + ">" + plainText + "</font>";
                                dest.write(completeNewText + lineseparator + lineseparator);
                            }
                        } else {
                            dest.write(text + lineseparator + lineseparator);
                        }
                    }
                    success = true;
                } catch (final Exception e) {
                    success = false;
                }
            }
        } finally {
            try {
                dest.close();
            } catch (final IOException e) {
            }
        }
        source.delete();
        return success;
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
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public String getDescription() {
        return "JDownloader's NDR Plugin helps downloading videoclips from ndr.de. NDR provides different video qualities.";
    }

    public static final boolean defaultFAST_LINKCHECK = false;

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_SUBTITLES, JDL.L("plugins.hoster.ndr.subtitles", "Download subtitle whenever possible")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        final ConfigEntry bestonly = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_BEST, JDL.L("plugins.hoster.ndr.best", "Load best version ONLY")).setDefaultValue(false);
        getConfig().addEntry(bestonly);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_LOW, JDL.L("plugins.hoster.ndr.loadlow", "Load low version")).setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_HIGH, JDL.L("plugins.hoster.ndr.loadhigh", "Load high version")).setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_VERYHIGH, JDL.L("plugins.hoster.ndr.loadveryhigh", "Load veryhigh version")).setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_HD, "Load HD version").setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FAST_LINKCHECK, JDL.L("plugins.hoster.ndr.fastlinkcheck", "Schnellen Linkcheck aktivieren?\r\n<html><b>WICHTIG: Dadurch erscheinen die Links schneller im Linkgrabber, aber die Dateigröße wird erst beim Downloadstart (oder manuellem Linkcheck) angezeigt.</b></html>")).setDefaultValue(defaultFAST_LINKCHECK));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}