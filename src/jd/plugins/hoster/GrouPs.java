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
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "grou.ps" }, urls = { "http://(www\\.)?decryptedgrou\\.ps/[a-z0-9]+/videos/\\d+" }, flags = { 0 })
public class GrouPs extends PluginForHost {

    public GrouPs(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String              DLLINK                   = null;
    private static final String VIDEOUNAVAILABLE         = "(This video is being encoded now\\.\\.\\.|Check back later\\.\\.\\.)";
    private static final String VIDEOUNAVAILABLEUSERTEXT = "This video is being encoded now, try again later!";

    @Override
    public String getAGBLink() {
        return "http://grou.ps/includes/homepage_files/content/tos.html";
    }

    public void correctDownloadLink(DownloadLink link) {
        // Links come from a decrypter
        link.setUrlDownload(link.getDownloadURL().replace("decryptedgrou.ps", "grou.ps"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(nodata\\.png\"|<title>Videos \\| )")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex(">Back to main page</a></div>(.*?)</h2>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>(.*?) \\| Videos \\| ").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (br.containsHTML(VIDEOUNAVAILABLE)) {
            // Don't set final filename in this case, it's set once the video is
            // downloadable again!
            downloadLink.setName(Encoding.htmlDecode(filename) + ".flv");
            downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.groups.videobeingencoded", VIDEOUNAVAILABLEUSERTEXT));
            return AvailableStatus.TRUE;
        }
        DLLINK = br.getRegex("addParam\\(\\'flashvars\\',\\'\\&file=(http://.*?)\\'").getMatch(0);
        if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLLINK = Encoding.htmlDecode(DLLINK);
        DLLINK = Encoding.urlDecode(DLLINK, true);
        filename = filename.trim();
        Browser br2 = br.cloneBrowser();
        br2.setFollowRedirects(false);
        br2.getPage(DLLINK);
        DLLINK = br2.getRedirectLocation();
        if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + DLLINK.substring(DLLINK.length() - 4, DLLINK.length()));
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html") && con.getLongContentLength() != 0) {
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
        if (br.containsHTML(VIDEOUNAVAILABLE)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.groups.videobeingencoded", VIDEOUNAVAILABLEUSERTEXT), 60 * 60 * 1000l);
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
