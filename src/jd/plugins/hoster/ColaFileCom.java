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
import jd.config.Property;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "colafile.com" }, urls = { "http://(www\\.)?colafile\\.com/file/\\d+" }, flags = { 0 })
public class ColaFileCom extends PluginForHost {

    public ColaFileCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.colafile.com/";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">当前文件不存在，请尝试其它链接。</div>|>当前文件所有者由于违反可乐云使用条例，帐号暂时被锁定，请联系可乐云客户服务中心。<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("class=\"file_name\"><a href=\"https?://(www\\.)?colafile\\.com/file/\\d+\">([^<>\"]*?)</a>").getMatch(1);
        if (filename == null) {
            filename = br.getRegex("class=\"download_filename\">([^<>\"]*?)</p>").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("class=\"b\\-fl ellipsis\" title=\"([^<>\"]*?)\"").getMatch(0);
        }
        // for mp3s
        if (filename == null) {
            filename = br.getRegex("<a href=\"file/\\d+\">([^<>\"]*?)</a>").getMatch(0);
        }
        String filesize = br.getRegex("class=\"file_detail\">[\t\n\r ]+<span>大小：([^<>\"]*?)</span>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("class=\"download_fileinfo\">([^<>\"]*?)\\&emsp;").getMatch(0);
        }
        if (filesize == null) {
            filesize = br.getRegex("<span>文件大小：</span><span>([^<>\"]*?)</span>").getMatch(0);
        }
        // for mp3s
        if (filesize == null) {
            filesize = br.getRegex("大小：([^<>\"]*?)</em>").getMatch(0);
        }
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(Encoding.htmlDecode(filesize.trim()) + "b"));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = checkDirectLink(downloadLink, "directlink");
        if (dllink == null) {
            br.getPage(br.getURL().replace("/file/", "/down/"));
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            final String fid = new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0);
            final String uid = br.getRegex("uid: \"(\\d+)\"").getMatch(0);
            if (uid == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.postPage("http://www.colafile.com/ajax.php", "action=file_views&file_id=" + fid + "&uid=" + uid + "&ref=");
            br.getPage("http://www.colafile.com/ajax.php?action=check_down&file_id=" + fid + "&_=" + System.currentTimeMillis());
            if (br.containsHTML("down_disabled\\(\\);")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
            }
            br.getPage("http://www.colafile.com/ajax.php?action=downaddress&file_id=" + fid);
            dllink = br.getRegex("downloadFile\\(\"(http[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            try {
                br.postPage("http://www.colafile.com/ajax.php", "action=down_process&file_id=" + fid + "&antiads=0&t=0." + System.currentTimeMillis());
            } catch (final Throwable e) {
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink", dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}