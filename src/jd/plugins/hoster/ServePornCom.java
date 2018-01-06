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

import org.jdownloader.plugins.components.antiDDoSForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "serveporn.com", "lanporno.com", "serviporno.com", "pornodingue.com", "seansporno.com", "koloporno.com", "einfachporno.com", "vielerporno.com", "pornozot.com", "voglioporno.com", "pornodoido.com", "bubbaporn.com", "pornodrome.tv", "nedporno.com", "sexoquente.tv", "filmikiporno.tv", "pornjam.com", "canalporno.com", "prendiporno.com", "prendiporno.tv", "guterporn.com", "guterporn.xxx", "pornalia.xxx", "bundesporno.xxx", "pornburst.xxx", "gauleporno.xxx", "muchoporno.xxx", "pornoheit.com" }, urls = { "https?://(?:www\\.)?serveporn.com\\.com/videos/[a-z0-9\\-_]+/", "https?://(?:www\\.)?lanporno\\.com\\.com/videolar/[a-z0-9\\-_]+/", "https?://(?:www\\.)?serviporno\\.com/videos?/[a-z0-9\\-_]+/", "https?://(?:www\\.)?pornodingue\\.com/videos?/[a-z0-9\\-_]+/",
        "https?://(?:www\\.)?seansporno\\.com/filmy/[a-z0-9\\-_]+/", "https?://(?:www\\.)?koloporno\\.com/filmy/[a-z0-9\\-_]+/", "https?://(?:www\\.)?(?:einfachporno\\.com|pornomenge\\.com)/filme/[a-z0-9\\-_]+/", "https?://(?:www\\.)?vielerporno\\.com/filme/[a-z0-9\\-_]+/", "https?://(?:www\\.)?pornozot\\.com/films/[a-z0-9\\-_]+/", "https?://(?:www\\.)?voglioporno\\.com/video/[a-z0-9\\-_]+/", "https?://(?:www\\.)?pornodoido\\.com/video/[a-z0-9\\-_]+/", "https?://(?:www\\.)?bubbaporn\\.com/videos?/[a-z0-9\\-_]+/", "https?://(?:www\\.)?pornodrome\\.tv/videos?/[a-z0-9\\-_]+/", "https?://(?:www\\.)?nedporno\\.com/films/[a-z0-9\\-_]+/", "https?://(?:www\\.)?sexoquente\\.tv/video(s?/)?[a-z0-9\\-_]+/", "https?://(?:www\\.)?filmikiporno\\.tv/filmy/[a-z0-9\\-_]+/", "https?://(?:www\\.)?pornjam\\.com/video/[a-z0-9\\-_]+/", "https?://(?:www\\.)?canalporno\\.com/ver/[a-z0-9\\-_]+/",
        "https?://(?:www\\.)?prendiporno\\.com/video/[a-z0-9\\-_]+/", "https?://(?:www\\.)?prendiporno\\.tv/video/[a-z0-9\\-_]+/", "https?://(?:www\\.)?guterporn\\.com/filme/[a-z0-9\\-_]+/", "https?://(?:www\\.)?guterporn\\.xxx/filme/[a-z0-9\\-_]+/", "https?://(?:www\\.)?pornalia\\.xxx/video/[a-z0-9\\-_]+/", "https?://(?:www\\.)?bundesporno\\.(?:xxx|com)/filme/[a-z0-9\\-_]+/", "https?://(?:www\\.)?pornburst\\.xxx/videos?/[a-z0-9\\-_]+/", "https?://(?:www\\.)?(?:gauleporno\\.xxx/(?:videos/|video-|filme/)|pornalia\\.xxx/video/)[a-z0-9\\-_]+/", "https?://(?:www\\.)?muchoporno\\.xxx/(?:videos|ver)/[a-z0-9\\-_]+/", "https?://(?:www\\.)?pornoheit\\.com/filme/[^/]+/" })
public class ServePornCom extends antiDDoSForHost {
    public ServePornCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        if ("muchoporno.xxx".equals(this.getHost())) {
            // spanish to english.
            link.setPluginPatternMatcher(link.getPluginPatternMatcher().replace("/ver/", "/videos/"));
        } else if ("einfachporno.com".equals(this.getHost())) {
            // new domain
            link.setPluginPatternMatcher(link.getPluginPatternMatcher().replace("einfachporno.com/", "pornomenge.com/"));
        } else if ("vielerporno.com".equals(this.getHost())) {
            link.setPluginPatternMatcher(link.getPluginPatternMatcher().replace("vielerporno.com/", "pornomenge.com/"));
        }
    }

    /* DEV NOTES */
    /* Porn_plugin */
    private String dllink = null;

    @Override
    public String getAGBLink() {
        /*
         * 2016-07-22: Plugins' main domain serveporn.com redirects to bubbaporn.com - we don't care and leave it in in case they
         * re-activate it
         */
        return "http://www.bubbaporn.com/disclamer/";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        final String url_filename = new Regex(downloadLink.getDownloadURL(), "(?:/video(?:s?/)?|/filme/)(.+?)/?$").getMatch(0);
        if (url_filename != null && !downloadLink.isNameSet()) {
            downloadLink.setName(url_filename + ".flv");
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage(downloadLink.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("content=(\"|')([^<>\"]*?)\\1 property=(\"|')og:title\\3\\s*/\\s*>").getMatch(1);
        if (filename == null) {
            filename = url_filename;
        }
        dllink = br.getRegex("url: '(https?://[^/]+/[^<>\"']*?\\.(?:flv|mp4)\\?key=[^<>\"/]*?)'").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("url: '(https?://cdn[^/]+/[^<>\"']*?\\.(?:flv|mp4)[^<>\"/]*?)'").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("src=\"([^\"]*?//cdn[^\"]+)\"").getMatch(0);
        }
        if (filename == null || dllink == null) {
            logger.info("filename: " + filename + ", DLLINK: " + dllink);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlDecode(dllink);
        filename = filename.trim();
        final String ext = getFileNameExtensionFromString(dllink, ".mp4");
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ext);
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(dllink);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
