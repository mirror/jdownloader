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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "eporner.com" }, urls = { "http://(www\\.)?eporner\\.com/hd\\-porn/\\w+(/[^/]+)?" }, flags = { 0 })
public class EPornerCom extends PluginForHost {

    public String dllink = null;

    public EPornerCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.eporner.com/terms/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getURL().equals("http://www.eporner.com/") || br.containsHTML("id=\"deletedfile\"")) {
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
        final String correctedBR = br.toString().replace("\\", "");
        final String continueLink = new Regex(correctedBR, "(\"|\\')(/config\\d+/\\w+/[0-9a-f]+(/)?)(\"|\\')").getMatch(1);
        if (continueLink == null || filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(Encoding.htmlDecode(continueLink) + (continueLink.endsWith("/") ? "1920" : "/1920"));
        dllink = br.getRegex("<hd\\.file>(https?://.*?)</hd\\.file>").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("<file>(https?://.*?)</file>").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("file:[\r\n\r ]*?\"(https?://[^<>\"]*?)\"").getMatch(0);
                if (dllink == null || "http://download.eporner.com/na.flv".equalsIgnoreCase(dllink)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        filename = filename.trim();
        downloadLink.setFinalFileName(filename + ".mp4");
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openHeadConnection(dllink);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
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