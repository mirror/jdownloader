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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "yourfreeporn.us" }, urls = { "http://(www\\.)?yourfreeporn\\.us/video/\\d+(/[a-z0-9\\-_]+)?" }, flags = { 0 })
public class YourFreePornUs extends PluginForHost {

    private String DLLINK = null;

    public YourFreePornUs(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.yourfreeporn.us/static/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    private static final String VIDEOTHERE = "video\\-frame\"";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.postPage(downloadLink.getDownloadURL(), "language=en_US");
        if (br.getURL().contains("/error/video_missing") || br.containsHTML(">This video cannot be found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = null;
        /**
         * Limit reached? We don't care, we can get the filename from the url and still start the download
         */
        if (!br.containsHTML(VIDEOTHERE)) {
            filename = new Regex(downloadLink.getDownloadURL(), "yourfreeporn\\.us/video/\\d+/([a-z0-9\\-_]+)").getMatch(0);
            if (filename == null) filename = new Regex(downloadLink.getDownloadURL(), "yourfreeporn\\.us/video/(\\d+)").getMatch(0);
        } else {
            filename = br.getRegex("<title>(.*?) \\- Free Videos Adult Sex Tube \\- yourfreeporn\\.us</title>").getMatch(0);
            if (filename == null) filename = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
        }
        downloadLink.setName(Encoding.htmlDecode(filename));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (br.containsHTML("<div align=\"center\"><a href=\"/signup\"><img src=\"/img/common/signup\\.jpg\"")) throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable for premium users");
        // Maybe not even needed anymore, seems like all videos are only for
        // premium
        if (!br.containsHTML(VIDEOTHERE)) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
        br.getPage("http://www.yourfreeporn.us/media/player/config.php?vkey=" + new Regex(downloadLink.getDownloadURL(), "yourfreeporn\\.us/video/(\\d+)").getMatch(0));
        DLLINK = br.getRegex("<src>(http://.*?)</src>").getMatch(0);
        if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLLINK = Encoding.htmlDecode(DLLINK);
        String ext = DLLINK.substring(DLLINK.lastIndexOf("."));
        if (ext == null) ext = ".flv";
        downloadLink.setFinalFileName(downloadLink.getName() + ext);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, -3);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(">403 \\- Forbidden<")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
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