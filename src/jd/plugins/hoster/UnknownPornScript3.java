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
import jd.config.Property;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.SiteType.SiteTemplate;

import org.jdownloader.plugins.components.antiDDoSForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pornup.me", "vid2c.com", "xxxkinky.com", "pornper.com", "pornmobo.com", "kinkytube.me", "sexytube.me", "hottube.me", "fantasy8.com", "pornstep.com", "erotictube.me", "freepornsite.me", "bestporntube.me", "sweetkiss.me", "freepornvideo.me" }, urls = { "http://(?:www\\.)?pornup\\.me/video/\\d+/[a-z0-9\\-]+", "http://(?:www\\.)?vid2c\\.com/video/\\d+/[a-z0-9\\-]+", "http://(?:www\\.)?xxxkinky\\.com/video/\\d+/[a-z0-9\\-]+", "http://(?:www\\.)?pornper\\.com/video/\\d+/[a-z0-9\\-]+", "http://(?:www\\.)?pornmobo\\.com/video/\\d+/[a-z0-9\\-]+", "http://(?:www\\.)?kinkytube\\.me/video/\\d+/[a-z0-9\\-]+", "http://(?:www\\.)?sexytube\\.me/video/\\d+/[a-z0-9\\-]+", "http://(?:www\\.)?hottube\\.me/video/\\d+/[a-z0-9\\-]+", "http://(?:www\\.)?fantasy8\\.com/video/\\d+/[a-z0-9\\-]+",
        "http://(?:www\\.)?pornstep\\.com/video/\\d+/[a-z0-9\\-]+", "http://(?:www\\.)?erotictube\\.me/video/\\d+/[a-z0-9\\-]+", "http://(?:www\\.)?freepornsite\\.me/video/\\d+/[a-z0-9\\-]+", "http://(?:www\\.)?bestporntube\\.me/video/\\d+/[a-z0-9\\-]+", "http://(?:www\\.)?sweetkiss\\.me/video/\\d+/[a-z0-9\\-]+", "http://(?:www\\.)?freepornvideo\\.me/video/\\d+/[a-z0-9\\-]+" })
public class UnknownPornScript3 extends antiDDoSForHost {
    public UnknownPornScript3(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    /* Porn_plugin */
    /* V0.5 */
    /* Tags: Script, template */
    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "http://www.fantasy8.com/static/terms";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage(downloadLink.getDownloadURL());
        if (br.getURL().contains("?m=e") || !br.getURL().contains("/video") || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<h1>([^<>\"]*?)</h1>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>([^<>\"]*?)\\- Free Porn Videos and Sex Movies at fantasy8\\.com Kinky Porn Tube</title>").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("class=\"heading\">[\t\n\r ]+<h2>([^<>\"]*?)</h2>").getMatch(0);
        }
        dllink = checkDirectLink(downloadLink, "directlink");
        if (dllink == null) {
            dllink = br.getRegex("\\'file\\': \\'(http[^<>\"]*?)\\'").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("var videoFile=\"(http[^<>\"]*?)\"").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("<source src=(?:'|\")(http[^<>'\"]*?)(?:'|\")").getMatch(0);
        }
        if (filename == null || dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlDecode(dllink);
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        String ext = getFileNameExtensionFromString(dllink, ".flv");
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        downloadLink.setFinalFileName(filename);
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            try {
                con = br2.openHeadConnection(dllink);
            } catch (final BrowserException e) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final long fsize = con.getLongContentLength();
            if (fsize == 0) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(fsize);
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            downloadLink.setProperty("directlink", dllink);
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
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.UnknownPornScript3;
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
