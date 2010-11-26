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
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "kickload.com" }, urls = { "http://[\\w\\.]*?kickload\\.com/(file/\\d+/.+|get/[A-Za-z0-9]+/.+)" }, flags = { 0 })
public class KickLoadCom extends PluginForHost {
    private static final String MAINPAGE            = "http://kickload.com/";
    private static final String PREMIUMONLYTEXT     = "To download this file you need a premium account";
    private static final String PREMIUMONLYUSERTEXT = "Only available for premium users";

    public KickLoadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://kickload.com/tos/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie(MAINPAGE, "lang", "en");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(<h2>This file couldn.t be found\\!</h2>|exit o has been deleted\\.)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>Download (.*?) \\(.*?\\) - kickload\\.com</title>").getMatch(0);
        if (filename == null) filename = br.getRegex("<h2>(.*?)</h2>").getMatch(0);
        String filesize = br.getRegex("<title>Download .*? \\(([0-9\\.]+ .*?)\\) - kickload\\.com</title>").getMatch(0);
        if (filesize == null) filesize = br.getRegex("class=\"download_details\">File size: (.*?) -  Downloads").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        if (!filesize.equals("unknow")) link.setDownloadSize(Regex.getSize(filesize));
        String md5 = br.getRegex("id=\"md5\">\\((.*?)\\)").getMatch(0);
        if (md5 != null && !md5.equals("")) link.setMD5Hash(md5.trim());
        if (br.containsHTML(PREMIUMONLYTEXT)) link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.kickloadcom.nofreedownloadlink", PREMIUMONLYUSERTEXT));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML(PREMIUMONLYTEXT)) throw new PluginException(LinkStatus.ERROR_FATAL, PREMIUMONLYUSERTEXT);
        br.postPage(downloadLink.getDownloadURL(), "free_download=1&free_download1.x=&free_download1.y=&free_download1=1");
        if (br.containsHTML("(ou are already downloading a file\\.|Please, wait until the file has been loaded\\.</h2>)")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads", 10 * 60 * 1000l);
        String postPage = br.getRegex("\"(http://srv\\d+\\.kickload\\.com/download\\.php\\?ticket=.*?)\"").getMatch(0);
        String ticket = br.getRegex("\\?ticket=(.*?)\"").getMatch(0);
        if (ticket == null) ticket = br.getRegex("name=\"ticket\" id=\"ticket\" value=\"(.*?)\"").getMatch(0);
        if (ticket == null || postPage == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, postPage, "ticket=" + ticket + "&x=&y=", false, 1);
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
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public boolean checkLinks(DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            Browser br = new Browser();
            br.setCookiesExclusive(true);
            StringBuilder sb = new StringBuilder();
            ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* we test 5 links at once, get request */
                    if (index == urls.length || links.size() > 5) break;
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                sb.append("url=");
                int c = 0;
                for (DownloadLink dl : links) {
                    if (c > 0) sb.append(Encoding.urlEncode("|"));
                    sb.append(Encoding.urlEncode(dl.getDownloadURL()));
                    c++;
                }
                /* post seems buggy */
                br.getPage("http://api.kickload.com/linkcheck.php?" + sb.toString());
                String infos[][] = br.getRegex(Pattern.compile("((\\d+)?;?;?(.*?);;(.*?);;(\\d+))|((\\d+);;FILE)")).getMatches();
                for (int i = 0; i < links.size(); i++) {
                    DownloadLink dL = links.get(i);
                    if (infos[i][2] == null) {
                        /* id not in response, so its offline */
                        dL.setAvailable(false);
                    } else {
                        if ("OK".equals(infos[i][2])) {
                            dL.setFinalFileName(infos[i][3].trim());
                            dL.setDownloadSize(Regex.getSize(infos[i][4]));
                            dL.setAvailable(true);
                        } else {
                            dL.setAvailable(false);
                        }
                    }
                }
                if (index == urls.length) break;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}