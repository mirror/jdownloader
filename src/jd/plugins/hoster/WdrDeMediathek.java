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
import java.util.HashMap;
import java.util.Scanner;

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
import jd.plugins.download.DownloadInterface;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wdr.de" }, urls = { "http://wdrdecrypted\\.de/\\?format=(mp3|mp4|xml)\\&quality=\\d+x\\d+\\&hash=[a-z0-9]+" }, flags = { 0 })
public class WdrDeMediathek extends PluginForHost {

    public WdrDeMediathek(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    private String DLLINK = null;

    @Override
    public String getAGBLink() {
        return "http://www1.wdr.de/themen/global/impressum/impressum116.html";
    }

    private static final String TYPE_ROCKPALAST = "http://(www\\.)?wdr\\.de/tv/rockpalast/extra/videos/\\d+/\\d+/\\w+\\.jsp";
    private static final String TYPE_INVALID    = "http://([a-z0-9]+\\.)?wdr\\.de/mediathek/video/sendungen/index\\.html";

    private static final String Q_LOW           = "Q_LOW";
    private static final String Q_MEDIUM        = "Q_MEDIUM";
    private static final String Q_BEST          = "Q_BEST";
    private static final String Q_SUBTITLES     = "Q_SUBTITLES";

    public void correctDownloadLink(final DownloadLink link) {
        final String player_part = new Regex(link.getDownloadURL(), "(\\-videoplayer(_size\\-[A-Z])?\\.html)").getMatch(0);
        if (player_part != null) {
            link.setUrlDownload(link.getDownloadURL().replace(player_part, ".html"));
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        if (downloadLink.getDownloadURL().matches(TYPE_INVALID) || downloadLink.getDownloadURL().contains("filterseite-") || downloadLink.getDownloadURL().contains("uebersicht") || downloadLink.getBooleanProperty("offline", false)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String startLink = downloadLink.getStringProperty("mainlink");
        br.getPage(startLink);

        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        if (startLink.matches(TYPE_ROCKPALAST)) {
            return requestRockpalastFileInformation(downloadLink);
        }

        if (br.getURL().contains("/fehler.xml")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = downloadLink.getStringProperty("plain_filename", null);
        DLLINK = downloadLink.getStringProperty("direct_link", null);

        DLLINK = Encoding.htmlDecode(DLLINK.trim());
        downloadLink.setFinalFileName(filename);
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    private AvailableStatus requestRockpalastFileInformation(final DownloadLink downloadlink) throws IOException, PluginException {
        String fileName = br.getRegex("<h1 class=\"wsSingleH1\">([^<]+)</h1>[\r\n]+<h2>([^<]+)<").getMatch(0);
        DLLINK = br.getRegex("dslSrc=(.*?)\\&amp").getMatch(0);
        if (fileName == null || DLLINK == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadlink.setFinalFileName(encodeUnicode(Encoding.htmlDecode(fileName).trim()) + ".mp4");
        return AvailableStatus.TRUE;
    }

    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (DLLINK.startsWith("rtmp")) {
            dl = new RTMPDownload(this, downloadLink, DLLINK);
            setupRTMPConnection(DLLINK, dl);
            ((RTMPDownload) dl).startDownload();
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (this.dl.startDownload()) {
                this.postprocess(downloadLink);
            }
        }
    }

    private void setupRTMPConnection(String dllink, DownloadInterface dl) {
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        rtmp.setUrl(dllink);
        rtmp.setResume(true);
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
     * Converts the ZDF Closed Captions subtitles to SRT subtitles. It runs after the completed download.
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
        /* Find hex color text --> code assignments */
        final HashMap<String, String> color_codes = new HashMap<String, String>();
        final String[][] found_color_codes = new Regex(xmlContent, "xml:id=\"([A-Za-z]+)\" tts:color=\"(#[A-Z0-9]+)\"").getMatches();
        if (found_color_codes != null && found_color_codes.length != 0) {
            for (final String[] color_info : found_color_codes) {
                color_codes.put(color_info[0], color_info[1]);
            }
        }

        final String[] matches = new Regex(xmlContent, "(<tt:p xml:id=\"sub\\d+\".*?</tt:p>)").getColumn(0);
        try {
            for (final String info : matches) {
                dest.write(counter++ + lineseparator);
                final Regex startInfo = new Regex(info, "begin=\"(\\d{2}:\\d{2}:\\d{2})\\.(\\d{3})\"");
                final Regex endInfo = new Regex(info, "end=\"(\\d{2}:\\d{2}:\\d{2})\\.(\\d{3})\"");
                final String start = startInfo.getMatch(0) + "," + startInfo.getMatch(1);
                final String end = endInfo.getMatch(0) + "," + endInfo.getMatch(1);
                dest.write(start + " --> " + end + lineseparator);

                final String[][] texts = new Regex(info, "<tt:span style=\"([A-Za-z0-9]+)\">([^<>\"]*?)</tt:span>").getMatches();
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

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_SUBTITLES, JDL.L("plugins.hoster.wdrdemediathek.subtitles", "Download subtitle whenever possible")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Video settings: "));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        final ConfigEntry bestonly = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_BEST, JDL.L("plugins.hoster.wdrdemediathek.best", "Load best version ONLY")).setDefaultValue(false);
        getConfig().addEntry(bestonly);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_LOW, JDL.L("plugins.hoster.wdrdemediathek.loadlow", "Load 512x288 version")).setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_MEDIUM, JDL.L("plugins.hoster.wdrdemediathek.loadmedium", "Load 960x544 version")).setDefaultValue(true).setEnabledCondidtion(bestonly, false));
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
