//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "flashx.tv" }, urls = { "http://((www\\.)?flashx\\.tv/video/[A-Z0-9]+/|play\\.flashx\\.tv/player/embed\\.php\\?.+)" }, flags = { 0 })
public class FlashxTv extends PluginForHost {

    public FlashxTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://flashx.tv/page/3/terms-of-service";
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        if (link.getDownloadURL().matches(".+play\\.flashx\\.tv/player.+")) {
            String hash = new Regex(link.getDownloadURL(), "(?i-)hash=([A-Z0-9]{12})").getMatch(0);
            if (hash != null) link.setUrlDownload(link.getDownloadURL().replaceAll("http://play.flashx.tv/player/.+", "http://www.flashx.tv/video/" + hash + "/"));
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>Requested page not found|>404 Error<)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<div class=\"video_title\">([^<>\"]*?)</div>").getMatch(0);
        if (filename == null) filename = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".flv");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String firstlink = br.getRegex("\"(http://flashx\\.tv/player/embed_player\\.php\\?[^<>\"]*?)\"").getMatch(0);
        if (firstlink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage(firstlink);
        final String seclink = br.getRegex("\"(http://play\\.flashx\\.tv/player/[^<>\"]*?)\"").getMatch(0);
        if (seclink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage(seclink);
        String dllink = br.getRegex("\"(http://fx\\d+.flashx\\.tv/dl/[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("url: \"(http://[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(">404 \\- Not Found<")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}