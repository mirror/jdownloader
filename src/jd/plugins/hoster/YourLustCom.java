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
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "yourlust.com" }, urls = { "http://(www\\.)?yourlust\\.com/videos/[a-z0-9\\-]+\\.html" }, flags = { 0 })
public class YourLustCom extends PluginForHost {

    public YourLustCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String DLLINK = null;

    @Override
    public String getAGBLink() {
        return "http://yourlust.com/terms.php";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML(">404 Not Found<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("class=\"block_header\">([^<>\"]*?)<").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>([^<>\"]*?)\\- Homemade porn tube video at YourLust\\.com\\!</title>").getMatch(0);
        DLLINK = br.getRegex("video_url: \\'(http://[^<>\"]*?)\\'").getMatch(0);
        if (filename == null || DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        final SimpleDateFormat formatter = new SimpleDateFormat("YYYYMMddhhmmss");
        final Date date = new Date();
        final String formattedDate = formatter.format(date);
        final String ahv = ahv(formattedDate);
        DLLINK += "?time=" + formattedDate + "&ahv=" + ahv;

        DLLINK = Encoding.htmlDecode(DLLINK);
        filename = filename.trim();
        String ext = DLLINK.substring(DLLINK.lastIndexOf("."));
        if (ext == null || ext.length() > 5) ext = ".flv";
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ext);
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            br2.setCookie("http://yourlust.com", "kt_tcookie", "1");
            br2.setCookie("http://yourlust.com", "kt_is_visited", "1");
            br2.getHeaders().put("Referer", "http://yourlust.com/player/kt_player_3.2.0.swfx");
            con = br2.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html"))
                downloadLink.setDownloadSize(con.getLongContentLength());
            else
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String ahv(final String date) {
        return JDHash.getMD5(DLLINK + date + "0520a96a6baba99b2040a98151b35576");
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
