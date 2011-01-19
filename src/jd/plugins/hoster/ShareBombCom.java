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

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sharebomb.com" }, urls = { "http://[\\w\\.]*?sharebomb\\.com/[0-9]+.*" }, flags = { 0 })
public class ShareBombCom extends PluginForHost {

    public ShareBombCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(2000l);
    }

    @Override
    public String getAGBLink() {
        return "http://www1.sharebomb.com/tos";
    }

    @Override
    public void init() {
        Browser.setRequestIntervalLimitGlobal(getHost(), 500);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        String url = downloadLink.getDownloadURL();
        String downloadName = null;
        String downloadSize = null;
        for (int i = 1; i < 3; i++) {
            try {
                br.getPage(url);
            } catch (Exception e) {
                continue;
            }
            downloadName = Encoding.htmlDecode(br.getRegex("<strong>(.*?)</strong>\\s*<ul id=\"dlinfo\">").getMatch(0));
            downloadSize = br.getRegex("Size:</strong> (.*?)<").getMatch(0);
            if (!(downloadName == null || downloadSize == null)) {
                if (downloadName.length() == 0) downloadName = br.getRegex("<title>sharebomb.com - (.*?)</title>").getMatch(0);
                downloadLink.setName(downloadName.trim());
                downloadLink.setDownloadSize(SizeFormatter.getSize(downloadSize.replaceAll(",", "\\.")));
                return AvailableStatus.TRUE;
            }
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        /* Link holen */
        String url = new Regex(br, Pattern.compile("<a href=\"/?(files/.*)\">", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (url == null) url = new Regex(br, Pattern.compile("dlLink=unescape\\('(.*?)'\\);", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (url == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        String linkurl;
        if (url.startsWith("http")) {
            linkurl = Encoding.htmlDecode(url);
        } else {
            linkurl = "http://www1.sharebomb.com/" + Encoding.htmlDecode(url);
        }
        String wait = new Regex(br, Pattern.compile("var waitTime = (\\d+);", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (wait != null) {
            this.sleep(Long.parseLong(wait.trim()) * 1000l, downloadLink);
        } else {
            this.sleep(10 * 1000l, downloadLink);
        }
        /* Datei herunterladen */
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, linkurl, false, 1);
        URLConnectionAdapter con = dl.getConnection();
        if (con.getResponseCode() != 200 && con.getResponseCode() != 206) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 1000l);
        }
        dl.startDownload();

    }

    @Override
    public int getMaxRetries() {
        return 1;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 2000;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 3;
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
