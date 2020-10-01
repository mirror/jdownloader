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

import org.jdownloader.controlling.filter.CompiledFiletypeFilter;

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
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "eroxia.com" }, urls = { "https?://(?:www\\.)?eroxia\\.com/([A-Za-z0-9_\\-]+/\\d+/.*?|video/[a-z0-9\\-]+\\d+)\\.html" })
public class EroxiaCom extends PluginForHost {
    public EroxiaCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Using playerConfig script */
    /* Tags: playerConfig.php */
    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "http://www.eroxia.com/contact.php";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getURL().equals("http://www.eroxia.com/") || br.containsHTML("Tube</title>|error\">(Page you are|We're sorry)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<h1(?: class=\"detail\\-title\")?>([^<>\"]*?)</h1>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>([^<>\"]*?)\\s*-\\s*Eroxia(?:\\.com)?</title>").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // Try to find direct link first
        dllink = br.getRegex("\\&file=(http[^<>\"]*?)\\&").getMatch(0);
        if (dllink == null) {
            /* 2020-10-01 */
            dllink = br.getRegex("\"(https?://[^/]+/thumbs/[^\"]*?\\.mp4)").getMatch(0);
            if (dllink != null) {
                dllink = dllink.replace("/thumbs/", "/");
            }
        }
        if (dllink == null) {
            dllink = br.getRegex("url: \\'(http://[^<>\"]*?)\\'").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("<source[^>]*\\s+src=(\"|'|)(.*?)\\1").getMatch(1);
                if (dllink == null) {
                    // No direct link there -> 2nd way
                    dllink = br.getRegex("(http://(www\\.)?eroxia\\.com/playerConfig\\.php\\?[^<>\"/\\&]*?)\"").getMatch(0);
                    if (dllink == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    br.getPage(Encoding.htmlDecode(dllink));
                    dllink = br.getRegex("flvMask:(http://[^<>\"]*?);").getMatch(0);
                    if (dllink == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
            }
        }
        dllink = Encoding.htmlDecode(dllink);
        filename = filename.trim();
        final String ext = getFileNameExtensionFromString(dllink, ".mp4");
        link.setFinalFileName(Encoding.htmlDecode(filename) + ext);
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openHeadConnection(dllink);
            if (this.looksLikeDownloadableContent(con)) {
                link.setDownloadSize(con.getCompleteContentLength());
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
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.UnknownPornScript4;
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
