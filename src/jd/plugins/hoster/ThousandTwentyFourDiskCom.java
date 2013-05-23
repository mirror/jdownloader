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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "1024disk.com", "32666.com" }, urls = { "http://(www\\.)?1024disk\\.com/(file|down)\\-\\d+\\.html", "http://(www\\.)?32666\\.com/(file|down)\\-\\d+\\.html" }, flags = { 0, 0 })
public class ThousandTwentyFourDiskCom extends PluginForHost {

    public ThousandTwentyFourDiskCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.1024disk.com/ann_list.php?aid=8";
    }

    private static final String THOUSANDTWENTYFOURDISKCOM = "http://(www\\.)?1024disk\\.com/file\\-\\d+\\.html";
    private static final String THREETWOSIXSIXSIXCOM      = "http://(www\\.)?32666\\.com/file\\-\\d+\\.html";

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("/-down", "/-file"));
    }

    private String HOST = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        HOST = new Regex(link.getDownloadURL(), "http://(www\\.)?([^<>\"]*?)/").getMatch(1);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        String filename = null, filesize = null;
        if (link.getDownloadURL().matches(THOUSANDTWENTYFOURDISKCOM)) {
            if (br.containsHTML("/>文件已经被删除<br")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            filename = br.getRegex("align=\\'absbottom\\' border=\\'0\\' />([^<>\"]*?)</h3>").getMatch(0);
            filesize = br.getRegex("<td>文件大小: ([^<>\"]*?)</td>").getMatch(0);
        } else {
            if (br.containsHTML(">此文件已经被删除。<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            filename = br.getRegex("<h4>([^<>\"]*?)</h4>").getMatch(0);
            filesize = br.getRegex(">大小：</span></td>[\t\n\r ]+<td align=\"center\" class=\"ftr\">([^<>\"]*?)</td>").getMatch(0);
        }
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize + "b"));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = checkDirectLink(downloadLink, "directlink");
        if (dllink == null) {
            br.getPage(downloadLink.getDownloadURL().replace("/file-", "/down-"));
            /** Captcha is still skippable */
            // if (!br.containsHTML("imagecode\\.php")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // for (int i = 1; i <= 3; i++) {
            // final String code = getCaptchaCode("http://www." + HOST + "/imagecode.php?t=" + System.currentTimeMillis(), downloadLink);
            // br.postPage("http://www." + HOST + "/ajax.php", "action=check_code&code=" + Encoding.urlEncode(code));
            // if (br.toString().equals("false")) continue;
            // break;
            // }
            // if (br.toString().equals("false")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            dllink = br.getRegex("\"(http://[a-z0-9]+\\." + HOST + ":\\d+/dl\\.php\\?[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
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
            } catch (Exception e) {
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
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}