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

import java.io.IOException;
import java.security.GeneralSecurityException;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ardmediathek.de" }, urls = { "decrypted://(www\\.)?(ardmediathek|mediathek\\.daserste)\\.de/[\\w\\-]+/([\\w\\-]+/)?[\\w\\-]+(\\?documentId=\\d+)?" }, flags = { 32 })
public class ARDMediathek extends PluginForHost {

    private static final String Q_LOW    = "Q_LOW";
    private static final String Q_MEDIUM = "Q_MEDIUM";
    private static final String Q_HIGH   = "Q_HIGH";
    private static final String Q_HD     = "Q_HD";
    private static final String Q_BEST   = "Q_BEST";
    private static final String AUDIO    = "AUDIO";

    public ARDMediathek(final PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("decrypted://", "http://"));
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
    public String getAGBLink() {
        return "http://www.ardmediathek.de/ard/servlet/content/3606532";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                if (dl.getConnection().getResponseCode() == 403) throw new PluginException(LinkStatus.ERROR_FATAL, "This Content is not longer available!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
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
        if (downloadLink.getStringProperty("directName", null) == null) downloadLink.setFinalFileName(getTitle(br) + ".mp4");
        if (!downloadLink.getStringProperty("directURL").startsWith("http")) return AvailableStatus.TRUE;
        // get filesize
        Browser br2 = br.cloneBrowser();
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
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_BEST, JDL.L("plugins.hoster.ard.best", "Load Best Version ONLY")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_LOW, JDL.L("plugins.hoster.ard.loadlow", "Load Low Version")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_MEDIUM, JDL.L("plugins.hoster.ard.loadmedium", "Load Medium Version")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_HIGH, JDL.L("plugins.hoster.ard.loadhigh", "Load High Version")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_HD, JDL.L("plugins.hoster.ard.loadhd", "Load HD Version")).setDefaultValue(false).setEnabled(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "For Dossier links"));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), AUDIO, JDL.L("plugins.hoster.ard.audio", "Load Audio Content")).setDefaultValue(false));
    }

}