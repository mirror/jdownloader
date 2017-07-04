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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "theyfuckwewatch.com" }, urls = { "http://(?:www\\.)?(?:worldstaramateurs\\.com|theyfuckwewatch\\.com)/\\d+" })
public class WorldStarAmateursCom extends PluginForHost {

    @Override
    public String[] siteSupportedNames() {
        return new String[] { "worldstaramateurs.com", "theyfuckwewatch.com" };
    }

    @Override
    public String rewriteHost(String host) {
        if (host == null) {
            return "theyfuckwewatch.com";
        }
        for (final String supportedName : siteSupportedNames()) {
            if (supportedName.equals(host)) {
                return "theyfuckwewatch.com";
            }
        }
        return super.rewriteHost(host);
    }

    public WorldStarAmateursCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "http://www." + getHost() + "/static/terms/";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        final String linkid = new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0);
        if (br.containsHTML(">We\\&#039;re sorry, the page you requested|>404 Not Found<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<h1(?:\\s*class=\"title\")?>([^<>\"]*?)</h1>").getMatch(0);
        if (filename == null) {
            filename = linkid;
        }
        br.getPage("/modules/video/player/nuevo/config.php?id=" + linkid);
        dllink = br.getRegex("<file>(http[^<>\"]*?)</file>").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("<file><\\!\\[CDATA\\[(http[^<>\"]*?)\\]\\]></file>").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dllink = Encoding.htmlDecode(dllink);
        filename = filename.trim();
        final String ext = getFileNameExtensionFromString(dllink, ".mp4");
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ext);
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(dllink);
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
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
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
