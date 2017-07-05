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

import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "4tube.com" }, urls = { "https?://(?:www\\.)?4tube\\.com/(?:embed|videos)/(\\d+)/?([\\w-]+)?" })
public class FourTubeCom extends antiDDoSForHost {

    /* DEV NOTES */
    /* Porn_plugin */
    /* tags: fux.com, porntube.com, 4tube.com, pornerbros.com */
    // /embed/UID are not transferable to /videos/UID -raztoki
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

    private String               dllink  = null;
    private URLConnectionAdapter con     = null;
    private String               uid     = null;
    private boolean              isEmbed = false;

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        dllink = downloadLink.getDownloadURL();
        uid = new Regex(dllink, this.getSupportedLinks()).getMatch(0);
        isEmbed = dllink.contains("/embed/");
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:31.0) Gecko/20100101 Firefox/31.0");
        br.setFollowRedirects(true);
        getPage(dllink);
        if (br.getHttpConnection() == null) {
            return AvailableStatus.UNCHECKABLE;
        }
        if (br.containsHTML("Page not found|>\\s*This\\s+Video\\s+Is\\s+No\\s+Longer\\s+Available\\s*<") || new Regex(br.getURL(), "/videos\\?error=\\d+").matches() || (br.containsHTML("<title>\\s*Video not found\\s*</title>") && isEmbed)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<meta property=\"og:title\" content=\"(.*?) \\| 4tube\"").getMatch(0);
        if (filename == null && isEmbed) {
            filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
        }
        if (filename == null) {
            filename = dllink.substring(dllink.lastIndexOf("/") + 1);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // seems to be listed in order highest quality to lowest. 20130513
        getDllink();
        String ext = "mp4";
        if (dllink.contains(".flv")) {
            ext = "flv";
        }
        filename = filename.endsWith(".") ? filename + ext : filename + "." + ext;
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        if (con != null && !con.getContentType().contains("html")) {
            downloadLink.setDownloadSize(con.getLongContentLength());
        } else {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    private void getDllink() throws Exception {
        final String mediaID = jd.plugins.hoster.PornTubeCom.getMediaid(this.br);
        String availablequalities = br.getRegex("\\((\\d+)\\s*,\\s*\\d+\\s*,\\s*\\[([0-9,]+)\\]\\);").getMatch(1);
        if (availablequalities != null) {
            availablequalities = availablequalities.replace(",", "+");
        } else {
            availablequalities = "1080+720+480+360+240";
        }
        if (mediaID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        Browser br2 = br.cloneBrowser();
        br2.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br2.getHeaders().put("Origin", "http://www.4tube.com");
        br2.getHeaders().put("Accept-Charset", null);
        br2.getHeaders().put("Content-Type", null);
        final boolean newWay = true;
        if (newWay) {
            /* 2017-05-31 */
            br2.postPage("https://tkn.kodicdn.com/" + mediaID + "/desktop/" + availablequalities, "");
        } else {
            br2.postPage("https://tkn.fux.com/" + mediaID + "/desktop/" + availablequalities, "");
        }
        String finallink = null;
        final String[] qualities = availablequalities.split("\\+");
        for (final String quality : qualities) {
            finallink = br2.getRegex("\"" + quality + "\":\\{\"status\":\"success\",\"token\":\"(http[^<>\"]*?)\"").getMatch(0);
            if (finallink == null) {
                continue;
            }
            finallink += "&start=0";
            if (checkDirectLink(finallink) != null) {
                break;
            }
        }
        if (finallink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = finallink;
    }

    private String checkDirectLink(String directlink) {
        if (directlink != null) {
            con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.getHeaders().put("Accept-Charset", null);
                br2.getHeaders().put("Content-Type", null);
                con = br2.openGetConnection(directlink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    directlink = null;
                }
            } catch (final Exception e) {
                directlink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable t) {
                }
            }
        }
        return directlink;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String playpath = null;
        String configUrl = br.getRegex("'flashvars','config=(.*?)'\\)").getMatch(0);
        if (configUrl == null) {
            configUrl = br.getRegex("addVariable\\('config',.*?'(.*?)'").getMatch(0);
        }
        if (configUrl != null) {
            playpath = br.getRegex("var videoUrl = (\'|\")([^\'\"]+)").getMatch(1);
            if (playpath == null) {
                getPage(configUrl);
                playpath = br.getRegex("<file>(.*?)</file>").getMatch(0);
            }
            String token = br.getRegex("<token>(.*?)</token>").getMatch(0);
            String url = br.getRegex("<streamer>(.*?)</streamer>").getMatch(0);
            if (playpath == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (!playpath.startsWith("http")) {
                dl = new RTMPDownload(this, downloadLink, url + "/" + playpath);
                jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
                String host = url.substring(0, url.lastIndexOf("/") + 1);
                String app = url.replace(host, "");
                if (host == null || app == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (app.equals("vod/")) {
                    rtmp.setLive(true);
                } else {
                    rtmp.setResume(true);
                }
                rtmp.setToken(token);
                rtmp.setPlayPath(playpath);
                rtmp.setApp(app);
                rtmp.setUrl(host + app);
                rtmp.setSwfUrl(Request.getLocation("//www.4tube.com/player2.swf", br.getRequest()));
                ((RTMPDownload) dl).startDownload();
                return;
            }
        } else {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, true, 0);
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