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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tokyo-tube.com" }, urls = { "http://(www\\.)?tokyo\\-tube\\.com/video/\\d+" }, flags = { 0 })
public class TokyoTubeCom extends PluginForHost {

    /** DEVNOTES: this hoster has broken gzip, which breaks stable support, that's why we disable it */
    private String DLLINK = null;

    public TokyoTubeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.tokyo-tube.com/static/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.getHeaders().put("Accept-Encoding", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("Accept-Encoding", "");
        br.postPage(downloadLink.getDownloadURL(), "language=en_US");
        if (br.getURL().contains("tokyo-tube.com/error/video_missing") || br.containsHTML("(>This video cannot be found|Are you sure you typed in the correct url\\?<|<title>無料アダルト動画 TokyoTube\\-Japanese Free Porn</title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<div class=\"left span\\-630\">[\t\n\r ]+<h2>(.*?)</h2>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<h1>([^\"\\'<>]+)無料アダルト動画 TokyoTube\\-Japanese Free Porn</h1>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>([^\"\\'<>]+)無料アダルト動画 TokyoTube\\-Japanese Free Porn</title>").getMatch(0);
            }
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        filename = filename.trim();
        br.getHeaders().put("Accept-Encoding", "");
        br.getPage("http://www.tokyo-tube.com/media/player/config.php?vkey=" + new Regex(downloadLink.getDownloadURL(), "tokyo\\-tube\\.com/video/(\\d+)").getMatch(0));
        final String[] types = { "hd", "src" };
        Browser br2 = br.cloneBrowser();
        br2.getHeaders().put("Accept-Encoding", "");
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            for (String type : types) {
                DLLINK = br.getRegex("<" + type + ">(http://[^<>]+)</" + type + ">").getMatch(0);
                if (DLLINK != null) {
                    DLLINK = DLLINK.trim();
                    // DLLINK = Encoding.htmlDecode(DLLINK);
                    DLLINK = DLLINK.replaceAll("%0D%0A", "").trim();
                    con = br2.openGetConnection(DLLINK);
                    if (!con.getContentType().contains("html")) {
                        downloadLink.setDownloadSize(con.getLongContentLength());
                        break;
                    } else {
                        try {
                            con.disconnect();
                        } catch (Throwable e) {
                        }
                        continue;
                    }
                }
            }
            if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (con.getContentType().contains("html")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            String ext = DLLINK.substring(DLLINK.lastIndexOf("."));
            if (ext == null) ext = ".flv";
            downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ext);
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
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