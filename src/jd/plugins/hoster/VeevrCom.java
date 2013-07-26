//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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

@HostPlugin(revision = "$Revision: 21813 $", interfaceVersion = 2, names = { "veevr.com" }, urls = { "http://(www\\.)?veevr\\.com/(embed|videos)/[a-zA-Z0-9\\_\\-]+" }, flags = { 0 })
public class VeevrCom extends PluginForHost {

    private String dllink = null;

    public VeevrCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("/videos/", "/embed/"));
    }

    @Override
    public String getAGBLink() {
        return "http://veevr.com/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML(">This video has been removed for violating the terms of use\\.<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        dllink = br.getRegex("url:\\s*'(http[^']+/videos/download/[^']+)").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getHeaders().put("Accept-Charset", null);
        br.getHeaders().put("Cache-Control", null);
        br.getHeaders().put("Pragma", null);
        br.getHeaders().put("Referer", "http://hwcdn.veevr.com/q4z7c2x6/cds/swf/flowplayer.commercial-3.2.12.swf");
        String page = Encoding.Base64Decode(br.getPage(dllink.replace("%3F", "?").replace("%26", "&")));
        dllink = new Regex(page, "url=\"(.*?)\"").getMatch(0);
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(dllink);
            // only way to check for made up links... or offline is here
            if (con.getResponseCode() == 404) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (!con.getContentType().contains("html")) {
                downloadLink.setName(getFileNameFromHeader(con));
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                br2.followConnection();
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
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