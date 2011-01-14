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

import java.io.IOException;

import jd.PluginWrapper;
import jd.network.rtmp.url.RtmpUrlConnection;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rutube.ru" }, urls = { "http://[\\w\\.]*?rutube\\.ru/tracks/\\d+\\.html" }, flags = { 0 })
public class RuTubeRu extends PluginForHost {

    public RuTubeRu(final PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(5000l);
    }

    @Override
    public String getAGBLink() {
        return "http://rutube.ru/agreement.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        this.requestFileInformation(downloadLink);
        String linkurl = this.br.getRegex("player.swf\\?buffer_first=1\\.0&file=(.*?)&xurl").getMatch(0);
        if (linkurl == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        linkurl = Encoding.urlDecode(linkurl, true);
        final String video_id = linkurl.substring(linkurl.lastIndexOf("/") + 1, linkurl.lastIndexOf("."));
        linkurl = linkurl.replaceFirst(linkurl.substring(linkurl.lastIndexOf(".") + 1, linkurl.length()), "xml");
        linkurl = linkurl + "?referer=" + Encoding.urlEncode(downloadLink.getDownloadURL() + "?v=" + video_id);
        this.br.getPage(linkurl);
        linkurl = this.br.getRegex("\\[CDATA\\[(.*?)\\]").getMatch(0);
        if (linkurl == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        if (linkurl.startsWith("rtmp:")) {
            this.dl = new RTMPDownload(this, downloadLink, linkurl);
            final RtmpUrlConnection rtmp = ((RTMPDownload) this.dl).getRtmpConnection();

            // Parametersetup
            // StreamUrl:
            // rtmp://video-1-2.rutube.ru:1935/rutube_vod_1/_definst_/mp4:vol32/movies/de/15/de157c2b8ffadecac37da56602f1c04e.mp4
            // <----------Host----------------><---------App---------><---------------------PlayPath---------------------------->
            // <---------------------TcUrl--------------------------->

            final String playpath = linkurl.substring(linkurl.lastIndexOf("mp4:"));
            String host = new Regex(linkurl, "(.*?)(rutube.ru/|rutube.ru:1935/)").getMatch(-1);
            final String app = linkurl.replace(playpath, "").replace(host, "");
            if ((playpath == null) || (host == null) || (app == null)) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            if (host.endsWith("ru/")) {
                host = host.replace("ru/", "ru:1935/");
            }
            if (app.equals("vod/")) {
                rtmp.setLive(true);
            }
            rtmp.setLive(app.equals("vod/"));
            rtmp.setApp(app);
            rtmp.setTcUrl(host + app);
            rtmp.setSwfUrl("http://rutube.ru/player.swf");
            rtmp.setSwfVfy(true);
            rtmp.setPlayPath(playpath);
            ((RTMPDownload) this.dl).startDownload();
        } else {
            this.br.setFollowRedirects(true);
            this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, linkurl, true, 0);
            if (this.dl.getConnection().getContentType().contains("html")) {
                this.br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            this.dl.startDownload();
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        this.br.setFollowRedirects(false);
        this.br.getPage(downloadLink.getDownloadURL());
        if (this.br.getRedirectLocation() != null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        final String fsk18 = this.br.getRegex("<br><b>.*?18.*?href=\"(http://rutube.ru/.*?confirm=.*?)\"").getMatch(0);
        if (fsk18 != null) {
            this.br.getPage(fsk18);
        }
        this.br.setFollowRedirects(true);
        String filename = this.br.getRegex("<title>(.*?):: Видео на RuTube").getMatch(0);
        if (filename == null) {
            filename = this.br.getRegex("meta name=\"title\" content=\"(.*?):: Видео на RuTube").getMatch(0);
        }
        final String filesize = this.br.getRegex("<span class=\"icn-size\"[^>]*>(.*?)</span>").getMatch(0);
        if ((filename == null) || (filesize == null)) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        downloadLink.setName(filename.trim() + ".flv");
        downloadLink.setDownloadSize(Regex.getSize(filesize.replace(",", ".")));
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
