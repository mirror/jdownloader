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

import org.appwork.utils.formatter.SizeFormatter;

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
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "fayloobmennik.cloud" }, urls = { "https?://(?:www\\.)?fayloobmennik\\.(?:net|cloud)/\\d+" })
public class FayloobmennikCloud extends PluginForHost {
    @Override
    public String[] siteSupportedNames() {
        return new String[] { "fayloobmennik.cloud", "fayloobmennik.net" };
    }

    @Override
    public String rewriteHost(String host) {
        if (host == null) {
            return "fayloobmennik.cloud";
        }
        for (final String supportedName : siteSupportedNames()) {
            if (supportedName.equals(host)) {
                return "fayloobmennik.cloud";
            }
        }
        return super.rewriteHost(host);
    }

    public FayloobmennikCloud(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.fayloobmennik.cloud/pravila.html";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME       = true;
    private static final int     FREE_MAXCHUNKS    = 0;
    private static final int     FREE_MAXDOWNLOADS = 20;
    private static final String  pwprotected       = "file_user_password";

    // private static final boolean ACCOUNT_FREE_RESUME = true;
    // private static final int ACCOUNT_FREE_MAXCHUNKS = 0;
    // private static final int ACCOUNT_FREE_MAXDOWNLOADS = 20;
    // private static final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private static final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    // private static final int ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    //
    // /* don't touch the following! */
    // private static AtomicInteger maxPrem = new AtomicInteger(1);
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String linkid = new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0);
        link.setLinkID(linkid);
        br.getPage(link.getDownloadURL());
        String filename = br.getRegex("<title>Скачать ([^<>\"]+)</title>").getMatch(0);
        if (filename == null) {
            /* Fallback to linkid */
            filename = linkid;
        }
        /* Check for password here as for pw protected urls, filesize is not visible before password. */
        if (br.containsHTML(pwprotected)) {
            // password not yet supported
            /* Try to get filename which is on another position than for non-pw-protected urls. */
            filename = br.getRegex("download_ico\\.png\"[^>]+>([^<>\"]+)<").getMatch(0);
            if (filename != null) {
                filename = Encoding.htmlDecode(filename).trim();
                link.setName(filename);
            }
            return AvailableStatus.TRUE;
        }
        String filesize = br.getRegex("class=\"note\">(\\d+[^<>\",]+), ").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("(\\d+(?:\\.\\d{1,2}? (?:MB|GB)))").getMatch(0);
        }
        if (filename != null) {
            filename = Encoding.htmlDecode(filename).trim();
            link.setName(filename);
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">Файл удалён<")) {
            /* 2017-11-15: Even dead links can contain filename- and filesize information so leave this check here */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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
        String passCode = downloadLink.getDownloadPassword();
        if (dllink == null) {
            if (br.containsHTML(pwprotected)) {
                if (passCode == null) {
                    passCode = Plugin.getUserInput("Password?", downloadLink);
                }
                br.postPage(br.getURL(), "file_user_password=" + Encoding.urlEncode(passCode));
                if (br.containsHTML(pwprotected)) {
                    downloadLink.setDownloadPassword(null);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
                }
            }
            dllink = br.getRegex("(https?://(?:www\\.)?fayloobmennik\\.(?:net|cloud)/files/go/[^<>\"]+)\"").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String server_filename = getFileNameFromHeader(dl.getConnection());
        if (server_filename != null) {
            server_filename = Encoding.htmlDecode(server_filename);
            downloadLink.setFinalFileName(server_filename);
        }
        downloadLink.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        if (passCode != null) {
            downloadLink.setDownloadPassword(passCode);
        }
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