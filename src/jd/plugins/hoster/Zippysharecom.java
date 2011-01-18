//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import jd.http.RandomUserAgent;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "zippyshare.com" }, urls = { "http://www\\d{0,}\\.zippyshare\\.com/(v/\\d+/file\\.html|.*?key=\\d+)" }, flags = { 0 })
public class Zippysharecom extends PluginForHost {

    public Zippysharecom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.zippyshare.com/terms.html";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        URLConnectionAdapter con = null;
        try {
            prepareBrowser(downloadLink);
            if (br.containsHTML("<title>Zippyshare.com - File does not exist</title>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (!br.containsHTML(">Share movie:")) {
                String filesize = br.getRegex(Pattern.compile("Size:</font>            <font style=.*?>(.*?)</font>", Pattern.CASE_INSENSITIVE)).getMatch(0);
                if (filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.replaceAll(",", "\\.")));
            }
            String u = getURL();
            con = br.openGetConnection(u);
            downloadLink.setName(Encoding.urlDecode(getFileNameFromHeader(con), false));
            return AvailableStatus.TRUE;
        } catch (Exception e) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } finally {
            if (con != null) con.disconnect();
        }
    }

    private String getURL() {
        return Encoding.urlDecode(br.getRegex("var (pong|foken) = \\'(http[^']+)\\'").getMatch(1), true).replace("konaworld", "zippyshare").replace("cxc", "www").replace("google", "zippyshare");

    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(true);
        this.setBrowserExclusive();
        prepareBrowser(downloadLink);
        if (br.containsHTML("<title>Zippyshare.com - File does not exist</title>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, getURL(), false, 1);
        dl.setFilenameFix(true);
        if (!(dl.getConnection().isContentDisposition())) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void prepareBrowser(DownloadLink downloadLink) throws IOException {
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setCookie("http://www.zippyshare.com", "ziplocale", "en");
        br.getPage(downloadLink.getDownloadURL().replaceAll("locale=..", "locale=en"));
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
