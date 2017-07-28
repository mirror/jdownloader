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
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.translate._JDT;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tele5.de" }, urls = { "tele5decrypted://.+" })
public class TeleFiveDe extends PluginForHost {
    private String dllink = null;

    @SuppressWarnings("deprecation")
    public TeleFiveDe(final PluginWrapper wrapper) {
        super(wrapper);
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

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return Tele5DeConfigInterface.class;
    }

    public static interface Tele5DeConfigInterface extends PluginConfigInterface {
        public static class TRANSLATION {
            public String getFastLinkcheckEnabled_label() {
                return _JDT.T.lit_enable_fast_linkcheck();
            }

            public String getGrabBESTEnabled_label() {
                return _JDT.T.lit_add_only_the_best_video_quality();
            }

            public String getOnlyBestVideoQualityOfSelectedQualitiesEnabled_label() {
                return _JDT.T.lit_add_only_the_best_video_quality_within_user_selected_formats();
            }

            public String getAddUnknownQualitiesEnabled_label() {
                return _JDT.T.lit_add_unknown_formats();
            }

            public String getGrabHTTP225kVideoEnabled_label() {
                return "Grab 225k 420x230?";
            }

            public String getGrabHTTP620kVideoEnabled_label() {
                return "Grab 620k 640x360?";
            }

            public String getGrabHTTP1250kVideoEnabled_label() {
                return "Grab 1250k 850x480?";
            }

            public String getGrabHTTP1650kVideoEnabled_label() {
                return "Grab 1625k 1024x576?";
            }

            public String getGrabHTTP2400kVideoEnabled_label() {
                return "Grab 2400k 1280x720?";
            }
        }

        public static final TRANSLATION TRANSLATION = new TRANSLATION();

        @DefaultBooleanValue(true)
        @Order(9)
        boolean isFastLinkcheckEnabled();

        void setFastLinkcheckEnabled(boolean b);

        @DefaultBooleanValue(false)
        @Order(20)
        boolean isGrabBESTEnabled();

        void setGrabBESTEnabled(boolean b);

        @AboutConfig
        @DefaultBooleanValue(false)
        @Order(21)
        boolean isOnlyBestVideoQualityOfSelectedQualitiesEnabled();

        void setOnlyBestVideoQualityOfSelectedQualitiesEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(21)
        boolean isAddUnknownQualitiesEnabled();

        void setAddUnknownQualitiesEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(30)
        boolean isGrabHTTP225kVideoEnabled();

        void setGrabHTTP225kVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(40)
        boolean isGrabHTTP620kVideoEnabled();

        void setGrabHTTP620kVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(50)
        boolean isGrabHTTP1250kVideoEnabled();

        void setGrabHTTP1250kVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(60)
        boolean isGrabHTTP1650kVideoEnabled();

        void setGrabHTTP1650kVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(70)
        boolean isGrabHTTP2400kVideoEnabled();

        void setGrabHTTP2400kVideoEnabled(boolean b);
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