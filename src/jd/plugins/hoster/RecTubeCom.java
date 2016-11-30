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
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.jdownloader.plugins.components.antiDDoSForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rec-tube.com" }, urls = { "https?://(?:www\\.)?rec\\-tube\\.com/watch/\\d+/" })
public class RecTubeCom extends antiDDoSForHost {

    public RecTubeCom(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("");
    }

    @Override
    public String getAGBLink() {
        return "https://rec-tube.com/";
    }

    /* Connection stuff */
    private final boolean FREE_RESUME                  = true;
    private final int     FREE_MAXCHUNKS               = 0;
    private final int     FREE_MAXDOWNLOADS            = 20;
    private final boolean ACCOUNT_FREE_RESUME          = true;
    private final int     ACCOUNT_FREE_MAXCHUNKS       = 0;
    private final int     ACCOUNT_FREE_MAXDOWNLOADS    = 20;
    private final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;

    private String        fid                          = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        /* Not sure if this is required. */
        // this.br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:50.0) Gecko/20100101 Firefox/50.0");
        fid = new Regex(link.getDownloadURL(), "(\\d+)/?$").getMatch(0);
        getPage(link.getDownloadURL());
        if (this.br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML("http\\-equiv=\"refresh\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<title>([^<>\"]+)</title").getMatch(0);
        if (filename == null) {
            filename = fid;
        }
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".mp4");
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
            br.setFollowRedirects(true);
            // getPage(this.br.cloneBrowser(), "https://rec-tube.com/cdn/css.css");
            // getPage(this.br.cloneBrowser(), "https://rec-tube.com/cdn/js.js");
            // getPage(this.br.cloneBrowser(), "https://rec-tube.com/cdn/filecss.css");
            // getPage(this.br.cloneBrowser(), "https://rec-tube.com/cdn/piwik.js");
            // getPage(this.br.cloneBrowser(), "https://rec-tube.com/cdn/rotation.js");
            getPage("/embed/" + this.fid + "/");
            if (this.br.toString().length() <= 100 || br.containsHTML("You have reached your daily view limit")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
            } else if (this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            final String id = this.br.getRegex("var id\\s*?=\\s*?\"(\\d+)\"").getMatch(0);
            if (id == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            this.br.setFollowRedirects(false);
            // dllink = "https://rec-tube.com/file/" + id + "/";
            getPage("https://rec-tube.com/file/" + id + "/");
            dllink = this.br.getRedirectLocation();
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        /* Not sure if this is required - but it makes sense. */
        this.br.getHeaders().put("Referer", "https://" + this.getHost() + "/embed/" + this.fid + "/");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            if (this.br.getURL().contains("/limit/")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
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