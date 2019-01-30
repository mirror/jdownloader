//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imgvip.net" }, urls = { "https?://(?:www\\.)?(?:vestimage\\.site|imgfile\\.net|fortstore\\.net|imgsky\\.net|iceimg\\.net|pixsense\\.net|chaosimg\\.site)/(site/v/\\d+|[a-z0-9]+)" })
public class ImgvipNet extends PluginForHost {
    public ImgvipNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://www.imgvip.net/terms";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        link.setMimeHint(CompiledFiletypeFilter.ImageExtensions.JPG);
        /* 2019-01-30: Important: Only set linkid if we have the real one! */
        String linkid = new Regex(link.getPluginPatternMatcher(), "([a-z0-9]+)$").getMatch(0);
        boolean isRealLinkid = !linkid.matches("\\d+");
        if (isRealLinkid) {
            link.setLinkID(linkid);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        final String defaultdomain = br.getRegex("var default_domain=\"([^\"]+)\";").getMatch(0);
        if (defaultdomain != null && br.containsHTML("document\\.location\\.href=redirect_url")) {
            String continue_id = br.getRegex("default_domain\\+\"(/[^\"\\']+)\"").getMatch(0);
            if (continue_id == null) {
                continue_id = "/" + linkid;
            }
            /* Redirect do other domain */
            String protocol_and_www = br.getRegex("var redirect_url=\"(https?://(?:www\\.)?)").getMatch(0);
            if (protocol_and_www == null) {
                protocol_and_www = "http://www.";
            }
            br.getPage(protocol_and_www + defaultdomain + continue_id);
        }
        if (br.getHttpConnection().getResponseCode() == 404 || (isRealLinkid && !br.getURL().contains(linkid)) || br.containsHTML(">This image has been deleted")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!isRealLinkid) {
            logger.info("Trying to find real linkid");
            linkid = new Regex(br.getURL(), "([a-z0-9]+)$").getMatch(0);
            isRealLinkid = !linkid.matches("\\d+");
            if (isRealLinkid) {
                logger.info("Found and set real linkid");
                link.setLinkID(linkid);
            } else {
                logger.info("Failed to find real linkid");
            }
        }
        String filename = br.getRegex("<title>([^<>\"\\']+)</title>").getMatch(0);
        if (filename == null) {
            filename = linkid + ".jpg";
        }
        link.setName(filename);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = checkDirectLink(downloadLink, "directlink");
        if (StringUtils.isEmpty(dllink)) {
            dllink = br.getRegex("\"(https?://[^/]+/images/[^<>\"\\']+)\"").getMatch(0);
            if (StringUtils.isEmpty(dllink)) {
                /* 2019-01-30: New */
                dllink = br.getRegex("<img src=\"(https?://[^<>\"\\']+)\"onclick=\"window\\.open\\(this\\.src\\)\">").getMatch(0);
            }
        }
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server issue");
        }
        downloadLink.setProperty("directlink", dl.getConnection().getURL().toString());
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
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        return true;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}