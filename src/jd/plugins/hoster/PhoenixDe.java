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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "phoenix.de", "tivi.de" }, urls = { "decrypted://phoenix\\.de/content/\\d+\\&quality=\\w+|decrypted://(www\\.)?zdf\\.de/ZDFmediathek/[^<>\"]*?beitrag/video/\\d+\\&quality=\\w+", "decrypted://tivi\\.de/content/\\d+\\&quality=\\w+" })
public class PhoenixDe extends PluginForHost {

    private String  dllink        = null;
    private boolean server_issues = false;

    public PhoenixDe(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("decrypted://", "http://"));
    }

    @Override
    public String getAGBLink() {
        return "http://www.phoenix.de/";
    }

    public static Browser prepBR(final Browser br) {
        br.getHeaders().put("User-Agent", "Opera/9.80 (Linux armv7l; HbbTV/1.1.1 (; Sony; KDL32W650A; PKG3.211EUA; 2013;); ) Presto/2.12.362 Version/12.11");
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        server_issues = false;
        final String filename;
        prepBR(this.br);
        dllink = link.getStringProperty("directURL", null);
        filename = link.getStringProperty("directName", null);
        if (filename == null || dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setFinalFileName(link.getStringProperty("directName", null));
        URLConnectionAdapter con = null;
        try {
            con = br.openHeadConnection(dllink);
            if (!con.getContentType().contains("html")) {
                link.setDownloadSize(con.getLongContentLength());
            } else {
                server_issues = true;
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
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        download(downloadLink);
    }

    private void setupRTMPConnection(String stream, DownloadInterface dl) {
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        rtmp.setUrl(stream);
        rtmp.setResume(true);
        rtmp.setRealTime();
    }

    private void download(final DownloadLink downloadLink) throws Exception {
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dllink.contains("hinweis_fsk")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Nur von 20-06 Uhr verfügbar!", 30 * 60 * 1000l);
        }
        if (dllink.startsWith("rtmp")) {
            dl = new RTMPDownload(this, downloadLink, dllink);
            setupRTMPConnection(dllink, dl);
            ((RTMPDownload) dl).startDownload();
        } else {
            boolean resume = true;
            int maxChunks = 0;
            if ("subtitle".equals(downloadLink.getStringProperty("streamingType", null))) {
                br.getHeaders().put("Accept-Encoding", "identity");
                downloadLink.setDownloadSize(0);
                resume = false;
                maxChunks = 1;
            }
            br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resume, maxChunks);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                if (br.getHttpConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 3 * 60 * 1000l);
                } else if (br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 3 * 60 * 1000l);
                }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
            }
            if (this.dl.startDownload()) {
                this.postprocess(downloadLink);
            }
        }
    }

    private void postprocess(final DownloadLink downloadLink) {
        if ("subtitle".equals(downloadLink.getStringProperty("streamingType", null))) {
            if (!convertSubtitle(downloadLink)) {
                logger.severe("Subtitle conversion failed!");
            } else {
                downloadLink.setFinalFileName(downloadLink.getStringProperty("directName", null).replace(".xml", ".srt"));
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
            /* Subtitle type used in ZdfDeMediathek and WdrDeMediathek, NdrDe */
            final String[][] matches = new Regex(xml.toString(), "<p begin=\"([^<>\"]*)\" end=\"([^<>\"]*)\"[^<>]*?>(.*?)</p>").getMatches();
            try {
                final int starttime = Integer.parseInt(downloadlink.getStringProperty("starttime", null));
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

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
    }

    @Override
    public String getDescription() {
        return "JDownloader's phoenix.de Plugin helps downloading videoclips from phoenix.de and tivi.de. Both websites provide different video qualities.";
    }

    private static final String Q_SUBTITLES                                         = "Q_SUBTITLES";
    private static final String Q_BEST                                              = "Q_BEST";
    private static final String Q_LOW                                               = "Q_LOW";
    private static final String Q_HIGH                                              = "Q_HIGH";
    private static final String Q_VERYHIGH                                          = "Q_VERYHIGH";
    private static final String Q_HD                                                = "Q_HD";
    private static final String FASTLINKCHECK                                       = "FASTLINKCHECK";
    public static final String  NEOMAGAZINROYALE_DE_ADD_ONLY_CURRENT_EPISODE        = "NEOMAGAZINROYALE_DE_ADD_ONLY_CURRENT_EPISODE";
    public static final boolean defaultNEOMAGAZINROYALE_DE_ADD_ONLY_CURRENT_EPISODE = false;

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_SUBTITLES, "Download subtitle whenever possible").setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        final ConfigEntry bestonly = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_BEST, "Load best version ONLY").setDefaultValue(false);
        getConfig().addEntry(bestonly);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_LOW, "Load low version").setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_HIGH, "Load high version").setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_VERYHIGH, "Load veryhigh version").setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_HD, "Load HD version").setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FASTLINKCHECK, "Aktiviere schnellen Linkcheck?\r\nFalls aktiv: Dateigrößen sind erst beim Downloadstart sichtbar!").setDefaultValue(false));
    }

}