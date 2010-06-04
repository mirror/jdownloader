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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wrzuc.to" }, urls = { "http://[\\w\\.]*?wrzuc\\.to/([a-zA-Z0-9]+(\\.wt|\\.html)|(en/)?linki/[a-zA-Z0-9]+)" }, flags = { 0 })
public class WrzucTo extends PluginForHost {

    public WrzucTo(PluginWrapper wrapper) {
        super(wrapper);
        br.setFollowRedirects(true);
    }

    @Override
    public String getAGBLink() {
        return "http://www.wrzuc.to/strona/regulamin";
    }

    public void correctDownloadLink(DownloadLink link) {
        String linkid = new Regex(link.getDownloadURL(), "/linki/([a-zA-Z0-9]+)").getMatch(0);
        if (linkid != null) link.setUrlDownload("http://www.wrzuc.to/en/" + linkid + ".wt");
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://www.wrzuc.to", "language", "en");
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        String filesize = br.getRegex(Pattern.compile("class=\"info\">.*?<tr>.*?<td>(.*?)</td>", Pattern.DOTALL)).getMatch(0);
        String name = br.getRegex(Pattern.compile("id=\"file_info\">.*?<strong>(.*?)</strong>", Pattern.DOTALL)).getMatch(0);
        if (name == null) {
            name = br.getRegex(Pattern.compile("<title>(.*?)</title>", Pattern.DOTALL)).getMatch(0);
        }
        if (name == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        filesize = filesize.replace("MiB", "mb");
        String md5 = br.getRegex("md5: \"(.*?)\"").getMatch(0);
        if (md5 != null) downloadLink.setMD5Hash(md5.trim());
        downloadLink.setName(name.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String md5 = downloadLink.getMD5Hash();
        String fid = br.getRegex(("file: \"(.*?)\"")).getMatch(0);
        if (md5 == null || fid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage("http://www.wrzuc.to/ajax/server/prepair", "md5=" + Encoding.htmlDecode(md5));
        br.getHeaders().put("Referer", downloadLink.getDownloadURL());
        br.postPage("http://www.wrzuc.to/ajax/server/download_link", "file=" + Encoding.htmlDecode(fid));
        String tempid = br.getRegex(("download_link\":\"(.*?)\"")).getMatch(0);
        String server = br.getRegex(("server.*?\":\"(.*?)\"")).getMatch(0);
        if (tempid == null || server == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String dllink = "http://" + server + ".wrzuc.to/pobierz/" + tempid;
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("No htmlCode read")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
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