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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "moviefap.com" }, urls = { "http://(www\\.)?moviefap\\.com/(videos/[a-z0-9]+/[a-z0-9\\-_]+\\.html|embedding_player/embedding_feed\\.php\\?viewkey=[a-z0-9]+)" })
public class MovieFapCom extends PluginForHost {

    public MovieFapCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "http://www.moviefap.com/dmca.php";
    }

    private static final String EMBEDLINK    = "http://(www\\.)?moviefap\\.com/embedding_player/embedding_feed\\.php\\?viewkey=[a-z0-9]+";
    private boolean             privatevideo = false;

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        dllink = null;
        privatevideo = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        final String url_filename = new Regex(downloadLink.getDownloadURL(), "([a-z0-9\\-_]+)(?:\\.html)?$").getMatch(0);
        String filename = null;
        if (downloadLink.getDownloadURL().matches(EMBEDLINK)) {
            filename = new Regex(downloadLink.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
            dllink = br.getRegex("<file>(http://[^<>\"]*?)</file>").getMatch(0);
        } else {
            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("video does not exist")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (this.br.containsHTML("This video is set to private")) {
                this.privatevideo = true;
            }
            filename = br.getRegex("<div id=\"view_title\"><h1>([^<>\"]*?)</h1>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("id=\"title\" name=\"title\" value=\"([^<>\"]*?)\"").getMatch(0);
            }
            dllink = br.getRegex("flashvars\\.config = escape\\(\"(http://[^<>\"]*?)\"\\);").getMatch(0);
            if (!this.privatevideo && dllink != null) {
                br.getPage(dllink);
                /* Video offline - not playable via browser either! */
                if (this.br.toString().length() < 30) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                final String[] vps = { "720p", "360p", "240p" }; // Vertical pixel
                for (final String vp : vps) {
                    dllink = br.getRegex("<res>" + vp + "</res>\\s*<videoLink>((http:)?//[^<>\"]*?)</videoLink>").getMatch(0);
                    if (dllink != null) {
                        dllink = Encoding.htmlDecode(dllink);
                        break;
                    }
                }
                if (dllink == null) {
                    dllink = br.getRegex("<videoLink>((?:http:)?//[^<>\"]*?)</videoLink>").getMatch(0);
                }
            }
        }
        if (filename == null) {
            filename = url_filename;
        }
        filename = filename.trim();
        filename = Encoding.htmlDecode(filename);
        String ext = null;
        if (dllink != null) {
            ext = getFileNameExtensionFromString(dllink, ".flv");
        }
        if (ext == null || ext.length() > 5) {
            ext = ".flv";
        }
        ext = ext.replace(".fid", ".flv"); // if (ext == ".fid") doesn't work?
        downloadLink.setFinalFileName(filename + ext);
        if (dllink != null) {
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
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (this.privatevideo) {
            /* Account only */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
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
