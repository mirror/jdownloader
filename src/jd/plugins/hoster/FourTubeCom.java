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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "4tube.com" }, urls = { "http://(www\\.)?4tube\\.com/videos/\\d+/?([\\w-]+)?" }, flags = { 32 })
public class FourTubeCom extends PluginForHost {

    public FourTubeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.4tube.com/legal/privacy";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private String DLLINK = null;

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        setBrowserExclusive();
        String dllink = downloadLink.getDownloadURL();
        br.setFollowRedirects(true);
        br.getPage(dllink);
        if (br.containsHTML("Page not found|This Video Is No Longer Available")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<meta property=\"og:title\" content=\"(.*?)\"").getMatch(0);
        if (filename == null) {
            filename = dllink.substring(dllink.lastIndexOf("/") + 1);
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final String mediaID = br.getRegex("idMedia: (\\d+)").getMatch(0);
        String availablequalities = br.getRegex("sources: \\[([^<>\"]*?)\\]").getMatch(0);
        if (availablequalities != null) {
            availablequalities = availablequalities.replace(",", "+");
        } else {
            availablequalities = "1080+720+480+360+240";
        }
        if (mediaID == null || filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getHeaders().put("Origin", "http://www.4tube.com");
        br.postPage("http://tkn.4tube.com/" + mediaID + "/desktop/" + availablequalities, "");
        // seems to be listed in order highest quality to lowest. 20130513
        getDllink();
        String ext = "mp4";
        if (DLLINK.contains(".flv")) ext = "flv";
        filename = filename.endsWith(".") ? filename + ext : filename + "." + ext;
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html"))
                downloadLink.setDownloadSize(con.getLongContentLength());
            else
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    private void getDllink() throws PluginException, IOException {
        String finallink = null;
        final String[] qualities = new String[] { "1080", "720", "480", "360", "240" };
        for (final String quality : qualities) {
            if (br.containsHTML("\"" + quality + "\"")) {
                finallink = br.getRegex("\"" + quality + "\":\\{\"status\":\"success\",\"token\":\"(http[^<>\"]*?)\"").getMatch(0);
                if (finallink != null && checkDirectLink(finallink) != null) break;
            }
        }
        if (finallink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLLINK = finallink;
    }

    private String checkDirectLink(String directlink) {
        if (directlink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(directlink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    directlink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                directlink = null;
            }
        }
        return directlink;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String playpath = null;
        String configUrl = br.getRegex("'flashvars','config=(.*?)'\\)").getMatch(0);
        if (configUrl == null) configUrl = br.getRegex("addVariable\\('config',.*?'(.*?)'").getMatch(0);
        if (configUrl != null) {
            playpath = br.getRegex("var videoUrl = (\'|\")([^\'\"]+)").getMatch(1);
            if (playpath == null) {
                br.getPage("http://" + br.getHost() + configUrl);
                playpath = br.getRegex("<file>(.*?)</file>").getMatch(0);
            }
            String token = br.getRegex("<token>(.*?)</token>").getMatch(0);
            String url = br.getRegex("<streamer>(.*?)</streamer>").getMatch(0);
            if (playpath == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

            if (!playpath.startsWith("http")) {
                dl = new RTMPDownload(this, downloadLink, url + "/" + playpath);
                jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();

                String host = url.substring(0, url.lastIndexOf("/") + 1);
                String app = url.replace(host, "");
                if (host == null || app == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

                if (app.equals("vod/")) {
                    rtmp.setLive(true);
                } else {
                    rtmp.setResume(true);
                }
                rtmp.setToken(token);
                rtmp.setPlayPath(playpath);
                rtmp.setApp(app);
                rtmp.setUrl(host + app);
                rtmp.setSwfUrl("http://www.4tube.com/player2.swf");

                ((RTMPDownload) dl).startDownload();
                return;
            }
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

}