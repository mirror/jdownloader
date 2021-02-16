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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "indianpornvideos.com", "freesexyindians.com" }, urls = { "https?://(?:www\\.)?indianpornvideos2?\\.com/(video/)?[A-Za-z0-9\\-_]+(\\.html)?", "https?://(?:www\\.)?freesexyindians\\.com/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+" })
public class IndianPornVideosCom extends PluginForHost {
    public IndianPornVideosCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "http://www.indianpornvideos.com/terms/";
    }

    public static String findStream(Browser br) {
        String dllink = br.getRegex("\"(https?://stream\\.indianpornvideos\\.com/[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"(https?://[^<>\"]+\\.mp4)\"").getMatch(0);
        }
        return dllink;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (link.getDownloadURL().matches("https?://(www.)?indianpornvideos.com/(account|categories|contact-us|dmca|faq|feed|login|privacy|report-abuse|terms|wp-content|wp-includes|wp-json)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (this.br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("This video (does not exist|Was Deleted)|video id not found")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<meta property=\"og:title\" content=\"([^\"]+)\" />").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>([^<>\"]+) \\- Indian Porn Videos</title>").getMatch(0);
        }
        String filename_url = new Regex(link.getPluginPatternMatcher(), "[^/]+/([a-z0-9\\-]+)(\\.html)?$").getMatch(0);
        dllink = findStream(br);
        if (dllink == null) {
            if (!br.containsHTML("id=\"video_views_count\"")) {
                /* Probably not a video-page e.g. '/about-us ' */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlDecode(dllink);
        /* Prefer filenames via URL as they're nearly always given */
        if (filename_url != null) {
            filename_url = filename_url.replace("-", " ");
            filename_url = filename_url.trim();
            link.setFinalFileName(filename_url + ".mp4");
        } else if (filename != null) {
            filename = filename.trim();
            link.setFinalFileName(Encoding.htmlDecode(filename) + ".mp4");
        }
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openHeadConnection(dllink);
            if (this.looksLikeDownloadableContent(con)) {
                link.setVerifiedFileSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
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
