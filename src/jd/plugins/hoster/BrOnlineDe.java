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
import java.text.DecimalFormat;
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
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "br-online.de" }, urls = { "http://brdecrypted\\-online\\.de/\\?format=(mp4|xml)\\&quality=\\d+x\\d+\\&hash=[a-z0-9]+" }, flags = { 0 })
public class BrOnlineDe extends PluginForHost {

    public BrOnlineDe(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    private String DLLINK = null;

    @Override
    public String getAGBLink() {
        return "http://www.br.de/unternehmen/service/kontakt/index.html";
    }

    private static final String Q_0         = "Q_0";
    private static final String Q_A         = "Q_A";
    private static final String Q_B         = "Q_B";
    private static final String Q_E         = "Q_E";
    private static final String Q_C         = "Q_C";
    private static final String Q_BEST      = "Q_BEST";
    private static final String Q_SUBTITLES = "Q_SUBTITLES";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        if (downloadLink.getBooleanProperty("offline", false)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String startLink = downloadLink.getStringProperty("mainlink");
        br.getPage(startLink);

        if (br.getHttpConnection().getResponseCode() == 404) {
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
            br2.getHeaders().put("Accept-Encoding", "identity");
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

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.getHeaders().put("Accept-Encoding", "identity");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
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
        try {
            /* Find hex color text --> code assignments */
            final HashMap<String, String> color_codes = new HashMap<String, String>();
            final String[][] found_color_codes = new Regex(xmlContent, "xml:id=\"([A-Za-z]+)\" tts:color=\"(#[A-Z0-9]+)\"").getMatches();
            if (found_color_codes != null && found_color_codes.length != 0) {
                for (final String[] color_info : found_color_codes) {
                    color_codes.put(color_info[0], color_info[1]);
                }
            }
            final DecimalFormat df = new DecimalFormat("00");
            final int offset_minus_hours = 10;
            final String[] matches = new Regex(xmlContent, "(<tt:p xml:id=\"sub\\d+\".*?</tt:p>)").getColumn(0);
            for (final String info : matches) {
                dest.write(counter++ + lineseparator);
                final Regex startInfo = new Regex(info, "begin=\"(\\d{2})(:\\d{2}:\\d{2})\\.(\\d{3})\"");
                final Regex endInfo = new Regex(info, "end=\"(\\d{2})(:\\d{2}:\\d{2})\\.(\\d{3})\"");
                final String start_hours_corrected = df.format(Integer.parseInt(startInfo.getMatch(0)) - offset_minus_hours);
                final String end_hours_corrected = df.format(Integer.parseInt(endInfo.getMatch(0)) - offset_minus_hours);
                final String start = start_hours_corrected + startInfo.getMatch(1) + "," + startInfo.getMatch(2);
                final String end = end_hours_corrected + endInfo.getMatch(1) + "," + endInfo.getMatch(2);
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

    @Override
    public String getDescription() {
        return "JDownloader's BR Plugin helps downloading videoclips from br.de. You can choose between different video qualities.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_SUBTITLES, JDL.L("plugins.hoster.brdemediathek.subtitles", "Download subtitle whenever possible")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Video settings: "));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        final ConfigEntry bestonly = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_BEST, JDL.L("plugins.hoster.brdemediathek.best", "Load best version ONLY")).setDefaultValue(false);
        getConfig().addEntry(bestonly);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_0, JDL.L("plugins.hoster.brdemediathek.load_q0", "Load 256x144 version")).setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_A, JDL.L("plugins.hoster.brdemediathek.load_qA", "Load 384x216 version")).setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_B, JDL.L("plugins.hoster.brdemediathek.load_qB", "Load 512x288 version")).setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_E, JDL.L("plugins.hoster.brdemediathek.load_qE", "Load 640x360 version")).setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_C, JDL.L("plugins.hoster.brdemediathek.load_qC", "Load 960x544 version")).setDefaultValue(true).setEnabledCondidtion(bestonly, false));
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
