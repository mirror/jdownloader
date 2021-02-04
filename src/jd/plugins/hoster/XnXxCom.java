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

import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xnxx.com" }, urls = { "https?://[\\w\\.]*?xnxx\\.(?:com|hot1000\\.ru)/video[a-z0-9\\-]+" })
public class XnXxCom extends PluginForHost {
    public XnXxCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    /* Porn_plugin */
    private String dllink = null;

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        /* Correct link for user 'open in browser' */
        final String addedlink = link.getDownloadURL();
        if (!addedlink.endsWith("/")) {
            final String user_url = addedlink + "/";
            link.setContentUrl(user_url);
        }
    }

    @Override
    public String getAGBLink() {
        return "http://www.xnxx.com/contact.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        // The regex only takes the short urls but these ones redirect to the real ones to if follow redirects is false the plugin doesn't
        // work at all!
        br.setFollowRedirects(true);
        br.getHeaders().put("Accept-Language", "en-gb");
        br.getPage(link.getPluginPatternMatcher() + "/");
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(Page not found|This page may be in preparation, please check back in a few minutes)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename_url = new Regex(link.getDownloadURL(), "/video([a-z0-9\\-]+)").getMatch(0);
        String filename = br.getRegex("<title>(.+) \\- [A-Za-z0-9\\.]+\\.[A-Za-z0-9]{3,8}</title>").getMatch(0);
        if (filename == null) {
            br.getRegex("<span class=\"style5\"><strong>(.*?)</strong>").getMatch(0);
            if (filename == null) {
                br.getRegex("name=description content=\"(.*?)free sex video").getMatch(0);
            }
        }
        if (filename == null) {
            /* Fallback */
            filename = filename_url;
        }
        filename = Encoding.unicodeDecode(filename);
        filename = Encoding.htmlDecode(filename);
        if (!br.containsHTML(".mp4")) {
            link.setFinalFileName(filename.trim() + ".flv");
        } else {
            link.setFinalFileName(filename.trim() + ".mp4");
        }
        /* 2020-08-12: HLS sometimes offers higher qualities than their http qualities --> Prefer that */
        dllink = br.getRegex("setVideoHLS\\('(https?://[^<>\"\\']+)").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("setVideoUrlHigh\\('(http.*?)'").getMatch(0);
            if (dllink != null) {
                checkDllink(link, dllink);
            }
            if (dllink == null) {
                dllink = br.getRegex("setVideoUrlLow\\('(http.*?)'").getMatch(0);
                if (dllink != null) {
                    checkDllink(link, dllink);
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    private String checkDllink(final DownloadLink link, final String flink) throws Exception {
        final Browser br2 = br.cloneBrowser();
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openHeadConnection(flink);
            if (this.looksLikeDownloadableContent(con)) {
                link.setDownloadSize(con.getCompleteContentLength());
                link.setVerifiedFileSize(con.getCompleteContentLength());
                dllink = flink;
            } else {
                dllink = null;
            }
        } catch (final Exception e) {
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return dllink;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dllink.contains(".m3u8")) {
            /* HLS download */
            br.getPage(this.dllink);
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
            if (hlsbest == null) {
                /* No content available --> Probably the user wants to download hasn't aired yet --> Wait and retry later! */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Sendung wurde noch nicht ausgestrahlt", 60 * 60 * 1000l);
            }
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, hlsbest.getDownloadurl());
            dl.startDownload();
        } else {
            /* HTTP download */
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
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