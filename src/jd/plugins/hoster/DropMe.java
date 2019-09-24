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

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "drop.me" }, urls = { "https?://(?:www\\.)?drop\\.me/[A-Za-z0-9]+" })
public class DropMe extends PluginForHost {
    public DropMe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://drop.me/";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME       = true;
    private static final int     FREE_MAXCHUNKS    = 0;
    private static final int     FREE_MAXDOWNLOADS = 20;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        this.br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]+)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("class=\"file\">([^<>\"]+)<").getMatch(0);
        }
        String filesize = br.getRegex("Size\\s*?:\\s*?(\\d+(?:\\.\\d{1,2}) [A-Za-z]+)").getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename).trim();
        /* 2017-01-21: Set final filename here as server sometimes sends filenames with '_' as ending ... */
        link.setFinalFileName(filename);
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        if (dllink == null) {
            this.br.setFollowRedirects(false);
            String csrftoken = this.br.getRegex("name=\"csrf\\-token\" content=\"([^<>\"]+)\"").getMatch(0);
            if (csrftoken == null) {
                csrftoken = this.br.getRegex("report\\._csrf\\s*?=\\s*?\\'([^<>\"\\']+)\\'").getMatch(0);
            }
            if (csrftoken == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            this.br.postPage(this.br.getURL(), "getFile=1&_csrf=" + Encoding.urlEncode(csrftoken));
            dllink = this.br.getRedirectLocation();
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        this.br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
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
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}