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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "zdf.de" }, urls = { "http://(www\\.)?zdf\\.de/ZDFmediathek/(hauptnavigation/startseite(/[a-z0-9\\-_]+#?)?/)?beitrag/video/\\d+/[^<>\"/\\?#]+" }, flags = { 0 })
public class ZdfDeMediathek extends PluginForHost {

    public ZdfDeMediathek(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://zdf.de";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        // Nur iPads bekommen (vermutlich aufgrund der veralteten Technik :D)
        // die Videos als HTTP Streams
        br.getHeaders().put("User-Agent", "iPad");
        br.getPage("http://www.zdf.de/ZDFmediathek/" + new Regex(link.getDownloadURL(), "(beitrag/video/\\d+/.+)").getMatch(0) + "?flash=off&ipad=true");
        final String filename = br.getRegex("<h1 class=\"beitragHeadline\">([^<>\"]*?)</h1>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()).replace("\"", "'").replace(":", " - ") + ".mp4");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = br.getRegex("<li>DSL 2000 <a href=\"(http://[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://rodl\\.zdf\\.de/de/zdf/\\d+/\\d+/[^<>\"/]*?_vh\\.mp4)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
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