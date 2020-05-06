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
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "soundclick.com" }, urls = { "https?://(?:www\\.)?soundclick\\.com/(?:bands/page_songInfo|html5/v4/player|music/songInfo)\\.cfm\\?(?:bandID=\\d+\\&)?songID=(\\d+)" })
public class SoundClickCom extends PluginForHost {
    public SoundClickCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "http://www.soundclick.com/docs/legal.cfm";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        /* Offline links should also have nice filenames */
        link.setName(getFID(link) + ".mp3");
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404 || br.getURL().contains("&content=music")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // br.getPage("https://www.soundclick.com/util/passkey.cfm?flash=true");
        // final String controlID = br.getRegex("<controlID>([^<>\"]*?)</controlID>").getMatch(0);
        // br.getPage("https://www.soundclick.com/util/xmlsong.cfm?songid=" + getid(link) + "&passkey=" + controlID + "&q=hi&ext=0");
        String songName = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        String artist = br.getRegex("<artist>([^<>\"]*?)</artist>").getMatch(0);
        String filename = null;
        if (songName != null && artist != null) {
            filename = Encoding.htmlDecode(artist.trim()) + " - " + Encoding.htmlDecode(songName.trim()).replace(".mp3", "") + ".mp3";
        }
        dllink = br.getRegex("<cdnFilename>(http[^<>\"]*?)</cdnFilename>").getMatch(0);
        // if (songName == null || artist == null || dllink == null) {
        // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // }
        /* 2020-05-06 */
        br.getPage("https://www.soundclick.com/utils_download/download_song.cfm?ID=" + getFID(link));
        dllink = String.format("https://www.soundclick.com/utils_download/download_songDeliver.cfm?songID=%s&ppID=0&selectLevel=160", this.getFID(link));
        if (filename != null) {
            link.setFinalFileName(filename);
        }
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(dllink);
            if (!con.getContentType().contains("html") && con.isContentDisposition()) {
                final String filename_server = Plugin.getFileNameFromDispositionHeader(con);
                link.setDownloadSize(con.getLongContentLength());
                if (filename == null) {
                    link.setFinalFileName(filename_server);
                }
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
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

    private String getid(final DownloadLink dl) {
        return new Regex(dl.getPluginPatternMatcher(), "(\\d+)$").getMatch(0);
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
