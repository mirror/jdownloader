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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "videoweed.com" }, urls = { "http://(www\\.)?videoweed\\.(com|es)/(file/|embed\\.php\\?.*?v=)[a-z0-9]+" }, flags = { 0 })
public class VideoWeedCom extends PluginForHost {

    private String dllink = null;

    public VideoWeedCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(DownloadLink link) {
        final String fileID = new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
        link.setUrlDownload("http://www.videoweed.es/file/" + fileID);
    }

    @Override
    public String getAGBLink() {
        return "http://www.videoweed.com/terms.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (br.containsHTML("error_msg=The video is being transfered")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Not downloadable at the moment, try again later...", 60 * 60 * 1000l);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(>This file no longer exists on our servers\\.<|The video file was removed)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("name=\"title\" content=\"(.*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("videoweed\\.com/file/[a-z0-9]+\\&title=(.*?)\\+\\-\\+VideoWeed\\.com\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<td><strong>Title: </strong>(.*?)</td>").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("<td width=\"580\">[\t\n\r ]+<div class=\"div_titlu\">(.*?) - <a").getMatch(0);
                    if (filename == null) filename = br.getRegex("colspan=\"2\"><strong>Title: </strong>(.*?)</td>").getMatch(0);
                }
            }
        }
        String key = br.getRegex("flashvars\\.filekey=\"(.*?)\"").getMatch(0);
        if (key == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("http://www.videoweed.es/api/player.api.php?user=undefined&codes=1&file=" + new Regex(downloadLink.getDownloadURL(), "videoweed\\.es/file/(.+)").getMatch(0) + "&pass=undefined&key=" + Encoding.urlEncode(key));
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        filename = filename.trim();
        downloadLink.setFinalFileName(filename.replace(filename.substring(filename.length() - 4, filename.length()), "") + ".flv");
        if (br.containsHTML("error_msg=The video is being transfered")) {
            downloadLink.getLinkStatus().setStatusText("Not downloadable at the moment, try again later...");
            return AvailableStatus.TRUE;
        }
        dllink = br.getRegex("url=(http://.*?)\\&title").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(dllink);
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