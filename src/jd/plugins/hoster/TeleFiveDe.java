//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tele5.de" }, urls = { "tele5decrypted://.+" })
public class TeleFiveDe extends PluginForHost {

    private String dllink = null;

    @SuppressWarnings("deprecation")
    public TeleFiveDe(final PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://www.tele5.de/nutzungsbedingungen";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("tele5decrypted://", "http://"));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        dllink = downloadLink.getDownloadURL();
        if (dllink.startsWith("http")) {
            URLConnectionAdapter con = null;
            try {
                con = this.br.openHeadConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                downloadLink.setProperty("directlink", dllink);
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        // downloadLink.setProperty("FLVFIXER", true);

        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        download(downloadLink);
    }

    private void download(final DownloadLink downloadLink) throws Exception {
        if (dllink.startsWith("rtmp")) {
            dl = new RTMPDownload(this, downloadLink, dllink);
            setupRTMPConnection(dl);
            ((RTMPDownload) dl).startDownload();

        } else {
            br.setFollowRedirects(true);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "This Content is not longer available!");
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    private void setupRTMPConnection(DownloadInterface dl) {
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        String[] streamValue = dllink.split("@");
        rtmp.setUrl(streamValue[0]);
        rtmp.setPlayPath(streamValue[1]);
        // rtmp.setLive(true);
        rtmp.setRealTime();
        rtmp.setSwfVfy("http://medianac.nacamar.de/p/657/sp/65700/flash/kdp3/v3.4.10.1/kdp3.swf");
        rtmp.setResume(true);
    }

    @Override
    public String getDescription() {
        return "JDownloader's Tele5 plugin helps downloading videoclips from tele5.de.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "FAST_LINKCHECK", "Enable fast linkcheck?\r\nNOTE: If enabled, links will appear faster but filesize won't be shown before downloadstart.").setDefaultValue(false));
        final ConfigEntry bestonly = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "BEST", "Load best version ONLY?").setDefaultValue(false);
        getConfig().addEntry(bestonly);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "ALLOW_225k", "Grab 225k 420x230?").setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "ALLOW_620k", "Grab 620k 640x360?").setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "ALLOW_225k", "Grab 1250k 850x480?").setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "ALLOW_1650k", "Grab 1625k 1024x576?").setDefaultValue(true).setEnabledCondidtion(bestonly, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "ALLOW_2400k", "Grab 2400k 1280x720?").setDefaultValue(true).setEnabledCondidtion(bestonly, false));

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