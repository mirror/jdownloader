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
import java.util.Random;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pornstarnetwork.com" }, urls = { "https?://(?:www\\.)?pornstarnetwork\\.com/video/(?:(?:[a-z0-9\\-_]+)?\\d+\\.html|embed\\?id=\\d+)" }) 
public class PornStarNetworkCom extends PluginForHost {

    public PornStarNetworkCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String type_embed = "http://(www\\.)?pornstarnetwork\\.com/video/embed\\?id=\\d+";

    private String              DLLINK     = null;

    @Override
    public String getAGBLink() {
        return "http://www.pornstarnetwork.com/terms.html";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(>Page not Found<|>Sorry, the page you are looking for cannot be found)") || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Browser br2 = this.br.cloneBrowser();
        String filename = null;
        if (downloadLink.getDownloadURL().matches(type_embed)) {
            DLLINK = br.getRegex("\"(http://download\\d+\\.pornstarnetwork\\.com/[^<>\"]*?)\"").getMatch(0);
            filename = new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0);
        } else {
            filename = br.getRegex("<div id=\"viewTitle\"><h1>Video \\- ([^<>]*?) \\&nbsp;</h1></div>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>([^<>]*?)</title>").getMatch(0);
            }
            br2.getPage("http://www.pornstarnetwork.com/streaming/getVideosZ/cntid/" + new Regex(downloadLink.getDownloadURL(), "(\\d+)\\.html$").getMatch(0) + "/quality/sd/" + new Random().nextInt(1000));
            DLLINK = br2.getRegex("swfUrl=(http[^<>\"]*?)\\&").getMatch(0);
            if (DLLINK == null) {
                br2.getPage("http://www.pornstarnetwork.com/streaming/getAuthUrl/cntid/" + new Regex(downloadLink.getDownloadURL(), "(\\d+)\\.html$").getMatch(0) + "/quality/sd/format/h264/" + new Random().nextInt(1000));
                DLLINK = br2.getRegex("swfUrl=(http[^<>\"]*?)\\&").getMatch(0);
            }
            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (DLLINK != null) {
            DLLINK = Encoding.htmlDecode(DLLINK);
        }
        filename = Encoding.htmlDecode(filename).trim();
        filename = encodeUnicode(filename);
        String ext = null;
        if (DLLINK != null) {
            ext = DLLINK.substring(DLLINK.lastIndexOf("."));
        }
        if (ext != null && ext.contains(".mp4")) {
            ext = ".mp4";
        } else if (ext != null && ext.contains(".flv")) {
            ext = ".flv";
        } else if (ext == null || ext.length() > 5) {
            ext = ".mp4";
        }
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ext);
        if (DLLINK != null) {
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openGetConnection(DLLINK);
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
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (DLLINK == null) {
            if (this.br.containsHTML("id=\"boxVidStills\"")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
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
