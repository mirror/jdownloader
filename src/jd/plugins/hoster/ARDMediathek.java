//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Scanner;

import jd.PluginWrapper;
import jd.http.Browser;
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
import jd.utils.JDUtilities;

import org.jdownloader.controlling.ffmpeg.json.StreamInfo;
import org.jdownloader.downloader.hls.HLSDownloader;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ard.de" }, urls = { "ardmediathek://.+" })
public class ARDMediathek extends PluginForHost {
    private String  dllink        = null;
    private boolean server_issues = false;

    public ARDMediathek(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.ardmediathek.de/ard/servlet/content/3606532";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("ardmediathek://", "http://"));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        dllink = downloadLink.getDownloadURL();
        /* Load this plugin as we use functions of it. */
        JDUtilities.getPluginForHost("br-online.de");
        String finalName = downloadLink.getStringProperty("directName", null);
        if (finalName == null) {
            /* TODO */
            finalName = getTitle(br) + ".mp4";
        }
        downloadLink.setFinalFileName(finalName);
        br.setFollowRedirects(true);
        if (dllink.contains(".m3u8")) {
            checkFFProbe(downloadLink, "Download a HLS Stream");
            final HLSDownloader downloader = new HLSDownloader(downloadLink, br, dllink);
            final StreamInfo streamInfo = downloader.getProbe();
            if (streamInfo == null) {
                // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                server_issues = true;
            } else {
                final long estimatedSize = downloader.getEstimatedSize();
                if (estimatedSize > 0) {
                    downloadLink.setDownloadSize(estimatedSize);
                }
            }
        } else {
            URLConnectionAdapter con = null;
            try {
                br.getHeaders().put("Accept-Encoding", "identity");
                con = br.openHeadConnection(dllink);
                if (con.isOK() && !con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                } else {
                    server_issues = true;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    private String getTitle(Browser br) {
        String title = br.getRegex("<div class=\"MainBoxHeadline\">([^<]+)</").getMatch(0);
        String titleUT = br.getRegex("<span class=\"BoxHeadlineUT\">([^<]+)</").getMatch(0);
        if (title == null) {
            title = br.getRegex("<title>ard\\.online \\- Mediathek: ([^<]+)</title>").getMatch(0);
        }
        if (title == null) {
            title = br.getRegex("<h2>(.*?)</h2>").getMatch(0);
        }
        if (title != null) {
            title = Encoding.htmlDecode(title + (titleUT != null ? "__" + titleUT.replaceAll(":$", "") : "").trim());
        }
        if (title == null) {
            title = "UnknownTitle_" + System.currentTimeMillis();
        }
        return title;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        download(downloadLink);
    }

    private void download(final DownloadLink downloadLink) throws Exception {
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dllink.contains(".m3u8")) {
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            dl = new HLSDownloader(downloadLink, br, dllink);
            dl.startDownload();
        } else {
            br.setFollowRedirects(true);
            // Workaround to avoid DOWNLOAD INCOMPLETE errors
            boolean resume = true;
            int maxChunks = 0;
            if ("subtitle".equals(downloadLink.getStringProperty("streamingType", null))) {
                br.getHeaders().put("Accept-Encoding", "identity");
                downloadLink.setDownloadSize(0);
                resume = false;
                maxChunks = 1;
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resume, maxChunks);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "This Content is not longer available!");
                }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
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
     * Converts the ARD Closed Captions subtitles to SRT subtitles. It runs after the completed download.
     *
     * @return The success of the conversion.
     */
    private boolean convertSubtitle(final DownloadLink downloadlink) {
        final File source = new File(downloadlink.getFileOutput());
        final StringBuilder xml = new StringBuilder();
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
        boolean success;
        final String xmlContent = xml.toString();
        /* They got two different subtitle formats */
        if (xmlContent.contains("<ebuttm:documentEbuttVersion>")) {
            success = jd.plugins.hoster.BrDe.convertSubtitleBrOnlineDe(downloadlink, xmlContent, 0);
        } else {
            int counter = 1;
            BufferedWriter dest;
            try {
                dest = new BufferedWriter(new FileWriter(new File(source.getAbsolutePath().replace(".xml", ".srt"))));
            } catch (IOException e1) {
                return false;
            }
            final String[] matches = new Regex(xmlContent, "(<p id=\"subtitle\\d+\".*?</p>)").getColumn(0);
            try {
                /* Find style --> color assignments */
                final HashMap<String, String> styles_color_names = new HashMap<String, String>();
                final String[] styles = new Regex(xmlContent, "(<style id=\"s\\d+\".*?/>)").getColumn(0);
                if (styles != null) {
                    for (final String style_info : styles) {
                        final String style_id = new Regex(style_info, "<style id=\"(s\\d+)\"").getMatch(0);
                        final String style_color = new Regex(style_info, "tts:color=\"([a-z]+)\"").getMatch(0);
                        if (style_id != null && style_color != null) {
                            styles_color_names.put(style_id, style_color);
                        }
                    }
                }
                styles_color_names.put("s1", "black");
                for (final String info : matches) {
                    dest.write(counter++ + lineseparator);
                    final DecimalFormat df = new DecimalFormat("00");
                    final Regex startInfo = new Regex(info, "begin=\"(\\d{2}):([^<>\"]*?)\"");
                    final Regex endInfo = new Regex(info, "end=\"(\\d{2}):([^<>\"]*?)\"");
                    int startHour = Integer.parseInt(startInfo.getMatch(0));
                    int endHour = Integer.parseInt(endInfo.getMatch(0));
                    startHour -= 10;
                    endHour -= 10;
                    final String start = df.format(startHour) + ":" + startInfo.getMatch(1).replace(".", ",");
                    final String end = df.format(endHour) + ":" + endInfo.getMatch(1).replace(".", ",");
                    dest.write(start + " --> " + end + lineseparator);
                    final String[][] color_texts = new Regex(info, "style=\"(s\\d+)\">?(.*?)</p>").getMatches();
                    String text = "";
                    for (final String[] style_text : color_texts) {
                        final String style = style_text[0];
                        text = style_text[1];
                        text = text.replaceAll(lineseparator, " ");
                        text = text.replaceAll("&apos;", "\\\\u0027");
                        text = Encoding.unicodeDecode(text);
                        text = HTMLEntities.unhtmlentities(text);
                        text = HTMLEntities.unhtmlAmpersand(text);
                        text = HTMLEntities.unhtmlAngleBrackets(text);
                        text = HTMLEntities.unhtmlSingleQuotes(text);
                        text = HTMLEntities.unhtmlDoubleQuotes(text);
                        text = text.replaceAll("<br />", lineseparator);
                        text = text.replaceAll("</?(p|span)>?", "");
                        text = text.trim();
                        final String remove_color = new Regex(text, "( ?tts:color=\"[a-z0-9]+\">)").getMatch(0);
                        if (remove_color != null) {
                            text = text.replace(remove_color, "");
                        }
                        final String color = styles_color_names.get(style);
                        final String color_code = getColorCode(color);
                        text = "<font color=#" + color_code + ">" + text + "</font>";
                    }
                    dest.write(text + lineseparator + lineseparator);
                }
                success = true;
            } catch (final Exception e) {
                success = false;
            } finally {
                try {
                    dest.close();
                } catch (IOException e) {
                }
            }
            source.delete();
        }
        return success;
    }

    private static String getColorCode(final String colorName) {
        /* Unhandled case/standard = white */
        String colorCode;
        if (colorName == null) {
            /* Use black for missing/not used/given colors */
            colorCode = "FFFFFF";
        } else if (colorName.equals("blue")) {
            colorCode = "0000FF";
        } else if (colorName.equals("yellow")) {
            colorCode = "FFFF00";
        } else if (colorName.equals("aqua")) {
            colorCode = "00FFFF";
        } else if (colorName.equals("lime")) {
            colorCode = "00FF00";
        } else if (colorName.equals("fuchsia")) {
            colorCode = "FF00FF";
        } else if (colorName.equals("green")) {
            colorCode = "008000";
        } else if (colorName.equals("cyan")) {
            colorCode = "00FFFF";
        } else {
            colorCode = "FFFFFF";
        }
        return colorCode;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}