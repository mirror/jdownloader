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

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vidobu.com" }, urls = { "http://(www\\.)?vidobu\\.com/videoPlayer\\.php\\?videoID=\\d+" }, flags = { 0 })
public class VidobuCom extends PluginForHost {

    private String clipData;
    private String finalURL;

    public VidobuCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.vidobu.com/gizlilik.php";
    }

    private String getClipData(final String tag) {
        return new Regex(clipData, "\"" + tag + "\":\"?(.*?)\"?,").getMatch(0);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finalURL, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        if (downloadLink.getBooleanProperty("offline", false)) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        downloadLink.setName(new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0));
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("http://www\\.vidobu\\.com/uyari_ip\\.php") || br.getHttpConnection().getResponseCode() == 404) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        final String nextUrl = br.getRegex("<iframe src=\"(http://[^<>]+)\"").getMatch(0);
        clipData = br.getPage(Encoding.htmlDecode(nextUrl));
        String title = br.getRegex("<title>(.*?)</title>").getMatch(0);
        title = title == null ? "Vimeo_private_video_" + getClipData("id") + "_" + System.currentTimeMillis() : title;
        final String dlURL = "/play_redirect?clip_id=" + getClipData("id") + "&sig=" + getClipData("signature") + "&time=" + getClipData("timestamp") + "&quality=" + (getClipData("hd").equals("1") ? "hd" : "sd") + "&codecs=H264,VP8,VP6&type=moogaloop&embed_location=" + Encoding.htmlDecode(getClipData("referrer"));
        br.setFollowRedirects(false);
        br.getPage(dlURL);
        finalURL = br.getRedirectLocation();
        if (finalURL == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        title = Encoding.htmlDecode(title);
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(finalURL);
            if (con.getContentType() != null && con.getContentType().contains("mp4")) {
                downloadLink.setName(title + ".mp4");
            } else {
                downloadLink.setName(title + ".flv");
            }
            downloadLink.setDownloadSize(con.getLongContentLength());
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
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

}