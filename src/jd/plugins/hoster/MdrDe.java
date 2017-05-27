//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mdr.de", "kika.de", "sputnik.de" }, urls = { "http://mdrdecrypted\\.de/\\d+", "http://kikadecrypted\\.de/\\d+", "http://sputnikdecrypted\\.de/\\d+" })
public class MdrDe extends PluginForHost {

    /** Settings stuff */
    private static final String ALLOW_SUBTITLES      = "ALLOW_SUBTITLES";
    private static final String ALLOW_BEST           = "ALLOW_BEST";
    private static final String ALLOW_AUDIO_256      = "ALLOW_AUDIO_256000";
    private static final String ALLOW_1280x720       = "ALLOW_1280x7";
    private static final String ALLOW_720x576        = "ALLOW_720x5";
    private static final String ALLOW_960x544        = "ALLOW_960x5";
    private static final String ALLOW_640x360        = "ALLOW_640x3";
    private static final String ALLOW_512x288        = "ALLOW_512x2";
    private static final String ALLOW_480x272_higher = "ALLOW_480x2_higher";
    private static final String ALLOW_480x272_lower  = "ALLOW_480x2_lower";
    private static final String ALLOW_256x144        = "ALLOW_256x1";

    public MdrDe(final PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://www.mdr.de/impressum/index.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        setBrowserExclusive();
        final String mainlink = link.getStringProperty("mainlink", null);
        /* Filter old links */
        if (mainlink == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        try {
            br.getPage(mainlink);
        } catch (final BrowserException eb) {
            final long response = br.getRequest().getHttpConnection().getResponseCode();
            if (response == 404 || response == 410) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw eb;
        }
        final String filename = link.getStringProperty("plain_filename", null);
        final String plain_filesize = link.getStringProperty("plain_filesize", null);
        link.setFinalFileName(filename);
        link.setDownloadSize(Long.parseLong(plain_filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        br.setFollowRedirects(true);
        boolean resume = true;
        int maxchunks = 0;
        if ("subtitle".equals(link.getStringProperty("plain_qualityString", null))) {
            /* Workaround for old downloadcore bug that can lead to incomplete files */
            br.getHeaders().put("Accept-Encoding", "identity");
            resume = false;
            maxchunks = 1;
        }
        final String dllink = link.getStringProperty("directlink", null);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (this.dl.startDownload()) {
            this.postprocess(link);
        }
    }

    private void postprocess(final DownloadLink downloadLink) {
        if ("subtitle".equals(downloadLink.getStringProperty("plain_qualityString", null))) {
            if (!convertSubtitle(downloadLink)) {
                logger.severe("Subtitle conversion failed!");
            } else {
                downloadLink.setFinalFileName(downloadLink.getStringProperty("plain_filename", null).replace(".xml", ".srt"));
            }
        }
    }

    /**
     * Converts the ARD Closed Captions subtitles to SRT subtitles. It runs after the completed download.
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

        final String[] matches = new Regex(xmlContent, "(<p begin.*?</p>)").getColumn(0);
        try {

            for (String info : matches) {
                dest.write(counter++ + lineseparator);
                // final DecimalFormat df = new DecimalFormat("00");
                final Regex startInfo = new Regex(info, "begin=\"(\\d{2}:\\d{2}:\\d{2}:\\d{2})\"");
                final Regex endInfo = new Regex(info, "end=\"(\\d{2}:\\d{2}:\\d{2}:\\d{2})\"");

                final String start = startInfo.getMatch(0);
                final String end = endInfo.getMatch(0);
                dest.write(convertSubtitleTime(start) + " --> " + convertSubtitleTime(end) + lineseparator);

                info = info.replaceAll(lineseparator, " ");
                info = info.replaceAll("<br />", lineseparator);
                final String[][] color_texts = new Regex(info, "<span style=\"([a-z]+)\">(.*?)</span>").getMatches();
                String stitle_text = "";
                for (final String[] style_text : color_texts) {
                    final String style = style_text[0];
                    String text = style_text[1];
                    text = text.replaceAll("&apos;", "\\\\u0027");
                    text = Encoding.unicodeDecode(text);
                    text = HTMLEntities.unhtmlentities(text);
                    text = HTMLEntities.unhtmlAmpersand(text);
                    text = HTMLEntities.unhtmlAngleBrackets(text);
                    text = HTMLEntities.unhtmlSingleQuotes(text);
                    text = HTMLEntities.unhtmlDoubleQuotes(text);
                    text = text.replaceAll("</?(p|span)>?", "");
                    text = text.trim();

                    final String color_code = getColorCode(style);
                    stitle_text += "<font color=#" + color_code + ">" + text + "</font>";

                }
                dest.write(stitle_text + lineseparator + lineseparator);
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
        /* Unhandled case/standard = white */
        String colorCode = "FFFFFF";
        if (colorName.equals("blue")) {
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
        }
        return colorCode;
    }

    /**
     * Converts the the time of the ZDF format to the SRT format.
     *
     * @param time
     *            . The time from the ZDF XML.
     * @return The converted time as String.
     */
    private static String convertSubtitleTime(final String input) {
        final Regex info = new Regex(input, "(\\d{2}):(\\d{2}):(\\d{2}):(\\d{2})");

        final String milliseconds = info.getMatch(3) + "0";
        // Result
        String result = info.getMatch(0) + ":" + info.getMatch(1) + ":" + info.getMatch(2) + "," + milliseconds;

        return result;
    }

    @Override
    public String getDescription() {
        return "JDownloader's mdr Plugin helps downloading videoclips from mdr.de. You can choose between different video qualities.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_SUBTITLES, JDL.L("plugins.hoster.MdrDe.grabsubtitles", "Grab subtitles whenever possible")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_AUDIO_256, JDL.L("plugins.hoster.MdrDe.loadAudio256kbps", "Load audio 256 kbps (e.g. available for sputnik.de)")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        final ConfigEntry bestonly = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_BEST, JDL.L("plugins.hoster.MdrDe.best", "Load best version ONLY")).setDefaultValue(false);
        getConfig().addEntry(bestonly);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_1280x720, JDL.L("plugins.hoster.MdrDe.load1280x720", "Load 1280x720")).setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_720x576, JDL.L("plugins.hoster.MdrDe.load256x144", "Load 720x576")).setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_960x544, JDL.L("plugins.hoster.MdrDe.load960x544", "Load 960x5XX")).setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_640x360, JDL.L("plugins.hoster.MdrDe.load640x360", "Load 640x360")).setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_512x288, JDL.L("plugins.hoster.MdrDe.load512x288", "Load 512x288")).setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_480x272_higher, JDL.L("plugins.hoster.MdrDe.load480x272_higher", "Load 480x272 higher")).setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_480x272_lower, JDL.L("plugins.hoster.MdrDe.load480x272_lower", "Load 480x272 lower")).setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_256x144, JDL.L("plugins.hoster.MdrDe.load256x144", "Load 256x144")).setDefaultValue(true).setEnabledCondidtion(bestonly, false));
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
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