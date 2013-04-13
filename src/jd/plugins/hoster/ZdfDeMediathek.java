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

import java.io.IOException;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "zdf.de" }, urls = { "decrypted://(www\\.)?zdf\\.de/ZDFmediathek/[^<>\"]*?beitrag/video/\\d+\\&quality=\\w+" }, flags = { 0 })
public class ZdfDeMediathek extends PluginForHost {

    private static final String Q_LOW      = "Q_LOW";
    private static final String Q_HIGH     = "Q_HIGH";
    private static final String Q_VERYHIGH = "Q_VERYHIGH";
    private static final String Q_HD       = "Q_HD";
    private static final String Q_BEST     = "Q_BEST";

    public ZdfDeMediathek(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("decrypted://", "http://"));
    }

    @Override
    public String getAGBLink() {
        return "http://zdf.de";
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
        String dllink = downloadLink.getStringProperty("directURL", null);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (dllink.startsWith("rtmp")) {
            dl = new RTMPDownload(this, downloadLink, dllink);
            setupRTMPConnection(dllink, dl);
            ((RTMPDownload) dl).startDownload();
        } else {
            br.setFollowRedirects(true);
            if (dllink.contains("hinweis_fsk")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Nur von 20-06 Uhr verfügbar!", 30 * 60 * 1000l);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        if (link.getStringProperty("directURL", null) == null) {
            /* fetch fresh directURL */
            this.setBrowserExclusive();
            br.setFollowRedirects(true);
            // Nur iPads bekommen (vermutlich aufgrund der veralteten Technik :D)
            // die Videos als HTTP Streams
            br.getHeaders().put("User-Agent", "iPad");
            br.getPage("http://www.zdf.de/ZDFmediathek/" + new Regex(link.getDownloadURL(), "(beitrag/video/\\d+(/.+)?)").getMatch(0) + "?flash=off&ipad=true");
            if (br.containsHTML("Der Beitrag konnte nicht gefunden werden")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            final String filename = br.getRegex("<h1 class=\"beitragHeadline\">([^<>\"]*?)</h1>").getMatch(0);
            if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            link.setFinalFileName(Encoding.htmlDecode(filename.trim()).replace("\"", "'").replace(":", " - ").replace("?", "") + ".mp4");
        }
        return AvailableStatus.TRUE;
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

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_BEST, JDL.L("plugins.hoster.zdf.best", "Load Best Version ONLY")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_LOW, JDL.L("plugins.hoster.zdf.loadlow", "Load Low Version")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_HIGH, JDL.L("plugins.hoster.zdf.loadhigh", "Load High Version")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_VERYHIGH, JDL.L("plugins.zdf.dreisat.loadveryhigh", "Load VeryHigh Version")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_HD, JDL.L("plugins.hoster.zdf.loadhd", "Load HD Version")).setDefaultValue(true));

    }

}