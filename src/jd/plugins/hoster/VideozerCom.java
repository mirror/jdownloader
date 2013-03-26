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
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "videozer.com" }, urls = { "http://(www\\.)?videozer\\.com/(video|embed)/[A-Za-z0-9]+" }, flags = { 0 })
public class VideozerCom extends PluginForHost {

    private static int[] internalKeys = { 215678, 516929, 962043, 461752, 141994 };

    public VideozerCom(final PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(2000l + (long) 1000 * (int) Math.round(Math.random() * 3 + Math.random() * 3));
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("/embed/", "/video/"));
    }

    @Override
    public String getAGBLink() {
        return "http://www.videozer.com/toc.php";
    }

    /* See jd.plugin.hoster.VideoBbCom */
    private String getFinalLink(final DownloadLink downloadLink, final String token) {
        try {
            /* you have to make sure the plugin is loaded! */
            JDUtilities.getPluginForHost("videobb.com");
            return new jd.plugins.hoster.VideoBbCom.getFinallinkValue(internalKeys, token, br).DLLINK;
        } catch (final Throwable e) {
            logger.severe(e.getMessage());
            return null;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        final String dllink = getFinalLink(downloadLink, "token1");
        if (dllink == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        if (dllink.equals("ALGO_CONTROL_ERROR")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, VideoBbCom.ALGOCONTROLERROR, 15 * 1000l); }
        sleep(3 * 1000l, downloadLink); // Flasplayer to slow
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("No htmlCode read")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError: ", 30 * 1000l); }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("<div class=\"error_404\">")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        br.getPage("http://www.videozer.com/player_control/settings.php?v=" + new Regex(downloadLink.getDownloadURL(), "videozer\\.com/video/(.+)").getMatch(0) + "&fv=v1.1.14");
        if (br.containsHTML("(\"The page you have requested cannot be found|>The web page you were attempting to view may not exist or may have moved|>Please try to check the web address for typos)")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = br.getRegex("\"video\":\\{\"title\":\"(.*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<meta content=\"videozer \\- ([^\"\\']+)\"  name=\"\" property=\"og:title\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>VideoZer \\- Fast, Free and Reliable Video Hosting \\- (.*?)</title>").getMatch(0);
            }
        }
        if (filename == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        filename = filename.trim();
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ".flv");
        return AvailableStatus.TRUE;
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