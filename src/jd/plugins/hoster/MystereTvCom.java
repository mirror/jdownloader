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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mystere-tv.com" }, urls = { "http://(www\\.)?decryptedmystere\\-tv\\.com/.*?\\-v\\d+\\.html" }, flags = { 0 })
public class MystereTvCom extends PluginForHost {

    private String DLLINK = null;

    public MystereTvCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        // Links are coming from a decrypter
        link.setUrlDownload(link.getDownloadURL().replace("decryptedmystere-tv.com", "mystere-tv.com"));
    }

    @Override
    public String getAGBLink() {
        return "http://www.mystere-tv.com/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 1);
        if (dl.getConnection().getContentType().contains("html") && dl.getConnection().getLongContentLength() < 100) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getURL().equals("http://www.mystere-tv.com/") || br.containsHTML("<title>Paranormal \\- Ovni \\- Mystere TV </title>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<h1 class=\"videoTitle\">(.*?)</h1>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<br /><br /><strong><u>(.*?)</u></strong>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>(.*?) \\- Paranormal</title>").getMatch(0);
            }
        }
        DLLINK = br.getRegex("addVariable\\(\"file\",\"(http://.*?)\"\\)").getMatch(0);
        if (DLLINK == null) {
            DLLINK = br.getRegex("\"?(http://(www\\.)?mystere\\-tv\\.(net|com)/flv/[\\w-\\.\\?=]+)\"?").getMatch(0);
        }
        if (filename != null && DLLINK == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (filename == null || DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLLINK = Encoding.htmlDecode(DLLINK);
        filename = filename.trim();
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ".flv");
        if (!br.containsHTML("\\.flv\\?key=")) {
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openGetConnection(DLLINK);
                if (con.getContentType().contains("html") && con.getLongContentLength() < 100) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                    return AvailableStatus.TRUE;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        } else {
            // no Content-Length/!html Header
            return AvailableStatus.TRUE;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}