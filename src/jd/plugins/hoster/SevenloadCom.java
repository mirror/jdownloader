//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sevenload.com" }, urls = { "http://de\\.(wwe\\.)?sevenload\\.com/videos/[\\w\\-]+" }, flags = { 32 })
public class SevenloadCom extends PluginForHost {

    private String CLIPURL = null;
    private String SWFURL  = null;

    public SevenloadCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private void download(final DownloadLink downloadLink) throws Exception {
        dl = new RTMPDownload(this, downloadLink, CLIPURL);
        setupRTMPConnection(dl);
        ((RTMPDownload) dl).startDownload();
    }

    @Override
    public String getAGBLink() {
        return "http://www.myvideo.de/AGB";
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

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        String filename = br.getRegex("<meta name=\"title\" content=\"([^\"]+)").getMatch(0);
        if (filename == null) filename = br.getRegex("<h1>(.*?)</h1>").getMatch(0);

        String next = br.getRegex("<param name=\"flashVars\" value=\"configPath=(http[^\"]+)").getMatch(0);
        if (next == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        SWFURL = "http://static.sevenload.net/swf/player/player.swf?referer=" + Encoding.urlEncode(downloadLink.getDownloadURL());
        br.getPage(Encoding.htmlDecode(next));

        String[] tmpUrls = br.getRegex("(rtmp[^<]+)").getColumn(0);
        if (tmpUrls == null || tmpUrls.length == 0) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }

        CLIPURL = tmpUrls[tmpUrls.length - 1];
        String ext = CLIPURL.substring(CLIPURL.lastIndexOf("."));
        ext = ext == null || ext.length() > 5 ? ".mp4" : ext;
        if (filename == null) filename = "unknown_sevenload_title" + System.currentTimeMillis();
        filename = filename.trim() + ext;
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename));
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

    private void setupRTMPConnection(final DownloadInterface dl) {
        final jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();

        rtmp.setUrl(CLIPURL);
        rtmp.setSwfVfy(SWFURL);
        rtmp.setResume(true);
    }

}