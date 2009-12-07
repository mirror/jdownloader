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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rutube.ru" }, urls = { "http://[\\w\\.]*?rutube\\.ru/tracks/\\d+\\.html" }, flags = { 0 })
public class RuTubeRu extends PluginForHost {

    public RuTubeRu(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(5000l);
    }

    @Override
    public String getAGBLink() {
        return "http://rutube.ru/agreement.html";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String fsk18 = br.getRegex("<br><b>.*?18.*?href=\"(http://rutube.ru/.*?confirm=.*?)\"").getMatch(0);
        if (fsk18 != null) br.getPage(fsk18);
        br.setFollowRedirects(true);
        String filename = br.getRegex(Pattern.compile("<h3[^>]*>(.*?)</h3>", Pattern.CASE_INSENSITIVE)).getMatch(0);
        String filesize = br.getRegex("<span class=\"size\"[^>]*>(.*?)</span>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim() + ".flv");
        downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String linkurl = br.getRegex("player.swf\\?buffer_first=1\\.0&file=(.*?)&xurl").getMatch(0);
        if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        linkurl = Encoding.urlDecode(linkurl, true);
        String video_id = linkurl.substring(linkurl.lastIndexOf("/") + 1, linkurl.lastIndexOf("."));
        linkurl = linkurl.replaceFirst(linkurl.substring(linkurl.lastIndexOf(".") + 1, linkurl.length()), "xml");
        linkurl = linkurl + "?referer=" + Encoding.urlEncode(downloadLink.getDownloadURL() + "?v=" + video_id);
        br.getPage(linkurl);
        linkurl = br.getRegex("\\[CDATA\\[(.*?)\\]").getMatch(0);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, linkurl, true, 0);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
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
