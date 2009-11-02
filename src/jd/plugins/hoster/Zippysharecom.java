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
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "zippyshare.com" }, urls = { "http://www\\d{0,}\\.zippyshare\\.com/(v/\\d+/file\\.html|.*?key=\\d+)" }, flags = { 0 })
public class Zippysharecom extends PluginForHost {

    private Pattern linkIDPattern = Pattern.compile(".*?zippyshare\\.com/v/([0-9]+)/file.html");
    private Pattern fileExtPattern = Pattern.compile("pong = 'fckhttp.*?(\\.\\w+)';");

    public Zippysharecom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(2000l);
        br.setFollowRedirects(true);
    }

    @Override
    public String getAGBLink() {
        return "http://www.zippyshare.com/terms.html";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setCookie("http://www.zippyshare.com", "ziplocale", "en");
        br.getPage(downloadLink.getDownloadURL().replaceAll("locale=..", "locale=en"));
        if (br.containsHTML("<title>Zippyshare.com - File does not exist</title>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filesize = br.getRegex(Pattern.compile("<strong>Size: </strong>(.*?)</font><br", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String name = br.getRegex(Pattern.compile("<title>Zippyshare.com -(.*?)</title>", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (name == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (name.endsWith("...")) {
            String linkID = new Regex(downloadLink.getDownloadURL(), linkIDPattern).getMatch(0);
            String fileExt = br.getRegex(fileExtPattern).getMatch(0);
            if (linkID != null) name = (name.substring(0, name.length() - 3) + "_" + linkID);
            if (fileExt != null) name = name + fileExt;
        }
        downloadLink.setName(name.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        int index = -1;
        while (true) {
            index++;
            // String allreplaces =
            // br.getRegex("ziptime.*?unescape(.*?)(var|'|;)").getMatch(0);
            // String[][] replaces = new Regex(allreplaces,
            // "replace\\((.*?),.*?\"(.*?)\"").getMatches();
            String page = Encoding.urlDecode(br.toString(), true);
            // for (String[] replace : replaces) {
            // replace[0] = replace[0].substring(1);
            // if (replace[0].endsWith("/")) replace[0] =
            // replace[0].substring(0, replace[0].length() - 1);
            // if (replace[0].endsWith("/g")) replace[0] =
            // replace[0].substring(0, replace[0].length() - 2);
            // if (replace[0].endsWith("/i")) replace[0] =
            // replace[0].substring(0, replace[0].length() - 2);
            // page = page.replace(replace[0], replace[1]);
            // }
            page = page.replaceAll("xxx", "zzz").replaceAll("zzz", "www").replaceAll("seven", "6").replaceAll("unh", "v").replaceAll("unf", "d");
            String[] links = HTMLParser.getHttpLinks(page, null);
            if (index > links.length - 1) break;
            String curlink = links[index];
            if (!new Regex(curlink, ".*?www\\d{1,2}\\.zippyshare\\.com/[^\\?]*\\..{1,4}$").matches()) {
                continue;
            }
            sleep(10000l, downloadLink);
            Browser brc = br.cloneBrowser();
            brc.setDebug(true);
            brc.getCookies(getHost()).remove(brc.getCookies(getHost()).get("zippop"));
            dl = BrowserAdapter.openDownload(brc, downloadLink, curlink);
            if (dl.getConnection().isContentDisposition()) {
                dl.setFilenameFix(true);
                dl.startDownload();
                return;
            } else {
                dl.getConnection().disconnect();
                br = new Browser();
                this.setBrowserExclusive();
                br.getHeaders().put("User-Agent", RandomUserAgent.generate());
                br.setCookie("http://www.zippyshare.com", "ziplocale", "en");
                br.getPage(downloadLink.getDownloadURL().replaceAll("locale=..", "locale=en"));
            }
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
