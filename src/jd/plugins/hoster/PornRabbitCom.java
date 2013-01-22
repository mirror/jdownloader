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
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pornrabbit.com" }, urls = { "http://(www\\.)?pornrabbitdecrypted\\.com/(\\d+/[a-z0-9_\\-]+\\.html|video/\\d+/)" }, flags = { 0 })
public class PornRabbitCom extends PluginForHost {

    private String DLLINK = null;

    public PornRabbitCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.pornrabbit.com/terms_of_service.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("pornrabbitdecrypted.com/", "pornrabbit.com/"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        final Browser br2 = br.cloneBrowser();
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(>Page Not Found<|>Sorry but the page you are looking for has)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>([^<>\"]*?): Porn Rabbit</title>").getMatch(0);
        if (filename == null) filename = br.getRegex("<h1>([^<>\"]*?)</h1>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String path = br.getRegex("path=(VideoFile_\\d+)\\&").getMatch(0);
        final String cb = br.getRegex("\\?cb=(\\d+)\\'").getMatch(0);
        if (path != null && cb != null) {
            // Try to get stream link first as downloadlinks might be broken (4 KB files)
            path = Encoding.urlEncode(path);
            try {
                br2.postPage("http://www.pornrabbit.com/getcdnurl/", "jsonRequest=%7B%22playerOnly%22%3A%22true%22%2C%22height%22%3A%22412%22%2C%22file%22%3A%22" + path + "%22%2C%22htmlHostDomain%22%3A%22www%2Epornrabbit%2Ecom%22%2C%22loaderUrl%22%3A%22http%3A%2F%2Fcdn1%2Estatic%2Eatlasfiles%2Ecom%2Fplayer%2Fmemberplayer%2Eswf%3Fcb%3D" + cb + "%22%2C%22path%22%3A%22" + path + "%22%2C%22request%22%3A%22getAllData%22%2C%22cb%22%3A%22" + cb + "%22%2C%22appdataurl%22%3A%22http%3A%2F%2Fwww%2Epornrabbit%2Ecom%2Fgetcdnurl%2F%22%2C%22width%22%3A%22744%22%2C%22returnType%22%3A%22json%22%7D&cacheBuster=" + System.currentTimeMillis());
                DLLINK = br2.getRegex("\"file\": \"(http://[^<>\"]*?)\"").getMatch(0);
            } catch (final Exception e) {
            }
        }
        if (DLLINK == null) {
            DLLINK = br.getRegex("class=\"download\"><a href=\"(http://[^<>\"]*?)\"").getMatch(0);
            if (DLLINK == null) DLLINK = br.getRegex("\"(http://cdn\\d+\\.media\\.pornrabbit\\.com/[^<>\"]*?)\"").getMatch(0);
        }
        if (filename == null || DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLLINK = Encoding.htmlDecode(DLLINK);
        filename = filename.trim();
        String ext = ".mp4";
        if (DLLINK.contains(".flv")) ext = ".flv";
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ext);
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