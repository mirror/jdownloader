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
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uptal.com" }, urls = { "http://(www\\.)?(new\\.)?uptal\\.(com|org)/\\?d=[A-Fa-f0-9]+" }, flags = { 0 })
public class UptalCom extends PluginForHost {

    public UptalCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(100l);
    }

    @Override
    public String getAGBLink() {
        return "http://www.uptal.com/faq.php";
    }

    private static final String CAPTCHATEXT = "captcha\\.php";

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("uptal.com", "uptal.org"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (!br.getURL().contains("uptal.org")) br.getPage(downloadLink.getDownloadURL());
        if (br.getURL().contains("DL_FileNotFound") || br.containsHTML(">Your requested file is not found<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("File name:</b></td>\\s+<td[^>]+>(.*?)</td>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("title=\"Click this to report (.*?)\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<h2 class=\"float-left\">(.*?)</h2>").getMatch(0);
            }
        }
        String filesize = br.getRegex("File size:</b></td>\\s+<td[^>]+>(.*?)</td>").getMatch(0);
        if (filesize == null) filesize = br.getRegex("<strong>File size</strong></li>[\t\n\r ]+<li class=\"[a-z0-9_-]+\">(.*?)</li>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        downloadLink.setName(Encoding.htmlDecode(filename.trim()));
        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.postPage(downloadLink.getDownloadURL(), "downloadtype=free&d=&Free=Go+on+downloading%21");
        String filename = downloadLink.getName();
        br.setDebug(true);
        br.setFollowRedirects(true);
        if (br.containsHTML(CAPTCHATEXT)) {
            String post = "captchacode=" + getCaptchaCode("http://www.uptal.org/captcha.php", downloadLink) + "&x=0&y=0";
            br.postPage(br.getURL(), post);
            if (br.containsHTML(CAPTCHATEXT)) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        String getlink = findLink();
        if (getlink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        getlink = getlink.replaceAll(" ", "%20");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, getlink, true, 1);
        downloadLink.setFinalFileName(filename);
        URLConnectionAdapter con = dl.getConnection();
        if (!con.isOK()) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        }
        if (con.getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String findLink() throws Exception {
        String finalLink = null;
        String[] sitelinks = HTMLParser.getHttpLinks(br.toString(), null);
        if (sitelinks == null || sitelinks.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        for (String alink : sitelinks) {
            alink = Encoding.htmlDecode(alink);
            if (alink.contains("access_key=") || alink.contains("getfile.php?")) {
                finalLink = alink;
                break;
            }
        }
        return finalLink;
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
