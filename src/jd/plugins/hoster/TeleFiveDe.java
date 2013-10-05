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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tele5.de" }, urls = { "decrypted://(www\\.)?tele5\\.de/(videos/)?[\\w/\\-]+/(video/)?[\\w/\\-]+\\.html\\&quality=\\d+\\&vId=[_0-9a-z]+" }, flags = { 0 })
public class TeleFiveDe extends PluginForHost {

    private String  DLLINK       = null;

    private boolean NOTFORSTABLE = false;

    public TeleFiveDe(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("decrypted://", "http://"));
    }

    @Override
    public String getAGBLink() {
        return "http://www.tele5.de/nutzungsbedingungen";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        download(downloadLink);
    }

    private void download(final DownloadLink downloadLink) throws Exception {
        if (DLLINK.startsWith("rtmp")) {
            dl = new RTMPDownload(this, downloadLink, DLLINK);
            setupRTMPConnection(dl);
            ((RTMPDownload) dl).startDownload();

        } else {
            br.setFollowRedirects(true);
            if (DLLINK == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            if (DLLINK.startsWith("mms")) { throw new PluginException(LinkStatus.ERROR_FATAL, "Protocol (mms://) not supported!"); }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 1);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                if (dl.getConnection().getResponseCode() == 403) throw new PluginException(LinkStatus.ERROR_FATAL, "This Content is not longer available!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    private void setupRTMPConnection(DownloadInterface dl) {
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        String[] streamValue = DLLINK.split("@");
        rtmp.setUrl(streamValue[0]);
        rtmp.setPlayPath(streamValue[1]);
        // rtmp.setLive(true);
        rtmp.setRealTime();
        rtmp.setSwfVfy("http://medianac.nacamar.de/p/657/sp/65700/flash/kdp3/v3.4.10.1/kdp3.swf");
        rtmp.setResume(true);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        DLLINK = downloadLink.getStringProperty("directURL");
        // downloadLink.setProperty("FLVFIXER", true);
        return AvailableStatus.TRUE;
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