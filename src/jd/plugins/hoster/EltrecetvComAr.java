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

import org.appwork.utils.Regex;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "eltrecetv.com.ar" }, urls = { "http://(www\\.)?eltrecetv\\.com\\.ar/[\\w-]+/nota/\\d+/.+" }, flags = { PluginWrapper.DEBUG_ONLY })
public class EltrecetvComAr extends PluginForHost {

    private String clipUrl = null;
    private String swfUrl  = null;

    public EltrecetvComAr(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.eltrecetv.com.ar/terminos";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        final String app = new Regex(clipUrl, "//.*?/(.*?),").getMatch(0);
        final String playPath = clipUrl.substring(clipUrl.indexOf(",") + 1);
        if (app == null || playPath == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

        final String host = clipUrl.replace("," + playPath, "");
        final String dllink = clipUrl;

        if (dllink.startsWith("rtmp")) {
            dl = new RTMPDownload(this, downloadLink, dllink);
            final RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();

            rtmp.setPlayPath(playPath);
            rtmp.setPageUrl(downloadLink.getDownloadURL());
            rtmp.setSwfVfy(swfUrl);
            rtmp.setFlashVer("WIN 10,1,102,64");
            rtmp.setApp(app);
            rtmp.setUrl(host);
            rtmp.setResume(true);
            rtmp.setTimeOut(10);

            ((RTMPDownload) dl).startDownload();
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        String filename = br.getRegex("<title>(.*?) \\|.*?</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("\\s+<h1>(.*?)</h1>").getMatch(0);
        }
        swfUrl = br.getRegex("<param name=\"movie\" value=\"(.*?)\"").getMatch(0);
        final String id = new Regex(downloadLink.getDownloadURL(), ".*?/(\\d+)/.*?").getMatch(0);
        br.getPage("http://www.eltrecetv.com.ar/feed/videowowza/" + id);
        clipUrl = br.getRegex("<media:content url=\"(.*?)\"").getMatch(0);
        if (filename == null || clipUrl == null || swfUrl == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        swfUrl = "http://www.eltrecetv.com.ar" + swfUrl;
        downloadLink.setFinalFileName(filename + ".flv");
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
