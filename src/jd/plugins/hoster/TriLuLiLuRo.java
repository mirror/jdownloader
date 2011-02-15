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
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "trilulilu.ro" }, urls = { "http://(www\\.)?trilulilu\\.ro/(?!video/)[A-Za-z0-9_]+/[a-z0-9]+" }, flags = { 0 })
public class TriLuLiLuRo extends PluginForHost {

    public TriLuLiLuRo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.trilulilu.ro/termeni-conditii";
    }

    private String              DLLINK       = null;
    private static final String VIDEOPLAYER  = "videoplayer2010HDmp4";
    private static final String LIMITREACHED = ">Ai atins limita de 5 ascultări de piese audio pe zi. Te rugăm să intri in cont ca să poţi";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(Fişierul căutat nu există|Fişierul nu este disponibil pentru vizionare în ţara dumneavoastră|Contul acestui utilizator a fost dezactivat)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML(LIMITREACHED)) return AvailableStatus.TRUE;
        String filename = br.getRegex("<div class=\"file_description floatLeft\">[\r\t\n ]+<h1>(.*?)</h1>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<meta name=\"title\" content=\"Trilulilu - (.*?) - Muzică Diverse\" />").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>Trilulilu - (.*?) - Muzică Diverse</title>").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("<div class=\"music_demo\">[\n\t\r ]+<h3>(.*?)\\.mp3 \\(demo 30 de secunde\\)</h3").getMatch(0);
                    if (filename == null) filename = br.getRegex("<div class=\"hentry\">[\t\n\r ]+<h1>(.*?)</h1>").getMatch(0);
                }
            }
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        filename = Encoding.htmlDecode(filename.trim());
        if (br.containsHTML(VIDEOPLAYER))
            downloadLink.setFinalFileName(filename + ".mp4");
        else
            downloadLink.setFinalFileName(filename + ".mp3");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (br.containsHTML(LIMITREACHED)) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        getDownloadUrl(downloadLink);
        int maxchunks = 1;
        // Videos have no chunk-limits!
        if (br.containsHTML(VIDEOPLAYER)) maxchunks = 0;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void getDownloadUrl(DownloadLink downloadLink) throws PluginException, IOException {
        Browser br2 = br.cloneBrowser();
        DLLINK = br.getRegex("<param name=\"flashvars\" value=\"song=(http.*?\\.mp3)").getMatch(0);
        if (DLLINK == null) {
            String server = br.getRegex("server=(\\d+)\\&").getMatch(0);
            Regex authorAndFileid = new Regex(downloadLink.getDownloadURL(), "trilulilu\\.ro/(.*?)/(.+)");
            String fileID = authorAndFileid.getMatch(1);
            String username = authorAndFileid.getMatch(0);
            if (fileID == null || username == null || server == null) {
                logger.warning("fileID or username or server is null!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (!br.containsHTML(VIDEOPLAYER)) {
                String key = br.getRegex("key=([a-z0-9]+)\"").getMatch(0);
                if (key == null) {
                    logger.warning("key is null!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                DLLINK = "http://fs" + server + ".trilulilu.ro/stream.php?type=audio&source=site&hash=" + fileID + "&username=" + username + "&key=" + key;
            } else {
                br2.getPage("http://fs" + server + ".trilulilu.ro/" + fileID + "/video-formats");
                String format = br2.getRegex("<format>(.*?)</format>").getMatch(0);
                if (format == null) {
                    logger.warning("format is null!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (br2.containsHTML("mp4-720p")) format = "mp4-720p";
                DLLINK = "http://fs" + server + ".trilulilu.ro/stream.php?type=video&source=site&hash=" + fileID + "&username=" + username + "&key=ministhebest&format=" + format + "&start=";
            }
        }
        if (DLLINK != null) DLLINK = Encoding.htmlDecode(DLLINK);
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