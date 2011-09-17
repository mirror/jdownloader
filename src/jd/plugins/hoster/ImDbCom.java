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
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imdb.com" }, urls = { "http://(www\\.)?imdb\\.com/video/(screenplay|imdb|[a-z0-9\\-_]+)/vi\\d+" }, flags = { 0 })
public class ImDbCom extends PluginForHost {

    public ImDbCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String DLLINK = null;

    @Override
    public String getAGBLink() {
        return "http://www.imdb.com/help/show_article?conditions";
    }

    private static final String IDREGEX = "(vi\\d+)$";

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload("http://www.imdb.com/video/screenplay/" + new Regex(link.getDownloadURL(), IDREGEX).getMatch(0));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL() + "/player");
        if (br.containsHTML("(<title>IMDb Video Player: </title>|This video is not available\\.)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>IMDb Video Player: (.*?)</title>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("http://www.imdb.com/video/imdb/" + new Regex(downloadLink.getDownloadURL(), IDREGEX).getMatch(0) + "/player?uff=3");
        DLLINK = br.getRegex("addVariable\\(\"file\", \"((http|rtmp).*?)\"\\)").getMatch(0);
        if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLLINK = Encoding.htmlDecode(DLLINK);
        filename = filename.trim();
        String ending = ".flv";
        if (DLLINK.contains(".mp4")) ending = ".mp4";
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ending);
        if (!DLLINK.startsWith("rtmp")) {
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
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (DLLINK.startsWith("rtmp")) {
            throw new Exception("4bismarck ;)");
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
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
