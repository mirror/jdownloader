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

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

/** Tags: pinapfile.com */
@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "dropbase.com", "pinapfile.com" }, urls = { "https?://(?:www\\.)?dropbbase\\.com/download/([a-f0-9]{32})\\.html", "https?://(?:www\\.)?pinapfile\\.com/download/([a-f0-9]{32})\\.html" })
public class DropbbaseCom extends PluginForHost {
    public DropbbaseCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://dropbbase.com/";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
        if (linkid != null) {
            return linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME       = false;
    private static final int     FREE_MAXCHUNKS    = 1;
    private static final int     FREE_MAXDOWNLOADS = 20;

    // private static final boolean ACCOUNT_FREE_RESUME = true;
    // private static final int ACCOUNT_FREE_MAXCHUNKS = 0;
    // private static final int ACCOUNT_FREE_MAXDOWNLOADS = 20;
    // private static final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private static final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    // private static final int ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    //
    // /* don't touch the following! */
    // private static AtomicInteger maxPrem = new AtomicInteger(1);
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        /* 2019-06-06: All files I found were .zip archives! */
        link.setMimeHint(CompiledFiletypeFilter.ArchiveExtensions.ZIP);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">Oops\\! Page not found|class=\\'error404\\'")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<title>[A-Za-z0-9]+\\.[a-z]+ \\| ([^<>\"]+)</title>").getMatch(0);
        if (StringUtils.isEmpty(filename)) {
            filename = br.getRegex("class=\"filenamemfbluet\">([^<>\"]+)\\.\\.\\.<br>").getMatch(0);
        }
        /*
         * Filesize is not always given. In my tests, website-layout may differ based on file (NOT domain!) Displayed filesizes may also be
         * completely incorrect e.g. it displays 80MB but the real filesize is 2MB - WTF!
         */
        String filesize = br.getRegex("<i class=\"fa fa\\-database\"[^<>]*?></i> <i>([^<>\"]+)</i>").getMatch(0);
        if (StringUtils.isEmpty(filesize)) {
            /* 2019-06-07: WTF, other site-layout */
            filesize = br.getRegex("<small[^>]*?>(\\d+(?:\\.\\d+)? [A-Za-z]+)</small>").getMatch(0);
        }
        if (filename != null) {
            filename = Encoding.htmlDecode(filename).trim();
            /*
             * 2019-06-06: Server will return original filename + different hash for every download-attempt which is why we set the final
             * filename here!
             */
            link.setFinalFileName(filename);
        } else {
            /* Failed to find filename? Set fallback name. */
            link.setName(this.getLinkID(link) + ".zip");
        }
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
            /**
             * TODO: 2019-06-06: Find out what this external site is really used for - this might be be useful for the implementation of
             * other services as well!
             */
            final String continue_url = br.getRegex("var dataUrl\\s*?=\\s*?\\'(http[^<>\"\\']+)").getMatch(0);
            if (continue_url == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Set these cookies to get the direct-URL after the first request. Via browser we'd have to access continue_url twice! */
            br.setCookie(Browser.getHost(continue_url), "uns", "1");
            br.setCookie(Browser.getHost(continue_url), "checkrefi", "https%3A%2F%2Fdropbbase.com%2F");
            br.getPage(continue_url);
            // final String archive_password = br.getRegex(">Archive password: <span style=\"[^\"]+\">([^<>]+)</span>").getMatch(0);
            // if (!StringUtils.isEmpty(archive_password)) {
            // /* TODO: Set extraction password here */
            // }
            dllink = br.getRegex("var targetUrl\\s*?=\\s*?\\'(http[^<>\"\\']+)").getMatch(0);
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
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
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (acc.getType() == AccountType.FREE) {
            /* Free accounts can have captchas */
            return true;
        }
        /* Premium accounts do not have captchas */
        return false;
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