//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "download.hr" }, urls = { "http://(www\\.)?download\\.hr/software\\-.*?\\.html" }, flags = { 0 })
public class DownloadHr extends PluginForHost {

    public DownloadHr(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.download.hr/about/impressum";
    }

    private static final String MAINPAGE = "http://www.download.hr";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.clearCookies(MAINPAGE);
        br.setCookie(MAINPAGE, "dhrgo", "da");
        br.getHeaders().put("Referer", "http://www.download.hr/download-k-lite-codec-pack-full.html");
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; de; rv:1.9.2.18) Gecko/20110614 Firefox/3.6.18");
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
        br.getHeaders().put("Accept-Encoding", "gzip,deflate");
        br.getHeaders().put("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        br.getPage(link.getDownloadURL());
        if ("http://www.download.hr/".equals(br.getRedirectLocation()) || br.containsHTML("<title>Download\\.hr \\- Free Software Downloads</title>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<h1 class=\"font18\">(.*?)</h2>").getMatch(0);
        String filesize = br.getRegex("<b>File size:</b>  </div>[\t\n\r ]+<div class=\"file_fileinfo_right\">(.*?)</div>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String fileID = br.getRegex("id=\"unit_long(\\d+)\"").getMatch(0);
        if (fileID == null) fileID = br.getRegex("id=\"unit_ul(\\d+)\"").getMatch(0);
        if (fileID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Not neede (yet)
        // br.getPage(downloadLink.getDownloadURL().replace("software-",
        // "download-"));
        br.getPage("http://www.download.hr/redirect.php?fileid=" + fileID);
        String finallink = br.getRedirectLocation();
        if (finallink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finallink, true, 0);
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
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}