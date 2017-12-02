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
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bigbooty.com" }, urls = { "https?://(?:www\\.)?bigbooty\\.com/video/\\d+" })
public class BigBootyCom extends PluginForHost {
    private String dllink = null;

    public BigBootyCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.bigbooty.com/static/dmca";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private static final String PREMIUMONLYUSERTEXT = JDL.L("plugins.hoster.bigbootycom", "Only downloadable for premium users");
    private static final String PREMIUMONLYTEXT     = "You must be premium user to view this video";

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        final String fid = new Regex(downloadLink.getDownloadURL(), "bigbooty\\.com/video/(\\d+)").getMatch(0);
        downloadLink.setLinkID(fid);
        if (br.getURL().contains("bigbooty.com/error/video_missing") || br.containsHTML("(>This video cannot be found\\. Are you sure you typed in the correct|<h2>ERROR</h2>|<title>Big Booty</title>)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.getURL().equals("http://www.bigbooty.com/upgrade")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("class=\"content\\-title\">[\t\n\r ]+<h\\d+>([^<>\"]*?)</h\\d+>").getMatch(0);
        if (filename == null) {
            filename = fid;
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!downloadLink.isNameSet()) {
            downloadLink.setFinalFileName(Encoding.htmlDecode(filename));
        }
        if (br.containsHTML("message\">[^<>]*?You must be")) {
            downloadLink.getLinkStatus().setStatusText(PREMIUMONLYUSERTEXT);
            return AvailableStatus.TRUE;
        }
        dllink = br.getRegex("flashvars=\"file=(http.*?)\\&image").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("<source[^>]*\\s+src\\s*=\\s*('|\"|)(.*?)\\1").getMatch(1);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + getFileNameExtensionFromString(dllink, ".mp4"));
        dllink = Encoding.htmlDecode(dllink);
        Browser br2 = br.cloneBrowser();
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
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (br.containsHTML("message\">[^<>]*?You must be")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, PREMIUMONLYUSERTEXT);
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