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

import jd.PluginWrapper;
import jd.network.rtmp.url.RtmpUrlConnection;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.Regex;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "arte.tv", "liveweb.arte.tv", "videos.arte.tv" }, urls = { "http://(www\\.)?arte\\.tv/[a-z]{2}/.+", "http://liveweb\\.arte\\.tv/[a-z]{2}/.+", "http://videos\\.arte\\.tv/[a-z]{2}/.+" }, flags = { PluginWrapper.DEBUG_ONLY, PluginWrapper.DEBUG_ONLY, PluginWrapper.DEBUG_ONLY })
public class ArteTv extends PluginForHost {

    private String CLIPURL     = null;
    private String FILENAME    = null;
    private String FLASHPLAYER = null;
    private String LANG        = null;

    public ArteTv(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.arte.tv/de/content/tv/02__Universes/U6__Tout_20sur_20ARTE/05_20Groupe_20ARTE/10-CGU/CGU/3664116.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);

        if (CLIPURL.startsWith("rtmp")) {
            dl = new RTMPDownload(this, downloadLink, CLIPURL);
            final RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();

            rtmp.setSwfVfy(FLASHPLAYER);
            rtmp.setUrl(CLIPURL);
            rtmp.setResume(true);

            ((RTMPDownload) dl).startDownload();
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        final String link = downloadLink.getDownloadURL();
        LANG = new Regex(link, "http://\\w+.arte.tv/(\\w+)/.+").getMatch(0);
        LANG = LANG == null ? "de" : LANG;
        br.setFollowRedirects(true);
        br.getPage(link);
        if (!link.matches("liveweb\\.arte\\.tv")) {
            requestVideosArte(downloadLink);
        } else {
            requestLivewebArte(downloadLink);
        }
        if (FILENAME == null || CLIPURL == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String ext = CLIPURL.substring(CLIPURL.lastIndexOf("."), CLIPURL.length());
        if (ext.length() > 4) {
            ext = null;
        }
        ext = ext == null ? ".flv" : ext;
        if (FILENAME.endsWith(".")) {
            FILENAME = FILENAME.substring(0, FILENAME.length() - 1);
        }
        downloadLink.setFinalFileName(FILENAME.trim() + ext);
        return AvailableStatus.TRUE;
    }

    private AvailableStatus requestLivewebArte(final DownloadLink downloadLink) throws Exception {
        final String eventId = br.getRegex("eventId=(\\d+)").getMatch(0);
        FILENAME = br.getRegex("<title>(.*?)\\s-.*?</title>").getMatch(0);
        FLASHPLAYER = "http://liveweb.arte.tv/flash/player.swf";
        if (eventId == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        br.getPage("http://arte.vo.llnwd.net/o21/liveweb/events/event-" + eventId + ".xml?" + System.currentTimeMillis());
        if (FILENAME == null) {
            FILENAME = br.getRegex("<nameDe>(.*?)</nameDe>").getMatch(0);
        }
        CLIPURL = br.getRegex("<urlHd>(.*?)</urlHd>").getMatch(0);
        if (CLIPURL == null) {
            CLIPURL = br.getRegex("<urlSd>(.*?)</urlSd>").getMatch(0);
        }
        CLIPURL = CLIPURL.replaceAll("MP4:", "mp4:");
        return AvailableStatus.TRUE;
    }

    private AvailableStatus requestVideosArte(final DownloadLink downloadLink) throws Exception {
        FILENAME = br.getRegex("<title>(.*?)\\s-.*?</title>").getMatch(0);
        String tmpUrl = br.getRegex("videorefFileUrl=(.*?)\"").getMatch(0);
        FLASHPLAYER = br.getRegex("<param name=\"movie\" value=\"(.*?)\\?").getMatch(0);
        if (FLASHPLAYER == null || tmpUrl == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        tmpUrl = Encoding.urlDecode(tmpUrl, true);
        br.getPage(tmpUrl);
        tmpUrl = null;
        tmpUrl = br.getRegex("<video lang=\"" + LANG + "\" ref=\"(.*?)\"").getMatch(0);
        if (tmpUrl == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        br.getPage(tmpUrl);
        if (FILENAME == null) {
            FILENAME = br.getRegex("<name>(.*?)</name>").getMatch(0);
        }
        CLIPURL = br.getRegex("<url quality=\"hd\">(.*?)</url>").getMatch(0);
        if (CLIPURL == null) {
            CLIPURL = br.getRegex("<url quality=\"sd\">.*?)</url>").getMatch(0);
        }
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
