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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "shareplace.com" }, urls = { "http://[\\w\\.]*?shareplace\\.com/\\?[\\w]+(/.*?)?" }, flags = { 0 })
public class Shareplacecom extends PluginForHost {

    private String url;

    public Shareplacecom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://shareplace.com/rules.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        url = downloadLink.getDownloadURL();
        setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setCustomCharset("UTF-8");
        br.setFollowRedirects(true);
        br.getPage(url);
        if (br.getRedirectLocation() == null) {

            String iframe = url = br.getRegex("<frame name=\"main\" src=\"(.*?)\">").getMatch(0);
            br.getPage(iframe);
            if (br.containsHTML("Your requested file is not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            String filename = br.getRegex("Filename:</font></b>(.*?)<b><br>").getMatch(0).trim();
            String filesize = br.getRegex("Filesize.*?b>(.*?)<b>").getMatch(0);
            if (filesize == null) filesize = br.getRegex("File.*?size.*?:.*?</b>(.*?)<b><br>").getMatch(0);
            if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            downloadLink.setFinalFileName(filename.trim());
            if (filesize != null) {
                downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.trim()));
            }
            return AvailableStatus.TRUE;
        } else {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String filename = downloadLink.getFinalFileName();
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        filename = Encoding.htmlDecode(filename.trim());
        filename = Encoding.deepHtmlDecode(filename);
        String page = Encoding.urlDecode(br.toString(), true);
        String[] links = HTMLParser.getHttpLinks(page, null);
        boolean found = false;
        // // waittime deactivated till shareplace blocks it ;)
        // String time = br.getRegex("var zzipitime =.*?(\\d+);").getMatch(0);
        // int tt = 15;
        // if (time != null && Integer.parseInt(time) < 30) tt =
        // Integer.parseInt(time);
        // sleep(tt * 1001l, downloadLink);
        for (String link : links) {
            String fakelink = Encoding.deepHtmlDecode(link);
            if (!fakelink.contains(filename)) continue;
            if (br.containsHTML("replace")) {
                fakelink = fakelink.replace("vvvvvvvvv", "");
                fakelink = fakelink.replace("teletubbies", "");
                fakelink = fakelink.substring(13);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }

            Browser brc = br.cloneBrowser();
            dl = BrowserAdapter.openDownload(brc, downloadLink, fakelink);
            if (dl.getConnection().isContentDisposition()) {
                String fakename = Plugin.getFileNameFromHeader(dl.getConnection());
                if (fakename.contains("README.TXT")) {
                    dl.getConnection().disconnect();
                    continue;
                }
                found = true;
                break;
            } else {
                dl.getConnection().disconnect();
            }
        }
        if (!found) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        /* Workaround für fehlerhaften Filename Header */
        String name = Plugin.getFileNameFromHeader(dl.getConnection());
        if (name != null) downloadLink.setFinalFileName(Encoding.deepHtmlDecode(name));
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
