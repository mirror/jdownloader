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
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "nuvid.com" }, urls = { "http://(www\\.)?nuvid\\.com/(video|embed)/\\d+" }, flags = { 0 })
public class NuVidCom extends PluginForHost {

    private String DLLINK = null;

    public NuVidCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.nuvid.com/static/terms";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload("http://www.nuvid.com/video/" + new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0));
    }

    private void getDllink(final String linkPart) throws IOException {
        final String vkey = new Regex(Encoding.htmlDecode(linkPart), "vkey=([0-9a-f]+)").getMatch(0);
        br.getPage("http://www.nuvid.com" + Encoding.htmlDecode(linkPart) + "&pkey=" + JDHash.getMD5(vkey + Encoding.Base64Decode("aHlyMTRUaTFBYVB0OHhS")));
        DLLINK = br.getRegex("<video_file>(http://.*?)</video_file>").getMatch(0);
        if (DLLINK == null) DLLINK = br.getRegex("<video_file><\\!\\[CDATA\\[(http://.*?)\\]\\]></video_file>").getMatch(0);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        // Offline1
        if (br.containsHTML("(This page cannot be found|Are you sure you typed in the correct url|<title>Most Recent Videos \\- Free Sex Adult Videos \\- NuVid\\.com</title>)")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        // Offline2
        if (br.containsHTML("This video was deleted\\.")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = br.getRegex("<h2>([^<>\"]*?)</h2>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>([^<>\"]*?) - Free Porn \\& Sex Video").getMatch(0);
        }
        final String linkPart = br.getRegex("\\'http://nuvid\\.com(/player/config\\.php\\?t=.*?)\\'\\);").getMatch(0);
        if (filename == null || linkPart == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        getDllink(linkPart);
        if (DLLINK == null && br.containsHTML("Invalid video key")) throw new PluginException(LinkStatus.ERROR_FATAL, "Plugin outdated, key has changed!");
        if (filename == null || DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLLINK = Encoding.htmlDecode(DLLINK);
        filename = filename.trim();
        String ext = DLLINK.substring(DLLINK.lastIndexOf("."));
        if (ext == null || ext.length() > 5) {
            ext = ".flv";
        }
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ext);
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
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

}