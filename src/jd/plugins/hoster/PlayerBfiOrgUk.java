//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "player.bfi.org.uk" }, urls = { "https?://(?:www\\.)?player\\.bfi\\.org\\.uk/film/[a-z0-9\\-]+" })
public class PlayerBfiOrgUk extends PluginForHost {

    public PlayerBfiOrgUk(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.bfi.org.uk/terms-use";
    }

    /* DEV NOTES */
    /* Tags: ooyala.com, ooyala Player, ooyala player API */

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = br.getRegex("property=\"og:title\" content=\"Watch ([^<>\"]*?)\"").getMatch(0);
        if (title == null) {
            title = new Regex(link.getDownloadURL(), "/film/(.+)$").getMatch(0);
        }
        title = Encoding.htmlDecode(title.trim());
        link.setFinalFileName("bfiorguk_" + title + ".mp4");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String videoid = br.getRegex("data\\-video=\"([^<>\"]*?)\"").getMatch(0);
        // final String playerid = br.getRegex("data\\-player=\"([^<>\"]*?)\"").getMatch(0);
        if (videoid == null) {
            /* No videoid found? We must have paid content ... */
            throw new PluginException(LinkStatus.ERROR_FATAL, "Paid content - you need to pay for this before you can download it!");
        }
        /* Dump session information - not necessarily needed! */
        this.br = new Browser();

        this.br.getPage("http://player.ooyala.com/player.js?embedCode=" + Encoding.urlEncode(videoid));
        String mobile_player_url = this.br.getRegex("mobile_player_url=\"(http[^<>\"]*?\\&device=)\"").getMatch(0);
        if (mobile_player_url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Pretend to be an "unknown" device */
        mobile_player_url += "unknown";
        this.br.getPage(mobile_player_url);

        String json_player = this.br.getRegex("var streams=window\\.oo_testEnv\\?\\[\\]:eval\\((.*?)\"\\);").getMatch(0);
        if (json_player == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        json_player = json_player.replace("\\", "");
        final String url_hls_main = PluginJSonUtils.getJsonValue(json_player, "ipad_url");
        if (url_hls_main == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.br.getPage(url_hls_main);
        final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
        if (hlsbest == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String url_hls = hlsbest.getDownloadurl();
        checkFFmpeg(downloadLink, "Download a HLS Stream");
        dl = new HLSDownloader(downloadLink, br, url_hls);
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
    public void resetDownloadlink(final DownloadLink link) {
    }

}