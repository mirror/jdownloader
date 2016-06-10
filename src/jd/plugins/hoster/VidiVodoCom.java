//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;

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
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "vidivodo.com" }, urls = { "https?://(?:www\\.)?(?:en\\.)?vidivodo\\.com/.+" }, flags = { 0 })
public class VidiVodoCom extends PluginForHost {

    private String dllink = null;

    public VidiVodoCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://en.vidivodo.com/pages.php?mypage_id=6";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private static final String TYPE_EMBEDDED = "https?://(?:www\\.)?(en\\.)?vidivodo\\.com/VideoPlayerShare\\.swf\\?u=[A-Za-z0-9=]+";
    private static final String TYPE_OLD      = "https?://(?:www\\.)?(?:en\\.)?vidivodo\\.com/video/[a-z0-9\\-]+/\\d+";

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("http://vidivodo.com/", "useradult", "1");
        if (downloadLink.getDownloadURL().matches(TYPE_EMBEDDED)) {
            final String id = new Regex(downloadLink.getDownloadURL(), "\\?u=([^<>\"\\&=]+)").getMatch(0);
            br.getPage("http://www.vidivodo.com/player/getxml?mediaid=" + id + "&publisherid=vidivodoEmbed&type=");
            final String newurl = br.getRegex("<pagelink>(http://[^<>\"]*?)</pagelink>").getMatch(0);
            if (newurl == null) {
                if (this.br.toString().length() < 10) {
                    /* E.g. html code "No" --> Offline! */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            downloadLink.setUrlDownload(newurl);
        }
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(>404\\. That\\'s an error\\.<|The video you have requested is not available<)") || br.containsHTML("<span>404</span>") || br.getURL().contains("arama?q=")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!this.br.containsHTML("name=\"allowFullScreen\"")) {
            /* Probably not a video! */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename_url = new Regex(this.br.getURL(), "vidivodo\\.com/(.+)").getMatch(0);
        String filename = br.getRegex("property=\"og:title\" content=\"(.*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("target=\"_blank\" title=\"(.*?)\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
            }
        }
        dllink = PluginJSonUtils.getJson(this.br, "contentUrl");
        String mediaID = br.getRegex("mediaid=(\\d+)").getMatch(0);
        if (mediaID == null) {
            /* For old urls we simply find the mediaID inside the url */
            mediaID = new Regex(downloadLink.getDownloadURL(), "vidivodo\\.com/video/[a-z0-9\\-]+/(\\d+)").getMatch(0);
        }
        if (mediaID == null && dllink == null && filename_url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (filename == null) {
            /* Last chance fallback */
            filename = filename_url;
        }
        if (dllink == null) {
            /* Fallback! */
            br.getPage("https://www.vidivodo.com/player/getxml?mediaid=" + mediaID + "");
            dllink = br.getRegex("<source><\\!\\[CDATA\\[(http://[^<>\"]*?)\\]\\]></source>").getMatch(0);
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename).trim();
        downloadLink.setFinalFileName(filename + ".flv");
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openHeadConnection(dllink);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e1) {
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
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