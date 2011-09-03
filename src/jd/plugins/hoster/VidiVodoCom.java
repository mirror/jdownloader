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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vidivodo.com" }, urls = { "http://(www\\.)?vidivodo\\.com/(videolar/top50_\\w+/\\d+|\\d+/?([\\w- ]+)?)" }, flags = { 0 })
public class VidiVodoCom extends PluginForHost {

    private String dllink = null;

    public VidiVodoCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private String createToken(final String arg0, final String arg1) {
        if (arg1 == null || arg1.equals("") || arg1.length() != 10) { return null; }
        final String cryptedString = Encoding.Base64Encode(arg1.substring(8, 10) + arg1.substring(0, 2) + arg0 + arg1.substring(4, 8) + arg1.substring(2, 4));
        String result = "";
        int i = 0;
        while (i < cryptedString.length()) {
            if (i % 2 == 0) {
                result = result + Integer.toHexString(cryptedString.codePointAt(i));
            }
            i++;
        }
        return result;
    }

    @Override
    public String getAGBLink() {
        return "http://en.vidivodo.com/pages.php?mypage_id=6";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
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
        if (br.containsHTML("(Video cannot be viewed\\. Removed or processing\\.|Video görüntülenemez\\. Silinmiş veya işlemdedir\\.)")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = br.getRegex("property=\"og:title\" content=\"(.*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("target=\"_blank\" title=\"(.*?)\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
            }
        }
        String u = br.getRegex("u=(.*?)\\&").getMatch(0);
        if (u == null || u.equals("")) {
            u = "BFBNQFxDWBI=";
        }
        br.getHeaders().put("Referer", br.getBaseURL() + "VidiPlayer_05.swf");
        br.getPage("http://www.vidivodo.com/playerConfig.php?var=" + (int) Math.random() * 1000);
        final String e = br.getRegex("<opt name=\"e\" value=\"(\\d+)\"").getMatch(0);
        final String token = createToken(u, e);
        if (token != null) {
            br.getPage("http://www.vidivodo.com/player_getxml.php?u=" + u + "&token=" + token);
            dllink = br.getRegex("File=\"(http://.*?)\"").getMatch(0);
        }
        if (filename == null || dllink == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        filename = filename.trim();
        downloadLink.setFinalFileName(filename + ".flv");
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(dllink);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e1) {
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
