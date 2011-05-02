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

import jd.PluginWrapper;
import jd.network.rtmp.url.RtmpUrlConnection;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rtl2.de" }, urls = { "rtmp://.+rtl2\\.de.+" }, flags = { PluginWrapper.DEBUG_ONLY })
public class RTL2De extends PluginForHost {

    public RTL2De(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.rtl2.de/3733.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        final String dllink = downloadLink.getDownloadURL();
        final String swfurl = "http://www.rtl2.de/flashplayer/FlashPlayer2011.swf";
        final String[] urlTmp = dllink.split("/", 5);
        if (urlTmp != null && urlTmp.length < 5) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

        final String host = urlTmp[0] + "//" + urlTmp[2] + ":80/" + urlTmp[3] + "?_fcs_vhost=" + urlTmp[2];
        final String playpath = urlTmp[4].substring(0, urlTmp[4].lastIndexOf("."));

        dl = new RTMPDownload(this, downloadLink, dllink);
        final RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();

        rtmp.setPlayPath(playpath);
        rtmp.setSwfVfy(swfurl);
        rtmp.setFlashVer("WIN 10,1,102,64");
        rtmp.setUrl(host);
        rtmp.setResume(true);

        ((RTMPDownload) dl).startDownload();

    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) {
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
