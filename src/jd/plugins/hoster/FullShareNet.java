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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fullshare.net" }, urls = { "http://[\\w\\.]*?fullshare\\.net/show/[a-z0-9]+/.+" }, flags = { 0 })
public class FullShareNet extends PluginForHost {

    public FullShareNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://fullshare.net/agb/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Datei konnte nicht gefunden werden")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("filename\" value=\"(.*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("\"http://fullshare\\.net/deliver/.*?/(.*?)\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("show/.*?/(.*?)\"").getMatch(0);
            }
        }
        Regex filesize = br.getRegex("<td>GR\\&Ouml;SSE <b>(.*?)</b>(.*?)</td>");
        if (filesize.getMatch(0) != null && filesize.getMatch(1) != null) {
            String fsize = filesize.getMatch(0) + filesize.getMatch(1);
            downloadLink.setDownloadSize(Regex.getSize(fsize.trim()));
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename);
        return AvailableStatus.TRUE;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String ttt = br.getRegex("Das Video wurde angefordert. Bitte warten Sie.*?(\\d+).*?Sekunden").getMatch(0);
        if (ttt != null) {
            String code = br.getRegex("name=\"code\" value=\"(.*?)\"").getMatch(0);
            if (code == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            int tt = Integer.parseInt(ttt);
            sleep(tt * 1001l, downloadLink);
            br.postPage(downloadLink.getDownloadURL(), "code=" + Encoding.urlEncode_light(code));
        }
        String dllink = br.getRegex("\"src\" value=\"(.*?)\"").getMatch(0);
        if (dllink == null) {
            logger.warning("dllink is null!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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
