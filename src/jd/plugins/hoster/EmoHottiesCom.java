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

import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "emohotties.com" }, urls = { "http://(?:www\\.)?decryptedemohotties\\.com/videos/[a-z0-9\\-]+\\-\\d+\\.html" })
public class EmoHottiesCom extends antiDDoSForHost {

    public EmoHottiesCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    /* Porn_plugin */

    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "http://emohotties.com/contact.php";
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("decryptedemohotties.com", "emohotties.com"));
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage(downloadLink.getDownloadURL());
        if (isOffline(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = getTitle(this.br);
        // DLLINK = br.getRegex("(http://emohotties\\.com/playerConfig\\.php\\?[^<>\"/]+\\.(flv|mp4)(\\|\\d+)?)\"").getMatch(0);
        /* Important: Remember that: http://jdownloader.net:8081/pastebin/123271 */
        dllink = br.getRegex("itemprop=\"contentURL\" content=\"(http://[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("clip: \\{\\s+url: \\'([^\\']+)\\'").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("<source src=\"([^\"]+)\"").getMatch(0);
        }
        if (filename == null || dllink == null) {
            logger.info("filename: " + filename + ", DLLINK: " + dllink);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlDecode(dllink);
        filename = filename.trim();
        final String ext = getFileNameExtensionFromString(dllink, ".flv");
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ext);
        final Browser br2 = br.cloneBrowser();
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

    public static String getTitle(final Browser br) {
        String filename = br.getRegex("itemprop=\"name\">([^<>\"]*?)</span>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>([^<>\"]*?)\\- Emo Hotties</title>").getMatch(0);
        }
        return filename;
    }

    public static boolean isOffline(final Browser br) {
        if (br.getURL().equals("http://emohotties.com/404.php") || br.getHttpConnection().getResponseCode() == 404) {
            return true;
        }
        return false;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
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
