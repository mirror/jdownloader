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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "qiannao.com" }, urls = { "http://(www\\.)?qiannao\\.com/(file/[a-z0-9]+/[a-z0-9]+|space/file/[^<>\"]*\\.page)" }, flags = { 0 })
public class QiannaoCom extends PluginForHost {

    public QiannaoCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.qiannao.com/user/terms.html";
    }

    private static final String SPACELINK = "http://(www\\.)?qiannao\\.com/space/file/[^<>\"]*\\.page";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("http://qiannao.com", "language", "en_us");
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:23.0) Gecko/20100101 Firefox/23.0");
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
        br.getPage(link.getDownloadURL());

        // No_access|user_banned|file_deleted|strange_exception
        if (br.containsHTML("<span> 资源已被禁止访问</span>|用户已被禁用。</span>|<span> 资源已被删除</span>|<span>java\\.io\\.FileNotFoundException")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        // Cookies needed to start the download
        final String vid = br.getRegex("setCookie\\(\"vid\", \"([a-z0-9]+)\"").getMatch(0);
        if (vid != null) br.setCookie("http://qiannao.com/", "vid", vid);
        final String vid1 = br.getRegex("setCookie\\(\"vid1\", \"([a-z0-9]+)\"").getMatch(0);
        if (vid1 != null) br.setCookie("http://qiannao.com/", "vid1", vid1);

        String filename = null, filesize = null;
        if (link.getDownloadURL().matches(SPACELINK)) {
            if (br.containsHTML(">文件大小：</div><span class=\"span2\">0 B\\&nbsp;<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            filename = new Regex(link.getDownloadURL(), "/([^<>\"/]*)/\\.page$").getMatch(0);
            filesize = br.getRegex(">文件大小：</div><span class=\"span2\">([^<>\"]*?)\\&nbsp;</span>").getMatch(0);
        } else {
            if (br.containsHTML("<title> \\- 千脑云电脑 我的在线电脑 \\| [^<>\"]*?</title>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            filename = br.getRegex("<h2 class=\"h2_title\">([^<>\"]*?)</h2>").getMatch(0);
            filesize = br.getRegex("<b>文件大小:</b>\\&nbsp;\\&nbsp;\\&nbsp;\\&nbsp;([^<>\"]*?)<br>").getMatch(0);
        }
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = null;
        if (downloadLink.getDownloadURL().matches(SPACELINK)) {
            final String[] dllinks = br.getRegex("\"(http://[a-z0-9\\-]+\\.qiannao\\.com:\\d+/servlet/FileDownload[^<>\"]*?)\"").getColumn(0);
            if (dllinks == null || dllinks.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            for (final String testDllink : dllinks) {
                if (!linkOk(downloadLink, testDllink)) continue;
                dllink = testDllink;
                break;
            }
        } else {
            final String[] dllinks = br.getRegex("\"(http://[a-z0-9\\-]+\\.qiannao\\.com/downfile/[^<>\"]*?)\"").getColumn(0);
            if (dllinks == null || dllinks.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            for (final String testDllink : dllinks) {
                if (!linkOk(downloadLink, testDllink)) continue;
                dllink = testDllink;
                break;
            }
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private boolean linkOk(final DownloadLink downloadLink, final String dllink) throws IOException {
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(dllink);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                return false;
            }
            return true;
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
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}