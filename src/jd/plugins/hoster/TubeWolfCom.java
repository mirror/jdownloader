//jDownloader - Downloadmanager
//Copyright (C) 2012 JD-Team support@jdownloader.org
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tubewolf.com" }, urls = { "http://(www\\.)?tubewolf\\.com/movies/[\\w\\-]+" }, flags = { 0 })
public class TubeWolfCom extends PluginForHost {

    private String DLLINK = null;
    private String AHV    = "MTEyYjA4ZTBiOGU0MWIxNmU2ZjFiZjMxYzZjZTEyNWQ=";

    public TubeWolfCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private String checkMD(final String videoUrl, final String time) {
        return JDHash.getMD5(videoUrl + time + Encoding.Base64Decode(AHV));
    }

    private String checkMD2(final String time) {
        return JDHash.getMD5(time + Encoding.Base64Decode(AHV));
    }

    private String checkTM() {
        final Date dt = new Date();
        final SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
        return df.format(dt);
    }

    @Override
    public String getAGBLink() {
        return "http://www.tubewolf.com/terms.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML(">Sorry, this video has been deleted")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = br.getRegex("<h3>(.*?)</h3>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
        }
        DLLINK = br.getRegex("video_url=(http://.*?)\\&amp;preview_url").getMatch(0);
        if (DLLINK == null) {
            DLLINK = br.getRegex("video_url:.*?\\('(http://.*?)'\\)").getMatch(0);
        }
        if (filename == null || DLLINK == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        DLLINK = Encoding.htmlDecode(DLLINK);
        filename = filename.trim();
        String ext = DLLINK.substring(DLLINK.lastIndexOf(".")).replaceAll("\\W", "");
        if (ext == null || ext.length() > 5) {
            ext = "flv";
        }
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + "." + ext);

        final String time = checkTM();
        final String ahv = checkMD(DLLINK, time);
        DLLINK = DLLINK + "?time=" + time + "&ahv=" + ahv + "&cv=" + checkMD2(time);

        final Browser br2 = br.cloneBrowser();
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
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        /** No limits but we limit it so hoster won't be overloaded */
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, -4);
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
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

}