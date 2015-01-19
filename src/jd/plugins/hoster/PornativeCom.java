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
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pornative.com" }, urls = { "http://(www\\.)?pornative\\.com/\\d+\\.html" }, flags = { 32 })
public class PornativeCom extends PluginForHost {

    private String dllink = null;

    public PornativeCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.pornative.com/tos.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null && "http://www.pornative.com".equalsIgnoreCase(br.getRedirectLocation())) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.getRedirectLocation() != null) {
            br.getPage(br.getRedirectLocation());
        }
        if (!br.getURL().contains(".html")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<title>Pornative.com \\- (.*?)</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
        }
        dllink = br.getRegex("netConnectionUrl: \"(rtmp://[^<>\"]*?)\"").getMatch(0);
        String url = br.getRegex("url: \"([^<>\"]*?)\"").getMatch(0);
        if (filename == null || dllink == null || url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = dllink + "&url=" + url;
        downloadLink.setFinalFileName(filename + ".mp4");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        download(downloadLink);
    }

    private void download(final DownloadLink downloadLink) throws Exception {
        dl = new RTMPDownload(this, downloadLink, dllink);
        setupRTMPConnection(dl);
        ((RTMPDownload) dl).startDownload();
    }

    private void setupRTMPConnection(DownloadInterface dl) {
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        rtmp.setPlayPath(dllink.split("\\&url=")[1]);
        rtmp.setUrl(dllink.split("\\&url=")[0]);
        rtmp.setSwfVfy("http://www.pornative.com/flash/player_new.swf");
        rtmp.setToken(Encoding.Base64Decode("ZG12N3NuMjl2bWJuZmQ2czg="));
        rtmp.setResume(false);
        rtmp.setTimeOut(5);
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

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
    }

}