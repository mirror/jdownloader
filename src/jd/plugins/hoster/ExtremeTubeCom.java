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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "extremetube.com" }, urls = { "http://(www\\.)?extremetube\\.com/(video/|embed_player\\.php\\?id=)[a-z0-9\\-]+" }, flags = { 0 })
public class ExtremeTubeCom extends PluginForHost {

    private String DLLINK = null;

    public ExtremeTubeCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(5 * 1000l);
    }

    @Override
    public String getAGBLink() {
        return "http://www.extremetube.com/information#terms-conditions";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        // More simultan downloads are possible but cause more and more errors!
        return 5;
    }

    public void correctDownloadLink(final DownloadLink link) {
        if (link.getDownloadURL().matches(EMBEDLINK)) {
            link.setUrlDownload("http://www.extremetube.com/video/" + new Regex(link.getDownloadURL(), "embed_player\\.php\\?id=(.+)").getMatch(0));
        }
    }

    private static final String EMBEDLINK = "http://(www\\.)?extremetube\\.com/embed_player\\.php\\?id=\\d+";

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        downloadLink.setName(new Regex(downloadLink.getDownloadURL(), "extremetube\\.com/video/(.+)").getMatch(0));
        // Set cookie so we can watch all videos ;)
        br.setCookie("http://www.extremetube.com/", "age_verified", "1");
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getURL().equals("http://www.extremetube.com/") || !br.containsHTML("\\.swf")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<h1 class=\"title\\-video\\-box float\\-left\" title=\"(.*?)\"").getMatch(0);
        DLLINK = br.getRegex("flashvars\\.video_url = \\'(http.*?)\\'").getMatch(0);
        if (DLLINK == null) DLLINK = br.getRegex("flashvars\" .+video\\_url=(.*?)\\&amp\\;").getMatch(0);
        if (filename == null || DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLLINK = Encoding.htmlDecode(DLLINK);
        filename = filename.trim();
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ".flv");
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
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
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}