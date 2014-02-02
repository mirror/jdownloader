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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "userporn.com" }, urls = { "http://(www\\.)?userporn\\.com/(video/|watch_video\\.php\\?v=|e/)\\w+" }, flags = { 0 })
public class UserPornCom extends PluginForHost {

    private static int[] internalKeys = { 526729, 269502, 264523, 130622, 575869 };

    public UserPornCom(final PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(2000l + (long) 1000 * (int) Math.round(Math.random() * 5 + Math.random() * 5));
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replace("userporn.com/e/", "userporn.com/watch_video.php?v="));
    }

    @Override
    public String getAGBLink() {
        return "http://www.userporn.com/terms.php";
    }

    /* See jd.plugin.hoster.VideoBbCom */
    private String getFinalLink(final DownloadLink downloadLink, final String token) throws Exception {
        final String setting = Encoding.Base64Decode(br.getRegex("<param value=\"setting=(.*?)\"").getMatch(0));
        if (setting == null || !setting.startsWith("http://")) { return null; }
        br.getPage(setting);
        try {
            /* you have to make sure the plugin is loaded! */
            JDUtilities.getPluginForHost("videobb.com");
            return new jd.plugins.hoster.VideoBbCom.getFinallinkValue(internalKeys, token, br).DLLINK;
        } catch (final Throwable e) {
            return null;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Before access to userporn\\.com")) {
            final String next = br.getRegex("<a href=\"(.*?)\">Enter</a>").getMatch(0);
            if (next != null) {
                br.getPage("http://userporn.com" + next);
            }
        }
        if (br.containsHTML("<title>Userporn \\- Your Best Private Porn Site</title>")) {
            br.getPage(downloadLink.getDownloadURL());
        }
        if (br.containsHTML("(>Video is not available<|>The page or video you are looking for cannot|<title>Userporn \\- Your Best Private Porn Site</title>)")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = br.getRegex("<title>(.*?) \\- Userporn \\- Your Best Private Porn Site</title>").getMatch(0);
        if (filename == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        filename = filename.trim();
        if (Boolean.FALSE.equals(downloadLink.getBooleanProperty("nameok"))) {
            downloadLink.setName(Encoding.htmlDecode(filename) + ".flv");
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        final String dllink = getFinalLink(downloadLink, "token1");
        if (dllink == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        if (dllink.equals("ALGO_CONTROL_ERROR")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, VideoBbCom.ALGOCONTROLERROR, 15 * 1000l); }
        sleep(3 * 1000l, downloadLink); // Flasplayer to slow
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML("No htmlCode read")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError: ", 30 * 1000l); }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String tempName = Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection()));
        downloadLink.setFinalFileName(downloadLink.getName().replace(".flv", tempName.substring(tempName.lastIndexOf("."))));
        downloadLink.setProperty("nameok", true);
        dl.startDownload();
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
