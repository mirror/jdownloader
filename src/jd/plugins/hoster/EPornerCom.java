//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

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

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "eporner.com" }, urls = { "https?://(?:www\\.)?eporner\\.com/hd\\-porn/\\w+(/[^/]+)?" })
public class EPornerCom extends PluginForHost {

    public String   dllink        = null;
    private boolean server_issues = false;

    public EPornerCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://www.eporner.com/terms/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (!this.br.getURL().contains("porn/") || br.containsHTML("id=\"deletedfile\"") || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<title>([^<>\"]*?) \\- EPORNER Free HD Porn Tube</title>").getMatch(0);
        if (filename == null) {
            /* Filename inside url */
            filename = new Regex(downloadLink.getDownloadURL(), "eporner\\.com/hd\\-porn/\\w+/(.+)").getMatch(0);
            if (filename != null) {
                /* url filename --> Nicer url filename */
                filename = filename.replace("-", " ");
            }
        }
        if (filename == null) {
            /* linkid inside url */
            filename = new Regex(downloadLink.getDownloadURL(), "eporner\\.com/hd\\-porn/(\\w+)").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        /* First try to get DOWNLOADurls */
        long filesize = 0;
        final String[][] dloadinfo = this.br.getRegex("href=\"(/dload/[^<>\"]+)\">Download MP4 \\(\\d+p, ([^<>\"]+)\\)</a>").getMatches();
        if (dloadinfo != null && dloadinfo.length != 0) {
            String tempurl = null;
            String tempsize = null;
            long tempsizel = 0;
            for (final String[] dlinfo : dloadinfo) {
                tempurl = dlinfo[0];
                tempsize = dlinfo[1];
                tempsizel = SizeFormatter.getSize(tempsize);
                if (tempsizel > filesize) {
                    filesize = tempsizel;
                    dllink = "http://www.eporner.com" + tempurl;
                }
            }
        }

        /* Failed to find DOWNLOADurls? Try to get STREAMurl. */
        if (dllink == null) {
            final String correctedBR = br.toString().replace("\\", "");
            final String continueLink = new Regex(correctedBR, "(\"|\\')(/config\\d+/\\w+/[0-9a-f]+(/)?)(\"|\\')").getMatch(1);
            if (continueLink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(Encoding.htmlDecode(continueLink) + (continueLink.endsWith("/") ? "1920" : "/1920"));
            dllink = br.getRegex("<hd\\.file>(https?://.*?)</hd\\.file>").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("<file>(https?://.*?)</file>").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("file:[\r\n\r ]*?\"(https?://[^<>\"]*?)\"").getMatch(0);
                }
            }
        }
        if ("http://download.eporner.com/na.flv".equalsIgnoreCase(dllink)) {
            server_issues = true;
        }
        filename = filename.trim();
        downloadLink.setFinalFileName(filename + ".mp4");
        if (filesize == 0 && dllink != null && !server_issues) {
            /* Only get filesize from url if we were not able to find it in html --> Saves us time! */
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openHeadConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    filesize = con.getLongContentLength();
                } else {
                    server_issues = true;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        downloadLink.setDownloadSize(filesize);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}