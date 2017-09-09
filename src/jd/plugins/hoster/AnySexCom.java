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
import java.text.SimpleDateFormat;
import java.util.Date;

import jd.PluginWrapper;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "anysex.com" }, urls = { "https?://(www\\.)?anysex\\.com/\\d+/" })
public class AnySexCom extends PluginForHost {
    public AnySexCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "http://yourlust.com/terms.php";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML(">404 Not Found<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<div class=\"movie\">[\t\n\r ]+<h1>([^<>\"]*?)</h1>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        }
        dllink = br.getRegex("video_url\\s*:\\s*\\'(https?://[^<>\"]*?)\\'").getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename).trim();
        String ext = null;
        if (dllink != null) {
            final SimpleDateFormat formatter = new SimpleDateFormat("YYYYMMddhhmmss");
            final Date date = new Date();
            final String formattedDate = formatter.format(date);
            final String ahv = ahv(formattedDate);
            dllink += "?time=" + formattedDate + "&ahv=" + ahv;
            dllink = Encoding.htmlDecode(dllink);
            ext = getFileNameExtensionFromString(dllink, ".flv");
        }
        downloadLink.setFinalFileName(filename + ext);
        return AvailableStatus.TRUE;
        /* Filesize check disabled as it will cause 404 --> Offline for some users */
        // final Browser br2 = br.cloneBrowser();
        // // In case the link redirects to the finallink
        // br2.setFollowRedirects(true);
        // URLConnectionAdapter con = null;
        // try {
        // br2.setCookie("http://anysex.com", "kt_tcookie", "1");
        // br2.setCookie("http://anysex.com", "kt_is_visited", "1");
        // br2.getHeaders().put("Referer", "http://anysex.com/player/kt_player_3.3.3.swfx");
        // con = br2.openGetConnection(DLLINK);
        // if (con.getLongContentLength() == 46241 || !con.isOK()) {
        // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // }
        // if (!con.getContentType().contains("html")) {
        // downloadLink.setDownloadSize(con.getLongContentLength());
        // } else {
        // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // }
        // return AvailableStatus.TRUE;
        // } finally {
        // try {
        // if (con != null) {
        // con.disconnect();
        // }
        // } catch (Throwable e) {
        // }
        // }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String ahv(final String date) {
        return JDHash.getMD5(dllink + date + Encoding.Base64Decode("ZDMwMTUyOTk1YWU4NzlmZTE1MWVkYWNiZjYwNWM3Nzk="));
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
