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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "porn.to" }, urls = { "http://(www\\.)?porndecrypted\\.to/video/[a-z0-9\\-]+" }, flags = { 0 })
public class PornTo extends PluginForHost {

    private String DLLINK = null;

    public PornTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.porn.to/pages/dmca";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("porndecrypted.to/", "porn.to/"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getURL().equals("http://www.porn.to/") || br.containsHTML("<title>Watch Free Porn Videos and Amateur Sex Movies</title>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<div class=\"row\">[\t\n\r ]+<div class=\"box box_medium float\\-left\">[\t\n\r ]+<h2>(.*?)</h2>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
        Regex theRegex = br.getRegex("createPlayer\\(\\'(http://.*?)\\',\\'http://.*?\\',\\'(.*?)\\',\\'(\\d+)\\'\\)");
        String token = theRegex.getMatch(1);
        String anID = theRegex.getMatch(2);
        boolean embeddedLink = false;
        DLLINK = theRegex.getMatch(0);
        if (DLLINK == null) {
            DLLINK = br.getRegex("\\&file=(http://embed\\.kickassratios\\.com/[^<>\"]*?)\\&").getMatch(0);
            embeddedLink = true;
        }
        if (filename == null || (DLLINK == null && token == null && !embeddedLink && anID == null)) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLLINK = Encoding.htmlDecode(DLLINK);
        filename = filename.trim();
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + DLLINK.substring(DLLINK.length() - 4, DLLINK.length()));
        if (!embeddedLink) DLLINK += "?start=0&id=videoplayer&client=FLASH%20WIN%2010," + anID + ",181,26&version=4.2.95&width=664&token=" + token;
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
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}