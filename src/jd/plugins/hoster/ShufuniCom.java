//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "shufuni.com" }, urls = { "http://(www\\.)?shufuni\\.com/(?!Flash/)(VideoLP\\.aspx\\?videoId=\\d+|[\\w\\-\\+]+\\d+|handlers/FLVStreamingv2\\.ashx\\?videoCode=[A-Z0-9\\-]+)" }, flags = { 2 })
public class ShufuniCom extends PluginForHost {

    private String dllink = null;

    public ShufuniCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.shufuni.com/doc/agreement.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        } catch (Exception e) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        con = br.openGetConnection(downloadLink.getDownloadURL());
        if (con.getResponseCode() == 500) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.followConnection();
        if (br.getURL().equals("http://www.shufuni.com/videoerrorpage.aspx") || br.containsHTML(">Sorry, this video cannot be found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = null;
        if (downloadLink.getDownloadURL().matches("http://(www\\.)?shufuni\\.com/handlers/FLVStreamingv2\\.ashx\\?videoCode=[A-Z0-9\\-]+")) {
            filename = downloadLink.getFinalFileName();
            if (filename == null) filename = downloadLink.getName();
            dllink = br.getRegex("CDNUrl=(http://[^<>\"]*?)\\&SeekType=").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String ext = null;
            if (filename.contains(".")) ext = filename.substring(filename.lastIndexOf("."));
            if (ext == null || ext.length() > 5) {
                ext = dllink.substring(dllink.lastIndexOf("."));
                if (ext == null || ext.length() > 5) ext = ".mp4";
            }
            if (!filename.endsWith(ext)) {
                downloadLink.setFinalFileName(filename + ext);
            } else {
                downloadLink.setFinalFileName(filename);
            }
        } else {
            // Link offline
            if (br.containsHTML(">no flv details<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            // Invalid link
            if (br.containsHTML("(<title>500 \\- Internal server error\\.</title>|<h2>500 \\- Internal server error\\.</h2>|<h3>There is a problem with the resource you are looking for|The page you are looking for is not available)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            filename = br.getRegex("var PTItemTitle = \"([^<>\"]*?)\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("class=\"VideoTitle\">([^<>\"]*?)</h1>").getMatch(0);
                if (filename == null) filename = br.getRegex("<title>([^<>\"]*?) \\- Free Sex Video[\t\n\r ]+</title>").getMatch(0);
            }
            dllink = br.getRegex("var  _videoFilePathHD = \"(http://[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) dllink = br.getRegex("var  videoFilePath = \"(http://[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String ext = dllink.substring(dllink.lastIndexOf("."));
            if (ext == null || ext.length() > 5) ext = ".mp4";
            downloadLink.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ext);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}