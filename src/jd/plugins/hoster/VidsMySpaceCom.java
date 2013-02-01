//    jDownloader - Downloadmanager
//    Copyright (C) 2010  JD-Team support@jdownloader.org
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
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vids.myspace.com" }, urls = { "http://(www\\.)?myspace\\.com/video/.*?\\d+$" }, flags = { 0 })
public class VidsMySpaceCom extends PluginForHost {

    private String DLLINK = null;

    public VidsMySpaceCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.myspace.com/index.cfm?fuseaction=misc.terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getURL().contains("myspace.com/error")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        final String qs = br.getRegex("\"qs\":\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null || qs == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("http://mediaservices.myspace.com/services/rss.ashx?cb=" + System.currentTimeMillis() + "&type=video&el=http%3A%2F%2Fwww%2Emyspace%2Ecom%2Fmodules%2Fvideos%2Fpages%2Fvideodetail%2Easpx%3F" + Encoding.urlEncode(qs).replace("-", "%2D") + "&videoID=" + new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0));
        DLLINK = br.getRegex("<media:content url=\"(http://[^<>\"]*?)\"").getMatch(0);
        filename = Encoding.htmlDecode(filename.trim());
        URLConnectionAdapter con = br.openGetConnection(DLLINK);
        if (!con.getContentType().contains("html")) {
            link.setDownloadSize(con.getLongContentLength());
            link.setFinalFileName(filename + ".flv");
        } else {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}