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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "soundclick.com" }, urls = { "http://(www\\.)?soundclick\\.com/bands/page_songInfo\\.cfm\\?bandID=\\d+\\&songID=\\d+" }, flags = { 0 })
public class SoundClickCom extends PluginForHost {

    public SoundClickCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String DLLINK = null;

    @Override
    public String getAGBLink() {
        return "http://www.soundclick.com/docs/legal.cfm";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        /* Offline links should also have nice filenames */
        downloadLink.setName(new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0) + ".mp3");
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || br.getURL().contains("&content=music")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String songName = br.getRegex("\"name\":\"([^<>\"]*?)\"").getMatch(0);
        final String artist = br.getRegex("<a href=\"/bands/default\\.cfm\\?bandID=\\d+\">([^<>\"]*?)</a>").getMatch(0);
        if (songName == null || artist == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("http://www.soundclick.com/util/passkey.cfm?flash=true");
        final String controlID = br.getRegex("<controlID>([^<>\"]*?)</controlID>").getMatch(0);
        if (controlID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("http://www.soundclick.com/util/xmlsong.cfm?songid=" + getid(downloadLink) + "&passkey=" + controlID + "&q=hi&ext=0");
        DLLINK = br.getRegex("<cdnFilename>(http[^<>\"]*?)</cdnFilename>").getMatch(0);
        if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        DLLINK = Encoding.htmlDecode(DLLINK);
        String ext = DLLINK.substring(DLLINK.lastIndexOf("."));
        if (ext == null || ext.length() > 5) ext = ".mp3";
        downloadLink.setFinalFileName(Encoding.htmlDecode(artist.trim()) + " - " + Encoding.htmlDecode(songName.trim()).replace(".mp3", "") + ext);
        final Browser br2 = br.cloneBrowser();
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

    private String getid(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "(\\d+)$").getMatch(0);
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
