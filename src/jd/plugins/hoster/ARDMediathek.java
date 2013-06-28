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
import java.security.GeneralSecurityException;
import java.text.DecimalFormat;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
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
import jd.plugins.download.DownloadInterface;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ard.de" }, urls = { "decrypted://(www\\.)?(ardmediathek|mediathek\\.daserste)\\.de/[\\w\\-]+/([\\w\\-]+/)?[\\w\\-]+(\\?documentId=\\d+)?\\&quality=\\w+" }, flags = { 32 })
public class ARDMediathek extends PluginForHost {

    private static final String Q_LOW       = "Q_LOW";
    private static final String Q_MEDIUM    = "Q_MEDIUM";
    private static final String Q_HIGH      = "Q_HIGH";
    private static final String Q_HD        = "Q_HD";
    private static final String Q_BEST      = "Q_BEST";
    private static final String AUDIO       = "AUDIO";
    private static final String Q_SUBTITLES = "Q_SUBTITLES";

    public ARDMediathek(final PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("decrypted://", "http://"));
    }

    @Override
    public String getAGBLink() {
        return "http://www.ardmediathek.de/ard/servlet/content/3606532";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException, GeneralSecurityException {
        if (downloadLink.getStringProperty("directURL", null) == null) {
            /* fetch fresh directURL */
            setBrowserExclusive();
            br.setFollowRedirects(true);
            br.getPage(downloadLink.getDownloadURL());

            if (br.containsHTML("<h1>Leider konnte die gew&uuml;nschte Seite<br />nicht gefunden werden.</h1>")) {
                logger.info("ARD-Mediathek: Nicht mehr verfügbar: " + downloadLink.getDownloadURL());
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }

            String newUrl[] = br.getRegex("mediaCollection\\.addMediaStream\\((\\d+), (" + downloadLink.getStringProperty("directQuality", "1") + "), \"([^\"]+|)\", \"([^\"]+)\", \"([^\"]+)\"\\);").getRow(0);
            // http
            if (newUrl == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            downloadLink.setProperty("directURL", newUrl[3] + "@");
            // rtmp
            if ("0".equals(downloadLink.getStringProperty("streamingType", "1"))) downloadLink.setProperty("directURL", newUrl[1] + "@" + newUrl[2].split("\\?")[0]);
        }
        String finalName = downloadLink.getStringProperty("directName", null);
        if (finalName == null) finalName = getTitle(br) + ".mp4";
        downloadLink.setFinalFileName(finalName);
        if (!downloadLink.getStringProperty("directURL").startsWith("http")) return AvailableStatus.TRUE;
        // get filesize
        final Browser br2 = br.cloneBrowser();
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(downloadLink.getStringProperty("directURL").split("@")[0]);
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

    private String getTitle(Browser br) {
        String title = br.getRegex("<div class=\"MainBoxHeadline\">([^<]+)</").getMatch(0);
        String titleUT = br.getRegex("<span class=\"BoxHeadlineUT\">([^<]+)</").getMatch(0);
        if (title == null) title = br.getRegex("<title>ard\\.online \\- Mediathek: ([^<]+)</title>").getMatch(0);
        if (title == null) title = br.getRegex("<h2>(.*?)</h2>").getMatch(0);
        if (title != null) title = Encoding.htmlDecode(title + (titleUT != null ? "__" + titleUT.replaceAll(":$", "") : "").trim());
        if (title == null) title = "UnknownTitle_" + System.currentTimeMillis();
        return title;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        download(downloadLink);
    }

    private void setupRTMPConnection(String[] stream, DownloadInterface dl) {
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        rtmp.setPlayPath(stream[1]);
        rtmp.setUrl(stream[0]);
        rtmp.setResume(true);
        rtmp.setTimeOut(10);
    }

    private void download(final DownloadLink downloadLink) throws Exception {
        if (downloadLink.getStringProperty("directURL", null) == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String stream[] = downloadLink.getStringProperty("directURL").split("@");
        if (stream[0].startsWith("rtmp")) {
            dl = new RTMPDownload(this, downloadLink, stream[0]);
            setupRTMPConnection(stream, dl);
            ((RTMPDownload) dl).startDownload();
        } else {
            br.setFollowRedirects(true);
            final String dllink = stream[0];
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (dllink.startsWith("mms")) throw new PluginException(LinkStatus.ERROR_FATAL, "Protocol (mms://) not supported!");
            // Workaround to avoid DOWNLOAD INCOMPLETE errors
            if ("subtitle".equals(downloadLink.getStringProperty("streamingType", null))) {
                downloadLink.setDownloadSize(0);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                if (dl.getConnection().getResponseCode() == 403) throw new PluginException(LinkStatus.ERROR_FATAL, "This Content is not longer available!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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

        final String[] matches = new Regex(xmlContent, "(<p id=\"subtitle\\d+\".*?</p>)").getColumn(0);
        try {
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

                String text = new Regex(info, "style=\"s\\d+\">?(.*?)</p>").getMatch(0);
                final String[] toRemove = new Regex(info, "(tts:backgroundColor=\"[a-z]+\")").getColumn(0);
                if (toRemove != null && toRemove.length != 0) {
                    for (final String remove : toRemove) {
                        text = text.replace(remove, "");
                    }
                }
                text = text.replaceAll(lineseparator, " ");
                text = text.replaceAll("&apos;", "\\\\u0027");
                text = unescape(text);
                text = HTMLEntities.unhtmlentities(text);
                text = HTMLEntities.unhtmlAmpersand(text);
                text = HTMLEntities.unhtmlAngleBrackets(text);
                text = HTMLEntities.unhtmlSingleQuotes(text);
                text = HTMLEntities.unhtmlDoubleQuotes(text);
                text = text.replaceAll("<br />", lineseparator);
                text = text.replaceAll("</?(p|span)>?", "");
                text = text.trim();
                // color=\"#([A-Z0-9]+)\">(.*?)($|tts:)
                final String[][] colorTags = new Regex(text, "color=\"([a-z0-9]+)\">(.*?)($|tts:)").getMatches();
                if (colorTags != null && colorTags.length != 0) {
                    for (final String[] singleText : colorTags) {
                        final String colorText = singleText[0];
                        final String plainText = singleText[1];
                        final String completeNewText = "<font color=#" + getColorCode(colorText) + ">" + plainText + "</font>";
                        final String completeOldText = "tts:color=\"" + colorText + "\">" + plainText;
                        text = text.replace(completeOldText, completeNewText);
                    }
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

    private static String getColorCode(final String colorName) {
        String colorCode = "FFFFFF";
        if (colorName.equals("blue")) {
            colorCode = "0000FF";
        } else if (colorName.equals("yellow")) {
            colorCode = "FFFF00";
        } else if (colorName.equals("aqua")) {
            colorCode = "00FFFF";
        } else if (colorName.equals("lime")) {
            colorCode = "00FF00";
        }
        return colorCode;
    }

    private static AtomicBoolean yt_loaded = new AtomicBoolean(false);

    private String unescape(final String s) {
        /* we have to make sure the youtube plugin is loaded */
        if (!yt_loaded.getAndSet(true)) JDUtilities.getPluginForHost("youtube.com");
        return jd.plugins.hoster.Youtube.unescape(s);
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

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_SUBTITLES, JDL.L("plugins.hoster.ardmediathek.subtitles", "Download subtitle whenever possible")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Video settings: "));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        final ConfigEntry bestonly = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_BEST, JDL.L("plugins.hoster.ard.best", "Load Best Version ONLY")).setDefaultValue(false);
        getConfig().addEntry(bestonly);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_LOW, JDL.L("plugins.hoster.ard.loadlow", "Load Low Version")).setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_MEDIUM, JDL.L("plugins.hoster.ard.loadmedium", "Load Medium Version")).setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_HIGH, JDL.L("plugins.hoster.ard.loadhigh", "Load High Version")).setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_HD, JDL.L("plugins.hoster.ard.loadhd", "Load HD Version")).setDefaultValue(false).setEnabled(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "For Dossier links:"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), AUDIO, JDL.L("plugins.hoster.ard.audio", "Load Audio Content")).setDefaultValue(false));
    }

}